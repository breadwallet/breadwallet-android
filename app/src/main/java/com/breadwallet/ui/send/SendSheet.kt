/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/25/19.
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
package com.breadwallet.ui.send

import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.crypto.PaymentProtocolRequest
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.ext.isZero
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.Link
import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.util.CurrencyCode
import com.breadwallet.util.isBitcoin
import drewcarlson.switchboard.MobiusUpdateSpec
import io.sweers.redacted.annotation.Redacted
import java.math.BigDecimal

object SendSheet {

    /**
     * [M] models the ability to send an [amount] of [currencyCode]
     * to the supplied [targetAddress].
     *
     * [amount] is a value of [fiatCode] when [isAmountCrypto] is true.
     *
     * @throws IllegalArgumentException When [currencyCode] or [fiatCode] are blank.
     */
    data class M(
        /** The [CurrencyCode] for the crypto be transferred. */
        val currencyCode: CurrencyCode,
        /** The fiat currency code to use as a reference for the value transferred. */
        val fiatCode: String,

        /** The wallet balance in [currencyCode]. */
        val balance: BigDecimal = BigDecimal.ZERO,
        /** The network fee to be used in [currencyCode]. */
        val networkFee: BigDecimal = BigDecimal.ZERO,

        /** The wallet balance in [fiatCode]. */
        val fiatBalance: BigDecimal = BigDecimal.ZERO,
        /** The network fee to be used in [fiatCode]. */
        val fiatNetworkFee: BigDecimal = BigDecimal.ZERO,

        /** Is the user entering a crypto (true) or fiat (false) amount. */
        val isAmountCrypto: Boolean = true,
        /** True if an amount key pad should be displayed. */
        val isAmountEditVisible: Boolean = false,
        /** True when [totalCost] is greater than [balance]. */
        val isTotalCostOverBalance: Boolean = false,

        /** The user supplied address to send the [amount] of [currencyCode] to. */
        @Redacted val targetAddress: String = "",
        /** The user supplied amount to send as a string. */
        val rawAmount: String = "",
        /** The user supplied amount as [BigDecimal]. */
        val amount: BigDecimal = BigDecimal.ZERO,
        /** A user provided memo to store with the transaction. */
        @Redacted val memo: String? = null,

        /** The user supplied amount in fiat. */
        val fiatAmount: BigDecimal = BigDecimal.ZERO,
        /** The current fiat exchange rate for [currencyCode] in [fiatCode]. */
        val fiatPricePerUnit: BigDecimal = BigDecimal.ZERO,
        /** The current fiat exchange rate for [feeCurrencyCode] in [fiatCode]. */
        val fiatPricePerFeeUnit: BigDecimal = BigDecimal.ZERO,

        /** The user selected [TransferSpeed] for this transaction. */
        val transferSpeed: TransferSpeed = TransferSpeed.REGULAR,

        /** The currency code used for paying transaction fee. */
        val feeCurrencyCode: CurrencyCode = currencyCode,

        /** True when the user is confirming the transaction details. */
        val isConfirmingTx: Boolean = false,

        /** True when the user is authenticating. */
        val isAuthenticating: Boolean = false,

        /** True when the transaction is being submitted to the network. */
        val isSendingTransaction: Boolean = false,

        /** The currently estimated [TransferFeeBasis], not null when [networkFee] > [BigDecimal.ZERO]. */
        val transferFeeBasis: TransferFeeBasis? = null,

        /** True when the latest fee request has failed. */
        val feeEstimateFailed: Boolean = false,

        /** An error with the current [targetAddress]. */
        val targetInputError: InputError? = null,

        /** An error with the current [rawAmount]. */
        val amountInputError: InputError? = null,

        /** True when the user can authenticate the transaction with his fingerprint */
        val isFingerprintAuthEnable: Boolean = false,

        /** Url from where we need to fetch the crypto request */
        @Redacted val cryptoRequestUrl: Link.CryptoRequestUrl? = null,
        /** A payment request fetched from bitpay */
        @Redacted val paymentProtocolRequest: PaymentProtocolRequest? = null,
        /** True when a payment request data is being fetched */
        val isFetchingPayment: Boolean = false
    ) {
        sealed class InputError {
            object Empty : InputError()
            object Invalid : InputError()
            object BalanceTooLow : InputError()

            object ClipboardEmpty : InputError()
            object ClipboardInvalid : InputError()

            object FailedToEstimateFee : InputError()
        }

        /** True when the user can select the [TransferSpeed], currently only BTC. */
        val showFeeSelect: Boolean = currencyCode.isBitcoin()

        /** The total cost of this transaction in [currencyCode]. */
        val totalCost: BigDecimal = when {
            currencyCode == feeCurrencyCode -> amount + networkFee
            else -> amount
        }

        /** The total cost of this transaction in [fiatCode]. */
        val fiatTotalCost: BigDecimal = fiatAmount + fiatNetworkFee

        /** True when the necessary inputs to estimate a fee are available. */
        val canEstimateFee: Boolean =
            targetAddress.isNotBlank() &&
                targetInputError == null &&
                !isTotalCostOverBalance &&
                !amount.isZero()

        /** True when we are displaying the information of a payment request */
        val isBitpayPayment: Boolean = paymentProtocolRequest != null

        companion object {

            /** Create a [SendSheetModel] using only the required values. */
            fun createDefault(
                currencyCode: CurrencyCode,
                fiatCode: String
            ) = M(
                currencyCode = currencyCode,
                fiatCode = fiatCode
            )
        }

        init {
            require(currencyCode.isNotBlank()) {
                "currencyCode cannot be blank."
            }
            require(fiatCode.isNotBlank()) {
                "fiatCode cannot be blank."
            }
        }

        /**
         * Updates the tx amount fields to [newRawAmount] keeping the crypto/fiat
         * conversions in-sync depending on [SendSheetModel.isAmountCrypto].
         */
        @Suppress("ComplexMethod")
        fun withNewRawAmount(newRawAmount: String): M {
            if (newRawAmount.isBlank() || BigDecimal(newRawAmount).isZero()) {
                return copy(
                    rawAmount = newRawAmount,
                    amount = BigDecimal.ZERO,
                    fiatAmount = BigDecimal.ZERO,
                    isTotalCostOverBalance = false,
                    amountInputError = null
                )
            }
            val newAmount: BigDecimal
            val newFiatAmount: BigDecimal
            val isTotalCostOverBalance: Boolean

            if (isAmountCrypto) {
                newAmount = BigDecimal(newRawAmount)
                newFiatAmount = if (fiatPricePerUnit > BigDecimal.ZERO) {
                    newAmount * fiatPricePerUnit
                } else {
                    fiatAmount
                }
                isTotalCostOverBalance = when {
                    currencyCode == feeCurrencyCode ->
                        newAmount + networkFee > balance
                    else -> newAmount > balance
                }
            } else {
                newFiatAmount = BigDecimal(newRawAmount)
                val hasRate = fiatPricePerUnit > BigDecimal.ZERO
                val hasFiatAmount = newFiatAmount > BigDecimal.ZERO
                newAmount = if (hasRate && hasFiatAmount) {
                    newFiatAmount.setScale(fiatPricePerUnit.scale())
                        .divide(fiatPricePerUnit, BRConstants.ROUNDING_MODE)
                } else {
                    amount
                }
                isTotalCostOverBalance = newFiatAmount + fiatNetworkFee > fiatBalance
            }

            return copy(
                rawAmount = newRawAmount,
                amount = newAmount,
                fiatAmount = newFiatAmount,
                isTotalCostOverBalance = isTotalCostOverBalance,
                amountInputError = if (isTotalCostOverBalance) {
                    InputError.BalanceTooLow
                } else null
            )
        }
    }

