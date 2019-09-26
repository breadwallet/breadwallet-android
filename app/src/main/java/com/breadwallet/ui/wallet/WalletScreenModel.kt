package com.breadwallet.ui.wallet

import com.breadwallet.model.PriceChange
import com.breadwallet.model.PriceDataPoint
import java.math.BigDecimal

@Suppress("DataClassPrivateConstructor")
data class WalletScreenModel private constructor(
        val currencyCode: String,
        val currencyName: String = "",
        val address: String = "",
        val fiatPricePerUnit: String = "",
        val balance: BigDecimal = BigDecimal.ZERO,
        val fiatBalance: BigDecimal = BigDecimal.ZERO,
        val transactions: List<WalletTransaction> = emptyList(),
        val filteredTransactions: List<WalletTransaction> = emptyList(),
        val isCryptoPreferred: Boolean = false,
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
        val fiatWhenSent: Float,
        val toAddress: String,
        val fromAddress: String,
        val isReceived: Boolean,
        val timeStamp: Long,
        val memo: String,
        val isErrored: Boolean,
        val isValid: Boolean,
        val fee: BigDecimal,
        val blockHeight: Int,
        val confirmations: Int,
        val confirmationsUntilFinal: Int,
        val currencyCode: String,
        val feeForToken: String? // TODO: token symbol?
) {
    val isPending: Boolean = confirmations < confirmationsUntilFinal

    val isComplete: Boolean = confirmations >= confirmationsUntilFinal

    val isFeeForToken: Boolean = !feeForToken.isNullOrBlank()
}

