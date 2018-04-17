package com.breadwallet.wallet.wallets.etherium;

import android.app.Activity;
import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.ethereum.BREthereumAmount;
import com.breadwallet.core.ethereum.BREthereumLightNode;
import com.breadwallet.core.ethereum.BREthereumNetwork;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.core.ethereum.BREthereumTransaction;
import com.breadwallet.core.ethereum.BREthereumWallet;
import com.breadwallet.core.ethereum.test.BREthereumLightNodeClientTest;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.tools.util.Utils;
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

import com.platform.JsonRpcConstants;
import com.platform.JsonRpcRequest;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.breadwallet.tools.util.BRConstants.ROUNDING_MODE;

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
    public static final String ETH_SCHEME = "ethereum";

    private static final String mName = "Ethereum";

    private List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
    private List<OnTxStatusUpdatedListener> txStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> syncListeners = new ArrayList<>();
    private List<OnTxListModified> txModifiedListeners = new ArrayList<>();

    private static WalletEthManager instance;
    private WalletUiConfiguration uiConfig;
    private WalletSettingsConfiguration settingsConfig;
    private final BigDecimal MAX_ETH = new BigDecimal("90000000000000000000000000"); // 90m ETH * 18 (WEI)
    private final BigDecimal ONE_ETH = new BigDecimal("1000000000000000000"); //1ETH = 1000000000000000000 WEI
    private BREthereumWallet mWallet;
    BREthereumLightNode.JSON_RPC node;
    private Context mContext;

    private WalletEthManager(final Context app, byte[] ethPubKey, BREthereumNetwork network) {
        uiConfig = new WalletUiConfiguration("#5e70a3", null, true);
        settingsConfig = new WalletSettingsConfiguration(app, ISO, getFingerprintLimits(app));

        if (Utils.isNullOrEmpty(ethPubKey)) {
            Log.e(TAG, "WalletEthManager: Using the paperKey to create");
            try {
                String paperKey = new String(BRKeyStore.getPhrase(app, 0));
                if (Utils.isNullOrEmpty(paperKey)) {
                    Log.e(TAG, "WalletEthManager: paper key is empty too, no wallet!");
                    instance = null;
                    return;
                }

                String[] words = lookupWords (app, paperKey, Locale.getDefault().getLanguage());

                if (null == words) {
                    Log.e (TAG, "WalletEthManager: paper key does not validate with BIP39 Words for: " +
                            Locale.getDefault().getLanguage());
                    instance = null;
                    return;
                }

                new BREthereumLightNode.JSON_RPC(this, network, paperKey, words);
                mWallet = node.getWallet();

                if (null == mWallet) {
                    Log.e (TAG, "WalletEthManager: failed to create the ETH wallet using paperKey.");
                    instance = null;
                    return;
                }

                ethPubKey = mWallet.getAccount().getPrimaryAddressPublicKey();
                BRKeyStore.putEthPublicKey(ethPubKey, app);
            } catch (UserNotAuthenticatedException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "WalletEthManager: Using the pubkey to create");
            new BREthereumLightNode.JSON_RPC(this, network, ethPubKey);
            mWallet = node.getWallet();

            if (null == mWallet) {
                Log.e (TAG, "WalletEthManager: failed to create the ETH wallet using saved publicKey.");
                instance = null;
                return;
            }
        }

        mContext = app;
        mWallet.estimateGasPrice();
        mWallet.setDefaultUnit(BREthereumAmount.Unit.ETHER_WEI);
        BREthereumWallet walletToken = node.createWallet(BREthereumToken.tokenBRD);
        walletToken.setDefaultUnit(BREthereumAmount.Unit.TOKEN_DECIMAL);
        node.connect();
    }

    public synchronized static WalletEthManager getInstance(Context app) {
        if (instance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(app);
            if (Utils.isNullOrEmpty(rawPubKey)) {
                Log.e(TAG, "getInstance: rawPubKey is null");
                return null;
            }
            byte[] ethPubKey = BRKeyStore.getEthPublicKey(app);
            if (Utils.isNullOrEmpty(ethPubKey)) {
                //check if there is a master key and if not means the wallet isn't created yet
                if (Utils.isNullOrEmpty(BRKeyStore.getMasterPublicKey(app))) {
                    return null;
                }
            }
            instance = new WalletEthManager(app, ethPubKey, BuildConfig.BITCOIN_TESTNET ? BREthereumNetwork.testnet : BREthereumNetwork.mainnet);

        }
        return instance;
    }

    private String[] lookupWords (Context app, String paperKey, String language) {
        if (null == language) language = "en";

        List<String> list = Bip39Reader.bip39List(app, language);
        String[] words = list.toArray(new String[list.size()]);

        if (words.length % Bip39Reader.WORD_LIST_SIZE != 0) {
            Log.e(TAG, "isPaperKeyValid: " + "The list size should divide by " + Bip39Reader.WORD_LIST_SIZE);
            BRReportsManager.reportBug(new IllegalArgumentException("words.length is not dividable by " + Bip39Reader.WORD_LIST_SIZE), true);
        }

        // If the paperKey is valid for `words`, then return `words`
        if (BRCoreMasterPubKey.validateRecoveryPhrase(words, paperKey))
            return words;

            // Otherwise if not English, then try English
        else if (!"en".equals(language))
            return lookupWords(app, paperKey, "en");

            // Otherwise, nothing
        else
            return null;
    }

    private List<BigDecimal> getFingerprintLimits(Context app) {
        List<BigDecimal> result = new ArrayList<>();
        result.add(ONE_ETH.divide(new BigDecimal(100), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(ONE_ETH.divide(new BigDecimal(10), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(ONE_ETH);
        result.add(ONE_ETH.multiply(new BigDecimal(10)));
        result.add(ONE_ETH.multiply(new BigDecimal(100)));
        return result;
    }

    @Override
    public int getForkId() {
        //No need for ETH
        return -1;
    }

    @Override
    public boolean isAddressValid(String address) {
        return !Utils.isNullOrEmpty(address) && address.startsWith("0x");
    }

    @Override
    public byte[] signAndPublishTransaction(BaseTransaction tx, byte[] phrase) {
        CryptoTransaction cryptoTransaction = (CryptoTransaction) tx;
        mWallet.sign(cryptoTransaction.getEtherTx(), new String(phrase));
        mWallet.submit(cryptoTransaction.getEtherTx());
        String hash = tx.getEtherTx().getHash();
        return hash == null ? new byte[0] : hash.getBytes();
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
        //todo implement
        return -1;
    }

    @Override
    public double getSyncProgress(long startHeight) {
        //Not needed for ETH, return fully synced always
        return 1.0;
    }

    @Override
    public double getConnectStatus() {
        //Not needed for ETH, return Connected always
        return 2;
    }

    @Override
    public void connect(Context app) {
        //Not needed for ETH
    }

    @Override
    public void disconnect(Context app) {
        //Not needed for ETH
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        //Not needed for ETH
        return false;
    }

    @Override
    public void rescan() {
        //Not needed for ETH
    }

    @Override
    public BaseTransaction[] getTxs() {
        return (BaseTransaction[]) mWallet.getTransactions();
    }

    @Override
    public BigDecimal getTxFee(BaseTransaction tx) {
        return new BigDecimal(tx.getEtherTx().getGasLimit())
                .multiply(new BigDecimal(tx.getEtherTx().getGasPrice(BREthereumAmount.Unit.ETHER_WEI)));
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        BigDecimal fee;
        if (amount == null) return null;
        if (amount.compareTo(new BigDecimal(0)) == 0) {
            fee = new BigDecimal(0);
        } else {
            fee = new BigDecimal(mWallet.transactionEstimatedFee(amount.toPlainString()));
        }
        return fee;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return null;
    }

    @Override
    public BaseAddress getTxAddress(BaseTransaction tx) {
        return new ETHAddress(tx.getEtherTx().getTargetAddress());
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        BigDecimal balance = getCachedBalance(app);
        if (balance.compareTo(new BigDecimal(0)) == 0) return new BigDecimal(0);
        BigDecimal fee = new BigDecimal(mWallet.transactionEstimatedFee(balance.toPlainString()));
        if (fee.compareTo(balance) > 0) return new BigDecimal(0);
        return balance.subtract(fee);
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return new BigDecimal(1); //1 WEI
    }

    @Override
    public BigDecimal getTransactionAmount(BaseTransaction tx) {
        return new BigDecimal(tx.getEtherTx().getAmount());
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return new BigDecimal(1); //1 WEI
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
            Log.d(TAG, "updateFee: " + getIso(app) + ":" + fee + "|" + economyFee);

            if (fee.compareTo(new BigDecimal(0)) > 0) {
                BRSharedPrefs.putFeeRate(app, getIso(app), fee);
                BRSharedPrefs.putFeeTime(app, getIso(app), System.currentTimeMillis()); //store the time of the last successful fee fetch
            } else {
                FirebaseCrash.report(new NullPointerException("Fee is weird:" + fee));
                Log.d(TAG, "Error: Fee is unexpected value");

            }
            if (economyFee.compareTo(new BigDecimal(0)) > 0) {
                BRSharedPrefs.putEconomyFeeRate(app, getIso(app), economyFee);
            } else {
                FirebaseCrash.report(new NullPointerException("Economy fee is weird:" + economyFee));
                Log.d(TAG, "Error: Economy fee is unexpected value");
            }
        } catch (JSONException e) {
            Log.e(TAG, "updateFeePerKb: FAILED: " + jsonString, e);
            BRReportsManager.reportBug(e);
            BRReportsManager.reportBug(new IllegalArgumentException("JSON ERR: " + jsonString));
        }

    }

    @Override
    public void refreshAddress(Context app) {
        Log.e(TAG, "refreshAddress: start");
        BaseAddress address = getReceiveAddress(app);
        Log.e(TAG, "refreshAddress: end");
        if (Utils.isNullOrEmpty(address.stringify())) {
            Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
        }
        BRSharedPrefs.putReceiveAddress(app, address.stringify(), getIso(app));
    }

    @Override
    public void refreshCachedBalance(final Context app) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                BigDecimal balance = new BigDecimal(mWallet.getBalance());
                BRSharedPrefs.putCachedBalance(app, ISO, balance);
            }
        });
    }

    @Override
    public List<TxUiHolder> getTxUiHolders(Context app) {
        BREthereumTransaction txs[] = mWallet.getTransactions();
        if (txs == null || txs.length <= 0) return null;
        List<TxUiHolder> uiTxs = new ArrayList<>();
        for (int i = txs.length - 1; i >= 0; i--) { //revere order
            BREthereumTransaction tx = txs[i];
//            TxMetaData md = KVStoreManager.getInstance().getTxMetaData(app, tx.getHash().getBytes()); //EXPENSIVE OPERATION
            //if the txMetaData isn't saved - SAVE IT!
//            if (md == null)
//                md = KVStoreManager.getInstance().createMetadata(app, this, new CryptoTransaction(tx));
//            if (md != null)
//                KVStoreManager.getInstance().putTxMetaData(app, md, tx.getHash().getBytes());
            uiTxs.add(new TxUiHolder(tx, tx.getTargetAddress().equalsIgnoreCase(mWallet.getAccount().getPrimaryAddress()), tx.getBlockTimestamp(),
                    (int) tx.getBlockNumber(), Utils.isNullOrEmpty(tx.getHash()) ? null : tx.getHash().getBytes(), tx.getHash(), null, tx,
                    tx.getTargetAddress(), tx.getSourceAddress(), null, 0,
                    new BigDecimal(tx.getAmount()), true));
        }

        return uiTxs;
    }

    @Override
    public boolean containsAddress(String address) {
        return mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(address);
    }

    @Override
    public boolean addressIsUsed(String address) {
        //Not needed for ETH
        return false;
    }

    @Override
    public BaseAddress createAddress(String address) {
        return new ETHAddress(address);
    }

    @Override
    public boolean generateWallet(Context app) {
        //Not needed for ETH
        return false;
    }

    @Override
    public String getSymbol(Context app) {
//        return BRConstants.symbolEther;
        return ISO;
    }

    @Override
    public String getIso(Context app) {
        return ISO;
    }

    @Override
    public String getScheme(Context app) {
        return ETH_SCHEME;
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
        return new ETHAddress(mWallet.getAccount().getPrimaryAddress());
    }

    @Override
    public BaseTransaction createTransaction(BigDecimal amount, String address) {
        BREthereumTransaction tx = mWallet.createTransaction(address, amount.toPlainString(), BREthereumAmount.Unit.ETHER_WEI);
        return new CryptoTransaction(tx);
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
        return 5;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, getIso(app));
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return new BigDecimal(0);
    }

    @Override
    public void wipeData(Context app) {
        Log.e(TAG, "wipeData: ");
    }

    @Override
    public void syncStarted() {
        //Not needed for ETH
    }

    @Override
    public void syncStopped(String error) {
        //Not needed for ETH
    }

    @Override
    public boolean networkIsReachable() {
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(app), balance);
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
    public WalletSettingsConfiguration getSettingsConfiguration() {
        return settingsConfig;
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        BigDecimal fiatData = getFiatForEth(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if (fiatData == null) return null;
        return fiatData; //dollars
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        if (app == null) return null;
        return getFiatForSmallestCrypto(app, getCachedBalance(app), null);
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        if (amount == null || amount.compareTo(new BigDecimal(0)) == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        if (ent != null) {
            //passed in a custom CurrencyEntity
            //get crypto amount
            BigDecimal cryptoAmount = amount.divide(ONE_ETH, 8, BRConstants.ROUNDING_MODE);
            //multiply by fiat rate
            return cryptoAmount.multiply(new BigDecimal(ent.rate));
        }
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(ONE_ETH, 8, BRConstants.ROUNDING_MODE);

        BigDecimal fiatData = getFiatForEth(app, cryptoAmount, iso);
        if (fiatData == null) return null;
        return fiatData;
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal fiatAmount) {
        if (fiatAmount == null || fiatAmount.compareTo(new BigDecimal(0)) == 0) return fiatAmount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        return getEthForFiat(app, fiatAmount, iso);

    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(new BigDecimal(0)) == 0) return amount;
        return amount.divide(ONE_ETH, 8, ROUNDING_MODE);
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(new BigDecimal(0)) == 0) return amount;
        return amount.multiply(ONE_ETH);
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(new BigDecimal(0)) == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        BigDecimal ethAmount = getEthForFiat(app, amount, iso);
        if (ethAmount == null) return null;
        return ethAmount.multiply(ONE_ETH);
    }

    //pass in a eth amount and return the specified amount in fiat
    //ETH rates are in BTC (thus this math)
    private BigDecimal getFiatForEth(Context app, BigDecimal ethAmount, String code) {
        //fiat rate for btc
        CurrencyEntity btcRate = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ether
        CurrencyEntity ethBtcRate = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), "BTC");
        if (btcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No USD rates for BTC");
            return null;
        }
        if (ethBtcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No BTC rates for ETH");
            return null;
        }

        return ethAmount.multiply(new BigDecimal(ethBtcRate.rate)).multiply(new BigDecimal(btcRate.rate));
    }

    //pass in a fiat amount and return the specified amount in ETH
    //ETH rates are in BTC (thus this math)
    private BigDecimal getEthForFiat(Context app, BigDecimal fiatAmount, String code) {
        //fiat rate for btc
        CurrencyEntity btcRate = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ether
        CurrencyEntity ethBtcRate = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), "BTC");
        if (btcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No USD rates for BTC");
            return null;
        }
        if (ethBtcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No BTC rates for ETH");
            return null;
        }

        return fiatAmount.divide(new BigDecimal(ethBtcRate.rate).multiply(new BigDecimal(btcRate.rate)), 8, BRConstants.ROUNDING_MODE);
    }


    /**
     * The JSON RPC callbacks
     * Implement JSON RPC methods synchronously
     */

    @Override
    public void assignNode(BREthereumLightNode node) {
        this.node = (BREthereumLightNode.JSON_RPC) node;
    }

    @Override
    public void getBalance(final int wid, final String address, final int rid) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "getBalance: " + address);
                final String eth_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_RPC_ENDPOINT;
