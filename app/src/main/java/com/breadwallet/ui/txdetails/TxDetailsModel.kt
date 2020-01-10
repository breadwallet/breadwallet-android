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

import com.breadwallet.crypto.TransferState
import com.breadwallet.crypto.TransferState.Type
import com.breadwallet.util.CurrencyCode
import java.math.BigDecimal
import java.util.*

data class TxDetailsModel(
    val currencyCode: CurrencyCode,
    val transactionHash: String,
    val isCryptoPreferred: Boolean,
    val preferredFiatIso: String,
    val showDetails: Boolean = false,
    val isEth: Boolean = false,
    val isErc20: Boolean = false,
    val cryptoTransferredAmount: BigDecimal = BigDecimal.ZERO,
    val fee: BigDecimal = BigDecimal.ZERO,
    val isReceived: Boolean = false,
    val fiatAmountNow: BigDecimal = BigDecimal.ZERO,
    val gasPrice: BigDecimal = BigDecimal.ZERO,
    val gasLimit: BigDecimal = BigDecimal.ZERO,
    val blockNumber: Int = 0,
    val toOrFromAddress: String = "",
    val memo: String = "",
    val exchangeRate: BigDecimal = BigDecimal.ZERO,
    val exchangeCurrencyCode: String = "",
    val confirmationDate: Date? = null,
    val confirmedInBlockNumber: String = "",
    val transactionState: TransactionState? = null,
    val feeToken: String = ""
) {
    companion object {
        /** Create a [TxDetailsModel] using only the required values. */
        fun createDefault(
            currencyCode: CurrencyCode,
            transactionHash: String,
            preferredFiatIso: String,
            isCryptoPreferred: Boolean
        ) = TxDetailsModel(
            currencyCode = currencyCode,
            transactionHash = transactionHash,
            preferredFiatIso = preferredFiatIso,
            isCryptoPreferred = isCryptoPreferred
        )
    }

    val transactionTotal: BigDecimal
        get() = cryptoTransferredAmount + fee

    val fiatAmountWhenSent: BigDecimal
        get() = cryptoTransferredAmount.multiply(exchangeRate)

    val isFeeForToken: Boolean
        get() = feeToken.isNotBlank()
}

enum class TransactionState {
    COMPLETED, CONFIRMING, FAILED, DELETED;

    companion object {
        fun valueOf(transferState: TransferState): TransactionState {
            return when (transferState.type) {
                Type.INCLUDED -> COMPLETED
                Type.FAILED -> FAILED
                Type.CREATED,
                Type.SIGNED,
                Type.SUBMITTED,
                Type.PENDING -> CONFIRMING
                Type.DELETED -> DELETED
            }
        }
    }
}
