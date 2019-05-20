/**
 * BreadWallet
 * <p/>
 * Created by Ahsan Butt on <ahsan.butt@breadwallet.com> 2/12/19.
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

package com.breadwallet.presenter.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.breadwallet.core.BRCorePeer;
import com.breadwallet.model.Wallet;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.services.SyncService;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BalanceUpdateListener;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.google.common.collect.Maps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MainViewModel encapsulates the wallet-related logic for a Wallet-centric Activity
 * (e.g., HomeActivity). It listens for balance and rate updates, providing updates back
 * to the Activity in a life-cycle aware manner using LiveData. It also initiates the
 * sync'ing of wallets, one at a time.
 */
public class MainViewModel extends AndroidViewModel {
    private static final String TAG = MainViewModel.class.getSimpleName();

    private MutableLiveData<List<Wallet>> mWallets;
    private MutableLiveData<BigDecimal> mAggregatedFiatBalance;
    private Map<String, BaseWalletManager> mCurrencyToWalletManager;

    private BalanceUpdater mBalanceUpdater;
    private ExchangeRatesUpdater mExchangeRatesUpdater;
    private SyncNotificationBroadcastReceiver mSyncReceiver;
    private ConnectionListener mConnectionListener;

    /**
     * Constructor initializes data, registers listeners, and kicks off various
     * background retrieval tasks.
     *
     * @param app The application context.
     */
    public MainViewModel(final Application app) {
        super(app);

        mWallets = new MutableLiveData<>();
        mAggregatedFiatBalance = new MutableLiveData<>();

        // Retrieve and initialize wallet data (i.e., wallet names and currency code)
        initializeWalletsAndManagers();

        // Invoke balance refresh
        refreshBalances();

        // Register as balance and rate update listeners
        mBalanceUpdater = new BalanceUpdater();
        mExchangeRatesUpdater = new ExchangeRatesUpdater();
        WalletsMaster.getInstance().addBalanceUpdateListener(mBalanceUpdater);
        RatesDataSource.getInstance(app).addOnDataChangedListener(mExchangeRatesUpdater);

        // Register as sync notification receiver
        mSyncReceiver = new SyncNotificationBroadcastReceiver();
        SyncService.registerSyncNotificationBroadcastReceiver(app, mSyncReceiver);

        // Register for changes to connectivity -- if connected, will invoke sync'ing process
        mConnectionListener = new ConnectionListener();
        InternetManager.registerConnectionReceiver(app, mConnectionListener);
        mConnectionListener.onConnectionChanged(InternetManager.getInstance().isConnected(app));
    }

    /**
     * Returns the list of wallets as an observable Live Data.
     *
     * @return The list of wallets wrapped in LiveData.
     */
    public LiveData<List<Wallet>> getWallets() {
        return mWallets;
    }

    /**
     * Returns the aggregated fiat balance as an observable Live Data.
     *
     * @return The aggregated fiat balance wrapped in LiveData.
     */
    public LiveData<BigDecimal> getAggregatedFiatBalance() {
        return mAggregatedFiatBalance;
    }

    /**
     * Refresh the list of wallets.
     */
    public void refreshWallets() {
        initializeWalletsAndManagers();
        refreshBalances();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // De-register listeners
        WalletsMaster.getInstance().removeBalanceUpdateListener(mBalanceUpdater);
        RatesDataSource.getInstance(getApplication()).removeOnDataChangedListener(mExchangeRatesUpdater);
        SyncService.unregisterSyncNotificationBroadcastReceiver(getApplication(), mSyncReceiver);
        InternetManager.unregisterConnectionReceiver(getApplication(), mConnectionListener);
    }

    /**
     * Retrieves and initializes the list of wallets.
     */
    private void initializeWalletsAndManagers() {
        // Retrieve the wallets
        List<BaseWalletManager> walletManagerList =
                WalletsMaster.getInstance().getAllWallets(getApplication());

        mCurrencyToWalletManager = new HashMap<>();

        // Instantiate wallets and update live data
        List<Wallet> wallets = new ArrayList<>();
        for (BaseWalletManager walletManager : walletManagerList) {
            mCurrencyToWalletManager.put(walletManager.getCurrencyCode(), walletManager);

            Wallet wallet = new Wallet(walletManager.getName(), walletManager.getCurrencyCode());
            wallets.add(wallet);
        }

        setWalletsLiveData(wallets);
    }

