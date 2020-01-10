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

import com.breadwallet.breadbox.feeForToken
import com.spotify.mobius.Next
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Update
import com.breadwallet.breadbox.isErc20
import com.breadwallet.breadbox.isEthereum
import com.breadwallet.breadbox.isReceived
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.breadbox.toSanitizedString
import com.platform.entities.TxMetaDataEmpty
import com.platform.entities.TxMetaDataValue
import java.util.Date

object TxDetailsUpdate : Update<TxDetailsModel, TxDetailsEvent, TxDetailsEffect>,
    TxDetailsUpdateSpec {

    override fun update(
        model: TxDetailsModel,
        event: TxDetailsEvent
    ): Next<TxDetailsModel, TxDetailsEffect> = patch(model, event)

    override fun onTransactionUpdated(
        model: TxDetailsModel,
        event: TxDetailsEvent.OnTransactionUpdated
    ): Next<TxDetailsModel, TxDetailsEffect> {

        with(event.transaction) {
            val updatedModel = model.copy(
                isEth = amount.currency.isEthereum(),
                isErc20 = amount.currency.isErc20(),
                cryptoTransferredAmount = amount.toBigDecimal(),
                fee = fee.toBigDecimal(),
                isReceived = isReceived(),
                blockNumber = confirmation.orNull()?.blockNumber?.toInt() ?: 0,
                toOrFromAddress = when {
                    isReceived() -> source
                    else -> target
                }.orNull()?.toSanitizedString() ?: "",
                confirmationDate = confirmation
                    .transform { it?.confirmationTime }
                    .or { Date() },
                confirmedInBlockNumber = confirmation
                    .transform { it?.blockNumber?.toString() }
                    .or(""),
                transactionState = TransactionState.valueOf(state),
                gasPrice = event.gasPrice,
                gasLimit = event.gasLimit,
                feeToken = feeForToken()
            )

            return next(
                updatedModel,
                setOf(
                    TxDetailsEffect.LoadFiatAmountNow(
                        updatedModel.cryptoTransferredAmount,
                        updatedModel.currencyCode,
                        updatedModel.preferredFiatIso
                    )
                )
            )
        }
    }

    override fun onFiatAmountNowUpdated(
        model: TxDetailsModel,
        event: TxDetailsEvent.OnFiatAmountNowUpdated
    ): Next<TxDetailsModel, TxDetailsEffect> =
        next(
            model.copy(
                fiatAmountNow = event.fiatAmountNow
            )
        )

    override fun onMetaDataUpdated(
        model: TxDetailsModel,
        event: TxDetailsEvent.OnMetaDataUpdated
    ): Next<TxDetailsModel, TxDetailsEffect> =
        when (event.metaData) {
            is TxMetaDataValue -> {
                next(
                    model.copy(
                        memo = event.metaData.comment ?: "",
                        exchangeCurrencyCode = event.metaData.exchangeCurrency ?: "",
                        exchangeRate = event.metaData.exchangeRate.toBigDecimal()
                    )
                )
            }
            is TxMetaDataEmpty -> next(model.copy(memo = ""))
        }

    override fun onMemoChanged(
        model: TxDetailsModel,
        event: TxDetailsEvent.OnMemoChanged
    ): Next<TxDetailsModel, TxDetailsEffect> =
        dispatch(
            setOf(
                TxDetailsEffect.UpdateMemo(
                    model.transactionHash,
                    event.memo
                )
            )
        )

    override fun onClosedClicked(model: TxDetailsModel): Next<TxDetailsModel, TxDetailsEffect> =
        dispatch(setOf(TxDetailsEffect.Close))

    override fun onShowHideDetailsClicked(
        model: TxDetailsModel
    ): Next<TxDetailsModel, TxDetailsEffect> = next(
        model.copy(
            showDetails = !model.showDetails
        )
    )
}
