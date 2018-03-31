package com.breadwallet.wallet.wallets.etherium;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.core.ethereum.BREthereumLightNode;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseAddress;
import com.breadwallet.wallet.abstracts.BaseTransaction;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONException;
import org.json.JSONObject;

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
public class WalletEthManager implements BaseWalletManager, BREthereumLightNode.ClientJSON_RPC {
    private static final String TAG = WalletEthManager.class.getSimpleName();

    private static String ISO = "ETH";
    public static final String ETH_SCHEME = null;

    private static final String mName = "Etherium";

    private List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
    private List<OnTxStatusUpdatedListener> txStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> syncListeners = new ArrayList<>();
    private List<OnTxListModified> txModifiedListeners = new ArrayList<>();

    private static WalletEthManager instance;
    private WalletUiConfiguration uiConfig;
    public final BigDecimal MAX_ETH = new BigDecimal("90000000000000000000000000"); // 90m ETH * 18 (WEI)

    private int mSyncRetryCount = 0;
    private static final int SYNC_MAX_RETRY = 3;

    private Executor listenerExecutor = Executors.newSingleThreadExecutor();


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
//            long fee = BRSharedPrefs.getFeeRate(app, getIso(app));
//            long economyFee = BRSharedPrefs.getEconomyFeeRate(app, getIso(app));
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

    private WalletEthManager() {
        uiConfig = new WalletUiConfiguration("#5e70a3", true, true, false);
    }

    public synchronized static WalletEthManager getInstance(Context app) {
        if (instance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(app);
            if (Utils.isNullOrEmpty(rawPubKey)) {
                Log.e(TAG, "getInstance: rawPubKey is null");
                return null;
            }
//            BRCoreMasterPubKey pubKey = new BRCoreMasterPubKey(rawPubKey, false);
//
//            BREthereumLightNode node = new BREthereumLightNode.JSON_RPC(test, BREthereumNetwork.testnet);
//            BREthereumAccount account = node.createAccount(USABLE_PAPER_KEY);
//
            instance = new WalletEthManager();

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

    @Override
    public int getForkId() {
        return 0;
    }

    @Override
    public byte[] signAndPublishTransaction(BaseTransaction tx, byte[] seed) {
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
    public long getRelayCount(byte[] txHash) {
        return 0;
    }

    @Override
    public double getSyncProgress(long startHeight) {
        return 1.0;
    }

    @Override
    public double getConnectStatus() {
        return 2;
    }

    @Override
    public void connect() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean useFixedNode(String node, int port) {
        return false;
    }

    @Override
    public void rescan() {

    }

    @Override
    public BaseTransaction[] getTxs() {
        return new BaseTransaction[0];
    }

    @Override
    public BigDecimal getTxFee(BaseTransaction tx) {
        return null;
    }

    @Override
    public BigDecimal getFeeForTxAmount(BigDecimal amount) {
        return null;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return null;
    }

    @Override
    public BaseAddress getTxAddress(BaseTransaction tx) {
        return null;
    }

    @Override
    public BigDecimal getMaxOutputAmount() {
        return null;
    }

    @Override
    public BigDecimal getMinOutputAmount() {
        return null;
    }

    @Override
    public BigDecimal getTransactionAmount(BaseTransaction tx) {
        return null;
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return null;
    }

    @Override
    public void updateFee(Context app) {

        if (app == null) {
            app = BreadApp.getBreadContext();

            if (app == null) {
                Log.d(TAG, "updateFee: FAILED, app is null");
                return;
            }
        }

        String jsonString = BRApiManager.urlGET(app, "https://" + BreadApp.HOST + "/fee-per-kb?currency=" + getIso(app));

        if (jsonString == null || jsonString.isEmpty()) {
            Log.e(TAG, "updateFeePerKb: failed to update fee, response string: " + jsonString);
            return;
        }

        BigDecimal fee;
        BigDecimal economyFee;
        try {
            JSONObject obj = new JSONObject(jsonString);
            fee = new BigDecimal(obj.getString("fee_per_kb"));
            economyFee = new BigDecimal(obj.getString("fee_per_kb_economy"));
            Log.e(TAG, "updateFee: " + getIso(app) + ":" + fee + "|" + economyFee);

            if (fee.compareTo(new BigDecimal(0)) > 0) {
                BRSharedPrefs.putFeeRate(app, getIso(app), fee);
                BRSharedPrefs.putFeeTime(app, getIso(app), System.currentTimeMillis()); //store the time of the last successful fee fetch
            } else {
                FirebaseCrash.report(new NullPointerException("Fee is weird:" + fee));
            }
            if (economyFee.compareTo(new BigDecimal(0)) > 0) {
                BRSharedPrefs.putEconomyFeeRate(app, getIso(app), economyFee);
            } else {
                FirebaseCrash.report(new NullPointerException("Economy fee is weird:" + economyFee));
            }
        } catch (JSONException e) {
            Log.e(TAG, "updateFeePerKb: FAILED: " + jsonString, e);
            BRReportsManager.reportBug(e);
            BRReportsManager.reportBug(new IllegalArgumentException("JSON ERR: " + jsonString));
        }

    }

    @Override
    public void refreshAddress(Context app) {

    }

    @Override
    public void refreshCachedBalance(Context app) {

    }

    @Override
    public List<TxUiHolder> getTxUiHolders() {
        return null;
    }

    @Override
    public boolean containsAddress(String address) {
        return false;
    }

    @Override
    public boolean addressIsUsed(String address) {
        return false;
    }

    @Override
    public BaseAddress createAddress(String address) {
        return new ETHAddress(address);
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
        return BRConstants.symbolEther;
    }

    @Override
    public String getIso(Context app) {
        return ISO;
    }

    @Override
    public String getScheme(Context app) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String getName(Context app) {
        return mName;
    }

    @Override
    public String getDenomination(Context app) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public BaseAddress getReceiveAddress(Context app) {
        return null;
    }

    @Override
    public BaseTransaction createTransaction(BigDecimal amount, String address) {
        return null;
    }

    @Override
    public String decorateAddress(Context app, String addr) {
        return addr;
    }

    @Override
    public String undecorateAddress(Context app, String addr) {
        return addr;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        return 18;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, getIso(app));
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return null;
    }

    @Override
    public void wipeData(Context app) {
        Log.e(TAG, "wipeData: ");
    }

    @Override
    public void syncStarted() {
        Log.e(TAG, "syncStarted: ");
    }

    @Override
    public void syncStopped(String error) {
        Log.e(TAG, "syncStopped: " + error);
    }

    @Override
    public boolean networkIsReachable() {
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(app), balance);
        refreshAddress(app);
        for (OnBalanceChangedListener listener : balanceListeners) {
            if (listener != null) listener.onBalanceChanged(getIso(app), balance);
        }

    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return MAX_ETH;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return uiConfig;
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        return new BigDecimal(0);
    }


    /**
     * The JSON RPC callbacks
     * Implement JSON RPC methods synchronously
     */

    @Override
    public void assignNode(BREthereumLightNode node) {

    }

    @Override
    public String getBalance(int id, String account) {
        return null;
    }

    @Override
    public String getGasPrice(int id) {
        return null;
    }

    @Override
    public String getGasEstimate(int id, String to, String amount, String data) {
        return null;
    }

    @Override
    public String submitTransaction(int id, String rawTransaction) {
        return null;
    }

    @Override
    public void getTransactions(int id, String account) {

    }
}
