/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 7/26/19.
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
package com.breadwallet.ui.wallet

import com.breadwallet.model.PriceChange
import com.breadwallet.model.PriceDataPoint
import java.math.BigDecimal

data class WalletScreenModel(
    val currencyCode: String,
    val currencyName: String = "",
    val address: String = "",
    val fiatPricePerUnit: String = "",
    val balance: BigDecimal = BigDecimal.ZERO,
    val fiatBalance: BigDecimal = BigDecimal.ZERO,
    val transactions: List<WalletTransaction> = emptyList(),
    val filteredTransactions: List<WalletTransaction> = emptyList(),
    val isCryptoPreferred: Boolean = false,
    val isShowingDelistedBanner: Boolean = false,
    val isShowingSearch: Boolean = false,
    val isShowingBrdRewards: Boolean = false,
    val isShowingReviewPrompt: Boolean = false,
    val showReviewPrompt: Boolean = false,
    val filterQuery: String = "",
    val filterSent: Boolean = false,
    val filterReceived: Boolean = false,
    val filterPending: Boolean = false,
    val filterComplete: Boolean = false,
    val syncProgress: Float = 0f,
    val syncingThroughMillis: Long = 0,
    val isSyncing: Boolean = false,
    val hasInternet: Boolean = true,
    val priceChartInterval: Interval = Interval.ONE_YEAR,
    val priceChartDataPoints: List<PriceDataPoint> = emptyList(),
    val selectedPriceDataPoint: PriceDataPoint? = null,
    val priceChange: PriceChange? = null
) {

    companion object {
        fun createDefault(currencyCode: String) =
            WalletScreenModel(currencyCode = currencyCode)
    }

    val hasSyncTime: Boolean
        get() = syncingThroughMillis != 0L

    val isFilterApplied: Boolean
        get() = filterQuery.isNotBlank() ||
            filterSent || filterReceived ||
            filterPending || filterComplete

    override fun toString(): String {
        return "WalletScreenModel(currencyCode='$currencyCode', " +
            "currencyName='$currencyName', " +
            "address='$address', " +
            "fiatPricePerUnit=$fiatPricePerUnit, " +
            "balance=$balance, " +
            "fiatBalance=$fiatBalance, " +
            "transactions=(size:${transactions.size}), " +
            "filteredTransactions=(size:${filteredTransactions.size}), " +
            "isCryptoPreferred=$isCryptoPreferred, " +
            "isShowingSearch=$isShowingSearch, " +
            "isShowingBrdRewards=$isShowingBrdRewards, " +
            "isShowingReviewPrompt=$isShowingReviewPrompt, " +
            "showReviewPrompt=$showReviewPrompt, " +
            "filterQuery='$filterQuery', " +
            "filterSent=$filterSent, " +
            "filterReceived=$filterReceived, " +
            "filterPending=$filterPending, " +
            "filterComplete=$filterComplete, " +
            "syncProgress=$syncProgress, " +
            "syncingThroughMillis=$syncingThroughMillis, " +
            "isSyncing=$isSyncing, " +
            "hasInternet=$hasInternet, " +
            "priceChartInterval=$priceChartInterval, " +
            "priceChartDataPoints=(size:${priceChartDataPoints.size}), " +
            "priceChange=$priceChange)"
    }
}

data class WalletTransaction(
    val txHash: String,
    val amount: BigDecimal,
    val amountInFiat: BigDecimal,
    val toAddress: String,
    val fromAddress: String,
    val isReceived: Boolean,
    val timeStamp: Long,
    val memo: String? = null,
    val isErrored: Boolean,
    val isValid: Boolean,
    val fee: BigDecimal,
    val confirmations: Int,
    val confirmationsUntilFinal: Int,
    val currencyCode: String,
    val feeToken: String = ""
) {
    val isPending: Boolean = confirmations < confirmationsUntilFinal && !isErrored

    val isComplete: Boolean = confirmations >= confirmationsUntilFinal && !isErrored

    val isFeeForToken: Boolean = feeToken.isNotBlank()
}

