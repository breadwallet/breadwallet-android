package com.breadwallet.ui.send

import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.util.CurrencyCode
import java.math.BigDecimal

sealed class SendSheetEffect {

    data class GoToFaq(
        val currencyCode: CurrencyCode
    ) : SendSheetEffect()
    data class GoToReceive(
        val currencyCode: CurrencyCode
    ) : SendSheetEffect()

    object GoToEthWallet : SendSheetEffect()
    object GoToScan : SendSheetEffect()
    object CloseSheet : SendSheetEffect()

    object ShowTransactionComplete : SendSheetEffect()

    data class ValidateAddress(
        val currencyCode: CurrencyCode,
        val address: String,
        val clearWhenInvalid: Boolean = false
    ) : SendSheetEffect() {
        override fun toString() = "ValidateAddress()"
    }

    data class ShowEthTooLowForTokenFee(
        val currencyCode: CurrencyCode,
        val networkFee: BigDecimal
    ) : SendSheetEffect()

    object LoadBalance : SendSheetEffect()
    data class LoadExchangeRate(
        val currencyCode: CurrencyCode,
        val fiatCode: String
    ) : SendSheetEffect()

    data class EstimateFee(
        val currencyCode: CurrencyCode,
        val address: String,
        val amount: BigDecimal,
        val transferSpeed: SendSheetModel.TransferSpeed
    ) : SendSheetEffect() {
        override fun toString(): String {
            return "EstimateFee(" +
                "currencyCode='$currencyCode', " +
                "address='***', " +
                "amount=$amount, " +
                "fee=$transferSpeed)"
        }
    }

    data class SendTransaction(
        val currencyCode: CurrencyCode,
        val address: String,
        val amount: BigDecimal,
        val transferFeeBasis: TransferFeeBasis
    ) : SendSheetEffect() {
        override fun toString(): String {
            return "SendTransaction(" +
                "currencyCode='$currencyCode', " +
                "address='***', " +
                "amount=$amount, " +
                "transferFeeBasis=$transferFeeBasis)"
        }
    }

    data class ParseClipboardData(
        val currencyCode: CurrencyCode
    ) : SendSheetEffect()

    object LoadAuthenticationSettings : SendSheetEffect()
}
