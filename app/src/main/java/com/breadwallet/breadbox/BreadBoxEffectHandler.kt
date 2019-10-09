/**
 * BreadWallet
 *
 * Created by Ahsan Butt on <ahsan.butt@breadwallet.com> 7/30/19.
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
package com.breadwallet.breadbox

import android.content.Context
import com.breadwallet.app.BreadApp
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferDirection
import com.breadwallet.crypto.TransferState
import com.breadwallet.ext.bindConsumerIn
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.ui.wallet.WalletTransaction
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
class BreadBoxEffectHandler(
    private val output: Consumer<BreadBoxEvent>,
    private val context: Context,
    private val currencyCode: String,
    private val breadBox: BreadBox
) : Connection<BreadBoxEffect>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        val walletFlow = breadBox.wallet(currencyCode)

        // Balance
        walletFlow
            .map { it.balance }
            .distinctUntilChanged()
            .map { BreadBoxEvent.OnBalanceUpdated(it.toBigDecimal(), getBalanceInFiat(it)) }
            .bindConsumerIn(output, this)

        // Currency name
        walletFlow
            .map { it.currency.name }
            .distinctUntilChanged()
            .map { BreadBoxEvent.OnCurrencyNameUpdated(it) }
            .bindConsumerIn(output, this)

        // Wallet transfers
        breadBox.walletTransfers(currencyCode)
            .mapLatest { transfers ->
                BreadBoxEvent.OnTransactionsUpdated(
                    transfers
                        .map { it.asWalletTransaction() }
                        .sortedByDescending { it.timeStamp }
                )
            }
            .bindConsumerIn(output, this)

        // Wallet sync state
        breadBox.walletSyncState(currencyCode)
            .mapLatest {
                BreadBoxEvent.OnSyncProgressUpdated(
                    it.percentComplete,
                    it.timestamp,
                    it.isSyncing
                )
            }
            .bindConsumerIn(output, this)
    }

    override fun accept(value: BreadBoxEffect) {
    }

    override fun dispose() {
        coroutineContext.cancelChildren()
    }
}

private fun getBalanceInFiat(balanceAmt: Amount): BigDecimal {
    val context = BreadApp.getBreadContext()
    return RatesRepository.getInstance(context).getFiatForCrypto(
        balanceAmt.toBigDecimal(),
        balanceAmt.currency.code,
        BRSharedPrefs.getPreferredFiatIso(context)
    ) ?: BigDecimal.ZERO
}

fun Transfer.asWalletTransaction(): WalletTransaction {
    // TODO: val context = BreadApp.getBreadContext()
    val txHash = hashString()
    // TODO: KVStoreManager.getTxMetaData(context, txHash.toByteArray())

    val confirmationsUntilFinal = wallet.walletManager.network.confirmationsUntilFinal
    val confirmation = confirmation.orNull()
    val state = state

    return WalletTransaction(
        txHash = txHash,
        amount = amount.toBigDecimal(),
        amountInFiat = getBalanceInFiat(amount),
        fiatWhenSent = 0f, // TODO: Rates info
        toAddress = target.orNull()?.toSanitizedString() ?: "<unknown>",
        fromAddress = source.orNull()?.toSanitizedString() ?: "<unknown>",
        isReceived = direction == TransferDirection.RECEIVED,
        isErrored = state.type == TransferState.Type.FAILED,
        memo = "", // TODO: metaData?.comment.orEmpty(),
        isValid = state.type != TransferState.Type.FAILED, // TODO: Is this correct?
        fee = fee.doubleAmount(unitForFee.base).or(0.0).toBigDecimal(),
        blockHeight = confirmation?.blockNumber?.toInt() ?: 0, // TODO: Use long to match core
        confirmations = confirmations.orNull()?.toInt() ?: 0,
        confirmationsUntilFinal = confirmationsUntilFinal.toInt(),
        timeStamp = confirmation?.confirmationTime?.time ?: 0,
        currencyCode = wallet.currency.code,
        // TODO: Either establish this via meta-data (like iOS)
        //  or compare toAddress with token addresses as in pre-Generic Core
        feeForToken = null
    )
}
