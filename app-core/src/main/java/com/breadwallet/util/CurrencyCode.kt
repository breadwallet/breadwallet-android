/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/10/19.
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
package com.breadwallet.util

import com.breadwallet.tools.util.bch
import com.breadwallet.tools.util.btc
import com.breadwallet.tools.util.eth
import com.breadwallet.tools.util.hbar
import com.breadwallet.tools.util.xtz

typealias CurrencyCode = String

fun CurrencyCode.isBitcoinLike(): Boolean = isBitcoin() || isBitcoinCash()
fun CurrencyCode.isBitcoin(): Boolean = equals(btc, true)
fun CurrencyCode.isBitcoinCash(): Boolean = equals(bch, true)
fun CurrencyCode.isEthereum(): Boolean = equals(eth, true)
fun CurrencyCode.isBrd(): Boolean = equals("brd", true)
fun CurrencyCode.isRipple(): Boolean = equals("xrp", true)
fun CurrencyCode.isHedera(): Boolean = equals(hbar, true)
fun CurrencyCode.isTezos(): Boolean = equals(xtz, true)
fun CurrencyCode.isErc20(): Boolean {
    return !isBitcoin() && !isBitcoinCash() && !isEthereum() && !isRipple() && !isHedera() && !isTezos()
}

