/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 1/21/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.ui.wallet

import io.sweers.redacted.annotation.Redacted
import java.math.BigDecimal

private const val TRUNCATED_ADDRESS_CHARS = 7

data class WalletTransaction(
    @Redacted val txHash: String,
    val amount: BigDecimal,
    val amountInFiat: BigDecimal,
    @Redacted val toAddress: String,
    @Redacted val fromAddress: String,
    val isReceived: Boolean,
    @Redacted val timeStamp: Long,
    @Redacted val memo: String? = null,
    val fee: BigDecimal,
    val confirmations: Int,
    val isComplete: Boolean,
    val isPending: Boolean,
    val isErrored: Boolean,
    val progress: Int,
    val currencyCode: String,
    val feeToken: String = "",
    val confirmationsUntilFinal: Int
) {
    val isFeeForToken: Boolean = feeToken.isNotBlank()

    val truncatedToAddress: String
        get() = "${toAddress.take(TRUNCATED_ADDRESS_CHARS)}...${toAddress.takeLast(TRUNCATED_ADDRESS_CHARS)}"

    val truncatedFromAddress: String
        get() = "${fromAddress.take(TRUNCATED_ADDRESS_CHARS)}...${fromAddress.takeLast(TRUNCATED_ADDRESS_CHARS)}"
}
