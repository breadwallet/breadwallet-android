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

import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.util.CurrencyCode
import dev.zacsweers.redacted.annotations.Redacted
import java.math.BigDecimal

object CreateGift {

    enum class State {
        LOADING,
        READY,
        SENDING
    }

    enum class Error(val isCritical: Boolean) {
        PAPER_WALLET_ERROR(true),
        SERVER_ERROR(true),
        INSUFFICIENT_BALANCE_ERROR(true),
        INSUFFICIENT_BALANCE_FOR_AMOUNT_ERROR(false),
        INPUT_AMOUNT_ERROR(false),
        INPUT_RECIPIENT_NAME_ERROR(false),
        TRANSACTION_ERROR( false)
    }

    enum class PaperWalletError {
        UNKNOWN
    }

    enum class MaxEstimateError {
        INSUFFICIENT_BALANCE,
        SERVER_ERROR,
        INVALID_TARGET
    }

    enum class FeeEstimateError {
        INSUFFICIENT_BALANCE,
        SERVER_ERROR,
        INVALID_TARGET
    }

    data class FiatAmountOption(
        val amount: BigDecimal,
        val enabled: Boolean = false,
        val fiatCurrencyCode: String
    )

    const val MAX_DIGITS = 8

    fun getFiatAmountOptions(fiatCurrencyCode: String) = listOf(25, 50, 100, 250, 500)
        .map { FiatAmountOption(it.toBigDecimal(), fiatCurrencyCode = fiatCurrencyCode) }

    data class M(
        val currencyId: String = "",
        val currencyCode: CurrencyCode = "btc", // TODO: Get from currencyId

        val fiatCurrencyCode: String = BRConstants.USD,
        val fiatAmountOptions: List<FiatAmountOption> = getFiatAmountOptions(fiatCurrencyCode),
        val transferSpeed: TransferSpeed = TransferSpeed.Regular(currencyCode),

        val recipientName: String = "",
        val amountSelectionIndex: Int = -1,
        val fiatAmount: BigDecimal = BigDecimal.ZERO,
        val fiatMaxAmount: BigDecimal = BigDecimal.ZERO,
        val fiatPerCryptoUnit: BigDecimal = BigDecimal.ZERO,

        val feeEstimate: TransferFeeBasis? = null,

        @Redacted val paperKey: String? = null,
        @Redacted val address: String? = null,
        @Redacted val giftUrl: String? = null,
        val txHash: String? = null,

        val state: State = State.LOADING,
        val attemptedSend: Boolean = false
    ) {
        companion object {
            fun createDefault(currencyId: String) =
                M(currencyId = currencyId)
        }
    }

    sealed class E {
        data class OnNameChanged(val name: String) : E()
        data class OnAmountClicked(val index: Int, val isChecked: Boolean) : E()

        data class OnPaperWalletCreated(
            @Redacted val privateKey: String,
            @Redacted val address: String,
            @Redacted val giftUrl: String
        ) : E()
        data class OnPaperWalletFailed(val error: PaperWalletError) : E()

        data class OnMaxEstimated(val fiatAmount: BigDecimal, val fiatPerCryptoUnit: BigDecimal) : E()
        data class OnMaxEstimateFailed(val error: MaxEstimateError) : E()

        data class OnFeeUpdated(val feeEstimate: TransferFeeBasis) : E()
        data class OnFeeFailed(val error: FeeEstimateError) : E()

        data class OnTransactionSent(val txHash: String): E()
        object OnTransferFailed : E()

        object OnGiftBackupDeleted: E()

        object OnGiftSaved: E()

        object OnCloseClicked : E()
        object OnCreateClicked : E()
        object OnTransactionConfirmClicked : E()
        object OnTransactionCancelClicked : E()
    }

    sealed class F {
        object CreatePaperWallet : F()
        object Close: F(), ViewEffect

        data class BackupGift(
            @Redacted val address: String,
            @Redacted val privateKey: String
        ): F()
        data class DeleteGiftBackup(@Redacted val address: String): F()

        data class EstimateMax(
            @Redacted val address: String,
            val transferSpeed: TransferSpeed,
            val fiatCurrencyCode: String
        ) : F()

        data class EstimateFee(
            @Redacted val address: String,
            val amount: BigDecimal,
            val transferSpeed: TransferSpeed
        ) : F()

        data class ConfirmTransaction(
            val name: String,
            val amount: BigDecimal,
            val currencyCode: CurrencyCode,
            val fiatAmount: BigDecimal,
            val fiatFee: BigDecimal,
            val fiatCurrencyCode: String
        ) : F(), ViewEffect

        data class SendTransaction(
            @Redacted val address: String,
            val amount: BigDecimal,
            val transferFeeBasis: TransferFeeBasis
        ) : F()

        data class SaveGift(
            @Redacted val address: String,
            @Redacted val privateKey: String,
            val recipientName: String,
            val txHash: String,
            val fiatCurrencyCode: String,
            val fiatAmount: BigDecimal,
            val fiatPerCryptoUnit: BigDecimal
        ) : F()

        data class GoToShareGift(
            @Redacted val giftUrl: String,
            @Redacted val txHash: String,
            @Redacted val recipientName: String,
            val giftAmount: BigDecimal,
            val giftAmountFiat: BigDecimal,
            val pricePerUnit: BigDecimal
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.ShareGift(
                giftUrl,
                txHash,
                recipientName,
                giftAmount,
                giftAmountFiat,
                pricePerUnit,
                replaceTop = true
            )
        }

        data class ShowError(val error: Error) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                error.name,
                messageResId = when(error) {
                    Error.PAPER_WALLET_ERROR -> R.string.CreateGift_unexpectedError
                    Error.SERVER_ERROR -> R.string.CreateGift_serverError
                    Error.INSUFFICIENT_BALANCE_ERROR -> R.string.CreateGift_insufficientBalanceError
                    Error.INSUFFICIENT_BALANCE_FOR_AMOUNT_ERROR -> R.string.CreateGift_insufficientBalanceForAmountError
                    Error.INPUT_AMOUNT_ERROR -> R.string.CreateGift_inputAmountError
                    Error.INPUT_RECIPIENT_NAME_ERROR -> R.string.CreateGift_inputRecipientNameError
                    Error.TRANSACTION_ERROR -> R.string.CreateGift_unexpectedError
                },
                positiveButtonResId = R.string.Button_ok
            )
        }
    }
}
