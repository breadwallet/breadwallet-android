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

import com.breadwallet.ext.replaceAt
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.wallet.WalletScreenEffect.*
import com.breadwallet.ui.wallet.WalletScreenEvent.*
import com.platform.entities.TxMetaDataEmpty
import com.platform.entities.TxMetaDataValue
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.*
import com.spotify.mobius.Update

@Suppress("TooManyFunctions", "ComplexMethod")
object WalletUpdate : Update<WalletScreenModel, WalletScreenEvent, WalletScreenEffect>,
    WalletScreenUpdateSpec {

    private const val TX_METADATA_PREFETCH = 10

    override fun update(
        model: WalletScreenModel,
        event: WalletScreenEvent
    ): Next<WalletScreenModel, WalletScreenEffect> = patch(model, event)

    override fun onFilterSentClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                filterSent = !model.filterSent,
                filterReceived = false,
                filteredTransactions = model.transactions.filtered(
                    model.filterQuery,
                    filterSent = !model.filterSent,
                    filterReceived = false,
                    filterComplete = model.filterComplete,
                    filterPending = model.filterPending
                )
            )
        )

    override fun onFilterReceivedClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                filterReceived = !model.filterReceived,
                filterSent = false,
                filteredTransactions = model.transactions.filtered(
                    model.filterQuery,
                    filterSent = false,
                    filterReceived = !model.filterReceived,
                    filterComplete = model.filterComplete,
                    filterPending = model.filterPending
                )
            )
        )

    override fun onFilterPendingClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                filterPending = !model.filterPending,
                filterComplete = false,
                filteredTransactions = model.transactions.filtered(
                    model.filterQuery,
                    filterSent = model.filterSent,
                    filterReceived = model.filterReceived,
                    filterComplete = false,
                    filterPending = !model.filterPending
                )
            )
        )

    override fun onFilterCompleteClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                filterComplete = !model.filterComplete,
                filterPending = false,
                filteredTransactions = model.transactions.filtered(
                    model.filterQuery,
                    filterSent = model.filterSent,
                    filterReceived = model.filterReceived,
                    filterComplete = !model.filterComplete,
                    filterPending = false
                )
            )
        )

    override fun onSearchClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                isShowingSearch = true,
                filteredTransactions = model.transactions
            )
        )

    override fun onSearchDismissClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                isShowingSearch = false,
                filteredTransactions = emptyList(),
                filterQuery = "",
                filterPending = false,
                filterComplete = false,
                filterSent = false,
                filterReceived = false
            )
        )

    override fun onBackClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        dispatch(
            effects(
                Nav.GoBack
            )
        )

    override fun onChangeDisplayCurrencyClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(isCryptoPreferred = !model.isCryptoPreferred),
            effects(UpdateCryptoPreferred(!model.isCryptoPreferred))
        )

    override fun onSendClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        dispatch(
            effects(
                Nav.GoToSend(model.currencyCode)
            )
        )

    override fun onReceiveClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        dispatch(
            effects(
                Nav.GoToReceive(model.currencyCode)
            )
        )

    override fun onBrdRewardsClicked(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        if (model.isShowingBrdRewards)
            dispatch(effects(Nav.GoToBrdRewards))
        else noChange()

    override fun onShowReviewPrompt(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                showReviewPrompt = true
            )
        )

    override fun onIsShowingReviewPrompt(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(isShowingReviewPrompt = true),
            effects(RecordReviewPrompt)
        )

    override fun onReviewPromptAccepted(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        dispatch(
            effects(
                GoToReview
            )
        )

    override fun onChartDataPointReleased(
        model: WalletScreenModel
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(selectedPriceDataPoint = null),
            effects(
                TrackEvent(
                    String.format(
                        EventUtils.EVENT_WALLET_CHART_SCRUBBED,
                        model.currencyCode
                    )
                )
            )
        )

    override fun onSyncProgressUpdated(
        model: WalletScreenModel,
        event: OnSyncProgressUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                isSyncing = event.isSyncing,
                syncProgress = event.progress,
                syncingThroughMillis = event.syncThroughMillis
            )
        )

    override fun onQueryChanged(
        model: WalletScreenModel,
        event: OnQueryChanged
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                filterQuery = event.query,
                filteredTransactions = model.transactions.filtered(
                    event.query,
                    filterSent = model.filterSent,
                    filterReceived = model.filterReceived,
                    filterComplete = model.filterComplete,
                    filterPending = model.filterPending
                )
            )
        )

    override fun onCurrencyNameUpdated(
        model: WalletScreenModel,
        event: OnCurrencyNameUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                currencyName = event.name
            )
        )

    override fun onBrdRewardsUpdated(
        model: WalletScreenModel,
        event: OnBrdRewardsUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                isShowingBrdRewards = event.showing
            )
        )

    override fun onBalanceUpdated(
        model: WalletScreenModel,
        event: OnBalanceUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                balance = event.balance,
                fiatBalance = event.fiatBalance
            )
        )

    override fun onFiatPricePerUpdated(
        model: WalletScreenModel,
        event: OnFiatPricePerUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                fiatPricePerUnit = event.pricePerUnit,
                priceChange = event.priceChange
            )
        )

    override fun onIsTokenSupportedUpdated(
        model: WalletScreenModel,
        event: OnIsTokenSupportedUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                isShowingDelistedBanner = !event.isTokenSupported
            )
        )

    override fun onCryptoTransactionsUpdated(
        model: WalletScreenModel,
        event: OnCryptoTransactionsUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        if (event.transactions.isNotEmpty()) {
            dispatch(
                setOf(
                    ConvertCryptoTransactions(
                        event.transactions
                    )
                )
            )
        } else {
            noChange()
        }

    override fun onTransactionsUpdated(
        model: WalletScreenModel,
        event: OnTransactionsUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        if (model.transactions.isNullOrEmpty() && event.walletTransactions.isNotEmpty()) {
            val txHashes = event.walletTransactions.take(TX_METADATA_PREFETCH).map { it.txHash }
            next(
                model.copy(transactions = event.walletTransactions),
                effects(
                    CheckReviewPrompt(model.currencyCode, event.walletTransactions),
                    LoadTransactionMetaData(txHashes)
                )
            )
        } else {
            next(
                model.copy(
                    transactions = event.walletTransactions
                )
            )
        }

    override fun onTransactionMetaDataUpdated(
        model: WalletScreenModel,
        event: OnTransactionMetaDataUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> {
        val index = model.transactions.indexOfFirst { it.txHash == event.transactionHash }
        val transaction = model.transactions[index].copy(
            memo = when (event.transactionMetaData) {
                is TxMetaDataValue -> event.transactionMetaData.comment ?: ""
                is TxMetaDataEmpty -> ""
            }
        )
        val transactions = model.transactions.replaceAt(index, transaction)
        return next(
            model.copy(transactions = transactions)
        )
    }

    override fun onTransactionAdded(
        model: WalletScreenModel,
        event: OnTransactionAdded
    ): Next<WalletScreenModel, WalletScreenEffect> =
        if (model.transactions.isNullOrEmpty())
            next(
                model.copy(transactions = listOf(event.walletTransaction) + model.transactions),
                effects(CheckReviewPrompt(model.currencyCode, model.transactions + event.walletTransaction))
            )
        else
            next(
                model.copy(
                    transactions = listOf(event.walletTransaction) + model.transactions
                )
            )

    override fun onVisibleTransactionsChanged(
        model: WalletScreenModel,
        event: OnVisibleTransactionsChanged
    ): Next<WalletScreenModel, WalletScreenEffect> =
        dispatch(effects(LoadTransactionMetaData(event.transactionHashes)))

    override fun onTransactionRemoved(
        model: WalletScreenModel,
        event: OnTransactionRemoved
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                transactions = model.transactions - event.walletTransaction
            )
        )

    override fun onConnectionUpdated(
        model: WalletScreenModel,
        event: OnConnectionUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                hasInternet = event.isConnected
            )
        )

    override fun onSendRequestGiven(
        model: WalletScreenModel,
        event: OnSendRequestGiven
    ): Next<WalletScreenModel, WalletScreenEffect> =
        dispatch(
            effects(
                Nav.GoToSend(model.currencyCode, event.cryptoRequest)
            )
        )

    override fun onTransactionClicked(
        model: WalletScreenModel,
        event: OnTransactionClicked
    ): Next<WalletScreenModel, WalletScreenEffect> =
        dispatch(
            effects(
                Nav.GoToTransaction(model.currencyCode, event.txHash)
            )
        )

    override fun onHideReviewPrompt(
        model: WalletScreenModel,
        event: OnHideReviewPrompt
    ): Next<WalletScreenModel, WalletScreenEffect> =
        if (event.isDismissed) {
            next(
                model.copy(
                    isShowingReviewPrompt = false,
                    showReviewPrompt = false
                ),
                effects(RecordReviewPromptDismissed)
            )
        } else {
            next(
                model.copy(
                    isShowingReviewPrompt = false,
                    showReviewPrompt = false
                )
            )
        }

    override fun onIsCryptoPreferredLoaded(
        model: WalletScreenModel,
        event: OnIsCryptoPreferredLoaded
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(model.copy(isCryptoPreferred = event.isCryptoPreferred))

    override fun onChartIntervalSelected(
        model: WalletScreenModel,
        event: OnChartIntervalSelected
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(
                priceChartInterval = event.interval
            ),
            effects(
                LoadChartInterval(event.interval, model.currencyCode),
                TrackEvent(
                    String.format(
                        EventUtils.EVENT_WALLET_CHART_AXIS_TOGGLE,
                        model.currencyCode
                    )
                )
            )
        )

    override fun onMarketChartDataUpdated(
        model: WalletScreenModel,
        event: OnMarketChartDataUpdated
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(priceChartDataPoints = event.priceDataPoints)
        )

    override fun onChartDataPointSelected(
        model: WalletScreenModel,
        event: OnChartDataPointSelected
    ): Next<WalletScreenModel, WalletScreenEffect> =
        next(
            model.copy(selectedPriceDataPoint = event.priceDataPoint)
        )
}

/**
 * Filter the list of transactions where the pairs [filterSent]:[filterReceived]
 * and [filterComplete]:[filterPending] are mutually exclusive and [filterQuery]
 * is applied unless it is blank.
 */
private fun List<WalletTransaction>.filtered(
    filterQuery: String,
    filterSent: Boolean,
    filterReceived: Boolean,
    filterComplete: Boolean,
    filterPending: Boolean
) = when {
    filterSent -> when {
        filterComplete -> filter { it.isComplete }
        filterPending -> filter { it.isPending }
        else -> this
    }.filter { !it.isReceived }
    filterReceived -> when {
        filterComplete -> filter { it.isComplete }
        filterPending -> filter { it.isPending }
        else -> this
    }.filter { it.isReceived }
    filterComplete -> filter { it.isComplete }
    filterPending -> filter { it.isPending }
    else -> this
}.filter {
    when {
        filterQuery.isBlank() -> true
        else -> {
            val lowerCaseQuery = filterQuery.toLowerCase()
            listOf(
                it.memo,
                it.toAddress,
                it.fromAddress
            ).any { subject ->
                subject?.toLowerCase()
                    ?.contains(lowerCaseQuery) ?: false
            }
        }
    }
}
