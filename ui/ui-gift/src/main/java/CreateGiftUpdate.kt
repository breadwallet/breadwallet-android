/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 12/09/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.ui.uigift

import com.breadwallet.breadbox.toBigDecimal
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Update
import com.breadwallet.ui.uigift.CreateGift.M
import com.breadwallet.ui.uigift.CreateGift.E
import com.breadwallet.ui.uigift.CreateGift.F
import com.breadwallet.ui.uigift.CreateGift.State
import com.breadwallet.ui.uigift.CreateGift.FiatAmountOption
import com.breadwallet.ui.uigift.CreateGift.MAX_DIGITS
import com.breadwallet.ui.uigift.CreateGift.Error
import com.breadwallet.ui.uigift.CreateGift.FeeEstimateError
import com.breadwallet.ui.uigift.CreateGift.MaxEstimateError
import com.spotify.mobius.Next
import com.spotify.mobius.Next.noChange
import java.math.BigDecimal

object CreateGiftUpdate : Update<M, E, F> {
    override fun update(model: M, event: E): Next<M, F> = when (event) {
        is E.OnNameChanged -> onNameChanged(model, event)
        is E.OnAmountClicked -> onAmountClicked(model, event)
        is E.OnPaperWalletCreated -> onPaperWalletCreated(model, event)
        is E.OnMaxEstimated -> onMaxEstimated(model, event)
        is E.OnMaxEstimateFailed -> onMaxEstimateFailed(model, event)
        is E.OnFeeUpdated -> onFeeUpdated(model, event)
        is E.OnFeeFailed -> onFeeFailed(model, event)
        is E.OnTransactionSent -> onTransactionSent(model, event)
        E.OnTransferFailed -> onTransferFailed(model)
        E.OnCloseClicked -> onCloseClicked(model)
        E.OnCreateClicked -> onCreateClicked(model)
        E.OnTransactionConfirmClicked -> onTransactionConfirmClicked(model)
        E.OnTransactionCancelClicked -> noChange()
        is E.OnPaperWalletFailed -> onPaperWalletFailed(model, event)
        is E.OnGiftBackupDeleted -> onGiftBackupDeleted()
        E.OnGiftSaved -> onGiftSaved(model)
    }

    private fun onPaperWalletCreated(model: M, event: E.OnPaperWalletCreated): Next<M, F> = next(
        model.copy(
            address = event.address,
            paperKey = event.privateKey,
            giftUrl = event.giftUrl
        ),
        setOf(
            F.BackupGift(
                address = event.address,
                privateKey = event.privateKey
            ),
            F.EstimateMax(
                address = event.address,
                transferSpeed = model.transferSpeed,
                fiatCurrencyCode = model.fiatCurrencyCode
            )
        )
    )

    private fun onMaxEstimated(model: M, event: E.OnMaxEstimated): Next<M, F> {
        if (event.fiatAmount < model.fiatAmountOptions[0].amount) {
            return dispatch(setOf(F.ShowError(Error.INSUFFICIENT_BALANCE_ERROR)))
        }
        val newFiatAmountOptions = model.fiatAmountOptions.map {
            FiatAmountOption(it.amount, event.fiatAmount >= it.amount, it.fiatCurrencyCode)
        }
        return next(
            model.copy(
                fiatMaxAmount = event.fiatAmount,
                fiatPerCryptoUnit = event.fiatPerCryptoUnit,
                state = State.READY,
                fiatAmountOptions = newFiatAmountOptions
            )
        )
    }

    private fun onAmountClicked(model: M, event: E.OnAmountClicked): Next<M, F> =
        if (event.isChecked) {
            val fiatAmount = model.fiatAmountOptions[event.index].amount
            next(
                model.copy(
                    fiatAmount = fiatAmount,
                    amountSelectionIndex = event.index,
                    feeEstimate = null,
                ),
                setOf(
                    F.EstimateFee(
                        model.address!!,
                        fiatAmount.convertFiatToCrypto(model.fiatPerCryptoUnit),
                        model.transferSpeed
                    )
                )
            )
        } else {
            next(
                model.copy(
                    fiatAmount = BigDecimal.ZERO,
                    amountSelectionIndex = -1,
                    feeEstimate = null
                )
            )
        }

    private fun onNameChanged(model: M, event: E.OnNameChanged): Next<M, F> = next(
        model.copy(recipientName = event.name)
    )

    private fun onFeeUpdated(model: M, event: E.OnFeeUpdated): Next<M, F> = next(
        model.copy(feeEstimate = event.feeEstimate)
    )

