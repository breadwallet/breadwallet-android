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
import com.breadwallet.ui.send.MAX_DIGITS
import com.breadwallet.util.CurrencyCode
import com.breadwallet.util.isBitcoin
import com.breadwallet.util.isBitcoinCash
import com.breadwallet.util.isEthereum
import java.math.BigDecimal

/** Models a screen capable of displaying the user's wallet address. */
data class ReceiveModel(
    val currencyCode: CurrencyCode,
    val fiatCurrencyCode: String,
    /** The name of the Wallet's currency. */
    val walletName: String = "",
    /** The network compatible address for transactions. */
    val receiveAddress: String = "",
    /** The address without network specific decoration. */
    val sanitizedAddress: String = "",
    /** True when the user copies their address and resets after some time.*/
    val isDisplayingCopyMessage: Boolean = false,
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

    /** True if the currency supports requesting an amount (native only). */
    val isRequestAmountSupported: Boolean =
        currencyCode.run { isEthereum() || isBitcoinCash() || isBitcoin() }

    fun withNewRawAmount(newRawAmount: String): ReceiveModel {
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
            ReceiveModel(currencyCode = currencyCode, fiatCurrencyCode = fiatCurrencyCode)
    }
}
