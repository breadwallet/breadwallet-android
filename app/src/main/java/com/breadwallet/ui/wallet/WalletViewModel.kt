/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 3/14/19.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.wallet

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.text.format.DateUtils
import android.util.Log
import com.breadwallet.model.PriceChange
import com.breadwallet.presenter.entities.TxUiHolder
import com.breadwallet.repository.WalletRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.sqlite.RatesDataSource
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.wallet.model.Balance
import com.breadwallet.ui.wallet.model.TxFilter
import com.breadwallet.wallet.WalletsMaster
import com.breadwallet.wallet.abstracts.BalanceUpdateListener
import com.breadwallet.wallet.abstracts.OnTxListModified
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.repository.RatesRepository
import com.breadwallet.wallet.util.SyncUpdateHandler
import com.breadwallet.wallet.wallets.CryptoTransaction
import com.platform.network.service.CurrencyHistoricalDataClient
import com.platform.tools.KVStoreManager
import com.platform.util.AppReviewPromptManager
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * ViewModel encapsulating WalletActivity's balance and transactions refresh.
 */
class WalletViewModel(application: Application) : AndroidViewModel(application) {

    val balanceLiveData = MutableLiveData<Balance>()
    val txListLiveData = MutableLiveData<List<TxUiHolder>>()
    val requestFeedbackLiveData = MutableLiveData<Boolean>()
    val priceDataPoints = MutableLiveData<List<PriceDataPoint>>()
    val priceChange = MutableLiveData<PriceChange>()
    val progressLiveData = Transformations.map(WalletRepository.getInstance(application).walletsLiveData)
    { wallets ->
        val wallet = wallets.findLast { wallet -> wallet.currencyCode == targetCurrencyCode }
        if (wallet?.isSyncing == true) wallet.syncProgress else SyncUpdateHandler.PROGRESS_FINISH
    }
    var targetCurrencyCode = ""
        set(value) {
            field = value
            WalletsMaster.getInstance().getWalletByIso(getApplication(), value)
                    .refreshAddress(getApplication())
        }
    var filter: TxFilter = TxFilter()
        set(value) {
            field = value
            // Apply filter out of the main thread.
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
                txListLiveData.postValue(filteredTxList())
            }
        }
    var chartInterval = MutableLiveData<Interval>().apply { value = Interval.ONE_DAY }
    private var mTxList = listOf<TxUiHolder>()
    private val mBalanceListener: BalanceUpdateListener
    private val mRatesListener: RatesDataSource.OnDataChanged
    private val mTxModifiedListener: OnTxListModified

    companion object {
        private val TAG = WalletViewModel::class.java.toString()
        private const val REVIEW_PROMPT_DELAY_SECONDS = 3L
    }

    init {
        mBalanceListener = object : BalanceUpdateListener {
            override fun onBalanceChanged(currencyCode: String, newBalance: BigDecimal) {
                if (currencyCode == targetCurrencyCode && balanceLiveData.hasActiveObservers()) {
                    refreshBalanceAndTxList()
                }
            }

            override fun onBalancesChanged(balanceMap: Map<String, BigDecimal>) {
                if (balanceMap.containsKey(targetCurrencyCode) && balanceLiveData.hasActiveObservers()) {
                    refreshBalanceAndTxList()
                }
            }
        }
        WalletsMaster.getInstance().addBalanceUpdateListener(mBalanceListener)
        mRatesListener = RatesDataSource.OnDataChanged { refreshBalanceAndTxList() }
        RatesDataSource.getInstance(application).addOnDataChangedListener(mRatesListener)
        mTxModifiedListener = OnTxListModified { refreshBalanceAndTxList() }
        WalletsMaster.getInstance().getCurrentWallet(getApplication())
                .addTxListModifiedListener(mTxModifiedListener)
    }

    @Override
    override fun onCleared() {
        super.onCleared()
        WalletsMaster.getInstance().addBalanceUpdateListener(mBalanceListener)
        RatesDataSource.getInstance(getApplication()).removeOnDataChangedListener(mRatesListener)
        WalletsMaster.getInstance().getCurrentWallet(getApplication())
                .removeTxListModifiedListener(mTxModifiedListener)
    }

    fun getInterval(interval: Interval) {
        val context = (getApplication() as Application).applicationContext
        val ratesRepository = RatesRepository.getInstance(context)
        chartInterval.value = interval
        BRExecutor.getInstance()
                .forLightWeightBackgroundTasks()
                .execute {
                    val toCurrency: String = BRSharedPrefs.getPreferredFiatIso()
                            ?: Currency.getInstance(Locale.US).currencyCode
                    priceDataPoints.postValue(ratesRepository.getHistoricalData(targetCurrencyCode, toCurrency, interval))
                }
    }

    private fun refreshPriceChange() {
        val context = getApplication<Application>()
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            val refCurrency = BRSharedPrefs.getPreferredFiatIso()
                    ?: Currency.getInstance(Locale.US).currencyCode
            val response = CurrencyHistoricalDataClient.fetch24HrsChange(context, listOf(targetCurrencyCode), refCurrency)
            priceChange.postValue(response[targetCurrencyCode])
        }
    }

    /**
     * Refresh the wallet's balance and transactions list.
     */
    fun refreshBalanceAndTxList() {
        val context = getApplication<Application>()
        val walletManager = WalletsMaster.getInstance().getCurrentWallet(context)
        val bigExchangeRate = walletManager.getFiatExchangeRate(context)

        val fiatExchangeRate = CurrencyUtils.getFormattedAmount(context,
                BRSharedPrefs.getPreferredFiatIso(context), bigExchangeRate)
        val fiatBalance = CurrencyUtils.getFormattedAmount(context,
                BRSharedPrefs.getPreferredFiatIso(context), walletManager.getFiatBalance(context))
        val cryptoBalance = CurrencyUtils.getFormattedAmount(context,
                walletManager.currencyCode, walletManager.balance,
                walletManager.uiConfiguration.maxDecimalPlacesForUi, false)
        val balance = Balance(targetCurrencyCode, bigExchangeRate, fiatExchangeRate,
                fiatBalance, cryptoBalance)
        balanceLiveData.postValue(balance)
        priceChange.postValue(RatesRepository.getInstance(context).getPriceChange(targetCurrencyCode))
        refreshTxList()
        refreshPriceChange()
    }

    /**
     * Save that the user dismissed the review prompt.
     */
    fun onRateAppPromptDismissed() = AppReviewPromptManager.onReviewPromptDismissed(getApplication())

    /**
     * Refresh the wallet's transactions list.
     */
    private fun refreshTxList() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            // TODO this code belongs to a repository
            val wallet = WalletsMaster.getInstance().getCurrentWallet(getApplication())
            if (wallet == null) {
                Log.e(TAG, "updateTxList: wallet is null")
            }
            val walletManager = WalletsMaster.getInstance().getCurrentWallet(getApplication())
            val transactionsList = mutableListOf<TxUiHolder>()
            for (txUiHolder in wallet!!.getTxUiHolders(getApplication()).orEmpty()) {
                var txMetaData = KVStoreManager.getTxMetaData(getApplication(), txUiHolder.txHash)
                if (System.currentTimeMillis() - txUiHolder.timeStamp < DateUtils.HOUR_IN_MILLIS) {
                    if (txMetaData == null) {
                        txMetaData = KVStoreManager.createMetadata(getApplication(), walletManager,
                                CryptoTransaction(txUiHolder.transaction))
                        KVStoreManager.putTxMetaData(getApplication(), txMetaData, txUiHolder.txHash)
                    } else if (txMetaData.exchangeRate == 0.0) {
                        Log.d(TAG, "MetaData not null")
                        txMetaData.exchangeRate = walletManager.getFiatExchangeRate(getApplication()).toDouble()
                        txMetaData.exchangeCurrency = BRSharedPrefs.getPreferredFiatIso(getApplication())
                        KVStoreManager.putTxMetaData(getApplication(), txMetaData, txUiHolder.txHash)
                    }
                }
                txUiHolder.metaData = txMetaData
                transactionsList.add(txUiHolder)
            }

            // The first time we get the list of transactions check if we should request the user to
            // rate the app in Google Play.
            if (mTxList.isEmpty() && transactionsList.isNotEmpty()) {
                checkIfShouldShowReviewPrompt()
            }

            mTxList = transactionsList
            txListLiveData.postValue(filteredTxList())
        }
    }


    private fun filteredTxList(): List<TxUiHolder> {
        // TODO this code belongs to a repository
        val wallet = WalletsMaster.getInstance().getCurrentWallet(getApplication())
        if (wallet == null) {
            Log.e("", "updateTxList: wallet is null")
        }
        return if (Utils.isNullOrEmpty(filter.query) && !filter.completed && !filter.pending
                && !filter.received && !filter.sent) {
            mTxList
        } else {
            mTxList.filter { item ->
                val matchesHash = item.hashReversed != null && item.hashReversed.contains(filter.query)
                val matchesAddress = item.from.contains(filter.query) || item.to.contains(filter.query)
                val matchesMemo = (item.metaData != null && item.metaData.comment != null
                        && item.metaData.comment.toLowerCase().contains(filter.query))
                var willAdd = false
                if (matchesHash || matchesAddress || matchesMemo) {
                    willAdd = true
                    //filter by sent and this is received
                    if (filter.sent && item.isReceived) {
                        willAdd = false
                    }
                    //filter by received and this is sent
                    if (filter.received && !item.isReceived) {
                        willAdd = false
                    }

                    val isComplete = item.isComplete(getApplication(), wallet.currencyCode)

                    //complete
                    if (filter.pending && isComplete) {
                        willAdd = false
                    }
                    //pending
                    if (filter.completed && !isComplete) {
                        willAdd = false
                    }
                }
                willAdd
            }
        }
    }

    private fun checkIfShouldShowReviewPrompt() {
        // Done with a delay to let the user see the the list of the transactions before showing the prompt
        Executors.newSingleThreadScheduledExecutor().schedule({
            val showPrompt = AppReviewPromptManager.showReview(getApplication(), targetCurrencyCode, mTxList)
            requestFeedbackLiveData.postValue(showPrompt)
        }, REVIEW_PROMPT_DELAY_SECONDS, TimeUnit.SECONDS)
    }

}