    @MobiusUpdateSpec(
        prefix = "SendSheet",
        baseModel = M::class,
        baseEffect = F::class
    )
    sealed class E {

        data class OnRequestScanned(
            val currencyCode: CurrencyCode,
            val amount: BigDecimal?,
            @Redacted val targetAddress: String?
        ) : E()

        data class OnExchangeRateUpdated(
            val fiatPricePerUnit: BigDecimal,
            val fiatPricePerFeeUnit: BigDecimal,
            val feeCurrencyCode: CurrencyCode
        ) : E()

        data class OnBalanceUpdated(
            val balance: BigDecimal,
            val fiatBalance: BigDecimal
        ) : E()

        object OnNetworkFeeError : E()

        data class OnNetworkFeeUpdated(
            @Redacted val targetAddress: String,
            val amount: BigDecimal,
            val networkFee: BigDecimal,
            val transferFeeBasis: TransferFeeBasis
        ) : E()

        data class OnTransferSpeedChanged(
            val transferSpeed: TransferSpeed
        ) : E()

        data class OnAddressValidated(
            @Redacted val address: String,
            val isValid: Boolean
        ) : E()

        sealed class OnAmountChange : E() {
            object AddDecimal : OnAmountChange()
            object Delete : OnAmountChange()
            object Clear : OnAmountChange()

            data class AddDigit(
                @Redacted val digit: Int
            ) : OnAmountChange()
        }

        data class OnTargetAddressChanged(
            @Redacted val toAddress: String
        ) : E()

        data class OnMemoChanged(@Redacted val memo: String) : E()

        sealed class ConfirmTx : E() {
            object OnConfirmClicked : ConfirmTx()
            object OnCancelClicked : ConfirmTx()
        }

        sealed class OnAddressPasted : E() {

