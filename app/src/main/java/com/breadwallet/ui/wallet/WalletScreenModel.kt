package com.breadwallet.ui.wallet

import com.breadwallet.tools.util.BRConstants
import java.math.BigDecimal

@Suppress("DataClassPrivateConstructor")
data class WalletScreenModel private constructor(
        val currencyCode: String,
        val currencyName: String = "",
        val address: String = "",
        val fiatPricePerUnit: Float = 0f,
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
        val syncProgress: Double = 0.0,
        val syncingThroughMillis: Long = 0,
        val hasInternet: Boolean = true
) {

    companion object {
        fun createDefault(currencyCode: String) =
                WalletScreenModel(currencyCode = currencyCode)
    }

    val isSyncing: Boolean
        get() = syncProgress < 1.0

    val hasSyncTime: Boolean
        get() = syncingThroughMillis != 0L

    val isFilterApplied: Boolean
        get() = filterQuery.isNotBlank() ||
                filterSent || filterReceived ||
                filterPending || filterComplete

    val hasPricePerUnit: Boolean
        get() = fiatPricePerUnit != 0f

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
                "syncingThroughMillis=$syncingThroughMillis)"
    }
}

data class WalletTransaction(
        val txHash: String,
        val amount: BigDecimal,
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
        val levels: Int,
        val currencyCode: String
) {
    val isPending: Boolean
        get() = levels > 0 && levels < BRConstants.CONFIRMED_BLOCKS_NUMBER

    val isComplete: Boolean
        get() = confirmations >= BRConstants.CONFIRMED_BLOCKS_NUMBER
}

