/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 3/26/19.
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
package com.breadwallet.repository

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import com.breadwallet.core.BRCorePeer
import com.breadwallet.model.Wallet
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.wallet.WalletsMaster
import com.breadwallet.wallet.abstracts.BaseWalletManager
import com.breadwallet.wallet.util.SyncUpdateHandler
import java.math.BigDecimal
import java.util.ArrayList
import java.util.HashMap

/**
 * Repository that handles wallets data.
 */
class WalletRepository private constructor() {

    val walletsLiveData = MutableLiveData<List<Wallet>>()
    val aggregatedFiatBalance = MutableLiveData<BigDecimal>()
    private var mCurrencyToWalletManager = hashMapOf<String, BaseWalletManager>()
    private var mWallets = listOf<Wallet>()
    private var mContext: Context? = null

    companion object {
        private val TAG = WalletRepository::class.java.toString()
        private var INSTANCE: WalletRepository? = null
        fun getInstance(context: Context): WalletRepository =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildWalletRepository(context).also { INSTANCE = it }
                }

        private fun buildWalletRepository(context: Context): WalletRepository {
            return WalletRepository().apply {
                mContext = context
                refreshWallets()
            }
        }
    }

    /**
     * Post an update over the synchronization progress.
     */
    fun updateProgress(currencyCode: String, progress: Double) {
        mWallets.findLast { it.currencyCode == currencyCode }?.apply {
            syncProgress = progress
            setIsSyncing(progress > SyncUpdateHandler.PROGRESS_START
                    && progress < SyncUpdateHandler.PROGRESS_FINISH)
        }
        if (progress == SyncUpdateHandler.PROGRESS_FINISH) {
            Log.d(TAG, "updateProgress: $currencyCode sync completed, starting next wallet sync")
            mContext?.let { startNextWalletSync(it) }
        }
        walletsLiveData.postValue(mWallets)
    }

    /**
     * Retrieves and initializes the list of wallets.
     */
    fun refreshWallets() {
        val walletManagerList = WalletsMaster.getInstance().getAllWallets(mContext)

        val currencyToWalletManager = HashMap<String, BaseWalletManager>()

        // Instantiate wallets and update live data
        val wallets = ArrayList<Wallet>()
        for (walletManager in walletManagerList) {
            currencyToWalletManager[walletManager.currencyCode] = walletManager

            val wallet = Wallet(walletManager.name, walletManager.currencyCode)
            wallets.add(wallet)
        }
        mWallets = wallets
        mCurrencyToWalletManager = currencyToWalletManager
        refreshBalances()
    }

    /**
     * Refresh all wallet's balances.
     */
    fun refreshBalances() {
        WalletsMaster.getInstance().refreshBalances()
        // Also, invoke refresh of "current" wallet's address
        WalletsMaster.getInstance().getCurrentWallet(mContext)?.refreshAddress(mContext)

        for (wallet in mWallets) {
            val walletManager = mCurrencyToWalletManager[wallet.currencyCode]

            if (walletManager != null) {
                wallet.exchangeRate = walletManager.getFiatExchangeRate(mContext)
                wallet.fiatBalance = walletManager.getFiatBalance(mContext)
                wallet.cryptoBalance = walletManager.balance
                wallet.priceChange = RatesRepository.getInstance(mContext).getPriceChange(wallet.currencyCode)
            } else {
                Log.e(TAG, "refreshBalances: No wallet manager for currency code: ${wallet.currencyCode}")
            }
        }

        // Get aggregated fiat balance
        val fiatTotalAmount = WalletsMaster.getInstance().getAggregatedFiatBalance(mContext)
        aggregatedFiatBalance.postValue(fiatTotalAmount)

        walletsLiveData.postValue(mWallets)
    }

    /**
     * Update the wallets with the new balances.
     *
     * @param balanceMap Map where key is the currency code and the value is the new balance.
     */
    fun updateBalances(balanceMap: Map<String, BigDecimal>, context: Context) {
        for (wallet in mWallets) {
            if (balanceMap.containsKey(wallet.currencyCode)) {
                val walletManager = mCurrencyToWalletManager[wallet.currencyCode]
                if (walletManager != null) {
                    wallet.exchangeRate = walletManager.getFiatExchangeRate(context)
                    wallet.fiatBalance = walletManager.getFiatBalance(context)
                    wallet.cryptoBalance = balanceMap[wallet.currencyCode]
                } else {
                    Log.e(TAG, "updateBalances: No wallet manager for currency code: ${wallet.currencyCode}")
                }
            }
        }
        walletsLiveData.postValue(mWallets)
        aggregatedFiatBalance.postValue(WalletsMaster.getInstance().getAggregatedFiatBalance(context))
    }


    /**
     * Determines the next wallet to sync, and if found, initiates the sync (all in a background
     * thread).
     */
    fun startNextWalletSync(context: Context) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(Runnable {
            // Choosing the next wallet to sync:
            // 1. If any wallet is already sync'ing, then exit (job is done)!
            // 2. Skip any wallets that are already sync'ed
            // 3. Prioritize the "current" wallet over remaining wallets
            var syncWallet: Wallet? = null
            val currentWallet = WalletsMaster.getInstance().getCurrentWallet(context)
            for (wallet in mWallets) {
                if (wallet.isSyncing) {
                    return@Runnable  // exit -- do not set next wallet to sync
                }

                if (wallet.syncProgress == SyncUpdateHandler.PROGRESS_FINISH) {
                    continue // skip this wallet
                }

                if (currentWallet != null
                        && currentWallet.currencyCode.equals(wallet.currencyCode, ignoreCase = true)) {
                    syncWallet = wallet // prioritize "current" wallet
                    break
                }

                if (syncWallet == null) {
                    val candidateWalletManager = mCurrencyToWalletManager[wallet.currencyCode]
                    if (candidateWalletManager?.connectStatus != BRCorePeer.ConnectStatus.Connected.value.toDouble()) {
                        syncWallet = wallet
                    }
                }
            }

            // Initiate sync, and listen for updates for this currency code
            if (syncWallet != null) {
                val walletManager = mCurrencyToWalletManager[syncWallet.currencyCode]
                walletManager!!.connect(context)
                SyncUpdateHandler.startWalletSync(context, syncWallet.currencyCode)
            }
        })
    }
}
