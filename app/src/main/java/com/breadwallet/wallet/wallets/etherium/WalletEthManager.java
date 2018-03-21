package com.breadwallet.wallet.wallets.etherium;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BuildConfig;
import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.core.BRCoreChainParams;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.BRCoreMerkleBlock;
import com.breadwallet.core.BRCorePeer;
import com.breadwallet.core.BRCorePeerManager;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreWallet;
import com.breadwallet.core.ethereum.BREthereumAccount;
import com.breadwallet.core.ethereum.BREthereumLightNode;
import com.breadwallet.core.ethereum.BREthereumNetwork;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.core.ethereum.BREthereumWallet;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BREventManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.wallets.bitcoincash.WalletBchManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/21/18.
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
public class WalletEthManager extends BREthereumWallet implements BaseWalletManager {
    private static final String TAG = WalletEthManager.class.getSimpleName();

    private static String ISO = "ETH";
    public static final String BCH_SCHEME = null;

    private static final String mName = "Etherium";

    private static WalletEthManager instance;
    private WalletUiConfiguration uiConfig;

    private int mSyncRetryCount = 0;
    private static final int SYNC_MAX_RETRY = 3;

    protected WalletEthManager(BREthereumLightNode node, long identifier, BREthereumAccount account, BREthereumNetwork network) {
        super(node, identifier, account, network);
    }

    protected WalletEthManager(BREthereumLightNode node, long identifier, BREthereumAccount account, BREthereumNetwork network, BREthereumToken token) {
        super(node, identifier, account, network, token);
    }

//    private boolean isInitiatingWallet;
//
//    private List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
//    private List<OnTxStatusUpdatedListener> txStatusUpdatedListeners = new ArrayList<>();
//    private List<SyncListener> syncListeners = new ArrayList<>();
//    private List<OnTxListModified> txModifiedListeners = new ArrayList<>();

//    private Executor listenerExecutor = Executors.newSingleThreadExecutor();

    public synchronized static WalletEthManager getInstance(Context app) {
        if (instance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(app);
            if (Utils.isNullOrEmpty(rawPubKey)) {
                Log.e(TAG, "getInstance: rawPubKey is null");
                return null;
            }
            BRCoreMasterPubKey pubKey = new BRCoreMasterPubKey(rawPubKey, false);
//            long time = BRKeyStore.getWalletCreationTime(app);
//            if (!BRSharedPrefs.getBchPreforkSynced(app) && time == 0)
//                time = BuildConfig.BITCOIN_TESTNET ? 1501597117 : 1501568580;
////            if (Utils.isEmulatorOrDebug(app)) time = 1517955529;
//            //long time = 1519190488;
////            long time = (System.currentTimeMillis() / 1000) - 3 * 7 * 24 * 60 * 60; // 3 * 7

//            instance = new WalletEthManager(app, pubKey, .., ..);
        }
        return instance;
    }

//    private WalletEthManager(final Context app, BRCoreMasterPubKey masterPubKey,
//                             BRCoreChainParams chainParams,
//                             double earliestPeerTime) {
//        super(masterPubKey, chainParams, earliestPeerTime);
//        if (isInitiatingWallet) return;
//        isInitiatingWallet = true;
//        try {
//            Log.d(TAG, "connectWallet:" + Thread.currentThread().getName());
//            if (app == null) {
//                Log.e(TAG, "connectWallet: app is null");
//                return;
//            }
//            String firstAddress = masterPubKey.getPubKeyAsCoreKey().address();
//            BRSharedPrefs.putFirstAddress(app, firstAddress);
//            long fee = BRSharedPrefs.getFeePerKb(app, getIso(app));
//            long economyFee = BRSharedPrefs.getEconomyFeePerKb(app, getIso(app));
//            if (fee == 0) {
//                fee = getWallet().getDefaultFeePerKb();
//                BREventManager.getInstance().pushEvent("wallet.didUseDefaultFeePerKB");
//            }
//            getWallet().setFeePerKb(BRSharedPrefs.getFavorStandardFee(app, getIso(app)) ? fee : economyFee);
//            if (BRSharedPrefs.getStartHeight(app, getIso(app)) == 0)
//                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        BRSharedPrefs.putStartHeight(app, getIso(app), getPeerManager().getLastBlockHeight());
//                    }
//                });
//
////            BRPeerManager.getInstance().updateFixedPeer(ctx);//todo reimplement the fixed peer
////        balanceListeners = new ArrayList<>();
//            uiConfig = new WalletUiConfiguration("#478559", true, true, false);
//
//        } finally {
//            isInitiatingWallet = false;
//        }

//    }

