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

import com.breadwallet.crypto.Transfer
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.model.PriceChange
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavigationEffect
import com.platform.entities.TxMetaData
import io.hypno.switchboard.MobiusUpdateSpec
import java.math.BigDecimal

object WalletScreen {
    data class M(
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
                M(currencyCode = currencyCode)
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

    @MobiusUpdateSpec(
        prefix = "WalletScreen",
        baseModel = M::class,
        baseEffect = F::class
    )
    sealed class E {
        data class OnSyncProgressUpdated(
            val progress: Float,
            val syncThroughMillis: Long,
            val isSyncing: Boolean
        ) : E() {
            init {
                require(progress in 0.0..1.0) {
                    "Sync progress must be in 0..1 but was $progress"
                }
            }
        }

        data class OnQueryChanged(val query: String) : E()

        data class OnCurrencyNameUpdated(val name: String) : E()
        data class OnBrdRewardsUpdated(val showing: Boolean) : E()
        data class OnBalanceUpdated(val balance: BigDecimal, val fiatBalance: BigDecimal) : E()

        data class OnFiatPricePerUpdated(val pricePerUnit: String, val priceChange: PriceChange?) : E()

        data class OnTransactionsUpdated(
            val walletTransactions: List<WalletTransaction>
        ) : E() {
            override fun toString() =
                "OnTransactionsUpdated(walletTransactions=(size:${walletTransactions.size}))"
        }

        data class OnTransactionMetaDataUpdated(
            val transactionHash: String,
            val transactionMetaData: TxMetaData
        ) : E()

        data class OnVisibleTransactionsChanged(
            val transactionHashes: List<String>
        ) : E()

        data class OnCryptoTransactionsUpdated(
            val transactions: List<Transfer>
        ) : E() {
            override fun toString() =
                "OnCryptoTransactionsUpdated(walletTransactions=(size:${transactions.size}))"
        }

        data class OnTransactionAdded(val walletTransaction: WalletTransaction) : E()
        data class OnTransactionRemoved(val walletTransaction: WalletTransaction) : E()
        data class OnConnectionUpdated(val isConnected: Boolean) : E()

        object OnFilterSentClicked : E()
        object OnFilterReceivedClicked : E()
        object OnFilterPendingClicked : E()
        object OnFilterCompleteClicked : E()

        object OnSearchClicked : E()
        object OnSearchDismissClicked : E()
        object OnBackClicked : E()

        object OnChangeDisplayCurrencyClicked : E()

        object OnSendClicked : E()
        data class OnSendRequestGiven(val cryptoRequest: CryptoRequest) : E()
        object OnReceiveClicked : E()

        data class OnTransactionClicked(val txHash: String) : E()

        object OnBrdRewardsClicked : E()

        object OnShowReviewPrompt : E()
        object OnIsShowingReviewPrompt : E()
        data class OnHideReviewPrompt(val isDismissed: Boolean) : E()
        object OnReviewPromptAccepted : E()

        data class OnIsCryptoPreferredLoaded(val isCryptoPreferred: Boolean) : E()

        data class OnChartIntervalSelected(val interval: Interval) : E()
        data class OnMarketChartDataUpdated(
            val priceDataPoints: List<PriceDataPoint>
        ) : E() {
            override fun toString() =
                "OnMarketChartDataUpdated(priceDataPoints=(size:${priceDataPoints.size}))"
        }

        data class OnChartDataPointSelected(val priceDataPoint: PriceDataPoint) : E()
        object OnChartDataPointReleased : E()

        data class OnIsTokenSupportedUpdated(val isTokenSupported: Boolean) : E()
    }

    sealed class F {
        data class UpdateCryptoPreferred(val cryptoPreferred: Boolean) : F()

        sealed class Nav : F(), NavEffectHolder {
            data class GoToSend(
                val currencyId: String,
                val cryptoRequest: CryptoRequest? = null
            ) : Nav() {
                override val navigationEffect =
                    NavigationEffect.GoToSend(currencyId, cryptoRequest)
            }

            data class GoToReceive(val currencyId: String) : Nav() {
                override val navigationEffect =
                    NavigationEffect.GoToReceive(currencyId)
            }
            data class GoToTransaction(
                val currencyId: String,
                val txHash: String
            ) : Nav() {
                override val navigationEffect =
                    NavigationEffect.GoToTransaction(currencyId, txHash)
            }

            object GoBack : Nav() {
                override val navigationEffect = NavigationEffect.GoBack
            }
            object GoToBrdRewards : Nav() {
                override val navigationEffect = NavigationEffect.GoToReview
            }
        }

        data class LoadCurrencyName(val currencyId: String) : F()
        data class LoadSyncState(val currencyId: String) : F()
        data class LoadWalletBalance(val currencyId: String) : F()
        data class LoadTransactions(val currencyId: String) : F()
        data class LoadFiatPricePerUnit(val currencyId: String) : F()
        data class LoadTransactionMetaData(val transactionHashes: List<String>) : F()
        data class LoadIsTokenSupported(val currencyCode: String) : F()

        object LoadCryptoPreferred : F()

        data class ConvertCryptoTransactions(
            val transactions: List<Transfer>
        ) : F() {
            override fun toString() =
                "ConvertCryptoTransactions(transactions=(size:${transactions.size}))"
        }

        data class CheckReviewPrompt(
            val currencyCode: String,
            val transactions: List<WalletTransaction>
        ) : F() {
            override fun toString() = "CheckReviewPrompt(transactions=(size:${transactions.size}))"
        }

        object RecordReviewPrompt : F()
        object RecordReviewPromptDismissed : F()
        object GoToReview : F()

        data class LoadChartInterval(
            val interval: Interval,
            val currencyCode: String
        ) : F()

        data class TrackEvent(
            val eventName: String,
            val attributes: Map<String, String>? = null
        ) : F()
    }
}
