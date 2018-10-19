package com.breadwallet.wallet.wallets.ethereum;

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
import com.breadwallet.core.ethereum.BREthereumBlock;
import com.breadwallet.core.ethereum.BREthereumWallet;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.util.JsonRpcHelper;
import com.breadwallet.wallet.wallets.CryptoAddress;
import com.breadwallet.wallet.wallets.CryptoTransaction;

import com.breadwallet.wallet.wallets.WalletManagerHelper;

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
public class WalletEthManager extends BaseEthereumWalletManager implements BREthereumLightNode.Client,
        BREthereumLightNode.Listener {
    private static final String TAG = WalletEthManager.class.getSimpleName();

    private CryptoTransaction mWatchedTransaction;
    private OnHashUpdated mWatchListener;

    public static final String ETH_CURRENCY_CODE = "ETH";
    public static final String HEX_PREFIX = "0x";
    public static final String ETH_SCHEME = "ethereum";
    //1ETH = 1000000000000000000 WEI
    public static final String ETHER_WEI = "1000000000000000000";
    //Max amount in ether
    public static final String MAX_ETH = "90000000";
    private final BigDecimal MAX_WEI = new BigDecimal(MAX_ETH).multiply(new BigDecimal(ETHER_WEI)); // 90m ETH * 18 (WEI)
    private final BigDecimal ONE_ETH = new BigDecimal(ETHER_WEI);
    private static final String NAME = "Ethereum";

    private static WalletEthManager mInstance;

    private WalletUiConfiguration mUiConfig;
    private WalletSettingsConfiguration mSettingsConfig;

    private BREthereumWallet mWallet;
    public BREthereumLightNode node;
    public List<OnTransactionEventListener> mTransactionEventListeners = new ArrayList<>();

    private WalletEthManager(final Context app, byte[] ethPubKey, BREthereumNetwork network) {
        mUiConfig = new WalletUiConfiguration("#5e6fa5", null,
                true, WalletManagerHelper.MAX_DECIMAL_PLACES_FOR_UI);
        mSettingsConfig = new WalletSettingsConfiguration();

        if (Utils.isNullOrEmpty(ethPubKey)) {
            Log.e(TAG, "WalletEthManager: Using the paperKey to create");
            String paperKey = null;
            try {
                paperKey = new String(BRKeyStore.getPhrase(app, 0));
            } catch (UserNotAuthenticatedException e) {
                e.printStackTrace();
                return;
            }
            if (Utils.isNullOrEmpty(paperKey)) {
                Log.e(TAG, "WalletEthManager: paper key is empty too, no wallet!");
                return;
            }

            String[] words = lookupWords(app, paperKey, Locale.getDefault().getLanguage());

            if (null == words) {
                Log.e(TAG, "WalletEthManager: paper key does not validate with BIP39 Words for: "
                        + Locale.getDefault().getLanguage());
                return;
            }


            node = new BREthereumLightNode(this, network, paperKey, words);
            node.addListener(this);

            mWallet = node.getWallet();

            if (null == mWallet) {
                Log.e(TAG, "WalletEthManager: failed to create the ETH wallet using paperKey.");
                return;
            }

            ethPubKey = mWallet.getAccount().getPrimaryAddressPublicKey();
            BRKeyStore.putEthPublicKey(ethPubKey, app);
        } else {
            Log.e(TAG, "WalletEthManager: Using the pubkey to create");
            node = new BREthereumLightNode(this, network, ethPubKey);
            node.addListener(this);

            mWallet = node.getWallet();

            if (null == mWallet) {
                Log.e(TAG, "WalletEthManager: failed to create the ETH wallet using saved publicKey.");
                return;
            }
        }
        mAddress = getReceiveAddress(app).stringify();
        if (Utils.isNullOrEmpty(mAddress)) {
            BRReportsManager.reportBug(new IllegalArgumentException("Eth address missing!"), true);
        }

        WalletsMaster.getInstance(app).setSpendingLimitIfNotSet(app, this);

        estimateGasPrice();
        mWallet.setDefaultUnit(BREthereumAmount.Unit.ETHER_WEI);
        node.connect();
    }

    public static synchronized WalletEthManager getInstance(Context app) {
        if (mInstance == null) {
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
            mInstance = new WalletEthManager(app, ethPubKey, BuildConfig.BITCOIN_TESTNET ? BREthereumNetwork.testnet : BREthereumNetwork.mainnet);

        }
        return mInstance;
    }

    public void estimateGasPrice() {
        mWallet.estimateGasPrice();
    }

    private String[] lookupWords(Context app, String paperKey, String l) {
        //ignore l since it can be english but the phrase in other language.

        List<String> list = Bip39Reader.detectWords(app, paperKey);
        if (Utils.isNullOrEmpty(list) || (list.size() % Bip39Reader.WORD_LIST_SIZE != 0)) {
            String message = "lookupWords: " + "Failed: " + list + ", size: " + (list == null ? "null" : list.size());
            Log.e(TAG, message);
            BRReportsManager.reportBug(new IllegalArgumentException(message), true);
            return null;
        }
        String[] words = list.toArray(new String[list.size()]);

        if (BRCoreMasterPubKey.validateRecoveryPhrase(words, paperKey)) {
            // If the paperKey is valid for `words`, then return `words`
            return words;
        } else {
            // Otherwise, nothing
            BRReportsManager.reportBug(new NullPointerException("invalid paper key for words:" + paperKey.substring(0, 10))); //see a piece of it
            return null;
        }
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
    public BREthereumAmount.Unit getUnit() {
        return BREthereumAmount.Unit.ETHER_WEI;
    }

    @Override
    public byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] phrase) {
        mWallet.sign(tx.getEtherTx(), new String(phrase));
        mWallet.submit(tx.getEtherTx());
        String hash = tx.getEtherTx().getHash();
        return hash == null ? new byte[0] : hash.getBytes();
    }

    @Override
    public void watchTransactionForHash(CryptoTransaction tx, OnHashUpdated listener) {
        mWatchedTransaction = tx;
        mWatchListener = listener;
    }

    @Override
    public long getRelayCount(byte[] txHash) {
        return 3;
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
        node.connect();
    }

    @Override
    public void disconnect(Context app) {
        node.disconnect();
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        //Not needed for ETH
        return false;
    }

    @Override
    public void rescan(Context app) {
        //Not needed for ETH
    }

    @Override
    public CryptoTransaction[] getTxs(Context app) {
        BREthereumTransaction[] txs = mWallet.getTransactions();
        CryptoTransaction[] arr = new CryptoTransaction[txs.length];
        for (int i = 0; i < txs.length; i++) {
            arr[i] = new CryptoTransaction(txs[i]);
        }

        return arr;
    }

    @Override
    public BigDecimal getTxFee(CryptoTransaction tx) {
        return new BigDecimal(tx.getEtherTx().getFee(BREthereumAmount.Unit.ETHER_WEI));
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        BigDecimal fee;
        if (amount == null) {
            return null;
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            fee = BigDecimal.ZERO;
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
    public String getTxAddress(CryptoTransaction tx) {
        return tx.getEtherTx().getTargetAddress();
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        BigDecimal balance = getCachedBalance(app);
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = new BigDecimal(mWallet.transactionEstimatedFee(balance.toPlainString()));
        if (fee.compareTo(balance) > 0) return BigDecimal.ZERO;
        return balance.subtract(fee);
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return new BigDecimal(1); //1 WEI
    }

    @Override
    public BigDecimal getTransactionAmount(CryptoTransaction tx) {
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

        String jsonString = BRApiManager.urlGET(app, "https://" + BreadApp.HOST + "/fee-per-kb?currency=" + getIso());

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
            Log.d(TAG, "updateFee: " + getIso() + ":" + fee + "|" + economyFee);

            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                BRSharedPrefs.putFeeRate(app, getIso(), fee);
                BRSharedPrefs.putFeeTime(app, getIso(), System.currentTimeMillis()); //store the time of the last successful fee fetch
            } else {
                BRReportsManager.reportBug(new NullPointerException("Fee is weird:" + fee));
                Log.d(TAG, "Error: Fee is unexpected value");

            }
            if (economyFee.compareTo(BigDecimal.ZERO) > 0) {
                BRSharedPrefs.putEconomyFeeRate(app, getIso(), economyFee);
            } else {
                BRReportsManager.reportBug(new NullPointerException("Economy fee is weird:" + economyFee));
                Log.d(TAG, "Error: Economy fee is unexpected value");
            }
        } catch (JSONException e) {
            Log.e(TAG, "updateFeePerKb: FAILED: " + jsonString, e);
        }

    }

    @Override
    public void refreshCachedBalance(final Context context) {
        final BigDecimal balance = new BigDecimal(mWallet.getBalance(getUnit()));
        onBalanceChanged(context, balance);
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
    public boolean generateWallet(Context app) {
        //Not needed for ETH
        return false;
    }

    @Override
    public String getSymbol(Context app) {
//        return BRConstants.symbolEther;
        return ETH_CURRENCY_CODE;
    }

    @Override
    public String getIso() {
        return ETH_CURRENCY_CODE;
    }

    @Override
    public String getScheme() {
        return ETH_SCHEME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDenominator() {
        return "1000000000000000000";
    }

    @Override
    public CryptoAddress getReceiveAddress(Context app) {
        return new CryptoAddress(mWallet.getAccount().getPrimaryAddress(), null);
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address) {
        BREthereumTransaction tx = mWallet.createTransaction(address, amount.toPlainString(), BREthereumAmount.Unit.ETHER_WEI);
        return new CryptoTransaction(tx);
    }

    @Override
    public String decorateAddress(String addr) {
        return addr;
    }

    @Override
    public String undecorateAddress(String addr) {
        return addr;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        return WalletManagerHelper.MAX_DECIMAL_PLACES;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, getIso());
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return BigDecimal.ZERO;
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
    public BigDecimal getMaxAmount(Context app) {
        return MAX_WEI;
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
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
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
        if (fiatAmount == null || fiatAmount.compareTo(BigDecimal.ZERO) == 0) return fiatAmount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        return getEthForFiat(app, fiatAmount, iso);

    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        return amount.divide(ONE_ETH, 8, ROUNDING_MODE);
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        return amount.multiply(ONE_ETH);
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        BigDecimal ethAmount = getEthForFiat(app, amount, iso);
        if (ethAmount == null) return null;
        return ethAmount.multiply(ONE_ETH);
    }

    //pass in a eth amount and return the specified amount in fiat
    //ETH rates are in BTC (thus this math)
    private BigDecimal getFiatForEth(Context app, BigDecimal ethAmount, String code) {
        //fiat rate for btc
        CurrencyEntity btcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ether
        CurrencyEntity ethBtcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), "BTC");
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
        CurrencyEntity btcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ether
        CurrencyEntity ethBtcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), "BTC");
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
    public void getBalance(final int wid, final String address, final int rid) {
        BREthereumWallet wallet = this.node.getWalletByIdentifier(wid);
        BREthereumToken token = wallet.getToken();
        if (null == token)
            getEtherBalance(wallet, wid, address, rid);
        else
            getTokenBalance(wallet, wid, token.getAddress(), address, rid);
    }

    protected void getEtherBalance(final BREthereumWallet wallet, final int wid, final String address, final int rid) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethRpcUrl = JsonRpcHelper.getEthereumRpcUrl();
                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put(JsonRpcHelper.JSONRPC, JsonRpcHelper.VERSION_2);
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_BALANCE);
                    params.put(address);
                    params.put(JsonRpcHelper.LATEST);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {

                                JSONObject responseObject = new JSONObject(jsonResult);
                                Log.d(TAG, "getBalance response -> " + responseObject.toString());

                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    String balance = responseObject.getString(JsonRpcHelper.RESULT);
                                    node.announceBalance(wid, balance, rid);
                                }
                            } else {
                                Log.e(TAG, "onRpcRequestCompleted: jsonResult is null");
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }

                    }
                });
            }
        });
    }

    protected void getTokenBalance(final BREthereumWallet wallet, final int wid,
                                   final String contractAddress,
                                   final String address,
                                   final int rid) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {

                String ethRpcUrl = JsonRpcHelper.createTokenTransactionsUrl(address, contractAddress);


                final JSONObject payload = new JSONObject();
                try {
                    payload.put(JsonRpcHelper.ID, String.valueOf(rid));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {
                                JSONObject responseObject = new JSONObject(jsonResult);

                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    String balance = responseObject.getString(JsonRpcHelper.RESULT);
                                    node.announceBalance(wid, balance, rid);

                                } else {
                                    Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                                }
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }

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
                final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
                Log.d(TAG, "Making rpc request to -> " + ethUrl);

                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_GAS_PRICE);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {
                                JSONObject responseObject = new JSONObject(jsonResult);
                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    String gasPrice = responseObject.getString(JsonRpcHelper.RESULT);
                                    node.announceGasPrice(wid, gasPrice, rid);
                                } else {
                                    Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                                }
                            }
                        } catch (JSONException je) {
                            je.printStackTrace();
                        }

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
                final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
                Log.d(TAG, "Making rpc request to -> " + ethUrl);

                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();
                final JSONObject param1 = new JSONObject();

                try {
                    param1.put(JsonRpcHelper.TO, to);
                    if (!amount.equalsIgnoreCase(HEX_PREFIX)) {
                        param1.put(JsonRpcHelper.VALUE, amount);
                    }
                    if (!data.equalsIgnoreCase(HEX_PREFIX)) {
                        param1.put(JsonRpcHelper.DATA, data);
                    }
                    params.put(param1);
                    payload.put(JsonRpcHelper.JSONRPC, JsonRpcHelper.VERSION_2);
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_ESTIMATE_GAS);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            JSONObject responseObject = new JSONObject(jsonResult);

                            if (responseObject.has(JsonRpcHelper.RESULT)) {
                                String gasEstimate = responseObject.getString(JsonRpcHelper.RESULT);
                                Log.d(TAG, "onRpcRequestCompleted: getGasEstimate: " + gasEstimate);
                                node.announceGasEstimate(wid, tid, gasEstimate, rid);
                            } else {
                                Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

    }

    @Override
    public void submitTransaction(final int wid, final int tid, final String rawTransaction, final int rid) {
        if (Utils.isEmulatorOrDebug(BreadApp.getBreadContext())) {
            Log.e(TAG, "submitTransaction: wid:" + wid);
            Log.e(TAG, "submitTransaction: tid:" + tid);
            Log.e(TAG, "submitTransaction: rawTransaction:" + rawTransaction);
            Log.e(TAG, "submitTransaction: rid:" + rid);
        }

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = JsonRpcHelper.getEthereumRpcUrl();
                Log.d(TAG, "Making rpc request to -> " + eth_url);

                JSONObject payload = new JSONObject();
                JSONArray params = new JSONArray();
                try {
                    payload.put(JsonRpcHelper.JSONRPC, "2.0");
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_SEND_RAW_TRANSACTION);
                    params.put(rawTransaction);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        String txHash = null;
                        int errCode = 0;
                        String errMessage = "";
                        if (!Utils.isNullOrEmpty(jsonResult)) {
                            try {
                                JSONObject responseObject = new JSONObject(jsonResult);
                                Log.d(TAG, "onRpcRequestCompleted: " + responseObject);
                                if (responseObject.has(JsonRpcHelper.RESULT)) {
                                    txHash = responseObject.getString(JsonRpcHelper.RESULT);
                                    Log.d(TAG, "onRpcRequestCompleted: " + txHash);
                                    node.announceSubmitTransaction(wid, tid, txHash, rid);
                                } else if (responseObject.has(JsonRpcHelper.ERROR)) {
                                    JSONObject errObj = responseObject.getJSONObject(JsonRpcHelper.ERROR);
                                    errCode = errObj.getInt(JsonRpcHelper.CODE);
                                    errMessage = errObj.getString(JsonRpcHelper.MESSAGE);
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
                                        UiUtils.showBreadSignal((Activity) app, app.getString(R.string.Alerts_sendSuccess),
                                                app.getString(R.string.Alerts_sendSuccessSubheader), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                                                    @Override
                                                    public void onComplete() {
                                                        UiUtils.killAllFragments((Activity) app);
                                                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                mWallet.updateBalance();
                                                            }
                                                        });
                                                    }
                                                });
                                    } else {
                                        String message = String.format(Locale.getDefault(), "(%d) %s", finalErrCode, finalErrMessage);
                                        BRDialog.showSimpleDialog(app, app.getString(R.string.WipeWallet_failedTitle), message);
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
                final String ethRpcUrl = JsonRpcHelper.createEthereumTransactionsUrl(address);

                final JSONObject payload = new JSONObject();
                try {
                    payload.put(JsonRpcHelper.ID, String.valueOf(id));
                    payload.put(JsonRpcHelper.ACCOUNT, address);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {

                        if (!Utils.isNullOrEmpty(jsonResult)) {
                            try {
                                // Convert response into JsonArray of transactions
                                JSONObject transactions = new JSONObject(jsonResult);

                                JSONArray transactionsArray = transactions.getJSONArray(JsonRpcHelper.RESULT);

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

                                    if (txObject.has(JsonRpcHelper.HASH)) {
                                        txHash = txObject.getString(JsonRpcHelper.HASH);
                                        // Log.d(TAG, "TxObject Hash -> " + txHash);

                                    }

                                    if (txObject.has(JsonRpcHelper.TO)) {
                                        txTo = txObject.getString(JsonRpcHelper.TO);
                                        // Log.d(TAG, "TxObject to -> " + txTo);

                                    }

                                    if (txObject.has(JsonRpcHelper.FROM)) {
                                        txFrom = txObject.getString(JsonRpcHelper.FROM);
                                        // Log.d(TAG, "TxObject from -> " + txFrom);

                                    }

                                    if (txObject.has(JsonRpcHelper.CONTRACT_ADDRESS)) {
                                        txContract = txObject.getString(JsonRpcHelper.CONTRACT_ADDRESS);
                                        // Log.d(TAG, "TxObject contractAddress -> " + txContract);

                                    }

                                    if (txObject.has(JsonRpcHelper.VALUE)) {
                                        txValue = txObject.getString(JsonRpcHelper.VALUE);
                                        // Log.d(TAG, "TxObject value -> " + txValue);

                                    }

                                    if (txObject.has(JsonRpcHelper.GAS)) {
                                        txGas = txObject.getString(JsonRpcHelper.GAS);
                                        // Log.d(TAG, "TxObject gas -> " + txGas);


                                    }

                                    if (txObject.has(JsonRpcHelper.GAS_PRICE)) {
                                        txGasPrice = txObject.getString(JsonRpcHelper.GAS_PRICE);
                                        // Log.d(TAG, "TxObject gasPrice -> " + txGasPrice);

                                    }

                                    if (txObject.has(JsonRpcHelper.NONCE)) {
                                        txNonce = txObject.getString(JsonRpcHelper.NONCE);
                                        // Log.d(TAG, "TxObject nonce -> " + txNonce);

                                    }

                                    if (txObject.has(JsonRpcHelper.GAS_USED)) {
                                        txGasUsed = txObject.getString(JsonRpcHelper.GAS_USED);
                                        // Log.d(TAG, "TxObject gasUsed -> " + txGasUsed);

                                    }

                                    if (txObject.has(JsonRpcHelper.BLOCK_NUMBER)) {
                                        txBlockNumber = txObject.getString(JsonRpcHelper.BLOCK_NUMBER);
                                        // Log.d(TAG, "TxObject blockNumber -> " + txBlockNumber);

                                    }

                                    if (txObject.has(JsonRpcHelper.BLOCK_HASH)) {
                                        txBlockHash = txObject.getString(JsonRpcHelper.BLOCK_HASH);
                                        // Log.d(TAG, "TxObject blockHash -> " + txBlockHash);

                                    }

                                    if (txObject.has(JsonRpcHelper.INPUT)) {
                                        txData = txObject.getString(JsonRpcHelper.INPUT);
                                        // Log.d(TAG, "TxObject input -> " + txData);

                                    }

                                    if (txObject.has(JsonRpcHelper.CONFIRMATIONS)) {
                                        txBlockConfirmations = txObject.getString(JsonRpcHelper.CONFIRMATIONS);
                                        // Log.d(TAG, "TxObject confirmations -> " + txBlockConfirmations);

                                    }

                                    if (txObject.has(JsonRpcHelper.TRANSACTION_INDEX)) {
                                        txBlockTransactionIndex = txObject.getString(JsonRpcHelper.TRANSACTION_INDEX);
                                        // Log.d(TAG, "TxObject transactionIndex -> " + txBlockTransactionIndex);

                                    }

                                    if (txObject.has(JsonRpcHelper.TIMESTAMP)) {
                                        txBlockTimestamp = txObject.getString(JsonRpcHelper.TIMESTAMP);
                                        // Log.d(TAG, "TxObject blockTimestamp -> " + txBlockTimestamp);

                                    }

                                    if (txObject.has(JsonRpcHelper.IS_ERROR)) {
                                        txIsError = txObject.getString(JsonRpcHelper.IS_ERROR);
                                        // Log.d(TAG, "TxObject isError -> " + txIsError);

                                    }

                                    node.announceTransaction(id, txHash,
                                            (mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(txFrom) ? address : txFrom),
                                            (mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(txTo) ? address : txTo),
                                            txContract, txValue, txGas, txGasPrice, txData, txNonce, txGasUsed, txBlockNumber, txBlockHash, txBlockConfirmations, txBlockTransactionIndex, txBlockTimestamp, txIsError);
                                    Context app = BreadApp.getBreadContext();

                                    int blockHeight = (int) node.getBlockHeight();
                                    if (app != null && blockHeight != Integer.MAX_VALUE && blockHeight > 0) {
                                        BRSharedPrefs.putLastBlockHeight(app, getIso(), blockHeight);
                                    }
                                }

                                Log.d(TAG, "Rpc Transactions array length -> " + transactionsArray.length());
                            } catch (JSONException e) {
                                Log.e(TAG, "onRpcRequestCompleted: ", e);

                            }
                        }
                    }
                });
            }
        });

    }

    @Override
    public void getLogs(final String contract, final String address, final String event, final int rid) {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethRpcUtl = JsonRpcHelper.createLogsUrl(address, contract, event);
                Log.d(TAG, "getLogs: " + ethRpcUtl);
                final JSONObject payload = new JSONObject();
                try {
                    payload.put(JsonRpcHelper.ID, String.valueOf(rid));
                    // ?? payload.put("account", address);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUtl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {

                        if (!Utils.isNullOrEmpty(jsonResult)) {
                            try {
                                // Convert response into JsonArray of logs
                                JSONObject logs = new JSONObject(jsonResult);
                                JSONArray logsArray = logs.getJSONArray(JsonRpcHelper.RESULT);

                                // Iterate through the list of transactions and call node.announceTransaction()
                                // to notify the core
                                for (int i = 0; i < logsArray.length(); i++) {
                                    JSONObject log = logsArray.getJSONObject(i);

                                    Log.d(TAG, "LogObject contains -> " + log.toString());

                                    JSONArray topicsArray = log.getJSONArray(JsonRpcHelper.TOPICS);
                                    String[] topics = new String[topicsArray.length()];
                                    for (int dex = 0; dex < topics.length; dex++) {
                                        topics[dex] = topicsArray.getString(dex);
                                    }

                                    node.announceLog(rid,
                                            log.getString(JsonRpcHelper.TRANSACTION_HASH),
                                            log.getString(JsonRpcHelper.ADDRESS), // contract
                                            topics,
                                            log.getString(JsonRpcHelper.DATA),
                                            log.getString(JsonRpcHelper.GAS_PRICE),
                                            log.getString(JsonRpcHelper.GAS_USED),
                                            log.getString(JsonRpcHelper.LOG_INDEX),
                                            log.getString(JsonRpcHelper.BLOCK_NUMBER),
                                            log.getString(JsonRpcHelper.TRANSACTION_INDEX),
                                            log.getString(JsonRpcHelper.TIMESTAMP));
                                }
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
    public void getBlockNumber(final int rid) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = JsonRpcHelper.getEthereumRpcUrl();
                Log.d(TAG, "Making rpc request to -> " + eth_url);

                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_BLOCK_NUMBER);
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            JSONObject responseObject = new JSONObject(jsonResult);

                            if (responseObject.has(JsonRpcHelper.RESULT)) {
                                String blockNumber = responseObject.getString(JsonRpcHelper.RESULT);
                                node.announceBlockNumber(blockNumber, rid);
                            } else {
                                Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void printInfo(String infoText, String walletIso, String eventName) {
        Log.d(TAG, String.format("%s (%s): %s", eventName, walletIso, infoText));
    }

    @Override
    public void handleWalletEvent(BREthereumWallet wallet, WalletEvent event,
                                  Status status,
                                  String errorDescription) {
        Context context = BreadApp.getBreadContext();

        if (context != null) {
            String iso = (null == wallet.getToken() ? getIso() : wallet.getToken().getSymbol());
            switch (event) {
                case CREATED:
                    printInfo("Wallet Created", iso, event.name());
                    break;
                case BALANCE_UPDATED:
                    if (status == Status.SUCCESS) {
                        WalletsMaster.getInstance(context).refreshBalances(context);
                        printInfo("New Balance: " + wallet.getBalance(), iso, event.name());
                    } else {
                        BRReportsManager.reportBug(new IllegalArgumentException("BALANCE_UPDATED: Failed to update balance: status:"
                                + status + ", err: " + errorDescription));
                    }
                    break;
                case DEFAULT_GAS_LIMIT_UPDATED:
                    printInfo("New Gas Limit: ...", iso, event.name());
                    break;
                case DEFAULT_GAS_PRICE_UPDATED:
                    printInfo("New Gas Price: " + BRSharedPrefs.getFeeRate(context, getIso()), iso, event.name());
                    break;
                case DELETED:
                    BRReportsManager.reportBug(new NullPointerException("Wallet was deleted:" + event.name()));
                    printInfo("Deleted: ", iso, event.name());
                    break;
            }
        }
    }

    @Override
    public void handleBlockEvent(BREthereumBlock block, BlockEvent event,
                                 Status status,
                                 String errorDescription) {
        Log.d(TAG, "handleBlockEvent: " + block + ", event: " + event);
        Context app = BreadApp.getBreadContext();
        if (app != null) {
            //String iso = (null == wallet.getToken() ? "ETH" : wallet.getToken().getSymbol());

            switch (event) {
                case CREATED:
                    printInfo("Block created: " + block.getNumber(), "UNK", event.name());
                    break;
                case DELETED:
                    printInfo("Block deleted: " + block.getNumber(), "UNK", event.name());
                    break;
            }
        }
    }

    @Override
    public void handleTransactionEvent(BREthereumWallet wallet,
                                       BREthereumTransaction transaction,
                                       TransactionEvent event,
                                       Status status,
                                       String errorDescription) {
        Context app = BreadApp.getBreadContext();

        if (app != null) {
            String iso = (null == wallet.getToken() ? getIso() : wallet.getToken().getSymbol());
            for (OnTransactionEventListener listener : mTransactionEventListeners) {
                listener.onTransactionEvent(event);
                mTransactionEventListeners.remove(listener);
            }
            switch (event) {
                case ADDED:
                    printInfo("New transaction added: ", iso, event.name());
                    getWalletManagerHelper().onTxListModified(transaction.getHash());
                    break;
                case REMOVED:
                    printInfo("Transaction removed: ", iso, event.name());
                    getWalletManagerHelper().onTxListModified(transaction.getHash());
                    break;
                case CREATED:
                    printInfo("Transaction created: " + transaction.getAmount(), iso, event.name());
                    break;
                case SIGNED:
                    printInfo("Transaction signed: " + transaction.getAmount(), iso, event.name());
                    break;
                case SUBMITTED:
                    if (mWatchedTransaction != null) {
                        Log.e(TAG, "handleTransactionEvent: mWatchedTransaction: " + mWatchedTransaction.getEtherTx().getNonce()
                                + ", actual: " + transaction.getNonce());
                        if (mWatchedTransaction.getEtherTx().getNonce() == transaction.getNonce()) {
                            String hash = transaction.getHash();
                            if (!Utils.isNullOrEmpty(hash)) {
                                if (mWatchListener != null)
                                    mWatchListener.onUpdated(hash);
                                mWatchListener = null;
                                mWatchedTransaction = null;
                            }
                        }
                    } else {
                        Log.e(TAG, "handleTransactionEvent: tx is null");
                    }
                    Log.d(TAG, "handleTransactionEvent: SUBMITTED: " + transaction.getHash());
                    printInfo("Transaction submitted: " + transaction.getAmount(), iso, event.name());
                    break;
                case BLOCKED:
                    printInfo("Transaction blocked: " + transaction.getAmount(), iso, event.name());
                    break;
                case ERRORED:
                    printInfo("Transaction error: " + transaction.getAmount(), iso, event.name());
                    break;
                case GAS_ESTIMATE_UPDATED:
                    printInfo("Transaction gas estimate updated: " + transaction.getAmount(), iso, event.name());
                    break;
                case BLOCK_CONFIRMATIONS_UPDATED:
                    printInfo("Transaction confirmations updated: " + transaction.getBlockConfirmations(), iso, event.name());
                    break;
            }
        }
    }

    @Override
    public void getNonce(final String address, final int rid) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
                Log.d(TAG, "Making rpc request to -> " + ethUrl);

                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put(JsonRpcHelper.METHOD, JsonRpcHelper.ETH_TRANSACTION_COUNT);
                    params.put(address);
                    params.put(JsonRpcHelper.LATEST);  // or "pending" ?
                    payload.put(JsonRpcHelper.PARAMS, params);
                    payload.put(JsonRpcHelper.ID, rid);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, new JsonRpcHelper.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            JSONObject responseObject = new JSONObject(jsonResult);

                            if (responseObject.has(JsonRpcHelper.RESULT)) {
                                String nonce = responseObject.getString(JsonRpcHelper.RESULT);
                                Log.d(TAG, "onRpcRequestCompleted: getNonce: " + nonce);
                                node.announceNonce(address, nonce, rid);
                            } else {
                                Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

    }

    public long getBlockHeight(){
        return node.getBlockHeight();
    }

    public BREthereumWallet getWallet() {
        return mWallet;
    }

    public void addTransactionEventListener(OnTransactionEventListener listener) {
        if (!mTransactionEventListeners.contains(listener)) {
            mTransactionEventListeners.add(listener);
        }
    }

    public void removeTransactionEventListener(OnTransactionEventListener listener) {
        mTransactionEventListeners.remove(listener);
    }

    public interface OnTransactionEventListener {
        void onTransactionEvent(TransactionEvent event);
    }

    @Override
    protected WalletEthManager getEthereumWallet() {
        return this;
    }

}
