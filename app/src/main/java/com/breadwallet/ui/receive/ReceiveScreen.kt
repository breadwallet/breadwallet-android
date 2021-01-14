/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/15/19.
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
package com.breadwallet.ui.receive

import com.breadwallet.ext.isZero
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.ui.send.MAX_DIGITS
import com.breadwallet.util.CurrencyCode
import dev.zacsweers.redacted.annotations.Redacted
import java.math.BigDecimal

object ReceiveScreen {

    /** Models a screen capable of displaying the user's wallet address. */
    data class M(
        val currencyCode: CurrencyCode,
        val fiatCurrencyCode: String,
        /** The name of the Wallet's currency. */
        val walletName: String = "",
        /** The network compatible address for transactions. */
        @Redacted val receiveAddress: String = "",
        /** The address without network specific decoration. */
        @Redacted val sanitizedAddress: String = "",
        /** The user supplied amount to receive as a string. */
        val rawAmount: String = "",
        /** The user supplied amount as [BigDecimal]. */
        val amount: BigDecimal = BigDecimal.ZERO,
        /** The user supplied amount in fiat. */
        val fiatAmount: BigDecimal = BigDecimal.ZERO,
        /** The current fiat exchange rate for [currencyCode] in [fiatCode]. */
        val fiatPricePerUnit: BigDecimal = BigDecimal.ZERO,
        /** True if an amount key pad should be displayed. */
        val isAmountEditVisible: Boolean = false,
        /** Is the user entering a crypto (true) or fiat (false) amount. */
        val isAmountCrypto: Boolean = true
    ) {

        /** True if the currency supports requesting an amount. */
        val isRequestAmountSupported: Boolean = true

        fun withNewRawAmount(newRawAmount: String): M {
            if (newRawAmount.isBlank() || BigDecimal(newRawAmount).isZero()) {
                return copy(
                    rawAmount = newRawAmount,
                    amount = BigDecimal.ZERO,
                    fiatAmount = BigDecimal.ZERO
                )
            }

            val newAmount: BigDecimal
            val newFiatAmount: BigDecimal

            if (isAmountCrypto) {
                newAmount = BigDecimal(newRawAmount)
                newFiatAmount = if (fiatPricePerUnit > BigDecimal.ZERO) {
                    (newAmount * fiatPricePerUnit)
                        .setScale(2, BRConstants.ROUNDING_MODE)
                } else {
                    fiatAmount
                }
            } else {
                newFiatAmount = BigDecimal(newRawAmount)
                val hasRate = fiatPricePerUnit > BigDecimal.ZERO
                val hasFiatAmount = newFiatAmount > BigDecimal.ZERO
                newAmount = if (hasRate && hasFiatAmount) {
                    (newFiatAmount.setScale(
                        fiatPricePerUnit.scale().coerceAtMost(MAX_DIGITS),
                        BRConstants.ROUNDING_MODE
                    ) / fiatPricePerUnit)
                } else {
                    amount
                }
            }

            return copy(
                rawAmount = newRawAmount,
                amount = newAmount,
                fiatAmount = newFiatAmount
            )
        }

        companion object {
            fun createDefault(currencyCode: CurrencyCode, fiatCurrencyCode: String) =
                M(currencyCode = currencyCode, fiatCurrencyCode = fiatCurrencyCode)
        }
    }

    sealed class E {

        data class OnExchangeRateUpdated(
            val fiatPricePerUnit: BigDecimal
        ) : E()

        data class OnWalletInfoLoaded(
            val walletName: String,
            @Redacted val address: String,
            @Redacted val sanitizedAddress: String
        ) : E()

        object OnCloseClicked : E()
        object OnFaqClicked : E()
        object OnShareClicked : E()
        object OnCopyAddressClicked : E()
        object OnAmountClicked : E()
        object OnToggleCurrencyClicked : E()

        sealed class OnAmountChange : E() {
            object AddDecimal : OnAmountChange()
            object Delete : OnAmountChange()

            data class AddDigit(
                @Redacted val digit: Int
            ) : OnAmountChange()
        }
    }

    sealed class F {
        data class LoadExchangeRate(
            val currencyCode: CurrencyCode
        ) : F()

        data class LoadWalletInfo(
            val currencyCode: CurrencyCode
        ) : F()

        object CloseSheet : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }

        data class GoToFaq(
            val currencyCode: CurrencyCode
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.SupportPage(
                BRConstants.FAQ_RECEIVE,
                currencyCode
            )
        }

        object ShowCopiedMessage : F(), ViewEffect
        data class CopyAddressToClipboard(
            @Redacted val address: String
        ) : F()

        data class ShareRequest(
            @Redacted val address: String,
            val amount: BigDecimal,
            val walletName: String
        ) : F(), ViewEffect
    }
}
