/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 12/18/19.
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
package com.breadwallet.model

import com.breadwallet.BuildConfig
import com.breadwallet.util.isBitcoin
import com.breadwallet.util.isBitcoinCash
import com.breadwallet.util.isEthereum
import com.breadwallet.util.isRipple

data class TokenItem(
    val address: String?,
    val symbol: String,
    val name: String,
    var image: String?,
    val isSupported: Boolean,
    val currencyId: String,
    val type: String = "",
    val startColor: String? = null,
    val endColor: String? = null,
    val cryptocompareAlias: String? = null
) {

    val exchangeRateCurrencyCode: String
        get() {
            return if (cryptocompareAlias.isNullOrBlank()) {
                symbol
            } else {
                cryptocompareAlias
            }
        }

    val isNative: Boolean = type.isBlank()

    val urlScheme: String?
        get() = when {
            symbol.isEthereum() || type == "erc20" -> "ethereum"
            symbol.isRipple() -> "xrp"
            symbol.isBitcoin() -> "bitcoin"
            symbol.isBitcoinCash() -> when {
                BuildConfig.BITCOIN_TESTNET -> "bchtest"
                else -> "bitcoincash"
            }
            else -> null
        }

    val urlSchemes: List<String>
        get() = when {
            symbol.isRipple() -> listOf(urlScheme!!, "xrpl", "ripple")
            else -> urlScheme?.run(::listOf) ?: emptyList()
        }
}
