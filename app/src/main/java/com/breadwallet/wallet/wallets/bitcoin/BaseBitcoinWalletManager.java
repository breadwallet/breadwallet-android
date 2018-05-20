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
import com.breadwallet.core.ethereum.BREthereumAmount;
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
import com.breadwallet.tools.manager.BRNotificationManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.sqlite.BtcBchTransactionDataStore;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.sqlite.TransactionStorageManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.wallets.CryptoAddress;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.WalletManagerHelper;
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

public abstract class BaseBitcoinWalletManager extends BRCoreWalletManager implements BaseWalletManager {

    protected static final int ONE_BITCOIN = 100000000; // 1 Bitcoin in satoshis
    private static final long MAXIMUM_AMOUNT = 21000000; // Maximum number of coins available
    private static final int SYNC_MAX_RETRY = 3;

    private WalletManagerHelper mWalletManagerHelper;
    private int mSyncRetryCount = 0;
    private int mCreateWalletAllowedRetries = 3;
    private WalletUiConfiguration mUiConfig;
    private WalletSettingsConfiguration mSettingsConfig;
    private Executor mListenerExecutor = Executors.newSingleThreadExecutor();

    public BaseBitcoinWalletManager(Context context, BRCoreMasterPubKey masterPubKey, BRCoreChainParams chainParams, double earliestPeerTime) {
        super(masterPubKey, chainParams, earliestPeerTime);
        mWalletManagerHelper = new WalletManagerHelper();

        Log.d(getTag(), "connectWallet:" + Thread.currentThread().getName());
        if (context == null) {
            Log.e(getTag(), "connectWallet: context is null");
            return;
        }
        String firstAddress = masterPubKey.getPubKeyAsCoreKey().address();
        BRSharedPrefs.putFirstAddress(context, firstAddress);

        mUiConfig = new WalletUiConfiguration(getColor(), null, true);
        mSettingsConfig = new WalletSettingsConfiguration(context, getIso(context), getFingerprintLimits(context));
    }

//    protected WalletManagerHelper getWalletManagerHelper() {
//        return mWalletManagerHelper;
//    }

    protected abstract String getTag();

    protected abstract String getColor();

    protected abstract  List<BigDecimal> getFingerprintLimits(Context context);

    protected BRCoreWallet createWalletRetry() {
        Context app = BreadApp.getBreadContext();
        if (0 == mCreateWalletAllowedRetries) {
            // The app is dead - tell the user...
            BRDialog.showSimpleDialog(app, "Wallet error!", "please contact support@breadwallet.com");
            // ... for now just this.  App crashes after this
            return null;
        }

        mCreateWalletAllowedRetries--;

        // clear out the SQL data - ensure that loadTransaction returns an empty array
        // mark this Manager a needing a sync.

        BtcBchTransactionDataStore.getInstance(app).deleteAllTransactions(app, getIso(app));
        BRReportsManager.reportBug(new RuntimeException("Wallet creation failed, after clearing tx size: " + loadTransactions().length));
        // Try again
        return createWallet();
    }

    @Override
    protected BRCoreWallet.Listener createWalletListener() {
        return new BRCoreWalletManager.WrappedExecutorWalletListener(
                super.createWalletListener(),
                mListenerExecutor);
    }

    @Override
    protected BRCorePeerManager.Listener createPeerManagerListener() {
        return new BRCoreWalletManager.WrappedExecutorPeerManagerListener(
                super.createPeerManagerListener(),
                mListenerExecutor);
    }

    @Override
    public CryptoTransaction[] getTxs(Context app) {
        BRCoreTransaction[] txs = getWallet().getTransactions();
        CryptoTransaction[] arr = new CryptoTransaction[txs.length];
        for (int i = 0; i < txs.length; i++) {
            arr[i] = new CryptoTransaction(txs[i]);
        }

        return arr;
    }