    private fun onCloseClicked(model: M): Next<M, F> = when {
        model.attemptedSend || model.address == null -> dispatch(setOf(F.Close))
        else -> dispatch(setOf(F.DeleteGiftBackup(model.address)))
    }

    private fun onGiftBackupDeleted(): Next<M, F> = dispatch(setOf(F.Close))

    private fun onCreateClicked(model: M): Next<M, F> = when {
        model.state == State.READY &&
            !model.address.isNullOrBlank() &&
            model.feeEstimate != null &&
            model.fiatAmount != BigDecimal.ZERO &&
            model.recipientName.isNotBlank() ->
            dispatch(
                setOf(
                    F.ConfirmTransaction(
                        name = model.recipientName,
                        amount = model.fiatAmount.convertFiatToCrypto(model.fiatPerCryptoUnit),
                        currencyCode = model.currencyCode,
                        fiatAmount = model.fiatAmount,
                        fiatFee = model.feeEstimate.fee.toBigDecimal() * model.fiatPerCryptoUnit,
                        fiatCurrencyCode = model.fiatCurrencyCode
                    )
                )
            )
        model.fiatAmount == BigDecimal.ZERO -> dispatch(setOf(F.ShowError(Error.INPUT_AMOUNT_ERROR)))
        model.recipientName.isBlank() -> dispatch(setOf(F.ShowError(Error.INPUT_RECIPIENT_NAME_ERROR)))
        else -> noChange()
    }

    private fun onTransactionConfirmClicked(model: M): Next<M, F> = when {
        model.state == State.READY &&
            !model.address.isNullOrBlank() &&
            model.feeEstimate != null &&
            model.fiatAmount != BigDecimal.ZERO &&
            model.recipientName.isNotBlank() -> next(
            model.copy(
                state = State.SENDING,
                attemptedSend = true
            ),
            setOf(
                F.SendTransaction(
                    model.address,
                    model.fiatAmount.convertFiatToCrypto(model.fiatPerCryptoUnit),
                    model.feeEstimate
                )
            )
        )
        else -> noChange()
    }

    private fun onPaperWalletFailed(model: M, event: E.OnPaperWalletFailed): Next<M, F> = dispatch(
        setOf(F.ShowError(Error.PAPER_WALLET_ERROR))
    )

    private fun onMaxEstimateFailed(model: M, event: E.OnMaxEstimateFailed): Next<M, F> {
        val error = when (event.error) {
            MaxEstimateError.INSUFFICIENT_BALANCE -> Error.INSUFFICIENT_BALANCE_ERROR
            MaxEstimateError.SERVER_ERROR -> Error.SERVER_ERROR
            MaxEstimateError.INVALID_TARGET -> Error.PAPER_WALLET_ERROR
        }
        return dispatch(setOf(F.ShowError(error)))
    }

    private fun onFeeFailed(model: M, event: E.OnFeeFailed): Next<M, F> {
        val error = when (event.error) {
            FeeEstimateError.INSUFFICIENT_BALANCE -> Error.INSUFFICIENT_BALANCE_FOR_AMOUNT_ERROR
            FeeEstimateError.SERVER_ERROR -> Error.SERVER_ERROR
            FeeEstimateError.INVALID_TARGET -> Error.PAPER_WALLET_ERROR
        }
        return dispatch(setOf(F.ShowError(error)))
    }

    private fun onTransferFailed(model: M): Next<M, F> = next(
        model.copy(state = State.READY),
        setOf(F.ShowError(Error.TRANSACTION_ERROR))
    )

    private fun onTransactionSent(model: M, event: E.OnTransactionSent): Next<M, F> = next(
        model.copy(
            txHash = event.txHash
        ),
        setOf(
            F.SaveGift(
                checkNotNull(model.address),
                checkNotNull(model.paperKey),
                model.recipientName,
                event.txHash,
                model.fiatCurrencyCode,
                model.fiatAmount,
                model.fiatPerCryptoUnit
            )
        )
    )

    private fun onGiftSaved(model: M): Next<M, F> = dispatch(
        setOf(
            F.GoToShareGift(
                checkNotNull(model.giftUrl),
                checkNotNull(model.txHash),
                model.recipientName,
                model.fiatAmount.convertFiatToCrypto(model.fiatPerCryptoUnit),
                model.fiatAmount,
                model.fiatPerCryptoUnit
            )
        )
    )

    private fun BigDecimal.convertFiatToCrypto(fiatPerCryptoUnit: BigDecimal) =
        setScale(MAX_DIGITS) / fiatPerCryptoUnit
}
