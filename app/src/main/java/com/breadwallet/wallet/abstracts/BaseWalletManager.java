package com.breadwallet.wallet.abstracts;

import android.content.Context;
import android.support.annotation.WorkerThread;

import com.breadwallet.core.ethereum.BREthereumAmount;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.wallets.CryptoAddress;
import com.breadwallet.wallet.wallets.CryptoTransaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/22/18.
 * Copyright (c) 2018 breadwallet LLC
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
public interface BaseWalletManager {

    /**
     * The methods that are annotated with @WorkerThread might block so can't be called in the UI Thread
     */

    public interface OnHashUpdated {
        void onUpdated(String hash);
    }

    //get the core wallet
    int getForkId();

    //get the currency unit ETHER_WEI...
    BREthereumAmount.Unit getUnit();

    String getAddress();

    boolean isAddressValid(String address);

    @WorkerThread
        //sign and publish the tx using the seed
    byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] seed);

    void addBalanceChangedListener(BalanceUpdateListener list);

    void onBalanceChanged(Context context, BigDecimal balance);

    void addSyncListener(SyncListener listener);

    void removeSyncListener(SyncListener listener);

    void addTxListModifiedListener(OnTxListModified list);

    void watchTransactionForHash(CryptoTransaction tx, OnHashUpdated listener);

    //get confirmation number
    @WorkerThread
    long getRelayCount(byte[] txHash);

    //get the syncing progress
    @WorkerThread
    double getSyncProgress(long startHeight);
    //get the connection status 0 - Disconnected, 1 - Connecting, 2 - Connected, 3 - Unknown

    @WorkerThread
    double getConnectStatus();

    //Connect the wallet (PeerManager for Bitcoin)
    @WorkerThread
    void connect(Context app);

    //Disconnect the wallet (PeerManager for Bitcoin)
    @WorkerThread
    void disconnect(Context app);

    //Use a fixed favorite node to connect
    @WorkerThread
    boolean useFixedNode(String node, int port);

    //Rescan the wallet (PeerManager for Bitcoin)
    @WorkerThread
    void rescan(Context app);

    @WorkerThread
        //get a list of all the transactions sorted by timestamp (e.g. BRCoreTransaction[] for BTC)
    CryptoTransaction[] getTxs(Context app);

    //get the transaction fee
    BigDecimal getTxFee(CryptoTransaction tx);

    //get the transaction fee
    BigDecimal getEstimatedFee(BigDecimal amount, String address);

    //get the fee for the transaction size
    BigDecimal getFeeForTransactionSize(BigDecimal size);

    //get the transaction to address
    String getTxAddress(CryptoTransaction tx);

    //get the maximum output amount possible for this wallet
    BigDecimal getMaxOutputAmount(Context app);

    //get the reasonable minimum output amount
    BigDecimal getMinOutputAmount(Context app);

    //get the transaction amount (negative if sent)
    BigDecimal getTransactionAmount(CryptoTransaction tx);

    //get the reasonable minimum output amount (not smaller than dust)
    BigDecimal getMinOutputAmountPossible();

    @WorkerThread
        //updates the fee for the current wallet (from an API)
    void updateFee(Context app);

    //get the core address and store it locally
    void refreshAddress(Context app);

    //get the core balance and store it locally
    void refreshCachedBalance(Context app);

    //get a list of all the transactions UI holders sorted by timestamp
    List<TxUiHolder> getTxUiHolders(Context app);

    //return true if this wallet owns this address
    boolean containsAddress(String address);

    //return true if this wallet already used this address
    boolean addressIsUsed(String address);

    @WorkerThread
        //generate the wallet if needed
    boolean generateWallet(Context app);

    //get the currency symbol e.g. Bitcoin - ₿, Ether - Ξ
    String getSymbol(Context app);

    //get the currency denomination e.g. Bitcoin - BTC, Ether - ETH
    String getIso();

    //get the currency scheme (bitcoin or bitcoincash)
    String getScheme();

    //get the currency name e.g. Bitcoin
    String getName();

    //get the currency denomination e.g. BCH, mBCH, Bits
    String getDenominator();

    //get the wallet's receive address
    @WorkerThread
    CryptoAddress getReceiveAddress(Context app);

    CryptoTransaction createTransaction(BigDecimal amount, String address);

    //decorate an address to a particular currency, if needed (like BCH address format)
    String decorateAddress(String addr);

    //convert to raw address to a particular currency, if needed (like BCH address format)
    String undecorateAddress(String addr);

    //get the number of decimal places to use for this currency
    int getMaxDecimalPlaces(Context app);

    //get the cached balance in the smallest unit:  satoshis.
    BigDecimal getCachedBalance(Context app);

    //get the total amount sent in the smallest crypto unit:  satoshis.
    BigDecimal getTotalSent(Context app);

    //wipe all wallet data
    void wipeData(Context app);

    void syncStarted();

    void syncStopped(String error);

    boolean networkIsReachable();

    //return the maximum amount for this currency
    BigDecimal getMaxAmount(Context app);

    /**
     * @return - the wallet's Ui configuration
     */
    WalletUiConfiguration getUiConfiguration();

    /**
     * @return - the wallet's Settings configuration (Settings items)
     */
    WalletSettingsConfiguration getSettingsConfiguration();

    /**
     * @return - the wallet's currency exchange rate in the user's favorite fiat currency (e.g. dollars)
     */
    BigDecimal getFiatExchangeRate(Context app);

    /**
     * @return - the total balance amount in the user's favorite fiat currency (e.g. dollars)
     */
    BigDecimal getFiatBalance(Context app);

    /**
     * @param amount - the smallest denomination amount in current wallet's crypto (e.g. Satoshis)
     * @param ent    - provide a currency entity if needed
     * @return - the fiat value of the amount in crypto (e.g. dollars)
     * or null if there is no fiat exchange data from the API yet
     */
    BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent);

    /**
     * @param amount - the amount in the user's favorite fiat currency (e.g. dollars)
     * @return - the crypto value of the amount in the current favorite denomination (e.g. BTC, mBTC, Bits..)
     * or null if there is no fiat exchange data from the API yet
     */
    BigDecimal getCryptoForFiat(Context app, BigDecimal amount);

    /**
     * @param amount - the smallest denomination amount in crypto (e.g. satoshis)
     * @return - the crypto value of the amount in the current favorite denomination (e.g. BTC, mBTC, Bits..)
     */
    BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount);

    /**
     * @param amount - the crypto value of the amount in the current favorite denomination (e.g. BTC, mBTC, Bits..)
     * @return - the smallest denomination amount in crypto (e.g. satoshis)
     */
    BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount);

    /**
     * @param amount - the fiat amount (e.g. dollars)
     * @return - the crypto value of the amount in the smallest denomination (e.g. satothis)
     * or null if there is no fiat exchange data from the API yet
     */
    BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount);


}
