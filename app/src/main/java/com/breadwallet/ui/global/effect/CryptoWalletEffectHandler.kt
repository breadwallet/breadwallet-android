/**
 * BreadWallet
 * <p/>
 * Created by Ahsan Butt on <ahsan.butt@breadwallet.com> 7/30/19.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.global.effect

import com.breadwallet.crypto.*

import com.breadwallet.BreadApp

import android.content.Context
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.ui.global.event.WalletEvent
import com.breadwallet.ui.util.bindConsumerIn
import com.breadwallet.ui.wallet.WalletTransaction
import com.platform.tools.KVStoreManager
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.math.BigDecimal

@UseExperimental(ExperimentalCoroutinesApi::class)
class CryptoWalletEffectHandler(
    private val output: Consumer<WalletEvent>,
    private val context: Context,
    private val currencyCode: String
) : Connection<WalletEffect>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        val breadBox = BreadApp.getBreadBox()
        val walletFlow = breadBox.wallet(currencyCode)

        // Balance
        walletFlow
            .map { it.balance }
            .distinctUntilChanged()
            .map { WalletEvent.OnBalanceUpdated(getBalance(it), getBalanceInFiat(it)) }
            .bindConsumerIn(output, this)

        // Currency name
        walletFlow
            .map { it.currency.name }
            .distinctUntilChanged()
            .map { WalletEvent.OnCurrencyNameUpdated(it) }
            .bindConsumerIn(output, this)

        // Wallet transfers
        breadBox.walletTransfers(currencyCode)
            .mapLatest { transfers ->
                WalletEvent.OnTransactionsUpdated(
                    transfers
                        .map { it.asWalletTransaction() }
                        .sortedByDescending { it.timeStamp }
                )
            }
            .bindConsumerIn(output, this)

        // Wallet sync state
        breadBox.walletSyncState(currencyCode)
            .mapLatest {
                WalletEvent.OnSyncProgressUpdated(
                    it.percentComplete,
                    it.timestamp,
                    it.isSyncing
                )
            }
            .bindConsumerIn(output, this)
    }

    override fun accept(value: WalletEffect) {
    }

    override fun dispose() {
        coroutineContext.cancelChildren()
    }

    private fun getBalance(balanceAmt: Amount): BigDecimal {
        return balanceAmt.doubleAmount(balanceAmt.unit).or(0.0).toBigDecimal()
    }

    private fun getBalanceInFiat(balanceAmt: Amount): BigDecimal {
        val balance = getBalance(balanceAmt)
        return RatesRepository.getInstance(context).getFiatForCrypto(
            balance,
            balanceAmt.currency.code,
            BRSharedPrefs.getPreferredFiatIso(context)
        ) ?: BigDecimal.ZERO
    }

    private fun Transfer.getHashAsString(): String {
        return hash.transform { it.toString() }.or("")
    }

    private fun Transfer.asWalletTransaction(): WalletTransaction {

        val txHash = getHashAsString()
        val metaData = KVStoreManager.getTxMetaData(context, txHash.toByteArray())

        val confirmationsUntilFinal = wallet.walletManager.network.confirmationsUntilFinal

        return WalletTransaction(
            txHash = txHash,
            amount = getBalance(amount),
            amountInFiat = getBalanceInFiat(amount),
            fiatWhenSent = 0f, // TODO: Rates info
            toAddress = target.transform { it.toString() }.or("<unknown>"),
            fromAddress = source.transform { it.toString() }.or("<unknown>"),
            isReceived = direction == TransferDirection.RECEIVED,
            isErrored = state.type == TransferState.Type.FAILED,
            memo = metaData?.comment.orEmpty(),
            isValid = true, // TODO: do we have this info?
            fee = fee.doubleAmount(unitForFee.base).or(0.0).toBigDecimal(),
            blockHeight = confirmation.transform { it?.blockNumber?.toInt() }.or(0),
            confirmations = confirmations.transform { it?.toInt() }.or(0),
            confirmationsUntilFinal = confirmationsUntilFinal.toInt(),
            timeStamp = confirmation.transform { it?.confirmationTime?.time }.or(0L),
            currencyCode = currencyCode,
            feeForToken = null // TODO: Either establish this via meta-data (like iOS) or compare toAddress with token addresses as in pre-Generic Core
        )
    }
}