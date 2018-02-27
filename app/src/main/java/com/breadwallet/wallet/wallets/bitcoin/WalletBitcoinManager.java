package com.breadwallet.wallet.wallets.bitcoin;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.core.BRCoreChainParams;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.BRCoreMerkleBlock;
import com.breadwallet.core.BRCorePeer;
import com.breadwallet.core.BRCorePeerManager;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreWallet;
import com.breadwallet.core.BRCoreWalletManager;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BREventManager;
import com.breadwallet.tools.manager.BRNotificationManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.BtcBchTransactionDataStore;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.sqlite.TransactionStorageManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.wallets.configs.WalletUiConfiguration;
import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.breadwallet.tools.util.BRConstants.ROUNDING_MODE;

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
public class WalletBitcoinManager extends BRCoreWalletManager implements BaseWalletManager {

    private static final String TAG = WalletBitcoinManager.class.getName();

    private static String ISO = "BTC";

    private static final String mName = "Bitcoin";
    public static final String BTC_SCHEME = "bitcoin";


    public static final long MAX_BTC = 21000000;

    private static WalletBitcoinManager instance;
    private WalletUiConfiguration uiConfig;

    private int mSyncRetryCount = 0;
    private static final int SYNC_MAX_RETRY = 3;

    private boolean isInitiatingWallet;

    private List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
    private List<OnTxStatusUpdatedListener> txStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> syncListeners = new ArrayList<>();
    private List<OnTxListModified> txModifiedListeners = new ArrayList<>();

    private Executor listenerExecutor = Executors.newSingleThreadExecutor();

    public static WalletBitcoinManager getInstance(Context app) {
        if (instance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(app);
            if (Utils.isNullOrEmpty(rawPubKey)) return null;
            BRCoreMasterPubKey pubKey = new BRCoreMasterPubKey(rawPubKey, false);
            long time = BRKeyStore.getWalletCreationTime(app);
//            if (Utils.isEmulatorOrDebug(app)) time = 1517955529;

            instance = new WalletBitcoinManager(app, pubKey, BuildConfig.BITCOIN_TESTNET ? BRCoreChainParams.testnetChainParams : BRCoreChainParams.mainnetChainParams, time);
        }
        return instance;
    }

    private WalletBitcoinManager(final Context app, BRCoreMasterPubKey masterPubKey,
                                 BRCoreChainParams chainParams,
                                 double earliestPeerTime) {
        super(masterPubKey, chainParams, earliestPeerTime);
        if (isInitiatingWallet) return;
        isInitiatingWallet = true;
        try {
            Log.d(TAG, "connectWallet:" + Thread.currentThread().getName());
            if (app == null) {
                Log.e(TAG, "connectWallet: app is null");
                return;
            }
            String firstAddress = masterPubKey.getPubKeyAsCoreKey().address();
            BRSharedPrefs.putFirstAddress(app, firstAddress);
            long fee = BRSharedPrefs.getFeePerKb(app, getIso(app));
            long economyFee = BRSharedPrefs.getEconomyFeePerKb(app, getIso(app));
            if (fee == 0) {
                fee = getWallet().getDefaultFeePerKb();
                BREventManager.getInstance().pushEvent("wallet.didUseDefaultFeePerKB");
            }
            getWallet().setFeePerKb(BRSharedPrefs.getFavorStandardFee(app, getIso(app)) ? fee : economyFee);
            if (BRSharedPrefs.getStartHeight(app, getIso(app)) == 0)
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        BRSharedPrefs.putStartHeight(app, getIso(app), getPeerManager().getLastBlockHeight());
                    }
                });

