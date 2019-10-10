package com.breadwallet.ui.send

import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.util.CurrencyCode
import com.breadwallet.util.isBitcoin
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

private const val ECONOMY_FEE_HOURS = 10L
private const val REGULAR_FEE_HOURS = 1L

/**
 * [SendSheetModel] models the ability to send an [amount] of [currencyCode]
 * to the supplied [toAddress].
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
    /** True when [amount] is greater than [balance]. */
    val isAmountOverBalance: Boolean = false,

    /** The user supplied address to send the [amount] of [currencyCode] to. */
    val toAddress: String = "",
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
    val transferFeeBasis: TransferFeeBasis? = null
) {
    enum class TransferSpeed(val targetTime: Long) {
        ECONOMY(TimeUnit.HOURS.toMillis(ECONOMY_FEE_HOURS)),
        REGULAR(TimeUnit.HOURS.toMillis(REGULAR_FEE_HOURS)),
        PRIORITY(0L);
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
    fun withNewRawAmount(newRawAmount: String): SendSheetModel {
        if (newRawAmount.isBlank() || BigDecimal(newRawAmount) == BigDecimal.ZERO) {
            return copy(
                rawAmount = newRawAmount,
                amount = BigDecimal.ZERO,
                fiatAmount = BigDecimal.ZERO,
                isAmountOverBalance = false
            )
        }
        val newAmount: BigDecimal
        val newFiatAmount: BigDecimal
        val isAmountOverBalance: Boolean

        if (isAmountCrypto) {
            newAmount = BigDecimal(newRawAmount)
            newFiatAmount = if (fiatPricePerUnit > BigDecimal.ZERO) {
                newAmount * fiatPricePerUnit
            } else {
                fiatAmount
            }
            isAmountOverBalance = newAmount > balance
        } else {
            newFiatAmount = BigDecimal(newRawAmount)
            val hasRate = fiatPricePerUnit > BigDecimal.ZERO
            val hasFiatAmount = newFiatAmount > BigDecimal.ZERO
            newAmount = if (hasRate && hasFiatAmount) {
                newFiatAmount.setScale(fiatPricePerUnit.scale()) / fiatPricePerUnit
            } else {
                amount
            }
            isAmountOverBalance = newFiatAmount > fiatBalance
        }

        return copy(
            rawAmount = newRawAmount,
            amount = newAmount,
            fiatAmount = newFiatAmount,
            isAmountOverBalance = isAmountOverBalance
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
            "isAmountOverBalance=$isAmountOverBalance, " +
            "toAddress='***', " +
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

fun CryptoRequest.asSendSheetModel(fiatCode: String) =
    SendSheetModel(
        currencyCode = currencyCode,
        fiatCode = fiatCode,
        amount = amount,
        toAddress = if (hasAddress()) getAddress(false) else ""
    )
