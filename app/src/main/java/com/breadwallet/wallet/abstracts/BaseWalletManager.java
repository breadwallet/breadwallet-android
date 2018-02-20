package com.breadwallet.wallet.abstracts;

import android.content.Context;

import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.core.BRCoreChainParams;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.BRCoreMerkleBlock;
import com.breadwallet.core.BRCorePeer;
import com.breadwallet.core.BRCorePeerManager;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreWallet;
import com.breadwallet.core.BRCoreWalletManager;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.wallet.wallets.configs.WalletUiConfiguration;

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

//    public BaseWalletManager(BRCoreMasterPubKey masterPubKey,
//                      BRCoreChainParams chainParams,
//                      double earliestPeerTime) {
//        super(masterPubKey, chainParams, earliestPeerTime);
//    }

    //get the core wallet
    BRCoreWallet getWallet();

    //get the core peerManager
    BRCorePeerManager getPeerManager();

    //sign and publish the tx using the seed
    byte[] signAndPublishTransaction(BRCoreTransaction tx, byte[] seed);

    void addBalanceChangedListener(OnBalanceChangedListener list);

    void addTxStatusUpdatedListener(OnTxStatusUpdatedListener list);

    void addSyncStoppedListener(OnSyncStopped list);

    //try sending a transaction
    boolean sendTransaction(Context app, PaymentItem item);

    //get a list of all the transactions sorted by timestamp
    BRCoreTransaction[] getTransactions();

    void updateFee(Context app);

    //get the core address and store it locally
    void refreshAddress(Context app);

    //get a list of all the transactions UI holders sorted by timestamp
    List<TxUiHolder> getTxUiHolders();

    //generate the wallet if needed
    boolean generateWallet(Context app);

    //init the current wallet
    boolean connectWallet(Context app);

    //get the currency symbol e.g. Bitcoin - ₿, Ether - Ξ
    String getSymbol(Context app);

    //get the currency denomination e.g. Bitcoin - BTC, Ether - ETH
    String getIso(Context app);

    //get the currency name e.g. Bitcoin
    String getName(Context app);

    //get the currency denomination e.g. BCH, mBCH, Bits
    String getDenomination(Context app);

    //get the wallet's receive address
    BRCoreAddress getReceiveAddress(Context app);

    //get the number of decimal places to use for this currency
    int getMaxDecimalPlaces(Context app);

    //get the cached balance in the smallest unit: cents, satoshis.
    long getCachedBalance(Context app);

    //get the total amount sent in the smallest crypto unit: cents, satoshis.
    long getTotalSent(Context app);

    //wipe all wallet data
    void wipeData(Context app);

    //load the txs from DB
    BRCoreTransaction[] loadTransactions();

    //load the blocks from DB
    BRCoreMerkleBlock[] loadBlocks();

    //load the peers from DB
    BRCorePeer[] loadPeers();

    void syncStarted();

    void syncStopped(String error);

    void onTxAdded(BRCoreTransaction transaction);

    void onTxDeleted(String hash, int notifyUser, int recommendRescan);

    void onTxUpdated(String hash, int blockHeight, int timeStamp);

    void txPublished(final String error);

    void balanceChanged(long balance);

    void txStatusUpdate();

    void saveBlocks(boolean replace, BRCoreMerkleBlock[] blocks);

    void savePeers(boolean replace, BRCorePeer[] peers);

    boolean networkIsReachable();


    /**
     * @param balance - the balance to be saved in the smallest unit.(e.g. cents, satoshis)
     */
    void setCashedBalance(Context app, long balance);

    //return the maximum amount for this currency
    BigDecimal getMaxAmount(Context app);

    /**
     * @return - the wallet's Ui configuration
     */
    WalletUiConfiguration getUiConfiguration();

    /**
     * @return - the wallet's currency exchange rate in the smallest denomination amount in the user's favorite fiat currency (e.g. cents)
     */
    long getFiatExchangeRate(Context app);

    /**
     * @return - the total balance in the smallest denomination amount in the user's favorite fiat currency (e.g. cents)
     */
    long getFiatBalance(Context app);

    /**
     * @param amount - the smallest denomination amount in current wallet's crypto (e.g. Satoshis)
     * @return - the fiat value of the amount in crypto in the smallest denomination (e.g. cents)
     * or null if there is no fiat exchange data from the API yet
     */
    BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount);

    /**
     * @param amount - the smallest denomination amount in the user's favorite fiat currency (e.g. cents)
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
     * @param amount - the fiat amount in the smallest denomination (e.g. cents)
     * @return - the crypto value of the amount in the smallest denomination (e.g. satothis)
     * or null if there is no fiat exchange data from the API yet
     */
    BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount);


}
