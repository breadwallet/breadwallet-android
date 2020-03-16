package com.breadwallet.legacy.wallet.wallets.bitcoin;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.BuildConfig;
import com.breadwallet.app.BreadApp;
import com.breadwallet.legacy.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.legacy.presenter.entities.BRPeerEntity;
import com.breadwallet.legacy.presenter.entities.BRTransactionEntity;
import com.breadwallet.legacy.presenter.entities.BlockEntity;
import com.breadwallet.legacy.presenter.entities.CurrencyEntity;
import com.breadwallet.legacy.presenter.entities.PeerEntity;
import com.breadwallet.legacy.wallet.abstracts.BalanceUpdateListener;
import com.breadwallet.legacy.wallet.abstracts.BaseWalletManager;
import com.breadwallet.legacy.wallet.abstracts.OnTxListModified;
import com.breadwallet.legacy.wallet.abstracts.SyncListener;
import com.breadwallet.legacy.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.legacy.wallet.configs.WalletUiConfiguration;
import com.breadwallet.legacy.wallet.wallets.CryptoAddress;
import com.breadwallet.legacy.wallet.wallets.CryptoTransaction;
import com.breadwallet.legacy.wallet.wallets.WalletManagerHelper;
import com.breadwallet.repository.RatesRepository;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.sqlite.BtcBchTransactionDataStore;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.breadwallet.tools.util.BRConstants.ROUNDING_MODE;

public abstract class BaseBitcoinWalletManager implements BaseWalletManager {

    private static final String TAG = BaseBitcoinWalletManager.class.getSimpleName();

    public static final int ONE_BITCOIN_IN_SATOSHIS = 100000000; // 1 Bitcoin in satoshis, 100 millions
    private static final long MAXIMUM_AMOUNT = 21000000; // Maximum number of coins available
    // TODO Android code shouldn't retry at all, all the retries should be handled by core, temporary fix for CORE-266
    private static final int SYNC_MAX_RETRY = 1;
    private static final int SYNC_RETRY_DELAY_SECONDS = 3;

    public static final String BITCOIN_CURRENCY_CODE = "BTC";
    public static final String BITCASH_CURRENCY_CODE = "BCH";

    private WalletSettingsConfiguration mSettingsConfig;

    private WalletManagerHelper mWalletManagerHelper;
    private int mSyncRetryCount = 0;
    private static final int CREATE_WALLET_MAX_RETRY = 3;
    private int mCreateWalletAllowedRetries = CREATE_WALLET_MAX_RETRY;
    private WalletUiConfiguration mUiConfig;

    private Executor mListenerExecutor = Executors.newSingleThreadExecutor();

    public enum RescanMode {
        FROM_BLOCK, FROM_CHECKPOINT, FULL
    }

    BaseBitcoinWalletManager(Context context, double earliestPeerTime) {
        mWalletManagerHelper = new WalletManagerHelper();

        Log.d(getTag(), "connectWallet:" + Thread.currentThread().getName());
        if (context == null) {
            Log.e(getTag(), "connectWallet: context is null");
            return;
        }
        mUiConfig = new WalletUiConfiguration(getColor(), null, true, WalletManagerHelper.MAX_DECIMAL_PLACES_FOR_UI);

    }

    protected abstract String getTag();

    protected abstract String getColor();

    @Override
    public CryptoTransaction[] getTxs(Context app) {
        return new CryptoTransaction[0];
    }

    public void setSettingsConfig(WalletSettingsConfiguration settingsConfig) {
        this.mSettingsConfig = settingsConfig;
    }