            data class ValidAddress(
                @Redacted val address: String
            ) : OnAddressPasted()

            object NoAddress : OnAddressPasted()
            object InvalidAddress : OnAddressPasted()
        }

        object GoToEthWallet : E()

        data class OnSendComplete(val transfer: Transfer) : E()
        object OnSendFailed : E()

        object OnSendClicked : E()
        object OnAuthSuccess : E()
        object OnAuthCancelled : E()

        object OnScanClicked : E()
        object OnFaqClicked : E()
        object OnCloseClicked : E()
        object OnPasteClicked : E()
        object OnAmountEditClicked : E()
        object OnAmountEditDismissed : E()

        object OnToggleCurrencyClicked : E()

        data class OnAuthenticationSettingsUpdated(internal val isFingerprintEnable: Boolean) :
            E()

        sealed class PaymentProtocol : E() {
            data class OnPaymentLoaded(
                val paymentRequest: PaymentProtocolRequest,
                val cryptoAmount: BigDecimal
            ) : PaymentProtocol()

            data class OnLoadFailed(val message: String) : PaymentProtocol()
            object OnPostCompleted : PaymentProtocol()
            object OnPostFailed : PaymentProtocol()
        }
    }

    sealed class F {

        sealed class Nav(
            override val navigationEffect: NavigationEffect
        ) : F(), NavEffectHolder {
            data class GoToFaq(
                val currencyCode: CurrencyCode
            ) : Nav(NavigationEffect.GoToFaq(BRConstants.FAQ_SEND, currencyCode))

            data class GoToReceive(
                val currencyCode: CurrencyCode
            ) : Nav(NavigationEffect.GoToReceive(currencyCode))

            object GoToScan : Nav(NavigationEffect.GoToQrScan)
            object CloseSheet : Nav(NavigationEffect.GoBack)
            object GoToEthWallet : Nav(NavigationEffect.GoToWallet("eth"))
            object GoToTransactionComplete : Nav(NavigationEffect.GoToTransactionComplete)
        }

        data class ValidateAddress(
            val currencyCode: CurrencyCode,
            @Redacted val address: String
        ) : F()

        data class ShowEthTooLowForTokenFee(
            val currencyCode: CurrencyCode,
            val networkFee: BigDecimal
        ) : F()

        data class LoadBalance(
            val currencyCode: CurrencyCode
        ) : F()

        data class LoadExchangeRate(
            val currencyCode: CurrencyCode,
            val fiatCode: String
        ) : F()

        data class EstimateFee(
            val currencyCode: CurrencyCode,
            @Redacted val address: String,
            val amount: BigDecimal,
            val transferSpeed: TransferSpeed
        ) : F()

        data class SendTransaction(
            val currencyCode: CurrencyCode,
            @Redacted val address: String,
            val amount: BigDecimal,
            val transferFeeBasis: TransferFeeBasis
        ) : F()

        data class AddTransactionMetaData(
            val transaction: Transfer,
            @Redacted val memo: String,
            val fiatCurrencyCode: String,
            val fiatPricePerUnit: BigDecimal
        ) : F()

        data class ParseClipboardData(
            val currencyCode: CurrencyCode,
            val feeCurrencyCode: CurrencyCode
        ) : F()

        object LoadAuthenticationSettings : F()

        data class ShowErrorDialog(
            val message: String
        ) : F()

        sealed class PaymentProtocol : F() {
            data class LoadPaymentData(
                @Redacted val cryptoRequestUrl: Link.CryptoRequestUrl
            ) : PaymentProtocol()

            data class ContinueWitPayment(
                val paymentProtocolRequest: PaymentProtocolRequest,
                val transferFeeBasis: TransferFeeBasis
            ) : PaymentProtocol()

            data class PostPayment(
                val paymentProtocolRequest: PaymentProtocolRequest,
                val transfer: Transfer
            ) : PaymentProtocol()
        }
    }
}

fun Link.CryptoRequestUrl.asSendSheetModel(fiatCode: String) =
    // TODO: Handle all request params
    SendSheet.M(
        currencyCode = currencyCode,
        fiatCode = fiatCode,
        isAmountCrypto = true,
        amount = amount ?: BigDecimal.ZERO,
        rawAmount = amount?.stripTrailingZeros()?.toPlainString() ?: "",
        targetAddress = address ?: "",
        cryptoRequestUrl = this
    )

fun CryptoRequest.asSendSheetModel(fiatCode: String) =
    SendSheet.M(
        currencyCode = currencyCode,
        fiatCode = fiatCode,
        isAmountCrypto = true,
        amount = amount ?: value ?: BigDecimal.ZERO,
        rawAmount = (amount ?: value)?.stripTrailingZeros()?.toPlainString() ?: "",
        targetAddress = if (hasAddress()) getAddress(false) else ""
    )