    @Override
    public BigDecimal getTxFee(CryptoTransaction tx) {
        return new BigDecimal(getWallet().getTransactionFee(tx.getCoreTx()));
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        BigDecimal fee;
        if (amount == null) return null;
        if (amount.longValue() == 0) {
            fee = new BigDecimal(0);
        } else {
            CryptoTransaction tx = null;
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
    public String getTxAddress(CryptoTransaction tx) {
        return getWallet().getTransactionAddress(tx.getCoreTx()).stringify();
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
    public BigDecimal getTransactionAmount(CryptoTransaction tx) {
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
                Log.e(getTag(), "updateFee: FAILED, app is null");
                return;
            }
        }
        String jsonString = BRApiManager.urlGET(app, "https://" + BreadApp.HOST + "/fee-per-kb?currency=" + getIso(app));
        if (jsonString == null || jsonString.isEmpty()) {
            Log.e(getTag(), "updateFeePerKb: failed to update fee, response string: " + jsonString);
            return;
        }
        BigDecimal fee;
        BigDecimal economyFee;
        try {
            JSONObject obj = new JSONObject(jsonString);
            fee = new BigDecimal(obj.getString("fee_per_kb"));
            economyFee = new BigDecimal(obj.getString("fee_per_kb_economy"));
            Log.d(getTag(), "updateFee: " + getIso(app) + ":" + fee + "|" + economyFee);

            if (fee.compareTo(new BigDecimal(0)) > 0 && fee.compareTo(new BigDecimal(getWallet().getMaxFeePerKb())) < 0) {
                BRSharedPrefs.putFeeRate(app, getIso(app), fee);
                getWallet().setFeePerKb(BRSharedPrefs.getFavorStandardFee(app, getIso(app)) ? fee.longValue() : economyFee.longValue());
                BRSharedPrefs.putFeeTime(app, getIso(app), System.currentTimeMillis()); //store the time of the last successful fee fetch
            } else {
                BRReportsManager.reportBug(new NullPointerException("Fee is weird:" + fee));
            }
            if (economyFee.compareTo(new BigDecimal(0)) > 0 && economyFee.compareTo(new BigDecimal(getWallet().getMaxFeePerKb())) < 0) {
                BRSharedPrefs.putEconomyFeeRate(app, getIso(app), economyFee);
            } else {
                BRReportsManager.reportBug(new NullPointerException("Economy fee is weird:" + economyFee));
            }
        } catch (JSONException e) {
            Log.e(getTag(), "updateFeePerKb: FAILED: " + jsonString, e);
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
                    tx.getReverseHash(), new BigDecimal(getWallet().getTransactionFee(tx)),
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
    public boolean generateWallet(Context app) {
        //no need, one key for all wallets so far
        return true;
    }

    @Override
    public String getSymbol(Context app) {

        String currencySymbolString = BRConstants.BITS_SYMBOL;
        if (app != null) {
            int unit = BRSharedPrefs.getCryptoDenomination(app, getIso(app));
            switch (unit) {
                case BRConstants.CURRENT_UNIT_BITS:
                    currencySymbolString = "μ" + getIso(app);
                    break;
                case BRConstants.CURRENT_UNIT_MBITS:
                    currencySymbolString = "m" + getIso(app);
                    break;
                case BRConstants.CURRENT_UNIT_BITCOINS:
                    currencySymbolString = getIso(app);
                    break;
            }
        }
        return currencySymbolString;
    }

    @Override
    public abstract String getIso(Context app);

    @Override
    public abstract String getScheme(Context app);

    @Override
    public abstract String getName(Context app);

    @Override
    public String getDenominator() {
        return "100000000";
    } //TODO Shouldn't this be ONE_BITCOIN?

    @Override
    public CryptoAddress getReceiveAddress(Context app) {
        BRCoreAddress addr = getWallet().getReceiveAddress();
        return new CryptoAddress(addr.stringify(), addr);
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address) {
        if (Utils.isNullOrEmpty(address)) {
            Log.e(getTag(), "createTransaction: can't create, address is null");
            return null;
        }
        BRCoreTransaction tx = getWallet().createTransaction(amount.longValue(), new BRCoreAddress(address));
        return tx == null ? null : new CryptoTransaction(tx);
    }

    @Override
    public abstract String decorateAddress(Context app, String addr);

    @Override
    public abstract String undecorateAddress(Context app, String addr);

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
    public void setCachedBalance(final Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(app), balance);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                refreshAddress(app);
            }
        });