//            BRPeerManager.getInstance().updateFixedPeer(ctx);//todo reimplement the fixed peer
//        balanceListeners = new ArrayList<>();

            uiConfig = new WalletUiConfiguration("#f29500", true, true, true);

        } finally {
            isInitiatingWallet = false;
        }

    }

    @Override
    protected BRCoreWallet.Listener createWalletListener() {
        return new BRCoreWalletManager.WrappedExecutorWalletListener(
                super.createWalletListener(),
                listenerExecutor);
    }

    @Override
    protected BRCorePeerManager.Listener createPeerManagerListener() {
        return new BRCoreWalletManager.WrappedExecutorPeerManagerListener(
                super.createPeerManagerListener(),
                listenerExecutor);
    }

    @Override
    public BRCoreTransaction[] getTransactions() {
        return getWallet().getTransactions();
    }

    @Override
    public void updateFee(Context app) {
        String jsonString = BRApiManager.urlGET(app, "https://" + BreadApp.HOST + "/fee-per-kb?currency=" + getIso(app));
        if (jsonString == null || jsonString.isEmpty()) {
            Log.e(TAG, "updateFeePerKb: failed to update fee, response string: " + jsonString);
            return;
        }
        long fee;
        long economyFee;
        try {
            JSONObject obj = new JSONObject(jsonString);
            fee = obj.getLong("fee_per_kb");
            economyFee = obj.getLong("fee_per_kb_economy");
            Log.e(TAG, "updateFee: " + getIso(app) + ":" + fee + "|" + economyFee);

            if (fee != 0 && fee < getWallet().getMaxFeePerKb()) {
                BRSharedPrefs.putFeePerKb(app, getIso(app), fee);
                getWallet().setFeePerKb(BRSharedPrefs.getFavorStandardFee(app, getIso(app)) ? fee : economyFee);
                BRSharedPrefs.putFeeTime(app, getIso(app), System.currentTimeMillis()); //store the time of the last successful fee fetch
            } else {
                FirebaseCrash.report(new NullPointerException("Fee is weird:" + fee));
            }
            if (economyFee != 0 && economyFee < getWallet().getMaxFeePerKb()) {
                BRSharedPrefs.putEconomyFeePerKb(app, getIso(app), economyFee);
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
    public List<TxUiHolder> getTxUiHolders() {
        BRCoreTransaction txs[] = getWallet().getTransactions();
//        Log.e(TAG, "getTxUiHolders: txs:" + txs.length);
        if (txs == null || txs.length <= 0) return null;
        List<TxUiHolder> uiTxs = new ArrayList<>();
        for (BRCoreTransaction tx : txs) {
            uiTxs.add(new TxUiHolder(tx.getTimestamp(), (int) tx.getBlockHeight(), tx.getHash(),
                    tx.getReverseHash(), getWallet().getTransactionAmountSent(tx),
                    getWallet().getTransactionAmountReceived(tx), getWallet().getTransactionFee(tx), tx.getOutputAddresses(), tx.getInputAddresses(),
                    getWallet().getBalanceAfterTransaction(tx), (int) tx.getSize(),
                    getWallet().getTransactionAmount(tx), getWallet().transactionIsValid(tx)));
        }
        return uiTxs;
    }

    @Override
    public boolean generateWallet(Context app) {
        //no need, one key for all wallets so far
        return true;
    }

    @Override
    public boolean connectWallet(final Context app) {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                getPeerManager().connect();
            }
        });

        return true;
    }


    @Override
    public String getSymbol(Context app) {

        String currencySymbolString = BRConstants.symbolBits;
        if (app != null) {
            int unit = BRSharedPrefs.getCryptoDenomination(app, getIso(app));
            switch (unit) {
                case BRConstants.CURRENT_UNIT_BITS:
                    currencySymbolString = BRConstants.symbolBits;
                    break;
                case BRConstants.CURRENT_UNIT_MBITS:
                    currencySymbolString = "m" + BRConstants.symbolBitcoin;
                    break;
                case BRConstants.CURRENT_UNIT_BITCOINS:
                    currencySymbolString = BRConstants.symbolBitcoin;
                    break;
            }
        }
        return currencySymbolString;
    }

    @Override
    public String getIso(Context app) {
        return ISO;
    }

    @Override
    public String getScheme(Context app) {
        return BTC_SCHEME;
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
    public BRCoreAddress getReceiveAddress(Context app) {
        return getWallet().getReceiveAddress();
    }

    @Override
    public String decorateAddress(Context app, String addr) {
        return addr; // no need to decorate
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso(app));
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                return 2;
            case BRConstants.CURRENT_UNIT_MBITS:
                return 5;
            default:
                return 8;
        }
    }

    @Override
    public long getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, getIso(app));
    }

    @Override
    public long getTotalSent(Context app) {
        return getWallet().getTotalSent();
    }

    @Override
    public void wipeData(Context app) {
        BtcBchTransactionDataStore.getInstance(app).deleteAllTransactions(app, this);
        MerkleBlockDataSource.getInstance(app).deleteAllBlocks(app, this);
        PeerDataSource.getInstance(app).deleteAllPeers(app, this);
        BRSharedPrefs.clearAllPrefs(app);
    }

    @Override
    public void setCashedBalance(Context app, long balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(app), balance);
        refreshAddress(app);
        for (OnBalanceChangedListener listener : balanceListeners) {
            if (listener != null) listener.onBalanceChanged(getIso(app), balance);
        }

    }

    @Override
    public void refreshAddress(Context app) {
        BRCoreAddress address = getReceiveAddress(app);
        if (Utils.isNullOrEmpty(address.stringify())) {
            Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
        }
        BRSharedPrefs.putReceiveAddress(app, address.stringify(), getIso(app));

    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        //return max bitcoin
        return new BigDecimal(MAX_BTC);
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return uiConfig;
    }

    @Override
    public long getFiatExchangeRate(Context app) {
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, this, BRSharedPrefs.getPreferredFiatIso(app));
        return ent == null ? 0 : (long) (ent.rate * 100); //cents
    }

    @Override
    public long getFiatBalance(Context app) {
        BigDecimal bal = getFiatForSmallestCrypto(app, new BigDecimal(getCachedBalance(app)));
        return bal == null ? 0 : bal.longValue();
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, this, iso);
        if (ent == null) return null;
        double rate = ent.rate;
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE);
        return cryptoAmount.multiply(new BigDecimal(rate)).multiply(new BigDecimal(100));
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, this, iso);
        if (ent == null) return null;
        double rate = ent.rate;
        //convert c to $.
        BigDecimal fiatAmount = amount.divide(new BigDecimal(100), ROUNDING_MODE);
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso(app));
        BigDecimal result = new BigDecimal(0);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = fiatAmount.divide(new BigDecimal(rate), 2, ROUNDING_MODE).multiply(new BigDecimal("1000000"));
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                result = fiatAmount.divide(new BigDecimal(rate), 5, ROUNDING_MODE).multiply(new BigDecimal("1000"));
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = fiatAmount.divide(new BigDecimal(rate), 8, ROUNDING_MODE);
                break;
        }
        return result;

    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        BigDecimal result = new BigDecimal(0);
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso(app));
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = amount.divide(new BigDecimal("100"), 2, ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                result = amount.divide(new BigDecimal("100000"), 5, ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = amount.divide(new BigDecimal("100000000"), 8, ROUNDING_MODE);
                break;
        }
        return result;
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        BigDecimal result = new BigDecimal(0);
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso(app));
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = amount.multiply(new BigDecimal("100"));
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                result = amount.multiply(new BigDecimal("100000"));
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = amount.multiply(new BigDecimal("100000000"));
                break;
        }
        return result;
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, this, iso);
        if (ent == null) {
            Log.e(TAG, "getSmallestCryptoForFiat: no exchange rate data!");
            return amount;
        }
        double rate = ent.rate;
        //convert c to $.
        BigDecimal fiatAmount = amount.divide(new BigDecimal(100), ROUNDING_MODE);
        return fiatAmount.divide(new BigDecimal(rate), 8, ROUNDING_MODE).multiply(new BigDecimal("100000000"));
    }

    @Override
    public int getForkId() {
        return super.getForkId();
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener listener) {
        if (listener != null && !balanceListeners.contains(listener))
            balanceListeners.add(listener);
    }

    @Override
    public void addTxStatusUpdatedListener(OnTxStatusUpdatedListener list) {
        if (list != null && !txStatusUpdatedListeners.contains(list))
            txStatusUpdatedListeners.add(list);
    }

    @Override
    public void addSyncListeners(SyncListener list) {
        if (list != null && !syncListeners.contains(list))
            syncListeners.add(list);
    }

    @Override
    public void addTxListModifiedListener(OnTxListModified list) {
        if (list != null && !txModifiedListeners.contains(list))
            txModifiedListeners.add(list);
    }


    @Override
    public void txPublished(final String error) {
        super.txPublished(error);
        final Context app = BreadApp.getBreadContext();
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (app instanceof Activity)
                    BRAnimator.showBreadSignal((Activity) app, Utils.isNullOrEmpty(error) ? app.getString(R.string.Alerts_sendSuccess) : app.getString(R.string.Alert_error),
                            Utils.isNullOrEmpty(error) ? app.getString(R.string.Alerts_sendSuccessSubheader) : "Error: " + error, Utils.isNullOrEmpty(error) ? R.drawable.ic_check_mark_white : R.drawable.ic_error_outline_black_24dp, new BROnSignalCompletion() {
                                @Override
                                public void onComplete() {
                                    if (!((Activity) app).isDestroyed())
                                        ((Activity) app).getFragmentManager().popBackStack();
                                }
                            });

            }
        });

    }

    @Override
    public void balanceChanged(long balance) {
        super.balanceChanged(balance);
        Context app = BreadApp.getBreadContext();
        setCashedBalance(app, balance);
        for (OnTxListModified list : txModifiedListeners)
            if (list != null) list.txListModified(null);

    }

    @Override
    public void txStatusUpdate() {
        super.txStatusUpdate();
        for (OnTxStatusUpdatedListener listener : txStatusUpdatedListeners)
            if (listener != null) listener.onTxStatusUpdated();
        for (OnTxListModified list : txModifiedListeners)
            if (list != null) list.txListModified(null);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                long blockHeight = getPeerManager().getLastBlockHeight();

                final Context ctx = BreadApp.getBreadContext();
                if (ctx == null) return;
                BRSharedPrefs.putLastBlockHeight(ctx, getIso(ctx), (int) blockHeight);
            }
        });

    }

    @Override
    public void saveBlocks(boolean replace, BRCoreMerkleBlock[] blocks) {
        super.saveBlocks(replace, blocks);
        Context app = BreadApp.getBreadContext();
        if (app == null) return;
        if (replace) MerkleBlockDataSource.getInstance(app).deleteAllBlocks(app, this);
        BlockEntity[] entities = new BlockEntity[blocks.length];
        for (int i = 0; i < entities.length; i++) {
            entities[i] = new BlockEntity(blocks[i].serialize(), (int) blocks[i].getHeight());
        }

        MerkleBlockDataSource.getInstance(app).putMerkleBlocks(app, this, entities);
    }

    @Override
    public void savePeers(boolean replace, BRCorePeer[] peers) {
        super.savePeers(replace, peers);
        Context app = BreadApp.getBreadContext();
        if (app == null) return;
        if (replace) PeerDataSource.getInstance(app).deleteAllPeers(app, this);
        PeerEntity[] entities = new PeerEntity[peers.length];
        for (int i = 0; i < entities.length; i++) {
            entities[i] = new PeerEntity(peers[i].getAddress(), TypesConverter.intToBytes(peers[i].getPort()), TypesConverter.long2byteArray(peers[i].getTimestamp()));
        }
        PeerDataSource.getInstance(app).putPeers(app, this, entities);

    }

    @Override
    public boolean networkIsReachable() {
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }


    @Override
    public BRCoreTransaction[] loadTransactions() {
        Context app = BreadApp.getBreadContext();

        List<BRTransactionEntity> txs = BtcBchTransactionDataStore.getInstance(app).getAllTransactions(app, this);
        if (txs == null || txs.size() == 0) return new BRCoreTransaction[0];
        BRCoreTransaction arr[] = new BRCoreTransaction[txs.size()];
        for (int i = 0; i < txs.size(); i++) {
            BRTransactionEntity ent = txs.get(i);
            arr[i] = new BRCoreTransaction(ent.getBuff(), ent.getBlockheight(), ent.getTimestamp());
        }
        return arr;
    }

    @Override
    public BRCoreMerkleBlock[] loadBlocks() {
        Context app = BreadApp.getBreadContext();
        List<BRMerkleBlockEntity> blocks = MerkleBlockDataSource.getInstance(app).getAllMerkleBlocks(app, this);
        if (blocks == null || blocks.size() == 0) return new BRCoreMerkleBlock[0];
        BRCoreMerkleBlock arr[] = new BRCoreMerkleBlock[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            BRMerkleBlockEntity ent = blocks.get(i);
            arr[i] = new BRCoreMerkleBlock(ent.getBuff(), ent.getBlockHeight());
        }
        return arr;
    }

    @Override
    public BRCorePeer[] loadPeers() {
        Context app = BreadApp.getBreadContext();
        List<BRPeerEntity> peers = PeerDataSource.getInstance(app).getAllPeers(app, this);
        if (peers == null || peers.size() == 0) return new BRCorePeer[0];
        BRCorePeer arr[] = new BRCorePeer[peers.size()];
        for (int i = 0; i < peers.size(); i++) {
            BRPeerEntity ent = peers.get(i);
            arr[i] = new BRCorePeer(ent.getAddress(), ent.getPort(), ent.getTimeStamp());
        }
        return arr;
    }

    @Override
    public void syncStarted() {
        super.syncStarted();
        Log.d(TAG, "syncStarted: ");
        final Context app = BreadApp.getBreadContext();
        if (Utils.isEmulatorOrDebug(app))
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(app, "syncStarted " + getIso(app), Toast.LENGTH_LONG).show();
                }
            });

        for (SyncListener list : syncListeners)
            if (list != null) list.syncStarted();

    }

    @Override
    public void syncStopped(final String error) {
        super.syncStopped(error);
        Log.d(TAG, "syncStopped: " + error);
        final Context app = BreadApp.getBreadContext();
        if (Utils.isNullOrEmpty(error))
            BRSharedPrefs.putAllowSpend(app, getIso(app), true);
        for (SyncListener list : syncListeners)
            if (list != null) list.syncStopped(error);
        if (Utils.isEmulatorOrDebug(app))
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(app, "SyncStopped " + getIso(app) + " err(" + error + ") ", Toast.LENGTH_LONG).show();
                }
            });

        Log.e(TAG, "syncStopped: peerManager:" + getPeerManager().toString());

        if (!Utils.isNullOrEmpty(error)) {
            if (mSyncRetryCount < SYNC_MAX_RETRY) {
                Log.e(TAG, "syncStopped: Retrying: " + mSyncRetryCount);
                //Retry
                mSyncRetryCount++;
                BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        getPeerManager().connect();
                    }
                });

            } else {
                //Give up
                Log.e(TAG, "syncStopped: Giving up: " + mSyncRetryCount);
                mSyncRetryCount = 0;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(app, "Syncing failed, retried " + SYNC_MAX_RETRY + " times.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

    }

    @Override
    public void onTxAdded(BRCoreTransaction transaction) {
        super.onTxAdded(transaction);
        final Context ctx = BreadApp.getBreadContext();
        final WalletsMaster master = WalletsMaster.getInstance(ctx);
        final long amount = getWallet().getTransactionAmount(transaction);
        if (amount > 0) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    String am = CurrencyUtils.getFormattedAmount(ctx, getIso(ctx), getCryptoForSmallestCrypto(ctx, new BigDecimal(amount)));
                    BigDecimal bigAmount = master.getCurrentWallet(ctx).getFiatForSmallestCrypto(ctx, new BigDecimal(amount));
                    String amCur = CurrencyUtils.getFormattedAmount(ctx, BRSharedPrefs.getPreferredFiatIso(ctx), bigAmount == null ? new BigDecimal(0) : bigAmount.divide(new BigDecimal(100), 2, BRConstants.ROUNDING_MODE));
                    String formatted = String.format("%s (%s)", am, amCur);
                    final String strToShow = String.format(ctx.getString(R.string.TransactionDetails_received), formatted);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!BRToast.isToastShown()) {
                                BRToast.showCustomToast(ctx, strToShow,
                                        BreadApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, R.drawable.toast_layout_black);
                                AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                    final MediaPlayer mp = MediaPlayer.create(ctx, R.raw.coinflip);
                                    if (mp != null) try {
                                        mp.start();
                                    } catch (IllegalArgumentException ex) {
                                        Log.e(TAG, "run: ", ex);
                                    }
                                }

                                if (!BreadApp.isAppInBackground(ctx) && BRSharedPrefs.getShowNotification(ctx))
                                    BRNotificationManager.sendNotification((Activity) ctx, R.drawable.notification_icon, ctx.getString(R.string.app_name), strToShow, 1);
                            }
                        }
                    }, 1000);


                }
            });
        }
        if (ctx != null)
            TransactionStorageManager.putTransaction(ctx, getInstance(ctx), new BRTransactionEntity(transaction.serialize(), transaction.getBlockHeight(), transaction.getTimestamp(), transaction.getReverseHash(), getIso(ctx)));
        else
            Log.e(TAG, "onTxAdded: ctx is null!");
        for (OnTxListModified list : txModifiedListeners)
            if (list != null) list.txListModified(transaction.getReverseHash());
    }

    @Override
    public void onTxDeleted(final String hash, int notifyUser, int recommendRescan) {
        super.onTxDeleted(hash, notifyUser, recommendRescan);
        Log.e(TAG, "onTxDeleted: " + String.format("hash: %s, notifyUser: %d, recommendRescan: %d", hash, notifyUser, recommendRescan));
        final Context ctx = BreadApp.getBreadContext();
        if (ctx != null) {
            if (recommendRescan != 0)
                BRSharedPrefs.putScanRecommended(ctx, getIso(ctx), true);
            if (notifyUser != 0)
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        //todo show a message
                        BRToast.showCustomToast(ctx, "Transaction deleted: " + hash, BRActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
                    }
                });
            TransactionStorageManager.removeTransaction(ctx, this, hash);
        } else {
            Log.e(TAG, "onTxDeleted: Failed! ctx is null");
        }
        for (OnTxListModified list : txModifiedListeners)
            if (list != null) list.txListModified(hash);
    }

    @Override
    public void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        super.onTxUpdated(hash, blockHeight, timeStamp);
        Log.d(TAG, "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash, blockHeight, timeStamp));
        Context ctx = BreadApp.getBreadContext();
        if (ctx != null) {
            TransactionStorageManager.updateTransaction(ctx, this, new BRTransactionEntity(null, blockHeight, timeStamp, hash, getIso(ctx)));

        } else {
            Log.e(TAG, "onTxUpdated: Failed, ctx is null");
        }
        for (OnTxListModified list : txModifiedListeners)
            if (list != null) list.txListModified(hash);
    }


}