    @Override
    public BRCoreWallet getWallet() {
        return null;
    }

    @Override
    public int getForkId() {
        return 0;
    }

    @Override
    public BRCorePeerManager getPeerManager() {
        return null;
    }

    @Override
    public byte[] signAndPublishTransaction(BRCoreTransaction tx, byte[] seed) {
        return new byte[0];
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener list) {

    }

    @Override
    public void addTxStatusUpdatedListener(OnTxStatusUpdatedListener list) {

    }

    @Override
    public void addSyncListeners(SyncListener list) {

    }

    @Override
    public void addTxListModifiedListener(OnTxListModified list) {

    }

    @Override
    public BRCoreTransaction[] getTransactions() {
        return new BRCoreTransaction[0];
    }

    @Override
    public void updateFee(Context app) {

    }

    @Override
    public void refreshAddress(Context app) {

    }

    @Override
    public List<TxUiHolder> getTxUiHolders() {
        return null;
    }

    @Override
    public boolean generateWallet(Context app) {
        return false;
    }

    @Override
    public boolean connectWallet(Context app) {
        return false;
    }

    @Override
    public String getSymbol(Context app) {
        return null;
    }

    @Override
    public String getIso(Context app) {
        return null;
    }

    @Override
    public String getScheme(Context app) {
        return null;
    }

    @Override
    public String getName(Context app) {
        return null;
    }

    @Override
    public String getDenomination(Context app) {
        return null;
    }

    @Override
    public BRCoreAddress getReceiveAddress(Context app) {
        return null;
    }

    @Override
    public String decorateAddress(Context app, String addr) {
        return null;
    }

    @Override
    public String undecorateAddress(Context app, String addr) {
        return null;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        return 0;
    }

    @Override
    public long getCachedBalance(Context app) {
        return 0;
    }

    @Override
    public long getTotalSent(Context app) {
        return 0;
    }

    @Override
    public void wipeData(Context app) {

    }

    @Override
    public BRCoreTransaction[] loadTransactions() {
        return new BRCoreTransaction[0];
    }

    @Override
    public BRCoreMerkleBlock[] loadBlocks() {
        return new BRCoreMerkleBlock[0];
    }

    @Override
    public BRCorePeer[] loadPeers() {
        return new BRCorePeer[0];
    }

    @Override
    public void syncStarted() {

    }

    @Override
    public void syncStopped(String error) {

    }

    @Override
    public void onTxAdded(BRCoreTransaction transaction) {

    }

    @Override
    public void onTxDeleted(String hash, int notifyUser, int recommendRescan) {

    }

    @Override
    public void onTxUpdated(String hash, int blockHeight, int timeStamp) {

    }

    @Override
    public void txPublished(String error) {

    }

    @Override
    public void balanceChanged(long balance) {

    }

    @Override
    public void txStatusUpdate() {

    }

    @Override
    public void saveBlocks(boolean replace, BRCoreMerkleBlock[] blocks) {

    }

    @Override
    public void savePeers(boolean replace, BRCorePeer[] peers) {

    }

    @Override
    public boolean networkIsReachable() {
        return false;
    }

    @Override
    public void setCashedBalance(Context app, long balance) {

    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return null;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return null;
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        return null;
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        return null;
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        return null;
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
        return null;
    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        return null;
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        return null;
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        return null;
    }
}
