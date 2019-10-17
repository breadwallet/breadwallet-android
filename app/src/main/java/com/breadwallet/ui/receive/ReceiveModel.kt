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

import com.breadwallet.util.CurrencyCode

/** Models a screen capable of displaying the user's wallet address. */
data class ReceiveModel(
    val currencyCode: CurrencyCode,
    /** The name of the Wallet's currency. */
    val walletName: String = "",
    /** The network compatible address for transactions. */
    val receiveAddress: String = "",
    /** The address without network specific decoration. */
    val sanitizedAddress: String = "",
    /** True if the currency supports requesting an amount (native only). */
    val isRequestAmountSupported: Boolean = false,
    /** True when the user copies their address and resets after some time.*/
    val isDisplayingCopyMessage: Boolean = false
) {
    companion object {
        fun createDefault(currencyCode: CurrencyCode) =
            ReceiveModel(currencyCode = currencyCode)
    }
}
