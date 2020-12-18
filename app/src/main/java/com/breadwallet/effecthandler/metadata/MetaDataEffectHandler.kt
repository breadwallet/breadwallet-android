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
import com.breadwallet.breadbox.getSize
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.mobius.bindConsumerIn
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.util.errorHandler
import com.breadwallet.platform.entities.TxMetaDataValue
import com.breadwallet.platform.interfaces.AccountMetaDataProvider
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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
        SupervisorJob() + Dispatchers.Default + errorHandler()

    private val commentUpdateChannel = Channel<CommentUpdateElem>()
    private val loadMetaDataChannel = Channel<List<MetaDataElem>>()

    init {
        commentUpdateChannel
            .consumeAsFlow()
            .debounce(COMMENT_UPDATE_DEBOUNCE)
            .onEach {
                metaDataProvider.putTxMetaData(
                    breadBox.walletTransfer(it.currencyCode, it.txHash).first(),
                    TxMetaDataValue(comment = it.comment)
                )
            }
            .launchIn(this)

        loadMetaDataChannel
            .consumeAsFlow()
            .flatMapLatest {
                it.asFlow()
                    .flatMapMerge {
                        val transaction =
                            breadBox.walletTransfer(it.currencyCode, it.txHash).first()
                        metaDataProvider
                            .txMetaData(transaction)
                            .map { txMetaData ->
                                Pair(transaction.hashString(), txMetaData)
                            }
                    }
            }
            .map {
                MetaDataEvent.OnTransactionMetaDataUpdated(it.first, it.second)
            }
            .bindConsumerIn(output, this)
    }

    override fun accept(effect: MetaDataEffect) {
        when (effect) {
            is MetaDataEffect.LoadTransactionMetaData ->
                loadTransactionMetaData(effect.currencyCode, effect.transactionHashes)
            is MetaDataEffect.LoadTransactionMetaDataSingle ->
                loadTransactionMetaDataSingle(effect.currencyCode, effect.transactionHashes)
            is MetaDataEffect.AddTransactionMetaData ->
                addTransactionMetaData(
                    effect.transaction,
                    effect.comment,
                    effect.fiatCurrencyCode,
                    effect.fiatPricePerUnit
                )
            is MetaDataEffect.UpdateTransactionComment ->
                updateTransactionComment(
                    effect.currencyCode,
                    effect.transactionHash,
                    effect.comment
                )
            is MetaDataEffect.UpdateWalletMode -> updateWalletMode(effect.currencyId, effect.mode)
            is MetaDataEffect.LoadWalletModes -> loadWalletModes()
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun loadTransactionMetaData(currencyCode: String, transactionHashes: List<String>) {
        loadMetaDataChannel.offer(
            transactionHashes.map {
                MetaDataElem(currencyCode, it)
            }
        )
    }

    private fun loadTransactionMetaDataSingle(
        currencyCode: String,
        transactionHashes: List<String>
    ) {
        launch {
            transactionHashes
                .asFlow()
                .flatMapMerge { hash ->
                    val transaction = breadBox.walletTransfer(currencyCode, hash).first()
                    metaDataProvider.txMetaData(transaction)
                        .take(1)
                        .map { hash to it }
                }
                .toList()
                .toMap()
                .run {
                    output.accept(MetaDataEvent.OnTransactionMetaDataSingleUpdated(this))
                }
        }
    }

    private fun addTransactionMetaData(
        transaction: Transfer,
        comment: String,
        fiatCurrencyCode: String,
        fiatPricePerUnit: BigDecimal
    ) {
        val deviceId = BRSharedPrefs.getDeviceId()
        val size = transaction.getSize()?.toInt() ?: 0
        val fee = transaction.fee.toBigDecimal().toDouble()
        val creationTime =
            (System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS).toInt()

        val metaData = TxMetaDataValue(
            deviceId,
            comment,
            fiatCurrencyCode,
            fiatPricePerUnit.toDouble(),
            transaction.wallet.walletManager.network.height.toLong(),
            fee,
            size,
            creationTime
        )

        BreadApp.applicationScope.launch {
            metaDataProvider.putTxMetaData(transaction, metaData)
        }
    }

    private fun updateTransactionComment(
        currencyCode: String,
        transactionHash: String,
        comment: String
    ) {
        commentUpdateChannel.offer(
            CommentUpdateElem(currencyCode, transactionHash, comment)
        )
    }

    private fun updateWalletMode(currencyId: String, mode: WalletManagerMode) {
        launch { metaDataProvider.putWalletMode(currencyId, mode) }
    }

    private fun loadWalletModes() =
        metaDataProvider
            .walletModes()
            .map { modeMap ->
                MetaDataEvent.OnWalletModesUpdated(modeMap)
            }
            .bindConsumerIn(output, this)

    data class CommentUpdateElem(val currencyCode: String, val txHash: String, val comment: String)
    data class MetaDataElem(val currencyCode: String, val txHash: String)
}