        onBalanceChanged(getIso(app), balance);
    }

    @Override
    public void refreshAddress(Context app) {
        BRCoreAddress address = getWallet().getReceiveAddress();
        if (Utils.isNullOrEmpty(address.stringify())) {
            Log.e(getTag(), "refreshAddress: WARNING, retrieved address:" + address);  // TODO was Log.e(TAG, "refreshAddress: i, retrieved address:" + address); in BTC wallet
        }
        BRSharedPrefs.putReceiveAddress(app, address.stringify(), getIso(app));

    }

    @Override
    public void refreshCachedBalance(Context app) {
        BigDecimal balance = new BigDecimal(getWallet().getBalance());
        BRSharedPrefs.putCachedBalance(app, getIso(app), balance);
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
    public WalletSettingsConfiguration getSettingsConfiguration() {
        return mSettingsConfig;
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        CurrencyEntity ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), BRSharedPrefs.getPreferredFiatIso(app));
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
            ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), iso);
        if (ent == null)
            return null;
        double rate = ent.rate;
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE);
        return cryptoAmount.multiply(new BigDecimal(rate));
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal fiatAmount) {
        if (fiatAmount.doubleValue() == 0) return fiatAmount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), iso);
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
        CurrencyEntity ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), iso);
        if (ent == null) {
            Log.e(getTag(), "getSmallestCryptoForFiat: no exchange rate data!");
            return amount;
        }
        double rate = ent.rate;
        //convert c to $.
        return amount.divide(new BigDecimal(rate), 8, ROUNDING_MODE).multiply(new BigDecimal("100000000"));
    }

    // TODO only ETH and ERC20
    @Override
    public BREthereumAmount.Unit getUnit() {
        throw new RuntimeException("stub");
    }

    @Override
    public boolean isAddressValid(String address) {
        return !Utils.isNullOrEmpty(address) && new BRCoreAddress(address).isValid();
    }

    @Override
    public byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] seed) {
        return super.signAndPublishTransaction(tx.getCoreTx(), seed);
    }

    // TODO only ETH and ERC20
    @Override
    public void watchTransactionForHash(CryptoTransaction tx, OnHashUpdated listener) {
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

    @Override
    public int getForkId() {
        return super.getForkId();
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener listener) {
        mWalletManagerHelper.addBalanceChangedListener(listener);
    }

    protected void onBalanceChanged(String uid, BigDecimal balance) {
        mWalletManagerHelper.onBalanceChanged(uid, balance);
    }

//    @Override
//    public void addTxStatusUpdatedListener(OnTxStatusUpdatedListener listener) {
//        mWalletManagerHelper.addTxStatusUpdatedListener(listener);
//    }

    @Override
    public void addSyncListeners(SyncListener listener) {
        mWalletManagerHelper.addSyncListeners(listener);
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

    public void onTxListModified(String hash) {
        mWalletManagerHelper.onTxListModified(hash);
    }

    public void balanceChanged(final long balance) {
        super.balanceChanged(balance);
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                Context app = BreadApp.getBreadContext();
                setCachedBalance(app, new BigDecimal(balance));
                onTxListModified(null);
            }
        });
    }

    public void txStatusUpdate() {
        super.txStatusUpdate();
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {

                // TODO This listener is never added... so this is not doing anything.
//                for (OnTxStatusUpdatedListener listener : txStatusUpdatedListeners)
//                    if (listener != null) listener.onTxStatusUpdated();
                onTxListModified(null);
            }
        });

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                long blockHeight = getPeerManager().getLastBlockHeight();

                final Context context = BreadApp.getBreadContext();
                if (context != null) {
                    BRSharedPrefs.putLastBlockHeight(context, getIso(context), (int) blockHeight);
                }
            }
        });
    }

    public void syncStarted() {
        super.syncStarted();
        Log.d(getTag(), "syncStarted: ");
        final Context app = BreadApp.getBreadContext();
        if (Utils.isEmulatorOrDebug(app)) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(app, "syncStarted " + getIso(app), Toast.LENGTH_LONG).show();
                }
            });
        }

        onSyncStarted();
    }

    protected abstract void syncStopped(Context context);

    public void syncStopped(final String error) {
        super.syncStopped(error);
        Log.d(getTag(), "syncStopped: " + error);
        final Context context = BreadApp.getBreadContext();
        if (Utils.isNullOrEmpty(error)) {
            BRSharedPrefs.putAllowSpend(context, getIso(context), true);
            syncStopped(context);
        }

        onSyncStopped(error);

        if (Utils.isEmulatorOrDebug(context))
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "SyncStopped " + getIso(context) + " err(" + error + ") ", Toast.LENGTH_LONG).show();
                }
            });

        Log.e(getTag(), "syncStopped: peerManager:" + getPeerManager().toString());

        if (!Utils.isNullOrEmpty(error)) {
            if (mSyncRetryCount < SYNC_MAX_RETRY) {
                Log.e(getTag(), "syncStopped: Retrying: " + mSyncRetryCount);
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
                Log.e(getTag(), "syncStopped: Giving up: " + mSyncRetryCount);
                mSyncRetryCount = 0;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Syncing failed, retried " + SYNC_MAX_RETRY + " times.", Toast.LENGTH_LONG).show();
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
                                        Log.e(getTag(), "run: ", ex);
                                    }
                                }
                                if (ctx instanceof Activity && BRSharedPrefs.getShowNotification(ctx))
                                    BRNotificationManager.sendNotification((Activity) ctx, R.drawable.notification_icon, ctx.getString(R.string.app_name), strToShow, 1);
                                else
                                    Log.e(getTag(), "onTxAdded: ctx is not activity");
                            }
                        }
                    }, 1000);


                }
            });
        }
        if (ctx != null)
            TransactionStorageManager.putTransaction(ctx, getIso(ctx), new BRTransactionEntity(transaction.serialize(), transaction.getBlockHeight(), transaction.getTimestamp(), BRCoreKey.encodeHex(transaction.getHash()), getIso(ctx)));
        else
            Log.e(getTag(), "onTxAdded: ctx is null!");

        onTxListModified(transaction.getReverseHash());
    }

    public void onTxDeleted(final String hash, int notifyUser, int recommendRescan) {
        super.onTxDeleted(hash, notifyUser, recommendRescan);
        Log.e(getTag(), "onTxDeleted: " + String.format("hash: %s, notifyUser: %d, recommendRescan: %d", hash, notifyUser, recommendRescan));
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
            Log.e(getTag(), "onTxDeleted: Failed! ctx is null");
        }
        onTxListModified(hash);
    }

    public void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        super.onTxUpdated(hash, blockHeight, timeStamp);
        Log.d(getTag(), "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash, blockHeight, timeStamp));
        Context ctx = BreadApp.getBreadContext();
        if (ctx != null) {
            TransactionStorageManager.updateTransaction(ctx, getIso(ctx), new BRTransactionEntity(null, blockHeight, timeStamp, hash, getIso(ctx)));

        } else {
            Log.e(getTag(), "onTxUpdated: Failed, ctx is null");
        }
        onTxListModified(hash);
    }


