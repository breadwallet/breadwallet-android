/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 10/24/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.effecthandler.metadata

import android.text.format.DateUtils
import com.breadwallet.app.BreadApp
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.getDefaultWalletManagerMode
import com.breadwallet.breadbox.getSize
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.ext.bindConsumerIn
import com.breadwallet.logger.logError
import com.breadwallet.tools.manager.BRSharedPrefs
import com.platform.entities.TxMetaData
import com.platform.entities.TxMetaDataValue
import com.platform.interfaces.AccountMetaDataProvider
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal

class MetaDataEffectHandler(
    private val output: Consumer<MetaDataEvent>,
    private val metaDataProvider: AccountMetaDataProvider,
    private val breadBox: BreadBox
) : Connection<MetaDataEffect>, CoroutineScope {

    companion object {
        private const val COMMENT_UPDATE_DEBOUNCE = 500L
    }

    override val coroutineContext =
        SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, throwable ->
                logError("Error in coroutine", throwable)
            }

    private val commentUpdateChannel = Channel<CommentUpdatePair>()
    private val loadMetaDataChannel = Channel<List<String>>()

    init {
        commentUpdateChannel
            .consumeAsFlow()
            .debounce(COMMENT_UPDATE_DEBOUNCE)
            .onEach {
                metaDataProvider.putTxMetaData(
                    TxMetaDataValue(comment = it.comment),
                    it.transactionHash
                )
            }
            .launchIn(this)

        loadMetaDataChannel
            .consumeAsFlow()
            .flatMapLatest { txHashes: List<String> ->
                txHashes.asFlow()
                    .flatMapMerge { transactionHash ->
                        metaDataProvider
                            .txMetaData(transactionHash)
                            .map { txMetaData ->
                                MetaDataPair(transactionHash, txMetaData)
                            }
                    }
            }
            .map {
                MetaDataEvent.OnTransactionMetaDataUpdated(it.transactionHash, it.txMetaData)
            }
            .bindConsumerIn(output, this)
    }

    override fun accept(effect: MetaDataEffect) {
        when (effect) {
            MetaDataEffect.RecoverMetaData -> recoverMetaData()
            is MetaDataEffect.LoadTransactionMetaData ->
                loadTransactionMetaData(effect.transactionHashes)
            is MetaDataEffect.AddTransactionMetaData ->
                addTransactionMetaData(
                    effect.transaction,
                    effect.comment,
                    effect.fiatCurrencyCode,
                    effect.fiatPricePerUnit
                )
            is MetaDataEffect.UpdateTransactionComment ->
                updateTransactionComment(effect.transactionHash, effect.comment)
            is MetaDataEffect.UpdateWalletMode -> updateWalletMode(effect.currencyId, effect.mode)
            is MetaDataEffect.LoadWalletModes -> loadWalletModes()
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun recoverMetaData() =
        metaDataProvider
            .recoverAll()
            .flowOn(Dispatchers.IO)
            .launchIn(BreadApp.applicationScope)

    private fun loadTransactionMetaData(transactionHashes: List<String>) {
        loadMetaDataChannel.offer(transactionHashes)
    }

    private fun addTransactionMetaData(
        transaction: Transfer,
        comment: String,
        fiatCurrencyCode: String,
        fiatPricePerUnit: BigDecimal
    ) {
        val deviceId = BRSharedPrefs.getDeviceId()
        val blockHeight = BRSharedPrefs.getLastBlockHeight(
            transaction.wallet.currency.code
        ) // TODO: this pref not being updated anymore
        val size = transaction.getSize()?.toInt() ?: 0
        val fee = transaction.fee.toBigDecimal().toPlainString()
        val creationTime =
            (System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS).toInt()

        val metaData = TxMetaDataValue(
            deviceId,
            comment,
            fiatCurrencyCode,
            fiatPricePerUnit.toDouble(),
            blockHeight,
            fee,
            size,
            creationTime
        )

        BreadApp.applicationScope.launch {
            metaDataProvider.putTxMetaData(metaData, transaction.hash.get().toString())
        }
    }

    private fun updateTransactionComment(transactionHash: String, comment: String) {
        commentUpdateChannel.offer(
            CommentUpdatePair(
                transactionHash,
                comment
            )
        )
    }

    private fun updateWalletMode(currencyId: String, mode: WalletManagerMode) {
        launch { metaDataProvider.putWalletMode(currencyId, mode) }
    }

    private fun loadWalletModes() =
        metaDataProvider
            .walletModes()
            .map { modeMap ->
                modeMap
                    .toMutableMap()
                    .mapValues { entry ->
                        when (entry.value) {
                            null -> {
                                breadBox.system().getDefaultWalletManagerMode(
                                    entry.key
                                ).first()
                            }
                            else -> entry.value
                        }
                    }
            }
            .map { modeMap ->
                MetaDataEvent.OnWalletModesUpdated(modeMap)
            }
            .bindConsumerIn(output, this)
}

data class CommentUpdatePair(
    val transactionHash: String,
    val comment: String
)

data class MetaDataPair(
    val transactionHash: String,
    val txMetaData: TxMetaData
)