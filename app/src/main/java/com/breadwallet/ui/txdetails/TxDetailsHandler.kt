/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/17/19.
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
package com.breadwallet.ui.txdetails

import android.content.Context
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.isEthereum
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.ext.throttleLatest
import com.breadwallet.repository.RatesRepository
import com.breadwallet.ui.txdetails.TxDetails.E
import com.breadwallet.ui.txdetails.TxDetails.F
import com.breadwallet.platform.entities.TxMetaDataValue
import com.breadwallet.platform.interfaces.AccountMetaDataProvider
import drewcarlson.mobius.flow.subtypeEffectHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import java.math.BigDecimal

private const val MEMO_THROTTLE_MS = 500L
private const val RATE_UPDATE_MS = 60_000L

fun createTxDetailsHandler(
    context: Context,
    breadBox: BreadBox,
    metaDataProvider: AccountMetaDataProvider
) = subtypeEffectHandler<F, E> {
    addTransformer<F.UpdateMemo> { effects ->
        effects
            .throttleLatest(MEMO_THROTTLE_MS)
            .transformLatest { effect ->
                metaDataProvider.putTxMetaData(
                    breadBox.walletTransfer(effect.currencyCode, effect.transactionHash).first(),
                    TxMetaDataValue(comment = effect.memo)
                )
            }
    }

    addTransformer<F.LoadTransactionMetaData> { effects ->
        effects.flatMapLatest { effect ->
            val transaction =
                breadBox.walletTransfer(effect.currencyCode, effect.transactionHash).first()
            metaDataProvider
                .txMetaData(transaction)
                .map { E.OnMetaDataUpdated(it) }
        }
    }

    addTransformer<F.LoadFiatAmountNow> { effects ->
        val rates = RatesRepository.getInstance(context)
        effects.transformLatest { effect ->
            while (true) {
                rates.getFiatForCrypto(
                    effect.cryptoTransferredAmount,
                    effect.currencyCode,
                    effect.preferredFiatIso
                )?.let { amount ->
                    emit(E.OnFiatAmountNowUpdated(amount))
                }

                delay(RATE_UPDATE_MS)
            }
        }
    }

    addTransformer<F.LoadTransaction> { effects ->
        effects.flatMapLatest { effect ->
            breadBox.walletTransfer(effect.currencyCode, effect.transactionHash)
                .mapLatest { transfer ->
                    var gasPrice = BigDecimal.ZERO
                    var gasLimit = BigDecimal.ZERO
                    if (transfer.amount.currency.isEthereum()) {
                        val feeBasis = transfer.run {
                            confirmedFeeBasis.orNull() ?: estimatedFeeBasis.get()
                        }

                        gasPrice = feeBasis.pricePerCostFactor
                            .convert(breadBox.gweiUnit())
                            .get()
                            .toBigDecimal()

                        gasLimit = feeBasis.costFactor.toBigDecimal()
                    }
                    E.OnTransactionUpdated(transfer, gasPrice, gasLimit)
                }
        }
    }
}

private fun BreadBox.gweiUnit() =
    getSystemUnsafe()!!
        .networks
        .first { it.currency.isEthereum() }
        .run { unitsFor(currency).get() }
        .first { it.symbol.contains("gwei", true) }
