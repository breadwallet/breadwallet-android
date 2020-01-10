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

import com.breadwallet.crypto.Transfer
import com.breadwallet.model.PriceChange
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.platform.entities.TxMetaData
import io.hypno.switchboard.MobiusUpdateSpec
import java.math.BigDecimal

@MobiusUpdateSpec(
    baseModel = WalletScreenModel::class,
    baseEffect = WalletScreenEffect::class
)
sealed class WalletScreenEvent {
    data class OnSyncProgressUpdated(
        val progress: Float,
        val syncThroughMillis: Long,
        val isSyncing: Boolean
    ) : WalletScreenEvent() {
        init {
            require(progress in 0.0..1.0) {
                "Sync progress must be in 0..1 but was $progress"
            }
        }
    }

    data class OnQueryChanged(val query: String) : WalletScreenEvent()

    data class OnCurrencyNameUpdated(val name: String) : WalletScreenEvent()
    data class OnBrdRewardsUpdated(val showing: Boolean) : WalletScreenEvent()
    data class OnBalanceUpdated(val balance: BigDecimal, val fiatBalance: BigDecimal) :
        WalletScreenEvent()

    data class OnFiatPricePerUpdated(val pricePerUnit: String, val priceChange: PriceChange?) :
        WalletScreenEvent()

    data class OnTransactionsUpdated(
        val walletTransactions: List<WalletTransaction>
    ) : WalletScreenEvent() {
        override fun toString() =
            "OnTransactionsUpdated(walletTransactions=(size:${walletTransactions.size}))"
    }

    data class OnTransactionMetaDataUpdated(
        val transactionHash: String,
        val transactionMetaData: TxMetaData
    ) : WalletScreenEvent()

    data class OnVisibleTransactionsChanged(
        val transactionHashes: List<String>
    ) : WalletScreenEvent()

    data class OnCryptoTransactionsUpdated(
        val transactions: List<Transfer>
    ) : WalletScreenEvent() {
        override fun toString() =
            "OnCryptoTransactionsUpdated(walletTransactions=(size:${transactions.size}))"
    }

    data class OnTransactionAdded(val walletTransaction: WalletTransaction) : WalletScreenEvent()
    data class OnTransactionRemoved(val walletTransaction: WalletTransaction) : WalletScreenEvent()
    data class OnConnectionUpdated(val isConnected: Boolean) : WalletScreenEvent()

    object OnFilterSentClicked : WalletScreenEvent()
    object OnFilterReceivedClicked : WalletScreenEvent()
    object OnFilterPendingClicked : WalletScreenEvent()
    object OnFilterCompleteClicked : WalletScreenEvent()

    object OnSearchClicked : WalletScreenEvent()
    object OnSearchDismissClicked : WalletScreenEvent()
    object OnBackClicked : WalletScreenEvent()

    object OnChangeDisplayCurrencyClicked : WalletScreenEvent()

    object OnSendClicked : WalletScreenEvent()
    data class OnSendRequestGiven(val cryptoRequest: CryptoRequest) : WalletScreenEvent()
    object OnReceiveClicked : WalletScreenEvent()

    data class OnTransactionClicked(val txHash: String) : WalletScreenEvent()

    object OnBrdRewardsClicked : WalletScreenEvent()

    object OnShowReviewPrompt : WalletScreenEvent()
    object OnIsShowingReviewPrompt : WalletScreenEvent()
    data class OnHideReviewPrompt(val isDismissed: Boolean) : WalletScreenEvent()
    object OnReviewPromptAccepted : WalletScreenEvent()

    data class OnIsCryptoPreferredLoaded(val isCryptoPreferred: Boolean) : WalletScreenEvent()

    data class OnChartIntervalSelected(val interval: Interval) : WalletScreenEvent()
    data class OnMarketChartDataUpdated(
        val priceDataPoints: List<PriceDataPoint>
    ) : WalletScreenEvent() {
        override fun toString() =
            "OnMarketChartDataUpdated(priceDataPoints=(size:${priceDataPoints.size}))"
    }

    data class OnChartDataPointSelected(val priceDataPoint: PriceDataPoint) : WalletScreenEvent()
    object OnChartDataPointReleased : WalletScreenEvent()

    data class OnIsTokenSupportedUpdated(val isTokenSupported: Boolean) : WalletScreenEvent()
}

