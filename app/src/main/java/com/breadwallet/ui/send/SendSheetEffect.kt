/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/13/19.
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
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.Link
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

    data class ShowErrorDialog(
        val message: String
    ): SendSheetEffect()

    sealed class PaymentProtocol : SendSheetEffect() {
        data class LoadPaymentData(val cryptoRequestUrl: Link.CryptoRequestUrl) : PaymentProtocol()
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