    @Override
    public BigDecimal getTxFee(CryptoTransaction tx) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        BigDecimal fee;
        if (amount == null) {
            return null;
        }
        if (amount.longValue() == 0) {
            fee = BigDecimal.ZERO;
        } else {
            CryptoTransaction tx = null;
            if (isAddressValid(address)) {
                tx = createTransaction(amount, address);
            }

            if (tx == null) {
                fee = BigDecimal.ZERO;
            } else {
                fee = getTxFee(tx);
                if (fee == null || fee.compareTo(BigDecimal.ZERO) <= 0) {
                    fee = BigDecimal.ZERO;
                }
            }
        }
        return fee;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return BigDecimal.ZERO;
    }

    @Override
    public String getTxAddress(CryptoTransaction tx) {
        return "";
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTransactionAmount(CryptoTransaction tx) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return BigDecimal.ZERO;
    }

    @Override
    public void updateFee(Context app) {
        if (app == null) {
            app = BreadApp.getBreadContext();
            if (app == null) {
                Log.e(getTag(), "updateFee: FAILED, app is null");
                return;
            }
        }
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
    public boolean generateWallet(Context app) {
        //no need, one key for all wallets so far
        return true;
    }

    @Override
    public String getSymbol(Context app) {

        String currencySymbolString = BRConstants.BITS_SYMBOL;
        if (app != null) {
            int unit = BRSharedPrefs.getCryptoDenomination(app, getCurrencyCode());
            switch (unit) {
                case BRConstants.CURRENT_UNIT_BITS:
                    currencySymbolString = "μ" + getCurrencyCode();
                    break;
                case BRConstants.CURRENT_UNIT_MBITS:
                    currencySymbolString = "m" + getCurrencyCode();
                    break;
                case BRConstants.CURRENT_UNIT_BITCOINS:
                    currencySymbolString = getCurrencyCode();
                    break;
            }
        }
        return currencySymbolString;
    }

    @Override
    public abstract String getCurrencyCode();

    @Override
    public abstract String getScheme();

    @Override
    public abstract String getName();

    @Override
    public String getDenominator() {
        return String.valueOf(ONE_BITCOIN_IN_SATOSHIS);
    }

    @Override
    public CryptoAddress getReceiveAddress(Context app) {
        return null;
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address) {
        return null;
    }

    @Override
    public abstract String decorateAddress(String address);

    @Override
    public abstract String undecorateAddress(String address);

    @Override
    public WalletSettingsConfiguration getSettingsConfiguration() {
        return mSettingsConfig;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        int unit = BRSharedPrefs.getCryptoDenomination(app, getCurrencyCode());
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                return 2;
            default:
                return WalletManagerHelper.MAX_DECIMAL_PLACES;
        }
    }

    @Override
    public BigDecimal getBalance() {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return BigDecimal.ZERO;
    }

    @Override
    public void wipeData(Context app) {
        BRSharedPrefs.clearAllPrefs(app);
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return new BigDecimal(MAXIMUM_AMOUNT);
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return mUiConfig;
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        CurrencyEntity btcFiatRate = RatesRepository.getInstance(app).getCurrencyByCode(WalletBitcoinManager.BITCOIN_CURRENCY_CODE, BRSharedPrefs.getPreferredFiatIso(app));
        CurrencyEntity currencyBtcRate = RatesRepository.getInstance(app).getCurrencyByCode(getCurrencyCode(), WalletBitcoinManager.BITCOIN_CURRENCY_CODE);
        if (btcFiatRate == null || currencyBtcRate == null) {
            return BigDecimal.ZERO;
        }
        return null;
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        if (app == null) {
            return null;
        }
        BigDecimal balance = getFiatForSmallestCrypto(app, getBalance(), null);
        if (balance == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(balance.doubleValue());
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        return null;
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal fiatAmount) {
        if (fiatAmount.doubleValue() == 0) {
            return fiatAmount;
        }
        BigDecimal rate = getFiatExchangeRate(app);
        if (rate == null || rate.equals(BigDecimal.ZERO)) {
            return null;
        }
        int unit = BRSharedPrefs.getCryptoDenomination(app, getCurrencyCode());
        BigDecimal result = BigDecimal.ZERO;
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = fiatAmount.divide(rate, 2, ROUNDING_MODE).multiply(new BigDecimal("1000000"));
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = fiatAmount.divide(rate, getMaxDecimalPlaces(app), ROUNDING_MODE);
                break;
        }
        return result;
    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        BigDecimal result = BigDecimal.ZERO;
        int unit = BRSharedPrefs.getCryptoDenomination(app, getCurrencyCode());
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
        BigDecimal result = BigDecimal.ZERO;
        int unit = BRSharedPrefs.getCryptoDenomination(app, getCurrencyCode());
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
        BigDecimal rate = getFiatExchangeRate(app);
        if (rate == null || rate.equals(BigDecimal.ZERO)) {
            Log.e(getTag(), "getSmallestCryptoForFiat: no exchange rate data!");
            return amount;
        }
        //convert c to $.
        return amount.divide(rate, 8, ROUNDING_MODE).multiply(new BigDecimal("100000000"));
    }

    @Override
    public String getAddress(Context context) {
        return BRSharedPrefs.getReceiveAddress(context, getCurrencyCode());
    }

    @Override
    public boolean isAddressValid(String address) {
        return false;
    }

    @Override
    public byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] seed) {
        return new byte[0];
    }

    // TODO only ETH and ERC20
    @Override
    public void watchTransactionForHash(CryptoTransaction tx, OnHashUpdated listener) {
    }

    @Override
    public long getRelayCount(byte[] txHash) {
        return 0;
    }

    @Override
    public double getSyncProgress(long startHeight) {
        return 0.0;
    }

    @Override
    public double getConnectStatus() {
        return 0.0;
    }

    @Override
    public void connect(Context app) {
    }

    @Override
    public void disconnect(Context app) {
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        return false;
    }

    /**
     * The rescan now operates in 3 modes (Stage 1: Incremental rescan):
     * <p>
     * 1)The latest block in which a send transaction originating from the user's wallet was confirmed.
     * If there are no sends, only the following two starting points will be used.
     * 2)The latest block checkpoint that is hardcoded in the app.
     * 3)500 blocks before the the block height at which the wallet was created (from KV store),
     * or the first block after the introduction of BIP39 if there is no date stored in the KV store.
     *
     * @param app
     */
    @Override
    public void rescan(Context app) {
        //the last time the app has done a rescan (not a regular scan)
        long lastRescanTime = BRSharedPrefs.getLastRescanTime(app, getCurrencyCode());
        long now = System.currentTimeMillis();
        //the last rescan mode that was used for rescan
        String lastRescanModeUsedValue = BRSharedPrefs.getLastRescanModeUsed(app, getCurrencyCode());
        //the last successful send transaction's blockheight (if there is one, 0 otherwise)
        long lastSentTransactionBlockheight = BRSharedPrefs.getLastSendTransactionBlockheight(app, getCurrencyCode());
        //was the rescan used within the last 24 hours
        boolean wasLastRescanWithin24h = now - lastRescanTime <= DateUtils.DAY_IN_MILLIS;

        if (wasLastRescanWithin24h) {
            if (isModeSame(RescanMode.FROM_BLOCK, lastRescanModeUsedValue)) {
                rescan(app, RescanMode.FROM_CHECKPOINT);
            } else if (isModeSame(RescanMode.FROM_CHECKPOINT, lastRescanModeUsedValue)) {
                rescan(app, RescanMode.FULL);
            }
        } else {
            if (lastSentTransactionBlockheight > 0) {
                rescan(app, RescanMode.FROM_BLOCK);
            } else {
                rescan(app, RescanMode.FROM_CHECKPOINT);
            }
        }
    }

    /**
     * Trigger the appropriate rescan and save the name and time to BRSharedPrefs
     *
     * @param app  android context to use
     * @param mode the RescanMode to be used
     */
    private void rescan(Context app, RescanMode mode) {
        if (RescanMode.FROM_BLOCK == mode) {
            long lastSentTransactionBlockheight = BRSharedPrefs.getLastSendTransactionBlockheight(app, getCurrencyCode());
            Log.d(TAG, "rescan -> with last block: " + lastSentTransactionBlockheight);
        } else if (RescanMode.FROM_CHECKPOINT == mode) {
            Log.e(TAG, "rescan -> from checkpoint");
        } else if (RescanMode.FULL == mode) {
            Log.e(TAG, "rescan -> full");
        } else {
            throw new IllegalArgumentException("RescanMode is invalid, mode -> " + mode);
        }
        long now = System.currentTimeMillis();
        BRSharedPrefs.putLastRescanModeUsed(app, getCurrencyCode(), mode.name());
        BRSharedPrefs.putLastRescanTime(app, getCurrencyCode(), now);
    }

    /**
     * @param mode       the RescanMode enum to compare to
     * @param stringMode the stored enum value
     * @return true if the same mode
     */
    private boolean isModeSame(RescanMode mode, String stringMode) {
        if (stringMode == null) {
            //prevent NPE
            stringMode = "";
        }
        try {
            if (mode == RescanMode.valueOf(stringMode)) {
                return true;
            }
        } catch (IllegalArgumentException ex) {
            //do nothing, illegal argument
        }
        return false;

    }

    public boolean networkIsReachable() {
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    @Override
    public int getForkId() {
        return 0;
    }

    @Override
    public void addBalanceChangedListener(BalanceUpdateListener listener) {
        mWalletManagerHelper.addBalanceChangedListener(listener);
    }

    @Override
    public void removeBalanceChangedListener(BalanceUpdateListener listener) {
        mWalletManagerHelper.removeBalanceChangedListener(listener);
    }

    @Override
    public void onBalanceChanged(BigDecimal balance) {
        mWalletManagerHelper.onBalanceChanged(getCurrencyCode(), balance);
    }

    protected void updateCachedAddress(Context context, String address) {
        if (Utils.isNullOrEmpty(address)) {
            Log.e(getTag(), "refreshAddress: WARNING, retrieved address:" + address);
        }
        BRSharedPrefs.putReceiveAddress(context, address, getCurrencyCode());
    }

    @Override
    public void addSyncListener(SyncListener listener) {
        mWalletManagerHelper.addSyncListener(listener);
    }

    @Override
    public void removeSyncListener(SyncListener listener) {
        mWalletManagerHelper.removeSyncListener(listener);
    }

    public void onSyncStarted() {
        mWalletManagerHelper.onSyncStarted();
    }

    public void onSyncStopped(String error) {
        mWalletManagerHelper.onSyncStopped(error);
    }

    @Override
    public void addTxListModifiedListener(OnTxListModified listener) {
        mWalletManagerHelper.addTxListModifiedListener(listener);
    }

    @Override
    public void removeTxListModifiedListener(OnTxListModified listener) {
        mWalletManagerHelper.removeTxListModifiedListener(listener);
    }

    public void onTxListModified(String hash) {
        mWalletManagerHelper.onTxListModified(hash);
    }

    /**
     * Core callback for balance updates.
     *
     * @param balance
     */
    public void balanceChanged(final long balance) {
        final Context context = BreadApp.getBreadContext();
        onBalanceChanged(new BigDecimal(balance));
        refreshAddress(context);
    }

    public void txStatusUpdate() {
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                onTxListModified(null);
            }
        });

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                long blockHeight = 0;

                final Context context = BreadApp.getBreadContext();
                if (context != null) {
                    BRSharedPrefs.putLastBlockHeight(context, getCurrencyCode(), (int) blockHeight);
                }
            }
        });
    }

    public void syncStarted() {
        Log.d(getTag(), "syncStarted: ");
        final Context app = BreadApp.getBreadContext();
        onSyncStarted();
    }

    protected abstract void syncStopped(Context context);

    public void syncStopped(final String error) {
        Log.d(getTag(), "syncStopped: " + error);
        final Context context = BreadApp.getBreadContext();
        if (Utils.isNullOrEmpty(error)) {
            BRSharedPrefs.putAllowSpend(context, getCurrencyCode(), true);
            syncStopped(context);
        }

        onSyncStopped(error);

        if (!Utils.isNullOrEmpty(error)) {
            if (mSyncRetryCount < SYNC_MAX_RETRY && networkIsReachable()) {
                Log.e(getTag(), "syncStopped: Retrying: " + mSyncRetryCount);
                //Retry
                mSyncRetryCount++;
                ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
            } else {
                //Give up
                Log.e(getTag(), "syncStopped: Giving up: " + mSyncRetryCount);
                mSyncRetryCount = 0;
                if (BuildConfig.DEBUG) {
                    new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, "Syncing failed, retried " + SYNC_MAX_RETRY + " times.", Toast.LENGTH_LONG).show());
                }
            }
        }

    }
    @Override
    public boolean checkConfirmations(int conformations) {
        return mWalletManagerHelper.checkConfirmations(conformations);
    }
}