//        Log.d(TAG, "Making rpc request to " + eth_url);
                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    String currentTime = String.valueOf(System.currentTimeMillis());
                    payload.put("jsonrpc", "2.0");
                    payload.put("method", "eth_getBalance");
                    params.put(address);
                    params.put("latest");
                    payload.put("params", params);
                    payload.put("id", rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final JsonRpcRequest request = new JsonRpcRequest();
                request.makeRpcRequest(mContext, eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        String balance = "0x";
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {

                                JSONObject responseObject = new JSONObject(jsonResult);
                                Log.d(TAG, "getBalance response -> " + responseObject.toString());

                                if (responseObject.has("result")) {
                                    balance = responseObject.getString("result");
                                    Log.e(TAG, "RPC:getBalance: " + balance);
                                }
                            } else {
                                Log.e(TAG, "onRpcRequestCompleted: jsonResult is null");
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }
                        node.announceBalance(wid, balance, rid);
                        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                for (OnBalanceChangedListener list : balanceListeners)
                                    if (list != null)
                                        list.onBalanceChanged(ISO, new BigDecimal(mWallet.getBalance()));
                            }
                        });

                    }
                });

            }
        });

    }

    @Override
    public void getGasPrice(final int wid, final int rid) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_RPC_ENDPOINT;
                Log.d(TAG, "Making rpc request to -> " + eth_url);
                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put("method", "eth_gasPrice");
                    payload.put("params", params);
                    payload.put("id", rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final JsonRpcRequest request = new JsonRpcRequest();

                request.makeRpcRequest(mContext, eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        String gasPrice = "0x";

                        try {

                            if (jsonResult != null) {

                                JSONObject responseObject = new JSONObject(jsonResult);

                                if (responseObject.has("result")) {
                                    gasPrice = responseObject.getString("result");
                                }
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }

                        Log.d(TAG, "gasPrice response -> " + jsonResult);
                        node.announceGasPrice(wid, gasPrice, rid);
                    }
                });
            }
        });

    }

    @Override
    public void getGasEstimate(final int wid, final int tid, final String to, final String amount, final String data, final int rid) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_RPC_ENDPOINT;
                Log.d(TAG, "Making rpc request to -> " + eth_url);
                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                params.put(to);
                params.put(amount);
                params.put(data);

                final JsonRpcRequest request = new JsonRpcRequest();


                try {
                    payload.put("method", "eth_estimateGas");
                    payload.put("params", params);
                    payload.put("id", rid);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                request.makeRpcRequest(mContext, eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        String gasEstimate = "0x";
                        try {
                            JSONObject responseObject = new JSONObject(jsonResult);

                            if (responseObject.has("result")) {
                                gasEstimate = responseObject.getString("result");

                                node.announceGasEstimate(wid, tid, gasEstimate, rid);
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        node.announceGasEstimate(wid, tid, gasEstimate, rid);
                    }
                });
            }
        });

    }

    @Override
    public void submitTransaction(final int wid, final int tid, final String rawTransaction, final int rid) {
        Log.e(TAG, "submitTransaction: wid:" + wid);
        Log.e(TAG, "submitTransaction: tid:" + tid);
        Log.e(TAG, "submitTransaction: rawTransaction:" + rawTransaction);
        Log.e(TAG, "submitTransaction: rid:" + rid);

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_RPC_ENDPOINT;
                Log.d(TAG, "Making rpc request to -> " + eth_url);

                JsonRpcRequest request = new JsonRpcRequest();
                JSONObject payload = new JSONObject();
                JSONArray params = new JSONArray();
                try {
                    payload.put("jsonrpc", "2.0");
                    payload.put("method", "eth_sendRawTransaction");
                    params.put(rawTransaction);
                    payload.put("params", params);
                    payload.put("id", rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                request.makeRpcRequest(mContext, eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        String txHash = null;
                        int errCode = 0;
                        String errMessage = "";
                        if (jsonResult != null) {
                            try {
                                JSONObject responseObject = new JSONObject(jsonResult);
                                Log.e(TAG, "onRpcRequestCompleted: " + responseObject);
                                if (responseObject.has("result")) {
                                    txHash = responseObject.getString("result");
                                    Log.e(TAG, "onRpcRequestCompleted: " + txHash);
                                    node.announceSubmitTransaction(wid, tid, txHash, rid);
                                } else if (responseObject.has("error")) {
                                    JSONObject errObj = responseObject.getJSONObject("error");
                                    errCode = errObj.getInt("code");
                                    errMessage = errObj.getString("message");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        final String finalTxHash = txHash;
                        final String finalErrMessage = errMessage;
                        final int finalErrCode = errCode;
                        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                final Context app = BreadApp.getBreadContext();
                                if (app != null && app instanceof Activity) {
                                    if (!Utils.isNullOrEmpty(finalTxHash)) {
                                        PostAuth.stampMetaData(app, finalTxHash.getBytes());
                                        BRAnimator.showBreadSignal((Activity) app, app.getString(R.string.Alerts_sendSuccess), app.getString(R.string.Alerts_sendSuccessSubheader), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                                            @Override
                                            public void onComplete() {
                                                BRAnimator.killAllFragments((Activity) app);
                                                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        for (OnTxStatusUpdatedListener list : txStatusUpdatedListeners)
                                                            if (list != null)
                                                                list.onTxStatusUpdated();
                                                        mWallet.updateBalance();
                                                    }
                                                });
                                            }
                                        });
                                    } else {
                                        BRDialog.showSimpleDialog(app, "Failed", String.format("(%d) %s", finalErrCode, finalErrMessage));
                                    }
                                } else {
                                    Log.e(TAG, "submitTransaction: app is null or not an activity");
                                }
                            }
                        });

                    }
                });

            }
        });
    }

    @Override
    public void getTransactions(final String address, final int id) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {

                //final String eth_rpc_url = String.format(JsonRpcConstants.ETH_RPC_TX_LIST, mWallet.getAccount().getPrimaryAddress());
                final String eth_rpc_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_TX_ENDPOINT + "query?module=account&action=txlist&address=" + address;

                final JsonRpcRequest request = new JsonRpcRequest();
                final JSONObject payload = new JSONObject();
                try {
                    payload.put("id", String.valueOf(id));
                    payload.put("account", address);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                request.makeRpcRequest(mContext, eth_rpc_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {


                        final String jsonRcpResponse = jsonResult;

                        if (jsonRcpResponse != null) {
                            try {
                                // Convert response into JsonArray of transactions
                                JSONObject transactions = new JSONObject(jsonResult);

                                JSONArray transactionsArray = transactions.getJSONArray("result");

                                String txHash = "";
                                String txTo = "";
                                String txFrom = "";
                                String txContract = "";
                                String txValue = "";
                                String txGas = "";
                                String txGasPrice = "";
                                String txNonce = "";
                                String txGasUsed = "";
                                String txBlockNumber = "";
                                String txBlockHash = "";
                                String txData = "";
                                String txBlockConfirmations = "";
                                String txBlockTransactionIndex = "";
                                String txBlockTimestamp = "";
                                String txIsError = "";

                                // Iterate through the list of transactions and call node.announceTransaction()
                                // to notify the core
                                for (int i = 0; i < transactionsArray.length(); i++) {
                                    JSONObject txObject = transactionsArray.getJSONObject(i);

                                    Log.d(TAG, "TxObject contains -> " + txObject.toString());

                                    if (txObject.has("hash")) {
                                        txHash = txObject.getString("hash");
                                        Log.d(TAG, "TxObject Hash -> " + txHash);

                                    }

                                    if (txObject.has("to")) {
                                        txTo = txObject.getString("to");
                                        Log.d(TAG, "TxObject to -> " + txTo);

                                    }

                                    if (txObject.has("from")) {
                                        txFrom = txObject.getString("from");
                                        Log.d(TAG, "TxObject from -> " + txFrom);

                                    }

                                    if (txObject.has("contractAddress")) {
                                        txContract = txObject.getString("contractAddress");
                                        Log.d(TAG, "TxObject contractAddress -> " + txContract);

                                    }

                                    if (txObject.has("value")) {
                                        txValue = txObject.getString("value");
                                        Log.d(TAG, "TxObject value -> " + txValue);

                                    }

                                    if (txObject.has("gas")) {
                                        txGas = txObject.getString("gas");
                                        Log.d(TAG, "TxObject gas -> " + txGas);


                                    }

                                    if (txObject.has("gasPrice")) {
                                        txGasPrice = txObject.getString("gasPrice");
                                        Log.d(TAG, "TxObject gasPrice -> " + txGasPrice);

                                    }

                                    if (txObject.has("nonce")) {
                                        txNonce = txObject.getString("nonce");
                                        Log.d(TAG, "TxObject nonce -> " + txNonce);

                                    }

                                    if (txObject.has("gasUsed")) {
                                        txGasUsed = txObject.getString("gasUsed");
                                        Log.d(TAG, "TxObject gasUsed -> " + txGasUsed);

                                    }

                                    if (txObject.has("blockNumber")) {
                                        txBlockNumber = txObject.getString("blockNumber");
                                        Log.d(TAG, "TxObject blockNumber -> " + txBlockNumber);

                                    }

                                    if (txObject.has("blockHash")) {
                                        txBlockHash = txObject.getString("blockHash");
                                        Log.d(TAG, "TxObject blockHash -> " + txBlockHash);

                                    }

                                    if (txObject.has("input")) {
                                        txData = txObject.getString("input");
                                        Log.d(TAG, "TxObject input -> " + txData);

                                    }

                                    if (txObject.has("confirmations")) {
                                        txBlockConfirmations = txObject.getString("confirmations");
                                        Log.d(TAG, "TxObject confirmations -> " + txBlockConfirmations);

                                    }

                                    if (txObject.has("transactionIndex")) {
                                        txBlockTransactionIndex = txObject.getString("transactionIndex");
                                        Log.d(TAG, "TxObject transactionIndex -> " + txBlockTransactionIndex);

                                    }

                                    if (txObject.has("timeStamp")) {
                                        txBlockTimestamp = txObject.getString("timeStamp");
                                        Log.d(TAG, "TxObject blockTimestamp -> " + txBlockTimestamp);

                                    }

                                    if (txObject.has("isError")) {
                                        txIsError = txObject.getString("isError");
                                        Log.d(TAG, "TxObject isError -> " + txIsError);

                                    }

                                    node.announceTransaction(id, txHash,
                                            (mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(txFrom) ? address : txFrom),
                                            (mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(txTo) ? address : txTo),
                                            txContract, txValue, txGas, txGasPrice, txData, txNonce, txGasUsed, txBlockNumber, txBlockHash, txBlockConfirmations, txBlockTransactionIndex, txBlockTimestamp, txIsError);
                                    Context app = BreadApp.getBreadContext();
                                    int blockHeight = Integer.valueOf(txBlockNumber) + Integer.valueOf(txBlockConfirmations);
                                    if (app != null && blockHeight != Integer.MAX_VALUE && blockHeight > 0) {
                                        Log.e(TAG, "onRpcRequestCompleted: putLastBlockHeight: " + blockHeight);
                                        BRSharedPrefs.putLastBlockHeight(app, getIso(app), blockHeight);
                                    }
                                }


                                Log.d(TAG, "Rpc Transactions array length -> " + transactionsArray.length());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });

    }

    @Override
    public void getLogs(String address, String event, int rid) {
            //implement for tokens
    }
}
