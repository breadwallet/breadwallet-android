package com.breadwallet.ui.send

import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.ext.isZero
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.tools.util.Link
import com.breadwallet.util.CurrencyCode
import com.breadwallet.util.isBitcoin
import java.math.BigDecimal

/**
 * [SendSheetModel] models the ability to send an [amount] of [currencyCode]
 * to the supplied [targetAddress].
 *
 * [amount] is a value of [fiatCode] when [isAmountCrypto] is true.
 *
 * @throws IllegalArgumentException When [currencyCode] or [fiatCode] are blank.
 */
data class SendSheetModel(
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
    val targetAddress: String = "",
    /** True when [targetAddress] is a valid address. */
    val isTargetValid: Boolean = false,
    /** The user supplied amount to send as a string. */
    val rawAmount: String = "",
    /** The user supplied amount as [BigDecimal]. */
    val amount: BigDecimal = BigDecimal.ZERO,
    /** A user provided memo to store with the transaction. */
    val memo: String? = null,

    /** The user supplied amount in fiat. */
    val fiatAmount: BigDecimal = BigDecimal.ZERO,
    /** The current fiat exchange rate for [currencyCode] in [fiatCode]. */
    val fiatPricePerUnit: BigDecimal = BigDecimal.ZERO,

    /** The user selected [TransferSpeed] for this transaction. */
    val transferSpeed: TransferSpeed = TransferSpeed.REGULAR,

    /** True when the [currencyCode] is an ERC-20 token. */
    val isErc20: Boolean = false,

    /** True when the user is confirming the transaction details. */
    val isConfirmingTx: Boolean = false,

    /** True when the user is authenticating. */
    val isAuthenticating: Boolean = false,

    /** True when the transaction is being submitted to the network. */
    val isSendingTransaction: Boolean = false,

    /** The currently estimated [TransferFeeBasis], not null when [networkFee] > [BigDecimal.ZERO]. */
    val transferFeeBasis: TransferFeeBasis? = null,

    /** An error with the current [targetAddress]. */
    val targetInputError: InputError? = null,

    /** An error with the current [rawAmount]. */
    val amountInputError: InputError? = null,
    
    /** True when the user can authenticate the transaction with his fingerprint */
    val isFingerprintAuthEnable: Boolean = false
) {
    sealed class InputError {
        object Empty : InputError()
        object Invalid : InputError()
        object BalanceTooLow : InputError()

        object ClipboardEmpty : InputError()
        object ClipboardInvalid : InputError()
    }

    /** True when the user can select the [TransferSpeed], currently only BTC. */
    val showFeeSelect: Boolean = currencyCode.isBitcoin()

    /** The total cost of this transaction in [currencyCode]. */
    val totalCost: BigDecimal = amount + networkFee

    /** The total cost of this transaction in [fiatCode]. */
    val fiatTotalCost: BigDecimal = fiatAmount + fiatNetworkFee

    companion object {

        /** Create a [SendSheetModel] using only the required values. */
        fun createDefault(
            currencyCode: CurrencyCode,
            fiatCode: String
        ) = SendSheetModel(
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
    fun withNewRawAmount(newRawAmount: String): SendSheetModel {
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
            isTotalCostOverBalance = newAmount + networkFee > balance
        } else {
            newFiatAmount = BigDecimal(newRawAmount)
            val hasRate = fiatPricePerUnit > BigDecimal.ZERO
            val hasFiatAmount = newFiatAmount > BigDecimal.ZERO
            newAmount = if (hasRate && hasFiatAmount) {
                newFiatAmount.setScale(fiatPricePerUnit.scale()) / fiatPricePerUnit
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

    override fun toString(): String {
        return "SendSheetModel(" +
            "currencyCode='$currencyCode', " +
            "fiatCode='$fiatCode', " +
            "balance=$balance, " +
            "networkFee=$networkFee, " +
            "fiatBalance=$fiatBalance, " +
            "fiatNetworkFee=$fiatNetworkFee, " +
            "isAmountCrypto=$isAmountCrypto, " +
            "isAmountEditVisible=$isAmountEditVisible, " +
            "isAmountOverBalance=$isTotalCostOverBalance, " +
            "targetAddress='***', " +
            "rawAmount='$rawAmount', " +
            "amount=$amount, " +
            "memo='***', " +
            "fiatAmount=$fiatAmount, " +
            "fiatPricePerUnit=$fiatPricePerUnit, " +
            "targetSpeed=$transferSpeed, " +
            "isErc20=$isErc20, " +
            "isConfirmingTx=$isConfirmingTx, " +
            "showFeeSelect=$showFeeSelect, " +
            "totalCost=$totalCost, " +
            "fiatTotalCost=$fiatTotalCost)"
    }
}

fun Link.CryptoRequestUrl.asSendSheetModel(fiatCode: String) =
    // TODO: Handle all request params
    SendSheetModel(
        currencyCode = currencyCode,
        fiatCode = fiatCode,
        isAmountCrypto = true,
        amount = amount ?: BigDecimal.ZERO,
        rawAmount = amount?.stripTrailingZeros()?.toPlainString() ?: "",
        targetAddress = address ?: ""
    )

fun CryptoRequest.asSendSheetModel(fiatCode: String) =
    SendSheetModel(
        currencyCode = currencyCode,
        fiatCode = fiatCode,
        isAmountCrypto = true,
        amount = amount ?: value ?: BigDecimal.ZERO,
        rawAmount = (amount ?: value)?.stripTrailingZeros()?.toPlainString() ?: "",
        targetAddress = if (hasAddress()) getAddress(false) else ""
    )
