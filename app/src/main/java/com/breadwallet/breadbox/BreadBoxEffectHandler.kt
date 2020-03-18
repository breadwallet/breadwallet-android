/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 7/30/19.
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

import com.breadwallet.app.BreadApp
import com.breadwallet.crypto.Amount
import com.breadwallet.ext.bindConsumerIn
import com.breadwallet.logger.logError
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineExceptionHandler
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
    private val currencyCode: String,
    private val breadBox: BreadBox
) : Connection<BreadBoxEffect>, CoroutineScope {

    override val coroutineContext =
        SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, throwable ->
                logError("Error in coroutine", throwable)
            }

    init {
        // Currency name
        breadBox.wallet(currencyCode)
            .map { it.currency.name }
            .distinctUntilChanged()
            .map { BreadBoxEvent.OnCurrencyNameUpdated(it) }
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

    override fun accept(effect: BreadBoxEffect) {
        when (effect) {
            is BreadBoxEffect.LoadWalletBalance -> {
                breadBox.wallet(currencyCode)
                    .map { it.balance }
                    .distinctUntilChanged()
                    .map { BreadBoxEvent.OnBalanceUpdated(it.toBigDecimal(), getBalanceInFiat(it)) }
                    .bindConsumerIn(output, this)
            }
            is BreadBoxEffect.LoadTransactions -> {
                breadBox.walletTransfers(currencyCode)
                    .mapLatest { transfers ->
                        BreadBoxEvent.OnTransactionsUpdated(
                            transfers.sortedByDescending {
                                it.confirmation.orNull()?.confirmationTime?.time
                                    ?: System.currentTimeMillis()
                            }
                        )
                    }
                    .bindConsumerIn(output, this)
            }
            is BreadBoxEffect.LoadTransaction -> {
                breadBox.walletTransfer(currencyCode, effect.transferHash)
                    .mapLatest { transfer ->
                        var gasPrice = BigDecimal.ZERO
                        var gasLimit = BigDecimal.ZERO
                        if (transfer.amount.currency.isEthereum()) {
                            val feeBasis = transfer.run {
                                confirmedFeeBasis.orNull() ?: estimatedFeeBasis.get()
                            }

                            gasPrice = feeBasis.pricePerCostFactor
                                .convert(gweiUnit())
                                .get()
                                .toBigDecimal()

                            gasLimit = feeBasis.costFactor.toBigDecimal()
                        }
                        BreadBoxEvent.OnTransactionUpdated(transfer, gasPrice, gasLimit)
                    }
                    .bindConsumerIn(output, this)
            }
        }
    }

    override fun dispose() {
        coroutineContext.cancelChildren()
    }

    private fun gweiUnit() =
        breadBox
            .getSystemUnsafe()!!
            .networks
            .first { it.currency.isEthereum() }
            .run { unitsFor(currency).get() }
            .first { it.symbol.contains("gwei", true) }
}

private fun getBalanceInFiat(balanceAmt: Amount): BigDecimal {
    val context = BreadApp.getBreadContext()
    return RatesRepository.getInstance(context).getFiatForCrypto(
        balanceAmt.toBigDecimal(),
        balanceAmt.currency.code,
        BRSharedPrefs.getPreferredFiatIso(context)
    ) ?: BigDecimal.ZERO
}