    /**
     * Invokes wallet balance refreshes in a background thread.
     */
    private void refreshBalances() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                WalletsMaster.getInstance().refreshBalances();

                // Also, invoke refresh of "current" wallet's address
                BaseWalletManager currentWalletManager = WalletsMaster.getInstance().getCurrentWallet(getApplication());
                if (currentWalletManager != null) {
                    currentWalletManager.refreshAddress(getApplication());
                }

                // Update our wallet data after refreshes
                updateWallets();
            }
        });
    }

    /**
     * Retrieves wallet balances and rates, and propagates updates to wallet observers.
     */
    private void updateWallets() {
        List<Wallet> wallets = mWallets.getValue();

        if (wallets == null) {
            return;
        }

        for (Wallet wallet : wallets) {
            BaseWalletManager walletManager = mCurrencyToWalletManager.get(wallet.getCurrencyCode());

            if (walletManager != null) {
                wallet.setExchangeRate(walletManager.getFiatExchangeRate(getApplication()));
                wallet.setFiatBalance(walletManager.getFiatBalance(getApplication()));
                wallet.setCryptoBalance(walletManager.getBalance());
            } else {
                Log.e(TAG, "No wallet manager for currency code: " + wallet.getCurrencyCode());
            }
        }

        // Get aggregated fiat balance
        BigDecimal fiatTotalAmount =
                WalletsMaster.getInstance().getAggregatedFiatBalance(getApplication());

        setWalletsLiveData(wallets);
        setAggregatedFiatBalanceLiveData(fiatTotalAmount);
    }

    /**
     * Updates the wallet's sync progress, specified by the currency code.
     *
     * @param currencyCode The currency code, which identifies the sync'ing wallet.
     * @param progress     The sync progress, between 0 and 1, indicating percentage complete.
     */
    private void updateSyncProgress(String currencyCode, double progress) {
        List<Wallet> wallets = mWallets.getValue();

        if (wallets == null) {
            return;
        }

        Wallet currentWallet = null;
        for (Wallet wallet : wallets) {
            if (wallet.getCurrencyCode().equalsIgnoreCase(currencyCode)) {
                currentWallet = wallet;
                break;
            }
        }

        // If no matching wallet found (weird) or progress hasn't changed, then exit
        if (currentWallet == null || currentWallet.getSyncProgress() == progress) {
            return;
        }

        if (progress > SyncService.PROGRESS_START && progress < SyncService.PROGRESS_FINISH) {
            // Sync in progress
            currentWallet.setIsSyncing(true);
            currentWallet.setSyncProgress(progress);
        } else if (progress == SyncService.PROGRESS_FINISH) {
            // Sync complete
            currentWallet.setIsSyncing(false);
            currentWallet.setSyncProgress(progress);

            // Start next sync
            startNextWalletSync();
        }

        setWalletsLiveData(wallets);
    }

    /**
     * Propagates the given wallet data to its observers.
     *
     * @param wallets The list of wallet data.
     */
    private void setWalletsLiveData(List<Wallet> wallets) {
        if (UiUtils.isMainThread()) {
            mWallets.setValue(wallets);
        } else {
            mWallets.postValue(wallets);
        }
    }

    /**
     * Propagates the given aggregated fiat balance data to its observers.
     *
     * @param aggregatedFiatBalance The aggregated fiat balance.
     */
    private void setAggregatedFiatBalanceLiveData(BigDecimal aggregatedFiatBalance) {
        if (UiUtils.isMainThread()) {
            mAggregatedFiatBalance.setValue(aggregatedFiatBalance);
        } else {
            mAggregatedFiatBalance.postValue(aggregatedFiatBalance);
        }
    }

    /**
     * Determines the next wallet to sync, and if found, initiates the sync (all in a background thread).
     */
    private void startNextWalletSync() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                List<Wallet> wallets = mWallets.getValue();

                if (wallets == null) {
                    return;
                }

                // Choosing the next wallet to sync:
                // 1. If any wallet is already sync'ing, then exit (job is done)!
                // 2. Skip any wallets that are already sync'ed
                // 3. Prioritize the "current" wallet over remaining wallets
                Wallet syncWallet = null;
                BaseWalletManager currentWallet =
                        WalletsMaster.getInstance().getCurrentWallet(getApplication());
                for (Wallet wallet : wallets) {
                    if (wallet.isSyncing()) {
                        return; // exit -- do not set next wallet to sync
                    }

                    if (wallet.getSyncProgress() == SyncService.PROGRESS_FINISH) {
                        continue; // skip this wallet
                    }

                    if (currentWallet != null
                            && currentWallet.getCurrencyCode().equalsIgnoreCase(wallet.getCurrencyCode())) {
                        syncWallet = wallet; // prioritize "current" wallet
                        break;
                    }

                    if (syncWallet == null) {
                        BaseWalletManager candidateWalletManager =
                                mCurrencyToWalletManager.get(wallet.getCurrencyCode());
                        if (candidateWalletManager.getConnectStatus() != BRCorePeer.ConnectStatus.Connected.getValue()) {
                            syncWallet = wallet;
                        }
                    }
                }

                // Initiate sync, and listen for updates for this currency code
                if (syncWallet != null) {
                    BaseWalletManager walletManager = mCurrencyToWalletManager.get(syncWallet.getCurrencyCode());
                    walletManager.connect(getApplication());
                    SyncService.startService(getApplication(), syncWallet.getCurrencyCode());
                }
            }
        });
    }

    /**
     * The {@link SyncNotificationBroadcastReceiver} is responsible for receiving updates from the
     * {@link SyncService}
     */
    private class SyncNotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SyncService.ACTION_SYNC_PROGRESS_UPDATE.equals(intent.getAction())) {
                String intentCurrencyCode = intent.getStringExtra(SyncService.EXTRA_WALLET_CURRENCY_CODE);
                double progress = intent.getDoubleExtra(SyncService.EXTRA_PROGRESS, SyncService.PROGRESS_NOT_DEFINED);

                if (progress >= SyncService.PROGRESS_START) {
                    updateSyncProgress(intentCurrencyCode, progress);
                } else {
                    Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: Progress not set:" + progress);
                }
            }
        }
    }

    /**
     * The Balance Updater is responsible for receiving balance updates and invoking an update
     * to the wallets.
     */
    private class BalanceUpdater implements BalanceUpdateListener {
        /**
         * Handler method for balance updates.
         *
         * @param newBalance The updated balance.
         */
        @Override
        public void onBalanceChanged(String currencyCode, BigDecimal newBalance) {
            Log.w(TAG, "onBalanceChanged: new update for " + currencyCode + " with balance " + newBalance);
            Map<String, BigDecimal> mapBalance = new HashMap<>();
            mapBalance.put(currencyCode, newBalance);
            onBalancesChanged(mapBalance);
        }

        @Override
        public void onBalancesChanged(Map<String, BigDecimal> balanceMap) {
            List<Wallet> wallets = mWallets.getValue();
            if (wallets == null) {
                return;
            }
            for (Wallet wallet : wallets) {
                if (balanceMap.containsKey(wallet.getCurrencyCode())) {
                    BaseWalletManager walletManager = mCurrencyToWalletManager.get(wallet.getCurrencyCode());
                    if (walletManager != null) {
                        wallet.setExchangeRate(walletManager.getFiatExchangeRate(getApplication()));
                        wallet.setFiatBalance(walletManager.getFiatBalance(getApplication()));
                        wallet.setCryptoBalance(balanceMap.get(wallet.getCurrencyCode()));
                    } else {
                        Log.e(TAG, "onBalancesChanged: No wallet manager for currency code: " + wallet.getCurrencyCode());
                    }
                }
            }

            // Get aggregated fiat balance
            BigDecimal fiatTotalAmount =
                    WalletsMaster.getInstance().getAggregatedFiatBalance(getApplication());

            setWalletsLiveData(wallets);
            setAggregatedFiatBalanceLiveData(fiatTotalAmount);
        }
    }

    /**
     * The Exchange Rates Updater is responsible for receiving rate updates and invoking an update
     * to the wallets.
     */
    private class ExchangeRatesUpdater implements RatesDataSource.OnDataChanged {
        /**
         * Handler method for rate updates.
         */
        @Override
        public void onChanged() {
            updateWallets();
        }
    }

    /**
     * The Connection Listener is responsible for receiving connectivity changes and invoking
     * the wallet sync logic.
     */
    private class ConnectionListener implements InternetManager.ConnectionReceiverListener {
        /**
         * Handler method for change in connectivity.
         *
         * @param isConnected Indicates whether we are connected.
         */
        @Override
        public void onConnectionChanged(boolean isConnected) {
            if (isConnected) {
                startNextWalletSync();
            }
        }
    }
}
