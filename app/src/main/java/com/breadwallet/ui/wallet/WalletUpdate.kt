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

import com.breadwallet.breadbox.WalletState
import com.breadwallet.ext.replaceAt
import com.breadwallet.tools.manager.MarketDataResult
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.wallet.WalletScreen.E
import com.breadwallet.ui.wallet.WalletScreen.F
import com.breadwallet.ui.wallet.WalletScreen.M
import com.platform.entities.TxMetaDataEmpty
import com.platform.entities.TxMetaDataValue
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update

@Suppress("TooManyFunctions", "ComplexMethod")
object WalletUpdate : Update<M, E, F>, WalletScreenUpdateSpec {

    private const val TX_METADATA_PREFETCH = 10

    override fun update(
        model: M,
        event: E
    ): Next<M, F> = patch(model, event)

    override fun onFilterSentClicked(
        model: M
    ): Next<M, F> =
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
        model: M
    ): Next<M, F> =
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
        model: M
    ): Next<M, F> =
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
        model: M
    ): Next<M, F> =
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
        model: M
    ): Next<M, F> =
        next(
            model.copy(
                isShowingSearch = true,
                filteredTransactions = model.transactions
            )
        )

    override fun onSearchDismissClicked(
        model: M
    ): Next<M, F> =
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
        model: M
    ): Next<M, F> =
        dispatch(
            effects(
                F.Nav.GoBack
            )
        )

    override fun onChangeDisplayCurrencyClicked(
        model: M
    ): Next<M, F> =
        next(
            model.copy(isCryptoPreferred = !model.isCryptoPreferred),
            effects(F.UpdateCryptoPreferred(!model.isCryptoPreferred))
        )

    override fun onSendClicked(
        model: M
    ): Next<M, F> =
        dispatch(
            effects(
                F.Nav.GoToSend(model.currencyCode)
            )
        )

    override fun onReceiveClicked(
        model: M
    ): Next<M, F> =
        dispatch(
            effects(
                F.Nav.GoToReceive(model.currencyCode)
            )
        )

    override fun onBrdRewardsClicked(
        model: M
    ): Next<M, F> =
        if (model.isShowingBrdRewards)
            dispatch(effects(F.Nav.GoToBrdRewards))
        else noChange()

    override fun onChartDataPointReleased(
        model: M
    ): Next<M, F> =
        next(
            model.copy(selectedPriceDataPoint = null),
            effects(
                F.TrackEvent(
                    String.format(
                        EventUtils.EVENT_WALLET_CHART_SCRUBBED,
                        model.currencyCode
                    )
                )
            )
        )

    override fun onSyncProgressUpdated(
        model: M,
        event: E.OnSyncProgressUpdated
    ): Next<M, F> =
        next(
            model.copy(
                isSyncing = event.isSyncing,
                syncProgress = event.progress,
                syncingThroughMillis = event.syncThroughMillis
            )
        )

    override fun onQueryChanged(
        model: M,
        event: E.OnQueryChanged
    ): Next<M, F> =
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
        model: M,
        event: E.OnCurrencyNameUpdated
    ): Next<M, F> =
        next(
            model.copy(
                currencyName = event.name
            )
        )

    override fun onBrdRewardsUpdated(
        model: M,
        event: E.OnBrdRewardsUpdated
    ): Next<M, F> =
        next(
            model.copy(
                isShowingBrdRewards = event.showing
            )
        )

    override fun onBalanceUpdated(
        model: M,
        event: E.OnBalanceUpdated
    ): Next<M, F> =
        next(
            model.copy(
                balance = event.balance,
                fiatBalance = event.fiatBalance
            )
        )

    override fun onFiatPricePerUpdated(
        model: M,
        event: E.OnFiatPricePerUpdated
    ): Next<M, F> =
        next(
            model.copy(
                fiatPricePerUnit = event.pricePerUnit,
                priceChange = event.priceChange
            )
        )

    override fun onIsTokenSupportedUpdated(
        model: M,
        event: E.OnIsTokenSupportedUpdated
    ): Next<M, F> =
        next(
            model.copy(
                isShowingDelistedBanner = !event.isTokenSupported
            )
        )

    override fun onCryptoTransactionsUpdated(
        model: M,
        event: E.OnCryptoTransactionsUpdated
    ): Next<M, F> =
        if (event.transactions.isNotEmpty()) {
            dispatch(
                setOf(
                    F.ConvertCryptoTransactions(
                        event.transactions
                    )
                )
            )
        } else {
            noChange()
        }

    override fun onTransactionsUpdated(
        model: M,
        event: E.OnTransactionsUpdated
    ): Next<M, F> =
        if (model.transactions.isNullOrEmpty() && event.walletTransactions.isNotEmpty()) {
            val txHashes = event.walletTransactions.take(TX_METADATA_PREFETCH).map { it.txHash }
            next(
                model.copy(transactions = event.walletTransactions),
                effects(
                    F.LoadTransactionMetaData(model.currencyCode, txHashes),
                    F.LoadTransactionMetaDataSingle(
                        model.currencyCode,
                        event.walletTransactions.map(WalletTransaction::txHash)
                    )
                )
            )
        } else {
            val newHashes = mutableListOf<String>()
            next(
                model.copy(
                    transactions = event.walletTransactions
                        .mapIndexed { index, walletTransaction ->
                            val oldTx = model.transactions.getOrNull(index)
                            if (oldTx == null) {
                                newHashes.add(walletTransaction.txHash)
                            }

                            if (oldTx?.memo != null) {
                                walletTransaction.copy(
                                    memo = oldTx.memo
                                )
                            } else walletTransaction
                        }
                ),
                effects(
                    F.LoadTransactionMetaDataSingle(model.currencyCode, newHashes)
                )
            )
        }

    override fun onTransactionMetaDataUpdated(
        model: M,
        event: E.OnTransactionMetaDataUpdated
    ): Next<M, F> {
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

    override fun onTransactionMetaDataLoaded(
        model: M,
        event: E.OnTransactionMetaDataLoaded
    ): Next<M, F> {
        return next(
            model.copy(
                transactions = model.transactions
                    .map { transaction ->
                        val metadata = event.metadata[transaction.txHash]
                        transaction.copy(
                            memo = when (metadata) {
                                is TxMetaDataValue -> metadata.comment ?: ""
                                is TxMetaDataEmpty -> ""
                                null -> transaction.memo
                            }
                        )
                    }
            )
        )
    }

    override fun onTransactionAdded(
        model: M,
        event: E.OnTransactionAdded
    ): Next<M, F> =
        if (model.transactions.isNullOrEmpty())
            next(
                model.copy(transactions = listOf(event.walletTransaction) + model.transactions)
            )
        else
            next(
                model.copy(
                    transactions = listOf(event.walletTransaction) + model.transactions
                )
            )

    override fun onVisibleTransactionsChanged(
        model: M,
        event: E.OnVisibleTransactionsChanged
    ): Next<M, F> =
        dispatch(effects(F.LoadTransactionMetaData(model.currencyCode, event.transactionHashes)))

    override fun onTransactionRemoved(
        model: M,
        event: E.OnTransactionRemoved
    ): Next<M, F> =
        next(
            model.copy(
                transactions = model.transactions - event.walletTransaction
            )
        )

    override fun onConnectionUpdated(
        model: M,
        event: E.OnConnectionUpdated
    ): Next<M, F> =
        next(
            model.copy(
                hasInternet = event.isConnected
            )
        )

    override fun onSendRequestGiven(
        model: M,
        event: E.OnSendRequestGiven
    ): Next<M, F> =
        dispatch(
            effects(
                F.Nav.GoToSend(model.currencyCode, event.cryptoRequest)
            )
        )

    override fun onTransactionClicked(
        model: M,
        event: E.OnTransactionClicked
    ): Next<M, F> =
        dispatch(
            effects(
                F.Nav.GoToTransaction(model.currencyCode, event.txHash)
            )
        )

    override fun onIsCryptoPreferredLoaded(
        model: M,
        event: E.OnIsCryptoPreferredLoaded
    ): Next<M, F> =
        next(model.copy(isCryptoPreferred = event.isCryptoPreferred))

    override fun onChartIntervalSelected(
        model: M,
        event: E.OnChartIntervalSelected
    ): Next<M, F> =
        next(
            model.copy(
                priceChartInterval = event.interval
            ),
            effects(
                F.LoadChartInterval(event.interval, model.currencyCode),
                F.TrackEvent(
                    String.format(
                        EventUtils.EVENT_WALLET_CHART_AXIS_TOGGLE,
                        model.currencyCode
                    )
                )
            )
        )

    override fun onMarketChartDataUpdated(
        model: M,
        event: E.OnMarketChartDataUpdated
    ): Next<M, F> =
        next(
            model.copy(
                priceChartIsLoading = false,
                priceChartDataPoints = event.priceDataPoints
            )
        )

    override fun onMarketDataUpdated(
        model: M,
        event: E.OnMarketDataUpdated
    ): Next<M, F> = when (event.marketData) {
        is MarketDataResult.Success -> {
            next(
                model.copy(
                    marketCap = event.marketData.marketCap,
                    totalVolume = event.marketData.totalVolume,
                    high24h = event.marketData.high24h,
                    low24h = event.marketData.low24h,
                    marketDataState = MarketDataState.LOADED
                )
            )
        }
        else -> next(
            model.copy(
                marketDataState = MarketDataState.ERROR
            )
        )
    }

    override fun onChartDataPointSelected(
        model: M,
        event: E.OnChartDataPointSelected
    ): Next<M, F> =
        next(
            model.copy(selectedPriceDataPoint = event.priceDataPoint)
        )

    override fun onWalletStateUpdated(model: M, event: E.OnWalletStateUpdated): Next<M, F> =
        when (event.walletState) {
            WalletState.Error -> next<M, F>(
                model.copy(state = event.walletState),
                setOf(F.ShowCreateAccountErrorDialog)
            )
            else -> next<M, F>(model.copy(state = event.walletState))
        }

    override fun onCreateAccountClicked(model: M): Next<M, F> =
        dispatch(effects(F.ShowCreateAccountDialog)) // TODO: disable button

    override fun onCreateAccountConfirmationClicked(model: M): Next<M, F> =
        dispatch(effects(F.CreateAccount(model.currencyCode)))
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
