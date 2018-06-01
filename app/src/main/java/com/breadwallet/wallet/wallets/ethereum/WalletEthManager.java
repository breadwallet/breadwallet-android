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
import com.breadwallet.tools.animation.BRAnimator;
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
import com.breadwallet.wallet.wallets.CryptoAddress;
import com.breadwallet.wallet.wallets.CryptoTransaction;

import com.breadwallet.wallet.wallets.WalletManagerHelper;
import com.platform.JsonRpcConstants;
import com.platform.JsonRpcRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
public class WalletEthManager extends BaseEthereumWalletManager implements BaseWalletManager,
        BREthereumLightNode.Client,
        BREthereumLightNode.Listener {
    private static final String TAG = WalletEthManager.class.getSimpleName();

    private CryptoTransaction mWatchedTransaction;
    private OnHashUpdated mWatchListener;

    private static String ISO = "ETH";
    public static final String ETH_SCHEME = "ethereum";

    private static final String mName = "Ethereum";


    private Map<String, Boolean> balanceStatuses = new HashMap<>();

    private static WalletEthManager instance;
    private WalletUiConfiguration uiConfig;
    private WalletSettingsConfiguration settingsConfig;
    private final BigDecimal MAX_ETH = new BigDecimal("90000000000000000000000000"); // 90m ETH * 18 (WEI)
    private final BigDecimal ONE_ETH = new BigDecimal("1000000000000000000"); //1ETH = 1000000000000000000 WEI
    private BREthereumWallet mWallet;
    public BREthereumLightNode node;

    private WalletEthManager(final Context app, byte[] ethPubKey, BREthereumNetwork network) {
        uiConfig = new WalletUiConfiguration("#5e6fa5", null,
                true, WalletManagerHelper.MAX_DECIMAL_PLACES_FOR_UI);
        settingsConfig = new WalletSettingsConfiguration(app, ISO, getFingerprintLimits(app));

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

        BreadApp.generateWalletIfIfNeeded(app, getReceiveAddress(app).stringify());
        WalletsMaster.getInstance(app).setSpendingLimitIfNotSet(app, this);

        mWallet.estimateGasPrice();
        mWallet.setDefaultUnit(BREthereumAmount.Unit.ETHER_WEI);
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

    public BREthereumWallet getEthWallet() {
        return mWallet;
    }

    @Override
    public BREthereumAmount.Unit getUnit() {
        return BREthereumAmount.Unit.ETHER_WEI;
    }

    @Override
    public boolean isAddressValid(String address) {
        return !Utils.isNullOrEmpty(address) && address.startsWith("0x");
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
        if (amount == null) return null;
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
        if (balance.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
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
    public void refreshAddress(Context app) {
        if (Utils.isNullOrEmpty(BRSharedPrefs.getReceiveAddress(app, getIso()))) {
            CryptoAddress address = getReceiveAddress(app);
            if (Utils.isNullOrEmpty(address.stringify())) {
                Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
                BRReportsManager.reportBug(new NullPointerException("empty address!"));
            }
            BRSharedPrefs.putReceiveAddress(app, address.stringify(), getIso());
        }
    }

    @Override
    public void refreshCachedBalance(final Context app) {
        if (wasBalanceUpdated(getIso())) {
            final BigDecimal balance = new BigDecimal(mWallet.getBalance(getUnit()));
            BRSharedPrefs.putCachedBalance(app, getIso(), balance);
        }

    }

    @Override
    public List<TxUiHolder> getTxUiHolders(Context app) {
        BREthereumTransaction txs[] = mWallet.getTransactions();
        if (txs == null || txs.length <= 0) return null;
        List<TxUiHolder> uiTxs = new ArrayList<>();
        for (int i = txs.length - 1; i >= 0; i--) { //revere order
            BREthereumTransaction tx = txs[i];
            uiTxs.add(new TxUiHolder(tx, tx.getTargetAddress().equalsIgnoreCase(mWallet.getAccount().getPrimaryAddress()), tx.getBlockTimestamp(),
                    (int) tx.getBlockNumber(), Utils.isNullOrEmpty(tx.getHash()) ? null : tx.getHash().getBytes(), tx.getHash(),
                    new BigDecimal(tx.getFee(BREthereumAmount.Unit.ETHER_WEI)),
                    tx.getTargetAddress(), tx.getSourceAddress(), null, 0,
                    new BigDecimal(tx.getAmount(BREthereumAmount.Unit.ETHER_WEI)), true));
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
    public String getIso() {
        return ISO;
    }

    @Override
    public String getScheme() {
        return ETH_SCHEME;
    }

    @Override
    public String getName() {
        return mName;
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
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_RPC_ENDPOINT;
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

                JsonRpcRequest.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {

                                JSONObject responseObject = new JSONObject(jsonResult);
                                Log.d(TAG, "getBalance response -> " + responseObject.toString());

                                if (responseObject.has("result")) {
                                    String balance = responseObject.getString("result");
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
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            Log.e(TAG, "getTokenBalance: App in background!");
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String host = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_TX_ENDPOINT + "query?";
                final String eth_rpc_url = host + "module=account&action=tokenbalance"
                        + "&address=" + address + "&contractaddress=" + contractAddress;

                final JSONObject payload = new JSONObject();
                try {
                    payload.put("id", String.valueOf(rid));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcRequest.makeRpcRequest(BreadApp.getBreadContext(), eth_rpc_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {
                                JSONObject responseObject = new JSONObject(jsonResult);

                                if (responseObject.has("result")) {
                                    String balance = responseObject.getString("result");
                                    node.announceBalance(wid, balance, rid);

                                } else {
                                    Log.e(TAG, "onRpcRequestCompleted: Response does not contain the key 'result'.");
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
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
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

                JsonRpcRequest.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            if (!Utils.isNullOrEmpty(jsonResult)) {
                                JSONObject responseObject = new JSONObject(jsonResult);
                                if (responseObject.has("result")) {
                                    String gasPrice = responseObject.getString("result");
                                    Log.e(TAG, "onRpcRequestCompleted: getGasPrice: " + gasPrice);
                                    node.announceGasPrice(wid, gasPrice, rid);
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
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
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

                try {
                    payload.put("method", "eth_estimateGas");
                    payload.put("params", params);
                    payload.put("id", rid);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcRequest.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            JSONObject responseObject = new JSONObject(jsonResult);

                            if (responseObject.has("result")) {
                                String gasEstimate = responseObject.getString("result");
                                Log.e(TAG, "onRpcRequestCompleted: getGasEstimate: " + gasEstimate);
                                node.announceGasEstimate(wid, tid, gasEstimate, rid);
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
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        if (Utils.isEmulatorOrDebug(BreadApp.getBreadContext())) {
            Log.e(TAG, "submitTransaction: wid:" + wid);
            Log.e(TAG, "submitTransaction: tid:" + tid);
            Log.e(TAG, "submitTransaction: rawTransaction:" + rawTransaction);
            Log.e(TAG, "submitTransaction: rid:" + rid);
        }

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_RPC_ENDPOINT;
                Log.d(TAG, "Making rpc request to -> " + eth_url);

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

                JsonRpcRequest.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        String txHash = null;
                        int errCode = 0;
                        String errMessage = "";
                        if (!Utils.isNullOrEmpty(jsonResult)) {
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
                                        BRAnimator.showBreadSignal((Activity) app, app.getString(R.string.Alerts_sendSuccess),
                                                app.getString(R.string.Alerts_sendSuccessSubheader), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                                                    @Override
                                                    public void onComplete() {
                                                        BRAnimator.killAllFragments((Activity) app);
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
                                        BRDialog.showSimpleDialog(app, "Failed", message);
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
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                //final String eth_rpc_url = String.format(JsonRpcConstants.ETH_RPC_TX_LIST, mWallet.getAccount().getPrimaryAddress());
                final String eth_rpc_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_TX_ENDPOINT
                        + "query?module=account&action=txlist&address=" + address;

                final JSONObject payload = new JSONObject();
                try {
                    payload.put("id", String.valueOf(id));
                    payload.put("account", address);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcRequest.makeRpcRequest(BreadApp.getBreadContext(), eth_rpc_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {

                        if (!Utils.isNullOrEmpty(jsonResult)) {
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
                                        // Log.d(TAG, "TxObject Hash -> " + txHash);

                                    }

                                    if (txObject.has("to")) {
                                        txTo = txObject.getString("to");
                                        // Log.d(TAG, "TxObject to -> " + txTo);

                                    }

                                    if (txObject.has("from")) {
                                        txFrom = txObject.getString("from");
                                        // Log.d(TAG, "TxObject from -> " + txFrom);

                                    }

                                    if (txObject.has("contractAddress")) {
                                        txContract = txObject.getString("contractAddress");
                                        // Log.d(TAG, "TxObject contractAddress -> " + txContract);

                                    }

                                    if (txObject.has("value")) {
                                        txValue = txObject.getString("value");
                                        // Log.d(TAG, "TxObject value -> " + txValue);

                                    }

                                    if (txObject.has("gas")) {
                                        txGas = txObject.getString("gas");
                                        // Log.d(TAG, "TxObject gas -> " + txGas);


                                    }

                                    if (txObject.has("gasPrice")) {
                                        txGasPrice = txObject.getString("gasPrice");
                                        // Log.d(TAG, "TxObject gasPrice -> " + txGasPrice);

                                    }

                                    if (txObject.has("nonce")) {
                                        txNonce = txObject.getString("nonce");
                                        // Log.d(TAG, "TxObject nonce -> " + txNonce);

                                    }

                                    if (txObject.has("gasUsed")) {
                                        txGasUsed = txObject.getString("gasUsed");
                                        // Log.d(TAG, "TxObject gasUsed -> " + txGasUsed);

                                    }

                                    if (txObject.has("blockNumber")) {
                                        txBlockNumber = txObject.getString("blockNumber");
                                        // Log.d(TAG, "TxObject blockNumber -> " + txBlockNumber);

                                    }

                                    if (txObject.has("blockHash")) {
                                        txBlockHash = txObject.getString("blockHash");
                                        // Log.d(TAG, "TxObject blockHash -> " + txBlockHash);

                                    }

                                    if (txObject.has("input")) {
                                        txData = txObject.getString("input");
                                        // Log.d(TAG, "TxObject input -> " + txData);

                                    }

                                    if (txObject.has("confirmations")) {
                                        txBlockConfirmations = txObject.getString("confirmations");
                                        // Log.d(TAG, "TxObject confirmations -> " + txBlockConfirmations);

                                    }

                                    if (txObject.has("transactionIndex")) {
                                        txBlockTransactionIndex = txObject.getString("transactionIndex");
                                        // Log.d(TAG, "TxObject transactionIndex -> " + txBlockTransactionIndex);

                                    }

                                    if (txObject.has("timeStamp")) {
                                        txBlockTimestamp = txObject.getString("timeStamp");
                                        // Log.d(TAG, "TxObject blockTimestamp -> " + txBlockTimestamp);

                                    }

                                    if (txObject.has("isError")) {
                                        txIsError = txObject.getString("isError");
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
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String host = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_TX_ENDPOINT + "query?";
                final String eth_rpc_url = host + "module=logs&action=getLogs"
                        + "&fromBlock=0&toBlock=latest"
                        + (null == contract ? "" : ("&address=" + contract))
                        + "&topic0=" + event
                        + "&topic1=" + address
                        + "&topic1_2_opr=or"
                        + "&topic2=" + address;
                Log.d(TAG, "run: " + eth_rpc_url);
                final JSONObject payload = new JSONObject();
                try {
                    payload.put("id", String.valueOf(rid));
                    // ?? payload.put("account", address);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcRequest.makeRpcRequest(BreadApp.getBreadContext(), eth_rpc_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {

                        if (!Utils.isNullOrEmpty(jsonResult)) {
                            try {
                                // Convert response into JsonArray of logs
                                JSONObject logs = new JSONObject(jsonResult);
                                JSONArray logsArray = logs.getJSONArray("result");

                                // Iterate through the list of transactions and call node.announceTransaction()
                                // to notify the core
                                for (int i = 0; i < logsArray.length(); i++) {
                                    JSONObject log = logsArray.getJSONObject(i);

                                    Log.d(TAG, "LogObject contains -> " + log.toString());

                                    JSONArray topicsArray = log.getJSONArray("topics");
                                    String[] topics = new String[topicsArray.length()];
                                    for (int dex = 0; dex < topics.length; dex++)
                                        topics[dex] = topicsArray.getString(dex);

                                    node.announceLog(rid,
                                            log.getString("transactionHash"),
                                            log.getString("address"), // contract
                                            topics,
                                            log.getString("data"),
                                            log.getString("gasPrice"),
                                            log.getString("gasUsed"),
                                            log.getString("logIndex"),
                                            log.getString("blockNumber"),
                                            log.getString("transactionIndex"),
                                            log.getString("timeStamp"));
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
        if (BreadApp.isAppInBackground(BreadApp.getBreadContext())) {
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final String eth_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_RPC_ENDPOINT;
                Log.d(TAG, "Making rpc request to -> " + eth_url);

                final JSONObject payload = new JSONObject();
                final JSONArray params = new JSONArray();

                try {
                    payload.put("method", "eth_blockNumber");
                    payload.put("params", params);
                    payload.put("id", rid);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcRequest.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        try {
                            JSONObject responseObject = new JSONObject(jsonResult);

                            if (responseObject.has("result")) {
                                String blockNumber = responseObject.getString("result");
                                Log.e(TAG, "onRpcRequestCompleted: getBlockNumber: " + blockNumber);
                                node.announceBlockNumber(blockNumber, rid);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public BREthereumLightNode getNode() {
        return node;
    }

    private void printInfo(String infoText, String walletIso, String eventName) {
        Log.d(TAG, String.format("%s (%s): %s", eventName, walletIso, infoText));
    }

    @Override
    public void handleWalletEvent(BREthereumWallet wallet, WalletEvent event,
                                  Status status,
                                  String errorDescription) {
        Context app = BreadApp.getBreadContext();

        if (app != null && Utils.isEmulatorOrDebug(BreadApp.getBreadContext())) {
            String iso = (null == wallet.getToken() ? getIso() : wallet.getToken().getSymbol());
            switch (event) {
                case CREATED:
                    printInfo("Wallet Created", iso, event.name());
                    break;

                case BALANCE_UPDATED:
                    notifyBalanceWasUpdated(wallet, iso);
                    setBalanceUpdated(iso);
                    printInfo("New Balance: " + wallet.getBalance(), iso, event.name());
                    break;
                case DEFAULT_GAS_LIMIT_UPDATED:
                    printInfo("New Gas Limit: ...", iso, event.name());
                    break;
                case DEFAULT_GAS_PRICE_UPDATED:
                    printInfo("New Gas Price: " + BRSharedPrefs.getFeeRate(app, getIso()), iso, event.name());
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
        if (app != null && Utils.isEmulatorOrDebug(app)) {
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

        if (app != null && Utils.isEmulatorOrDebug(BreadApp.getBreadContext())) {
            String iso = (null == wallet.getToken() ? getIso() : wallet.getToken().getSymbol());
            switch (event) {
                case ADDED:
                    printInfo("New transaction added: ", iso, event.name());
                    break;
                case REMOVED:
                    printInfo("Transaction removed: ", iso, event.name());
                    break;

                case CREATED:
                    printInfo("Transaction created: " + transaction.getAmount(), iso, event.name());
                    break;
                case SIGNED:
                    printInfo("Transaction signed: " + transaction.getAmount(), iso, event.name());
                    break;
                case SUBMITTED:
                    if (mWatchedTransaction != null) {
                        Log.e(TAG, "handleTransactionEvent: mWatchedTransaction: " + mWatchedTransaction.getEtherTx().getNonce() + ", actual: " + transaction.getNonce());
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
                    Log.e(TAG, "handleTransactionEvent: SUBMITTED: " + transaction.getHash());
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

    /**
     * refresh current wallet's balance by code
     *
     * @param code - wallet code for which the balance was updated
     */
    private void notifyBalanceWasUpdated(BREthereumWallet wallet, String code) {
        if (Utils.isNullOrEmpty(code)) {
            BRReportsManager.reportBug(new NullPointerException("Invalid code: " + code));
            return;
        }
        final Context app = BreadApp.getBreadContext();

        if (getIso().equalsIgnoreCase(code)) {
            //ETH wallet balance was updated

            final BigDecimal balance = new BigDecimal(wallet.getBalance(getUnit()));

            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    refreshCachedBalance(app);
                    onBalanceChanged(balance); //this, Eth wallet.
                }
            });

        } else {
            //ERC20 wallet balance was updated

            final BigDecimal balance = new BigDecimal(wallet.getBalance(BREthereumAmount.Unit.TOKEN_DECIMAL)); //use TOKEN_DECIMAL
            final String iso = wallet.getToken().getSymbol();
            if (app != null) {
                BaseWalletManager wm = WalletsMaster.getInstance(app).getWalletByIso(app, iso); //the token wallet being updated.
                if (wm != null) {
                    wm.refreshCachedBalance(app);
                    wm.onBalanceChanged(balance);
                }
            }
        }
    }

    /**
     * Store this wallet's code along with a boolean value that specifies
     * if the balance was updated in this particular launch
     *
     * @param code - wallet code for which the balance was updated
     */
    private void setBalanceUpdated(String code) {
        if (Utils.isNullOrEmpty(code)) {
            BRReportsManager.reportBug(new NullPointerException("Invalid code: " + code));
            return;
        }
        String upperCode = code.toUpperCase();
        balanceStatuses.put(upperCode, true);
    }

    /**
     * Get stored boolean value for this wallet's code that specifies
     * if the balance was updated in this particular launch
     *
     * @param code - wallet code for which the balance was updated
     */
    public boolean wasBalanceUpdated(String code) {
        if (Utils.isNullOrEmpty(code)) {
            BRReportsManager.reportBug(new NullPointerException("Invalid code: " + code));
            return false;
        }
        String upperCode = code.toUpperCase();
        return balanceStatuses.containsKey(upperCode) && balanceStatuses.get(upperCode);

    }
}
