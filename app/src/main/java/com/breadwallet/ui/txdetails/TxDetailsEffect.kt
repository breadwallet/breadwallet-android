/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/17/19.
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
package com.breadwallet.ui.txdetails

import com.breadwallet.util.CurrencyCode
import java.math.BigDecimal

sealed class TxDetailsEffect {

    data class LoadTransaction(
        val currencyCode: CurrencyCode,
        val transactionHash: String
    ) : TxDetailsEffect()

    data class LoadTransactionMetaData(val transactionHash: String) : TxDetailsEffect()

    data class LoadFiatAmountNow(
        val cryptoTransferredAmount: BigDecimal,
        val currencyCode: String,
        val preferredFiatIso: String
    ) : TxDetailsEffect()

    data class UpdateMemo(
        val transactionHash: String,
        val memo: String
    ) : TxDetailsEffect()

    object Close : TxDetailsEffect()
}
