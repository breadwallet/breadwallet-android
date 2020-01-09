package com.breadwallet.ui.send

import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.util.CurrencyCode
import java.math.BigDecimal

sealed class SendSheetEffect {

    sealed class Nav : SendSheetEffect(), NavEffectHolder {
        data class GoToFaq(
            val currencyCode: CurrencyCode
        ) : Nav() {
            override val navigationEffect =
                NavigationEffect.GoToFaq(BRConstants.FAQ_SEND, currencyCode)
        }

        data class GoToReceive(
            val currencyCode: CurrencyCode
        ) : Nav() {
            override val navigationEffect =
                NavigationEffect.GoToReceive(currencyCode)
        }

        object GoToEthWallet : Nav() {
            override val navigationEffect =
                NavigationEffect.GoToWallet("eth")
        }

        object GoToScan : Nav() {
            override val navigationEffect =
                NavigationEffect.GoToQrScan
        }

        object CloseSheet : Nav() {
            override val navigationEffect =
                NavigationEffect.GoBack
        }

        object GoToTransactionComplete : Nav() {
            override val navigationEffect =
                NavigationEffect.GoToTransactionComplete
        }
    }

    data class ValidateAddress(
        val currencyCode: CurrencyCode,
        val address: String
    ) : SendSheetEffect() {
        override fun toString() = "ValidateAddress()"
    }

    data class ShowEthTooLowForTokenFee(
        val currencyCode: CurrencyCode,
        val networkFee: BigDecimal
    ) : SendSheetEffect()

    data class LoadBalance(
        val currencyCode: CurrencyCode
    ) : SendSheetEffect()

    data class LoadExchangeRate(
        val currencyCode: CurrencyCode,
        val fiatCode: String
    ) : SendSheetEffect()

    data class EstimateFee(
        val currencyCode: CurrencyCode,
        val address: String,
        val amount: BigDecimal,
        val transferSpeed: TransferSpeed
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

    data class AddTransactionMetaData(
        val transaction: Transfer,
        val memo: String,
        val fiatCurrencyCode: String,
        val fiatPricePerUnit: BigDecimal
    ) : SendSheetEffect()

    data class ParseClipboardData(
        val currencyCode: CurrencyCode
    ) : SendSheetEffect()

    object LoadAuthenticationSettings : SendSheetEffect()
}
