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
import android.util.Log;

import com.breadwallet.model.Wallet;
import com.breadwallet.repository.WalletRepository;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BalanceUpdateListener;

import java.math.BigDecimal;
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

    private BalanceUpdater mBalanceUpdater;
    private ExchangeRatesUpdater mExchangeRatesUpdater;
    private ConnectionListener mConnectionListener;
    private WalletRepository mWalletRepository;

    /**
     * Constructor initializes data, registers listeners, and kicks off various
     * background retrieval tasks.
     *
     * @param app The application context.
     */
    public MainViewModel(final Application app) {
        super(app);

        mWalletRepository = WalletRepository.Companion.getInstance(app);
        // Retrieve and initialize wallet data (i.e., wallet names and currency code)
        mWalletRepository.refreshWallets();

        // Register as balance and rate update listeners
        mBalanceUpdater = new BalanceUpdater();
        mExchangeRatesUpdater = new ExchangeRatesUpdater();
        WalletsMaster.getInstance().addBalanceUpdateListener(mBalanceUpdater);
        RatesDataSource.getInstance(app).addOnDataChangedListener(mExchangeRatesUpdater);

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
        return mWalletRepository.getWalletsLiveData();
    }

    /**
     * Returns the aggregated fiat balance as an observable Live Data.
     *
     * @return The aggregated fiat balance wrapped in LiveData.
     */
    public LiveData<BigDecimal> getAggregatedFiatBalance() {
        return mWalletRepository.getAggregatedFiatBalance();
    }

    /**
     * Refresh the list of wallets.
     */
    public void refreshWallets() {
        mWalletRepository.refreshWallets();
        refreshBalances();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // De-register listeners
        WalletsMaster.getInstance().removeBalanceUpdateListener(mBalanceUpdater);
        RatesDataSource.getInstance(getApplication()).removeOnDataChangedListener(mExchangeRatesUpdater);
        InternetManager.unregisterConnectionReceiver(getApplication(), mConnectionListener);
    }

    /**
     * Invokes wallet balance refreshes in a background thread.
     */
    private void refreshBalances() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            mWalletRepository.refreshBalances();
        });
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
            Log.i(TAG, "onBalanceChanged: new update for " + currencyCode + " with balance " + newBalance);
            Map<String, BigDecimal> mapBalance = new HashMap<>();
            mapBalance.put(currencyCode, newBalance);
            onBalancesChanged(mapBalance);
        }

        @Override
        public void onBalancesChanged(Map<String, BigDecimal> balanceMap) {
            mWalletRepository.updateBalances(balanceMap, getApplication());
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
            mWalletRepository.refreshBalances();
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
                mWalletRepository.startNextWalletSync(getApplication());
            }
        }
    }
}