//
//    //get the currency unit ETHEREUM_WEI...
//    public abstract BREthereumAmount.Unit getUnit();
//
//    public abstract boolean isAddressValid(String address);
//
//    @WorkerThread
//        //sign and publish the tx using the seed
//    public abstract byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] seed);
//
//    public abstract void addBalanceChangedListener(OnBalanceChangedListener list);
//
//    public abstract void addTxStatusUpdatedListener(OnTxStatusUpdatedListener list);
//
//    public abstract void addSyncListeners(SyncListener list);
//
//    public abstract void addTxListModifiedListener(OnTxListModified list);
//
//    public abstract void watchTransactionForHash(CryptoTransaction tx, BaseWalletManager.OnHashUpdated listener);
//
//    @WorkerThread
//        //get confirmation number
//    public abstract long getRelayCount(byte[] txHash);
//
//    @WorkerThread
//        //get the syncing progress
//    public abstract double getSyncProgress(long startHeight);
//
//    @WorkerThread
//        //get the connection status 0 - Disconnected, 1 - Connecting, 2 - Connected, 3 - Unknown
//    public abstract double getConnectStatus();
//
//    @WorkerThread
//        //Connect the wallet (PeerManager for Bitcoin)
//    public abstract void connect(Context app);
//
//    @WorkerThread
//        //Disconnect the wallet (PeerManager for Bitcoin)
//    public abstract void disconnect(Context app);
//
//    @WorkerThread
//        //Use a fixed favorite node to connect
//    public abstract boolean useFixedNode(String node, int port);
//
//    @WorkerThread
//        //Rescan the wallet (PeerManager for Bitcoin)
//    public abstract void rescan();
//
//    @WorkerThread
//        //get a list of all the transactions sorted by timestamp (e.g. BRCoreTransaction[] for BTC)
//    public abstract CryptoTransaction[] getTxs(Context app);
//
//    //get the transaction fee
//    public abstract BigDecimal getTxFee(CryptoTransaction tx);
//
//    //get the transaction fee
//    public abstract BigDecimal getEstimatedFee(BigDecimal amount, String address);
//
//    //get the fee for the transaction size
//    public abstract BigDecimal getFeeForTransactionSize(BigDecimal size);
//
//    //get the transaction to address
//    public abstract String getTxAddress(CryptoTransaction tx);
//
//    //get the maximum output amount possible for this wallet
//    public abstract BigDecimal getMaxOutputAmount(Context app);
//
//    //get the reasonable minimum output amount
//    public abstract BigDecimal getMinOutputAmount(Context app);
//
//    //get the transaction amount (negative if sent)
//    public abstract BigDecimal getTransactionAmount(CryptoTransaction tx);
//
//    //get the reasonable minimum output amount (not smaller than dust)
//    public abstract BigDecimal getMinOutputAmountPossible();
//
//    @WorkerThread
//        //updates the fee for the current wallet (from an API)
//    public abstract void updateFee(Context app);
//
//    //get the core address and store it locally
//    public abstract void refreshAddress(Context app);
//
//    //get the core balance and store it locally
//    public abstract void refreshCachedBalance(Context app);
//
//    //get a list of all the transactions UI holders sorted by timestamp
//    public abstract List<TxUiHolder> getTxUiHolders(Context app);
//
//    //return true if this wallet owns this address
//    public abstract boolean containsAddress(String address);
//
//    //return true if this wallet already used this address
//    public abstract boolean addressIsUsed(String address);
//
//    @WorkerThread
//        //generate the wallet if needed
//    public abstract boolean generateWallet(Context app);
//
//    //get the currency symbol e.g. Bitcoin - ₿, Ether - Ξ
//    public abstract String getSymbol(Context app);
//
//    //get the currency denomination e.g. Bitcoin - BTC, Ether - ETH
//    public abstract String getIso(Context app);
//
//    //get the currency scheme (bitcoin or bitcoincash)
//    public abstract String getScheme(Context app);
//
//    //get the currency name e.g. Bitcoin
//    public abstract String getName(Context app);
//
//    //get the currency denomination e.g. BCH, mBCH, Bits
//    public abstract String getDenominator(Context app);
//
//    @WorkerThread
//        //get the wallet's receive address
//    public abstract CryptoAddress getReceiveAddress(Context app);
//
//    public abstract CryptoTransaction createTransaction(BigDecimal amount, String address);
//
//    //decorate an address to a particular currency, if needed (like BCH address format)
//    public abstract String decorateAddress(Context app, String addr);
//
//    //convert to raw address to a particular currency, if needed (like BCH address format)
//    public abstract String undecorateAddress(Context app, String addr);
//
//    //get the number of decimal places to use for this currency
//    public abstract int getMaxDecimalPlaces(Context app);
//
//    //get the cached balance in the smallest unit:  satoshis.
//    public abstract BigDecimal getCachedBalance(Context app);
//
//    //get the total amount sent in the smallest crypto unit:  satoshis.
//    public abstract BigDecimal getTotalSent(Context app);
//
//    //wipe all wallet data
//    public abstract void wipeData(Context app);
//
//    public abstract void syncStarted();
//
//    public abstract void syncStopped(String error);
//
//    public abstract boolean networkIsReachable();
//
//    /**
//     * @param balance - the balance to be saved in the smallest unit.(e.g. satoshis, wei)
//     */
//    public abstract void setCachedBalance(Context app, BigDecimal balance);
//
//    //return the maximum amount for this currency
//    public abstract BigDecimal getMaxAmount(Context app);
//
//    /**
//     * @return - the wallet's Ui configuration
//     */
//    public abstract WalletUiConfiguration getUiConfiguration();
//
//    /**
//     * @return - the wallet's Settings configuration (Settings items)
//     */
//    public abstract WalletSettingsConfiguration getSettingsConfiguration();
//
//    /**
//     * @return - the wallet's currency exchange rate in the user's favorite fiat currency (e.g. dollars)
//     */
//    public abstract BigDecimal getFiatExchangeRate(Context app);
//
//    /**
//     * @return - the total balance amount in the user's favorite fiat currency (e.g. dollars)
//     */
//    public abstract BigDecimal getFiatBalance(Context app);
//
//    /**
//     * @param amount - the smallest denomination amount in current wallet's crypto (e.g. Satoshis)
//     * @param ent    - provide a currency entity if needed
//     * @return - the fiat value of the amount in crypto (e.g. dollars)
//     * or null if there is no fiat exchange data from the API yet
//     */
//    public abstract BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent);
//
//    /**
//     * @param amount - the amount in the user's favorite fiat currency (e.g. dollars)
//     * @return - the crypto value of the amount in the current favorite denomination (e.g. BTC, mBTC, Bits..)
//     * or null if there is no fiat exchange data from the API yet
//     */
//    public abstract BigDecimal getCryptoForFiat(Context app, BigDecimal amount);
//
//    /**
//     * @param amount - the smallest denomination amount in crypto (e.g. satoshis)
//     * @return - the crypto value of the amount in the current favorite denomination (e.g. BTC, mBTC, Bits..)
//     */
//    public abstract BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount);
//
//    /**
//     * @param amount - the crypto value of the amount in the current favorite denomination (e.g. BTC, mBTC, Bits..)
//     * @return - the smallest denomination amount in crypto (e.g. satoshis)
//     */
//    public abstract BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount);
//
//    /**
//     * @param amount - the fiat amount (e.g. dollars)
//     * @return - the crypto value of the amount in the smallest denomination (e.g. satothis)
//     * or null if there is no fiat exchange data from the API yet
//     */
//    public abstract BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount);

}
