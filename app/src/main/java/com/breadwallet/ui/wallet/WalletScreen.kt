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

import com.breadwallet.R
import com.breadwallet.breadbox.WalletState
import com.breadwallet.crypto.Transfer
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.model.PriceChange
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.tools.manager.MarketDataResult
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.platform.entities.TxMetaData
import io.sweers.redacted.annotation.Redacted
import java.math.BigDecimal

object WalletScreen {

    const val DIALOG_CREATE_ACCOUNT = "create_account_dialog"

    data class M(
        val currencyCode: String,
        val currencyName: String = "",
        @Redacted val address: String = "",
        val fiatPricePerUnit: String = "",
        val balance: BigDecimal = BigDecimal.ZERO,
        val fiatBalance: BigDecimal = BigDecimal.ZERO,
        @Redacted val transactions: List<WalletTransaction> = emptyList(),
        @Redacted val filteredTransactions: List<WalletTransaction> = emptyList(),
        val isCryptoPreferred: Boolean = false,
        val isShowingDelistedBanner: Boolean = false,
        val isShowingSearch: Boolean = false,
        val isShowingBrdRewards: Boolean = false,
        val isShowingReviewPrompt: Boolean = false,
        val showReviewPrompt: Boolean = false,
        @Redacted val filterQuery: String = "",
        val filterSent: Boolean = false,
        val filterReceived: Boolean = false,
        val filterPending: Boolean = false,
        val filterComplete: Boolean = false,
        val syncProgress: Float = 0f,
        val syncingThroughMillis: Long = 0,
        val isSyncing: Boolean = false,
        val hasInternet: Boolean = true,
        val priceChartIsLoading: Boolean = true,
        val priceChartInterval: Interval = Interval.ONE_YEAR,
        @Redacted val priceChartDataPoints: List<PriceDataPoint> = emptyList(),
        val marketDataState: MarketDataState = MarketDataState.LOADING,
        val marketCap: BigDecimal? = null,
        val totalVolume: BigDecimal? = null,
        val high24h: BigDecimal? = null,
        val low24h: BigDecimal? = null,
        val selectedPriceDataPoint: PriceDataPoint? = null,
        val priceChange: PriceChange? = null,
        val state: WalletState = WalletState.Loading
    ) {

        companion object {
            fun createDefault(currencyCode: String) =
                M(currencyCode = currencyCode)
        }

        val isFilterApplied: Boolean
            get() = filterQuery.isNotBlank() ||
                filterSent || filterReceived ||
                filterPending || filterComplete
    }

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

        data class OnQueryChanged(@Redacted val query: String) : E()

        data class OnCurrencyNameUpdated(val name: String) : E()
        data class OnBrdRewardsUpdated(val showing: Boolean) : E()
        data class OnBalanceUpdated(val balance: BigDecimal, val fiatBalance: BigDecimal) : E()

        data class OnFiatPricePerUpdated(val pricePerUnit: String, val priceChange: PriceChange?) :
            E()

        data class OnTransactionsUpdated(
            @Redacted val walletTransactions: List<WalletTransaction>
        ) : E()

        data class OnTransactionMetaDataUpdated(
            @Redacted val transactionHash: String,
            val transactionMetaData: TxMetaData
        ) : E()

        data class OnTransactionMetaDataLoaded(
            val metadata: Map<String, TxMetaData>
        ) : E()

        data class OnVisibleTransactionsChanged(
            @Redacted val transactionHashes: List<String>
        ) : E()

        data class OnCryptoTransactionsUpdated(
            @Redacted val transactions: List<Transfer>
        ) : E()

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

        data class OnTransactionClicked(@Redacted val txHash: String) : E()

        object OnBrdRewardsClicked : E()

        data class OnIsCryptoPreferredLoaded(val isCryptoPreferred: Boolean) : E()

        data class OnChartIntervalSelected(val interval: Interval) : E()
        data class OnMarketChartDataUpdated(
            @Redacted val priceDataPoints: List<PriceDataPoint>
        ) : E()

        data class OnMarketDataUpdated(
            val marketData : MarketDataResult
        ) : E()

        data class OnChartDataPointSelected(val priceDataPoint: PriceDataPoint) : E()
        object OnChartDataPointReleased : E()

        data class OnIsTokenSupportedUpdated(val isTokenSupported: Boolean) : E()

        data class OnWalletStateUpdated(val walletState: WalletState) : E()
        object OnCreateAccountClicked : E()
        object OnCreateAccountConfirmationClicked : E()
    }

    sealed class F {
        data class UpdateCryptoPreferred(val cryptoPreferred: Boolean) : F()

        sealed class Nav : F(), NavigationEffect {
            data class GoToSend(
                val currencyId: String,
                val cryptoRequest: CryptoRequest? = null
            ) : Nav() {
                override val navigationTarget =
                    NavigationTarget.SendSheet(currencyId, cryptoRequest)
            }

            data class GoToReceive(val currencyId: String) : Nav() {
                override val navigationTarget =
                    NavigationTarget.ReceiveSheet(currencyId)
            }

            data class GoToTransaction(
                val currencyId: String,
                val txHash: String
            ) : Nav() {
                override val navigationTarget =
                    NavigationTarget.ViewTransaction(currencyId, txHash)
            }

            object GoBack : Nav() {
                override val navigationTarget = NavigationTarget.Back
            }

            object GoToBrdRewards : Nav() {
                override val navigationTarget = NavigationTarget.BrdRewards
            }
        }

        data class LoadCurrencyName(val currencyCode: String) : F()
        data class LoadSyncState(val currencyCode: String) : F()
        data class LoadWalletBalance(val currencyCode: String) : F()
        data class LoadTransactions(val currencyCode: String) : F()
        data class LoadFiatPricePerUnit(val currencyCode: String) : F()
        data class LoadTransactionMetaData(
            val currencyCode: String,
            @Redacted val transactionHashes: List<String>
        ) : F()

        data class LoadTransactionMetaDataSingle(
            val currencyCode: String,
            @Redacted val transactionHashes: List<String>
        ) : F()

        data class LoadIsTokenSupported(val currencyCode: String) : F()

        object LoadCryptoPreferred : F()

        data class ConvertCryptoTransactions(
            @Redacted val transactions: List<Transfer>
        ) : F()

        data class LoadChartInterval(
            val interval: Interval,
            val currencyCode: String
        ) : F()

        data class LoadMarketData(
            val currencyCode: String
        ) : F()

        data class TrackEvent(
            val eventName: String,
            val attributes: Map<String, String>? = null
        ) : F()

        data class LoadWalletState(val currencyCode: String) : F()
        object ShowCreateAccountDialog : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                titleResId = R.string.AccountCreation_title,
                messageResId = R.string.AccountCreation_body,
                positiveButtonResId = R.string.AccountCreation_create,
                negativeButtonResId = R.string.AccountCreation_notNow,
                dialogId = DIALOG_CREATE_ACCOUNT
            )
        }

        object ShowCreateAccountErrorDialog : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                titleResId = R.string.AccountCreation_title,
                messageResId = R.string.AccountCreation_error,
                positiveButtonResId = R.string.AccessibilityLabels_close
            )
        }

        data class CreateAccount(val currencyCode: String) : F()

        object LoadConnectivityState : F()
    }
}

enum class MarketDataState {
    LOADED, LOADING, ERROR
}
