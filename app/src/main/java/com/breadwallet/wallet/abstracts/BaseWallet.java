package com.breadwallet.wallet.abstracts;

import android.content.Context;

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
public interface BaseWallet {

//    public BaseWallet(BRCoreMasterPubKey masterPubKey,
//                      BRCoreChainParams chainParams,
//                      double earliestPeerTime) {
//        super(masterPubKey, chainParams, earliestPeerTime);
//    }

    //get the core wallet
    BRCoreWallet getWallet();

    void addBalanceChangedListener(OnBalanceChangedListener list);

    //get the core peerManager
    BRCorePeerManager getPeerManager();

    //try sending a transaction
    boolean sendTransaction(Context app, PaymentItem item);

    //get a list of all the transactions sorted by timestamp
    BRCoreTransaction[] getTransactions();

    void updateFee(Context app);

    //get a list of all the transactions UI holders sorted by timestamp
    List<TxUiHolder> getTxUiHolders();

    //generate the wallet if needed
    boolean generateWallet(Context app);

    //init the current wallet
    boolean initWallet(Context app);

    //get the currency symbol e.g. Bitcoin - ₿, Ether - Ξ
    String getSymbol(Context app);

    //get the currency denomination e.g. Bitcoin - BTC, Ether - ETH
    String getIso(Context app);

    //get the currency name e.g. Bitcoin
    String getName(Context app);

    //get the currency denomination e.g. BCH, mBCH, Bits
    String getDenomination(Context app);

    //get the wallet's receive address
    String getReceiveAddress(Context app);

    //get the number of decimal places to use for this currency
    int getMaxDecimalPlaces(Context app);

    //get the cached balance in the smallest unit: cents, satoshis.
    long getCachedBalance(Context app);

    //get the total amount sent in the smallest crypto unit: cents, satoshis.
    long getTotalSent(Context app);

    //wipe all wallet data
    void wipeData(Context app);

    boolean trySweepWallet(Context app, String privKey);

    //load the txs from DB
    BRCoreTransaction[] loadTransactions();

    //load the blocks from DB
    BRCoreMerkleBlock[] loadBlocks();

    //load the peers from DB
    BRCorePeer[] loadPeers();

    /**
     * @param balance - the balance to be saved in the smallest unit.(e.g. cents, satoshis)
     */
    void setCashedBalance(Context app, long balance);

    //return the maximum amount for this currency
    BigDecimal getMaxAmount(Context app);

    /**
     * @return - true if the uri is compatible with the  and can be processed (proceed with processing the uri)
     */
    boolean tryUri(Context app, String uriStr);

    /**
     * @return - the wallet's Ui configuration
     */
    WalletUiConfiguration getUiConfiguration();

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
