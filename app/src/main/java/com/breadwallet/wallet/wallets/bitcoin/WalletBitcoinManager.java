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
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.BRCoreMerkleBlock;
import com.breadwallet.core.BRCorePeer;
import com.breadwallet.core.BRCorePeerManager;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreWallet;
import com.breadwallet.core.BRCoreWalletManager;
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
import com.breadwallet.tools.animation.BRDialog;
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
import com.breadwallet.tools.util.SymbolUtils;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseAddress;
import com.breadwallet.wallet.abstracts.BaseTransaction;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.google.firebase.crash.FirebaseCrash;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

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

    public static final int ONE_BITCOIN = 100000000;

    private static final String mName = "Bitcoin";
    public static final String BTC_SCHEME = "bitcoin";

    public static final long MAX_BTC = 21000000;

    private static WalletBitcoinManager instance;
    private WalletUiConfiguration uiConfig;
    private WalletSettingsConfiguration settingsConfig;

    private int mSyncRetryCount = 0;
    private static final int SYNC_MAX_RETRY = 3;
    protected int createWalletAllowedRetries = 3;

    private boolean isInitiatingWallet;

    private List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
    private List<OnTxStatusUpdatedListener> txStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> syncListeners = new ArrayList<>();
    private List<OnTxListModified> txModifiedListeners = new ArrayList<>();

    private Executor listenerExecutor = Executors.newSingleThreadExecutor();

    public synchronized static WalletBitcoinManager getInstance(Context app) {
        if (instance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(app);
            if (Utils.isNullOrEmpty(rawPubKey)) {
                Log.e(TAG, "getInstance: rawPubKey is null");
                return null;
            }
            BRCoreMasterPubKey pubKey = new BRCoreMasterPubKey(rawPubKey, false);
            long time = BRKeyStore.getWalletCreationTime(app);
//            if (Utils.isEmulatorOrDebug(app)) time = 1517955529;
            //long time = 1519190488;
//            long time = (System.currentTimeMillis() / 1000) - 3 * 7 * 24 * 60 * 60; // 3 * 7

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

            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    if (BRSharedPrefs.getStartHeight(app, getIso(app)) == 0)
                        BRSharedPrefs.putStartHeight(app, getIso(app), getPeerManager().getLastBlockHeight());

                    BigDecimal fee = BRSharedPrefs.getFeeRate(app, getIso(app));
                    BigDecimal economyFee = BRSharedPrefs.getEconomyFeeRate(app, getIso(app));
                    if (fee.compareTo(new BigDecimal(0)) == 0) {
                        fee = new BigDecimal(getWallet().getDefaultFeePerKb());
                        BREventManager.getInstance().pushEvent("wallet.didUseDefaultFeePerKB");
                    }
                    getWallet().setFeePerKb(BRSharedPrefs.getFavorStandardFee(app, getIso(app)) ? fee.longValue() : economyFee.longValue());
                    WalletsMaster.getInstance(app).updateFixedPeer(app, WalletBitcoinManager.this);
                }
            });

            uiConfig = new WalletUiConfiguration("f29500", null,  true);
            settingsConfig = new WalletSettingsConfiguration(app, ISO, getFingerprintLimits(app));
        } finally {
            isInitiatingWallet = false;
        }

    }

    private List<BigDecimal> getFingerprintLimits(Context app) {
        List<BigDecimal> result = new ArrayList<>();
        result.add(new BigDecimal(ONE_BITCOIN).divide(new BigDecimal(1000), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(new BigDecimal(ONE_BITCOIN).divide(new BigDecimal(100), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(new BigDecimal(ONE_BITCOIN).divide(new BigDecimal(10), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(new BigDecimal(ONE_BITCOIN));
        result.add(new BigDecimal(ONE_BITCOIN).multiply(new BigDecimal(10)));
        return result;
    }

    protected BRCoreWallet createWalletRetry() {
        Context app = BreadApp.getBreadContext();
        if (0 == createWalletAllowedRetries) {
            // The app is dead - tell the user...
            BRDialog.showSimpleDialog(app, "Wallet error!", "please contact support@breadwallet.com");
            // ... for now just this.  App crashes after this
            return null;
        }

        createWalletAllowedRetries--;

        // clear out the SQL data - ensure that loadTransaction returns an empty array
        // mark this Manager a needing a sync.

        BtcBchTransactionDataStore.getInstance(app).deleteAllTransactions(app, ISO);
        BRReportsManager.reportBug(new RuntimeException("Wallet creation failed, after clearing tx size: " + loadTransactions().length));
        // Try again
        return createWallet();
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
    public BaseTransaction[] getTxs() {
        return (BaseTransaction[]) getWallet().getTransactions();
    }

    @Override
    public BigDecimal getTxFee(BaseTransaction tx) {
        return new BigDecimal(getWallet().getTransactionFee(tx.getCoreTx()));
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        BigDecimal fee;
        if (amount == null) return null;
        if (amount.longValue() == 0) {
            fee = new BigDecimal(0);
        } else {
            BaseTransaction tx = null;
            if (isAddressValid(address)) {
                tx = createTransaction(amount, address);
            }

            if (tx == null) {
                fee = new BigDecimal(getWallet().getFeeForTransactionAmount(amount.longValue()));
            } else {
                fee = getTxFee(tx);
                if (fee == null || fee.compareTo(new BigDecimal(0)) <= 0)
                    fee = new BigDecimal(getWallet().getFeeForTransactionAmount(amount.longValue()));
            }
        }
        return fee;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return new BigDecimal(getWallet().getFeeForTransactionSize(size.longValue()));
    }

    @Override
    public BaseAddress getTxAddress(BaseTransaction tx) {
        return createAddress(getWallet().getTransactionAddress(tx.getCoreTx()).stringify());
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        return new BigDecimal(getWallet().getMaxOutputAmount());
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return new BigDecimal(getWallet().getMinOutputAmount());
    }

    @Override
    public BigDecimal getTransactionAmount(BaseTransaction tx) {
        return new BigDecimal(getWallet().getTransactionAmount(tx.getCoreTx()));
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return new BigDecimal(BRCoreTransaction.getMinOutputAmount());
    }

    @Override
    public void updateFee(Context app) {
        if (app == null) {
            app = BreadApp.getBreadContext();
            if (app == null) {
                Log.e(TAG, "updateFee: FAILED, app is null");
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

            if (fee.compareTo(new BigDecimal(0)) > 0 && fee.compareTo(new BigDecimal(getWallet().getMaxFeePerKb())) < 0) {
                BRSharedPrefs.putFeeRate(app, getIso(app), fee);
                getWallet().setFeePerKb(BRSharedPrefs.getFavorStandardFee(app, getIso(app)) ? fee.longValue() : economyFee.longValue());
                BRSharedPrefs.putFeeTime(app, getIso(app), System.currentTimeMillis()); //store the time of the last successful fee fetch
            } else {
                FirebaseCrash.report(new NullPointerException("Fee is weird:" + fee));
            }
            if (economyFee.compareTo(new BigDecimal(0)) > 0 && economyFee.compareTo(new BigDecimal(getWallet().getMaxFeePerKb())) < 0) {
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
    public List<TxUiHolder> getTxUiHolders(Context app) {
        BRCoreTransaction txs[] = getWallet().getTransactions();
        if (txs == null || txs.length <= 0) return null;
        List<TxUiHolder> uiTxs = new ArrayList<>();
        for (int i = txs.length - 1; i >= 0; i--) { //revere order
            BRCoreTransaction tx = txs[i];
            String toAddress = null;
            //if sent
            if (getWallet().getTransactionAmountSent(tx) > 0) {
                toAddress = tx.getOutputAddresses()[0];
            } else {
                for (String to : tx.getOutputAddresses()) {
                    if (containsAddress(to)) {
                        toAddress = to;
                        break;
                    }
                }
            }
            if (toAddress == null) throw new NullPointerException("Failed to retrieve toAddress");
            uiTxs.add(new TxUiHolder(tx, getWallet().getTransactionAmountSent(tx) <= 0, tx.getTimestamp(), (int) tx.getBlockHeight(), tx.getHash(),
                    tx.getReverseHash(), new BigDecimal(getWallet().getTransactionFee(tx)), null,
                    toAddress, tx.getInputAddresses()[0],
                    new BigDecimal(getWallet().getBalanceAfterTransaction(tx)), (int) tx.getSize(),
                    new BigDecimal(getWallet().getTransactionAmount(tx)), getWallet().transactionIsValid(tx)));
        }

        return uiTxs;
    }

    @Override
    public boolean containsAddress(String address) {
        return !Utils.isNullOrEmpty(address) && getWallet().containsAddress(new BRCoreAddress(address));
    }

    @Override
    public boolean addressIsUsed(String address) {
        return !Utils.isNullOrEmpty(address) && getWallet().addressIsUsed(new BRCoreAddress(address));
    }

    @Override
    public BaseAddress createAddress(String address) {
        return new BTCAddress(address);
    }

    @Override
    public boolean generateWallet(Context app) {
        //no need, one key for all wallets so far
        return true;
    }

    @Override
    public String getSymbol(Context app) {

        String currencySymbolString = BRConstants.symbolBits;
        if (app != null) {
            int unit = BRSharedPrefs.getCryptoDenomination(app, getIso(app));
            switch (unit) {
                case BRConstants.CURRENT_UNIT_BITS:
                    currencySymbolString = "μ" + ISO;
                    break;
                case BRConstants.CURRENT_UNIT_MBITS:
                    currencySymbolString = "m" + ISO;
                    break;
                case BRConstants.CURRENT_UNIT_BITCOINS:
                    currencySymbolString =  ISO;
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
    public BaseAddress getReceiveAddress(Context app) {
        return createAddress(getWallet().getReceiveAddress().stringify());
    }

    @Override
    public BaseTransaction createTransaction(BigDecimal amount, String address) {
        if (Utils.isNullOrEmpty(address)) {
            Log.e(TAG, "createTransaction: can't create, address is null");
            return null;
        }
        BRCoreTransaction tx = getWallet().createTransaction(amount.longValue(), new BRCoreAddress(address));
        return tx == null ? null : new CryptoTransaction(tx);
    }

    @Override
    public String decorateAddress(Context app, String addr) {
        return addr; // no need to decorate
    }

    @Override
    public String undecorateAddress(Context app, String addr) {
        return addr; //no need to undecorate
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        int unit = BRSharedPrefs.getCryptoDenomination(app, getIso(app));
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                return 2;
            default:
                return 5;
        }
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, getIso(app));
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return new BigDecimal(getWallet().getTotalSent());
    }

    @Override
    public void wipeData(Context app) {
        BtcBchTransactionDataStore.getInstance(app).deleteAllTransactions(app, getIso(app));
        MerkleBlockDataSource.getInstance(app).deleteAllBlocks(app, getIso(app));
        PeerDataSource.getInstance(app).deleteAllPeers(app, getIso(app));
        BRSharedPrefs.clearAllPrefs(app);
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
    public void refreshAddress(Context app) {
        BRCoreAddress address = getWallet().getReceiveAddress();
        if (Utils.isNullOrEmpty(address.stringify())) {
            Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
        }
        BRSharedPrefs.putReceiveAddress(app, address.stringify(), getIso(app));

    }

    @Override
    public void refreshCachedBalance(Context app) {
        BigDecimal balance = new BigDecimal(getWallet().getBalance());
        BRSharedPrefs.putCachedBalance(app, ISO, balance);
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
    public WalletSettingsConfiguration getSettingsConfiguration() {
        return settingsConfig;
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), BRSharedPrefs.getPreferredFiatIso(app));
        return new BigDecimal(ent == null ? 0 : ent.rate); //dollars
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        if (app == null) return null;
        BigDecimal bal = getFiatForSmallestCrypto(app, getCachedBalance(app), null);
        return new BigDecimal(bal == null ? 0 : bal.doubleValue());
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        if (amount.doubleValue() == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        if (ent == null)
            ent = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), iso);
        if (ent == null) {
            return null;
        }
        double rate = ent.rate;
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE);
        return cryptoAmount.multiply(new BigDecimal(rate));
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal fiatAmount) {
        if (fiatAmount.doubleValue() == 0) return fiatAmount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), iso);
        if (ent == null) return null;
        double rate = ent.rate;
        //convert c to $.
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
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), iso);
        if (ent == null) {
            Log.e(TAG, "getSmallestCryptoForFiat: no exchange rate data!");
            return amount;
        }
        double rate = ent.rate;
        //convert c to $.
        return amount.divide(new BigDecimal(rate), 8, ROUNDING_MODE).multiply(new BigDecimal("100000000"));
    }

    @Override
    public int getForkId() {
        return super.getForkId();
    }

    @Override
    public boolean isAddressValid(String address) {
        return !Utils.isNullOrEmpty(address) && new BRCoreAddress(address).isValid();
    }

    @Override
    public byte[] signAndPublishTransaction(BaseTransaction tx, byte[] seed) {
        return super.signAndPublishTransaction(tx.getCoreTx(), seed);
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
    public long getRelayCount(byte[] txHash) {
        if (Utils.isNullOrEmpty(txHash)) return 0;
        return getPeerManager().getRelayCount(txHash);
    }

    @Override
    public double getSyncProgress(long startHeight) {
        return getPeerManager().getSyncProgress(startHeight);
    }

    @Override
    public double getConnectStatus() {
        BRCorePeer.ConnectStatus status = getPeerManager().getConnectStatus();
        if (status == BRCorePeer.ConnectStatus.Disconnected)
            return 0;
        else if (status == BRCorePeer.ConnectStatus.Connecting)
            return 1;
        else if (status == BRCorePeer.ConnectStatus.Connected)
            return 2;
        else if (status == BRCorePeer.ConnectStatus.Unknown)
            return 3;
        else
            throw new IllegalArgumentException();
    }

    @Override
    public void connect(Context app) {
        getPeerManager().connect();
    }

    @Override
    public void disconnect(Context app) {
        getPeerManager().disconnect();
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        return false;
    }

    @Override
    public void rescan() {
        getPeerManager().rescan();
    }


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

    public void balanceChanged(long balance) {
        super.balanceChanged(balance);
        Context app = BreadApp.getBreadContext();
        setCachedBalance(app, new BigDecimal(balance));
        for (OnTxListModified list : txModifiedListeners)
            if (list != null) list.txListModified(null);

    }

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

    public void saveBlocks(boolean replace, BRCoreMerkleBlock[] blocks) {
        super.saveBlocks(replace, blocks);

        Context app = BreadApp.getBreadContext();
        if (app == null) return;
        if (replace) MerkleBlockDataSource.getInstance(app).deleteAllBlocks(app, getIso(app));
        BlockEntity[] entities = new BlockEntity[blocks.length];
        for (int i = 0; i < entities.length; i++) {
            entities[i] = new BlockEntity(blocks[i].serialize(), (int) blocks[i].getHeight());
        }

        MerkleBlockDataSource.getInstance(app).putMerkleBlocks(app, getIso(app), entities);
    }

    public void savePeers(boolean replace, BRCorePeer[] peers) {
        super.savePeers(replace, peers);
        Context app = BreadApp.getBreadContext();
        if (app == null) return;
        if (replace) PeerDataSource.getInstance(app).deleteAllPeers(app, getIso(app));
        PeerEntity[] entities = new PeerEntity[peers.length];
        for (int i = 0; i < entities.length; i++) {
            entities[i] = new PeerEntity(peers[i].getAddress(), TypesConverter.intToBytes(peers[i].getPort()), TypesConverter.long2byteArray(peers[i].getTimestamp()));
        }
        PeerDataSource.getInstance(app).putPeers(app, getIso(app), entities);

    }

    public boolean networkIsReachable() {
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    public BRCoreTransaction[] loadTransactions() {
        Context app = BreadApp.getBreadContext();

        List<BRTransactionEntity> txs = BtcBchTransactionDataStore.getInstance(app).getAllTransactions(app, getIso(app));
        if (txs == null || txs.size() == 0) return new BRCoreTransaction[0];
        BRCoreTransaction arr[] = new BRCoreTransaction[txs.size()];
        for (int i = 0; i < txs.size(); i++) {
            BRTransactionEntity ent = txs.get(i);
            arr[i] = new BRCoreTransaction(ent.getBuff(), ent.getBlockheight(), ent.getTimestamp());
        }
        return arr;
    }

    public BRCoreMerkleBlock[] loadBlocks() {
        Context app = BreadApp.getBreadContext();
        List<BRMerkleBlockEntity> blocks = MerkleBlockDataSource.getInstance(app).getAllMerkleBlocks(app, getIso(app));
        if (blocks == null || blocks.size() == 0) return new BRCoreMerkleBlock[0];
        BRCoreMerkleBlock arr[] = new BRCoreMerkleBlock[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            BRMerkleBlockEntity ent = blocks.get(i);
            arr[i] = new BRCoreMerkleBlock(ent.getBuff(), ent.getBlockHeight());
        }
        return arr;
    }

    public BRCorePeer[] loadPeers() {
        Context app = BreadApp.getBreadContext();
        List<BRPeerEntity> peers = PeerDataSource.getInstance(app).getAllPeers(app, getIso(app));
        if (peers == null || peers.size() == 0) return new BRCorePeer[0];
        BRCorePeer arr[] = new BRCorePeer[peers.size()];
        for (int i = 0; i < peers.size(); i++) {
            BRPeerEntity ent = peers.get(i);
            arr[i] = new BRCorePeer(ent.getAddress(), TypesConverter.bytesToInt(ent.getPort()), TypesConverter.byteArray2long(ent.getTimeStamp()));
        }
        return arr;
    }

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

    public void onTxAdded(BRCoreTransaction transaction) {
        super.onTxAdded(transaction);
        final Context ctx = BreadApp.getBreadContext();
        final WalletsMaster master = WalletsMaster.getInstance(ctx);

        TxMetaData metaData = KVStoreManager.getInstance().createMetadata(ctx, this, new CryptoTransaction(transaction));
        KVStoreManager.getInstance().putTxMetaData(ctx, metaData, transaction.getHash());

        final long amount = getWallet().getTransactionAmount(transaction);
        if (amount > 0) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    String am = CurrencyUtils.getFormattedAmount(ctx, getIso(ctx), getCryptoForSmallestCrypto(ctx, new BigDecimal(amount)));
                    BigDecimal bigAmount = master.getCurrentWallet(ctx).getFiatForSmallestCrypto(ctx, new BigDecimal(amount), null);
                    String amCur = CurrencyUtils.getFormattedAmount(ctx, BRSharedPrefs.getPreferredFiatIso(ctx), bigAmount == null ? new BigDecimal(0) : bigAmount);
                    String formatted = String.format("%s (%s)", am, amCur);
                    final String strToShow = String.format(ctx.getString(R.string.TransactionDetails_received), formatted);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!BRToast.isToastShown()) {
                                if (Utils.isEmulatorOrDebug(ctx))
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
                                if (ctx instanceof Activity && BRSharedPrefs.getShowNotification(ctx))
                                    BRNotificationManager.sendNotification((Activity) ctx, R.drawable.notification_icon, ctx.getString(R.string.app_name), strToShow, 1);
                                else
                                    Log.e(TAG, "onTxAdded: ctx is not activity");
                            }
                        }
                    }, 1000);


                }
            });
        }
        if (ctx != null)
            TransactionStorageManager.putTransaction(ctx, getIso(ctx), new BRTransactionEntity(transaction.serialize(), transaction.getBlockHeight(), transaction.getTimestamp(), BRCoreKey.encodeHex(transaction.getHash()), getIso(ctx)));
        else
            Log.e(TAG, "onTxAdded: ctx is null!");
        for (OnTxListModified list : txModifiedListeners)
            if (list != null) list.txListModified(transaction.getReverseHash());
    }

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
                        BRDialog.showSimpleDialog(ctx, "Transaction failed!", hash);
                    }
                });
            TransactionStorageManager.removeTransaction(ctx, getIso(ctx), hash);
        } else {
            Log.e(TAG, "onTxDeleted: Failed! ctx is null");
        }
        for (OnTxListModified list : txModifiedListeners)
            if (list != null) list.txListModified(hash);
    }

    public void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        super.onTxUpdated(hash, blockHeight, timeStamp);
        Log.d(TAG, "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash, blockHeight, timeStamp));
        Context ctx = BreadApp.getBreadContext();
        if (ctx != null) {
            TransactionStorageManager.updateTransaction(ctx, getIso(ctx), new BRTransactionEntity(null, blockHeight, timeStamp, hash, getIso(ctx)));

        } else {
            Log.e(TAG, "onTxUpdated: Failed, ctx is null");
        }
        for (OnTxListModified list : txModifiedListeners)
            if (list != null) list.txListModified(hash);
    }

}
