package com.breadwallet.ui.wallet

import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.wallet.WalletScreenEffect.*
import com.breadwallet.ui.wallet.WalletScreenEvent.*
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next.*
import com.spotify.mobius.Update

val WalletUpdate = Update<WalletScreenModel, WalletScreenEvent, WalletScreenEffect> { model, event ->

    when (event) {
        OnBackClicked ->
            dispatch(effects(
                    GoBack
            ))
        OnSendClicked ->
            dispatch(effects(
                    GoToSend(model.currencyCode)
            ))
        is OnSendRequestGiven ->
            dispatch(effects(
                    GoToSend(model.currencyCode, event.cryptoRequest)
            ))
        is OnSyncProgressUpdated ->
            next(model.copy(
                isSyncing = event.isSyncing,
                syncProgress = event.progress,
                syncingThroughMillis = event.syncThroughMillis
            ))
        OnSearchClicked ->
            next(model.copy(
                    isShowingSearch = true,
                    filteredTransactions = model.transactions
            ))
        OnSearchDismissClicked ->
            next(model.copy(
                    isShowingSearch = false,
                    filteredTransactions = emptyList(),
                    filterQuery = "",
                    filterPending = false,
                    filterComplete = false,
                    filterSent = false,
                    filterReceived = false
            ))
        OnChangeDisplayCurrencyClicked -> {
            val newIsCryptoPreferred = !model.isCryptoPreferred
            next(
                    model.copy(isCryptoPreferred = newIsCryptoPreferred),
                    effects(UpdateCryptoPreferred(newIsCryptoPreferred))
            )
        }
        is OnIsCryptoPreferredLoaded ->
            next(model.copy(isCryptoPreferred = event.isCryptoPreferred))
        OnReceiveClicked ->
            dispatch(effects(
                    GoToReceive(model.currencyCode)
            ))
        OnBrdRewardsClicked ->
            if (model.isShowingBrdRewards)
                dispatch<WalletScreenModel, WalletScreenEffect>(effects(GoToBrdRewards))
            else noChange<WalletScreenModel, WalletScreenEffect>()
        is OnQueryChanged ->
            next(model.copy(
                    filterQuery = event.query,
                    filteredTransactions = model.transactions.filtered(
                            event.query,
                            filterSent = model.filterSent,
                            filterReceived = model.filterReceived,
                            filterComplete = model.filterComplete,
                            filterPending = model.filterPending
                    )
            ))
        is OnBalanceUpdated ->
            next(model.copy(
                    balance = event.balance,
                    fiatBalance = event.fiatBalance
            ))
        is OnFiatPricePerUpdated ->
            next(model.copy(
                    fiatPricePerUnit = event.pricePerUnit,
                    priceChange = event.priceChange
            ))
        is OnTransactionsUpdated ->
            if (model.transactions.isNullOrEmpty() && event.walletTransactions.isNotEmpty())
            next<WalletScreenModel, WalletScreenEffect>(
                        model.copy(transactions = event.walletTransactions),
                        effects(WalletScreenEffect.CheckReviewPrompt(event.walletTransactions)))
            else
                next(model.copy(
                        transactions = event.walletTransactions
                ))
        is OnTransactionAdded ->
            if (model.transactions.isNullOrEmpty())
                next<WalletScreenModel, WalletScreenEffect>(
                        model.copy(transactions = listOf(event.walletTransaction) + model.transactions),
                        effects(CheckReviewPrompt(model.transactions + event.walletTransaction)))
            else
                next(model.copy(
                        transactions = listOf(event.walletTransaction) + model.transactions
                ))
        is OnTransactionRemoved ->
            next(model.copy(
                    transactions = model.transactions - event.walletTransaction
            ))
        is OnTransactionUpdated -> {
            val index = model.transactions.indexOfFirst { it.txHash == event.walletTransaction.txHash }
            if (index == -1)
                next<WalletScreenModel, WalletScreenEffect>(model.copy(
                        transactions = listOf(event.walletTransaction) + model.transactions
                ))
            else
                next(model.copy(
                    transactions = model.transactions
                            .toMutableList()
                            .apply { set(index, event.walletTransaction) }
                ))
        }
        OnFilterSentClicked ->
            next(model.copy(
                    filterSent = !model.filterSent,
                    filterReceived = false,
                    filteredTransactions = model.transactions.filtered(
                            model.filterQuery,
                            filterSent = !model.filterSent,
                            filterReceived = false,
                            filterComplete = model.filterComplete,
                            filterPending = model.filterPending
                    )
            ))
        OnFilterReceivedClicked ->
            next(model.copy(
                    filterReceived = !model.filterReceived,
                    filterSent = false,
                    filteredTransactions = model.transactions.filtered(
                            model.filterQuery,
                            filterSent = false,
                            filterReceived = !model.filterReceived,
                            filterComplete = model.filterComplete,
                            filterPending = model.filterPending
                    )
            ))
        OnFilterPendingClicked ->
            next(model.copy(
                    filterPending = !model.filterPending,
                    filterComplete = false,
                    filteredTransactions = model.transactions.filtered(
                            model.filterQuery,
                            filterSent = model.filterSent,
                            filterReceived = model.filterReceived,
                            filterComplete = false,
                            filterPending = !model.filterPending
                    )
            ))
        OnFilterCompleteClicked ->
            next(model.copy(
                    filterComplete = !model.filterComplete,
                    filterPending = false,
                    filteredTransactions = model.transactions.filtered(
                            model.filterQuery,
                            filterSent = model.filterSent,
                            filterReceived = model.filterReceived,
                            filterComplete = !model.filterComplete,
                            filterPending = false
                    )
            ))
        is OnTransactionClicked ->
            dispatch(effects(
                    GoToTransaction(model.currencyCode, event.txHash)
            ))
        is OnBrdRewardsUpdated ->
            next(model.copy(
                    isShowingBrdRewards = event.showing
            ))
        is OnCurrencyNameUpdated ->
            next(model.copy(
                    currencyName = event.name
            ))
        is OnConnectionUpdated ->
            next(model.copy(
                    hasInternet = event.isConnected
            ))
        OnShowReviewPrompt ->
            next(model.copy(
                    showReviewPrompt = true
            ))
        OnIsShowingReviewPrompt ->
            next(
                    model.copy(isShowingReviewPrompt = true),
                    effects(RecordReviewPrompt)
            )
        is OnHideReviewPrompt ->
            if (event.isDismissed) {
                next<WalletScreenModel, WalletScreenEffect>(
                    model.copy(
                        isShowingReviewPrompt = false,
                        showReviewPrompt = false),
                     effects(RecordReviewPromptDismissed)
                )
            } else {
                next(model.copy(
                        isShowingReviewPrompt = false,
                        showReviewPrompt = false
                ))
            }
        OnReviewPromptAccepted ->
            dispatch(effects(
                    GoToReview
            ))
        is OnChartIntervalSelected -> {
            next<WalletScreenModel, WalletScreenEffect>(
                model.copy(
                    priceChartInterval = event.interval
                ),
                effects(
                    LoadChartInterval(event.interval),
                    TrackEvent(String.format(EventUtils.EVENT_WALLET_CHART_AXIS_TOGGLE, model.currencyCode))
            ))
        }
        is OnMarketChartDataUpdated ->
            next(
                    model.copy(priceChartDataPoints = event.priceDataPoints)
            )
        is OnChartDataPointSelected ->
            next(
                    model.copy(selectedPriceDataPoint = event.priceDataPoint)
            )
        is OnChartDataPointReleased -> {
            next<WalletScreenModel, WalletScreenEffect>(
                    model.copy(selectedPriceDataPoint = null),
                    effects(TrackEvent(String.format(EventUtils.EVENT_WALLET_CHART_SCRUBBED, model.currencyCode)))
            )
        }
    }
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
                    it.fromAddress,
                    it.fiatWhenSent.toString()
            ).any { subject ->
                subject.toLowerCase()
                        .contains(lowerCaseQuery)
            }
        }
    }
}
