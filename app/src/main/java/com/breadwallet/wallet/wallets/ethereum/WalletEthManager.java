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
import com.breadwallet.core.ethereum.BREthereumBlock;
import com.breadwallet.core.ethereum.BREthereumEWM;
import com.breadwallet.core.ethereum.BREthereumNetwork;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.core.ethereum.BREthereumTransfer;
import com.breadwallet.core.ethereum.BREthereumWallet;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.repository.RatesRepository;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.FileHelper;
import com.breadwallet.tools.util.TokenUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.util.JsonRpcHelper;
import com.breadwallet.wallet.wallets.CryptoAddress;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.WalletManagerHelper;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.breadwallet.tools.util.BRConstants.ROUNDING_MODE;

/**
 * Ethereum wallet.
 * All the BREthereumEWM.Client methods are called in a separate thread already so no need to create new threads.
 */
public class WalletEthManager extends BaseEthereumWalletManager implements BREthereumEWM.Client {
    private static final String TAG = WalletEthManager.class.getSimpleName();

    private CryptoTransaction mWatchedTransaction;
    private OnHashUpdated mWatchListener;

    public static final String NAME = "Ethereum";
    public static final String ETH_CURRENCY_CODE = "ETH";
    private static final String HEX_PREFIX = "0x";
    private static final String ETH_SCHEME = "ethereum";
    //1ETH = 1000000000000000000 WEI
    private static final String ETHER_WEI = "1000000000000000000";
    //Max amount in ether
    private static final String MAX_ETH = "90000000";
    private static final BigDecimal MAX_WEI = new BigDecimal(MAX_ETH).multiply(new BigDecimal(ETHER_WEI)); // 90m ETH * 18 (WEI)
    private static final BigDecimal ONE_ETH = new BigDecimal(ETHER_WEI);
    private static final String JSON_ERROR_MESSAGE_PREP = "json exception (prep)";
    private static final String JSON_ERROR_MESSAGE_REPLY = "json exception (reply)";
    private static final int JSON_ERROR_CODE = -1;


    public BREthereumEWM node;
    private static WalletEthManager mInstance;
    private WalletUiConfiguration mUiConfig;
    private WalletSettingsConfiguration mSettingsConfig;

    private BREthereumWallet mWallet;
    private Context mContext;

    private List<OnTokenLoadedListener> mOnTokenLoadedListeners = new ArrayList<>();
    private List<OnTransactionEventListener> mTransactionEventListeners = new ArrayList<>();

    private WalletEthManager(final Context context, byte[] ethPubKey, BREthereumNetwork network) {
        mContext = context.getApplicationContext();
        mUiConfig = new WalletUiConfiguration("#5e6fa5", null,
                true, WalletManagerHelper.MAX_DECIMAL_PLACES_FOR_UI);
        mSettingsConfig = new WalletSettingsConfiguration();

        if (Utils.isNullOrEmpty(ethPubKey)) {
            Log.e(TAG, "WalletEthManager: Using the paperKey to create");
            String paperKey = null;
            try {
                paperKey = new String(BRKeyStore.getPhrase(context, 0));
            } catch (UserNotAuthenticatedException e) {
                Log.e(TAG, "WalletEthManager: ", e);
                return;
            }
            if (Utils.isNullOrEmpty(paperKey)) {
                Log.e(TAG, "WalletEthManager: paper key is empty too, no wallet!");
                return;
            }

            String[] words = lookupWords(context, paperKey, Locale.getDefault().getLanguage());

            if (null == words) {
                Log.e(TAG, "WalletEthManager: paper key does not validate with BIP39 Words for: "
                        + Locale.getDefault().getLanguage());
                return;
            }

            node = new BREthereumEWM(this, BREthereumEWM.Mode.API_WITH_P2P_SEND, network, FileHelper.getCoreDataFilePath(context), paperKey, words);

            mWallet = node.getWallet();

            if (null == mWallet) {
                Log.e(TAG, "WalletEthManager: failed to create the ETH wallet using paperKey.");
                return;
            }

            ethPubKey = mWallet.getAccount().getPrimaryAddressPublicKey();
            BRKeyStore.putEthPublicKey(ethPubKey, context);
        } else {
            Log.d(TAG, "WalletEthManager: Using the pubkey to create");
            node = new BREthereumEWM(this, BREthereumEWM.Mode.API_WITH_P2P_SEND, network, FileHelper.getCoreDataFilePath(context), ethPubKey);

            mWallet = node.getWallet();

            if (null == mWallet) {
                Log.e(TAG, "WalletEthManager: failed to create the ETH wallet using saved publicKey.");
                return;
            }
        }
        mAddress = getReceiveAddress(context).stringify();
        if (Utils.isNullOrEmpty(mAddress)) {
            BRReportsManager.reportBug(new IllegalArgumentException("Eth address missing!"), true);
        }

        WalletsMaster.getInstance().setSpendingLimitIfNotSet(context, this);

        estimateGasPrice();
        mWallet.setDefaultUnit(BREthereumAmount.Unit.ETHER_WEI);
        node.connect();
    }

    public static synchronized WalletEthManager getInstance(Context context) {
        if (mInstance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(context);
            if (Utils.isNullOrEmpty(rawPubKey)) {
                Log.e(TAG, "getInstance: rawPubKey is null");
                return null;
            }
            byte[] ethPubKey = BRKeyStore.getEthPublicKey(context);
            if (Utils.isNullOrEmpty(ethPubKey)) {
                //check if there is a master key and if not means the wallet isn't created yet
                if (Utils.isNullOrEmpty(BRKeyStore.getMasterPublicKey(context))) {
                    return null;
                }
            }
            mInstance = new WalletEthManager(context, ethPubKey, BuildConfig.BITCOIN_TESTNET ? BREthereumNetwork.testnet : BREthereumNetwork.mainnet);

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
        String hash = tx.getHash();
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
        BREthereumTransfer[] txs = mWallet.getTransfers();
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
        if (amount == null) {
            return null;
        }
        BigDecimal fee = BigDecimal.ZERO;
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            fee = new BigDecimal(mWallet.transferEstimatedFee(amount.toPlainString()));
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
        BigDecimal balance = getBalance();
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = new BigDecimal(mWallet.transferEstimatedFee(balance.toPlainString()));
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
        // do nothing
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
    public String getCurrencyCode() {
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
        return ETHER_WEI;
    }

    @Override
    public CryptoAddress getReceiveAddress(Context app) {
        return new CryptoAddress(mWallet.getAccount().getPrimaryAddress(), null);
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address) {
        BREthereumTransfer tx = mWallet.createTransfer(address, amount.toPlainString(), BREthereumAmount.Unit.ETHER_WEI);
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
    public BigDecimal getBalance() {
        return new BigDecimal(mWallet.getBalance(getUnit()));
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
        BigDecimal fiatData = RatesRepository.getInstance(app)
                .getFiatForCrypto( BigDecimal.ONE, getCurrencyCode(), BRSharedPrefs.getPreferredFiatIso(app));
        if (fiatData == null) {
            return BigDecimal.ZERO;
        }
        return fiatData; //fiat, e.g. dollars
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        if (app == null) {
            return null;
        }
        return getFiatForSmallestCrypto(app, getBalance(), null);
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return amount;
        }
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        if (ent != null) {
            //passed in a custom CurrencyEntity
            //get crypto amount
            BigDecimal cryptoAmount = amount.divide(ONE_ETH, SCALE, BRConstants.ROUNDING_MODE);
            //multiply by fiat rate
            return cryptoAmount.multiply(new BigDecimal(ent.rate));
        }
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(ONE_ETH, SCALE, BRConstants.ROUNDING_MODE);

        BigDecimal fiatData = RatesRepository.getInstance(app).getFiatForCrypto(cryptoAmount, getCurrencyCode(), iso);
        if (fiatData == null) {
            return null;
        }
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
        return amount.divide(ONE_ETH, SCALE, ROUNDING_MODE);
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

    //pass in a fiat amount and return the specified amount in ETH
    //ETH rates are in BTC (thus this math)
    private BigDecimal getEthForFiat(Context app, BigDecimal fiatAmount, String code) {
        //fiat rate for btc
        CurrencyEntity btcRate = RatesRepository.getInstance(app).getCurrencyByCode(WalletBitcoinManager.BITCOIN_CURRENCY_CODE, code);
        //Btc rate for ether
        CurrencyEntity ethBtcRate = RatesRepository.getInstance(app).getCurrencyByCode(getCurrencyCode(), WalletBitcoinManager.BITCOIN_CURRENCY_CODE);
        if (btcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No USD rates for BTC");
            return null;
        }
        if (ethBtcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No BTC rates for ETH");
            return null;
        }

        return fiatAmount.divide(new BigDecimal(ethBtcRate.rate).multiply(new BigDecimal(btcRate.rate)), SCALE, BRConstants.ROUNDING_MODE);
    }

    protected void getEtherBalance(final BREthereumWallet wallet, final long walletIdentifier, final String address, final int requestIdentifier) {
        final String ethRpcUrl = JsonRpcHelper.getEthereumRpcUrl();
        final JSONObject payload = new JSONObject();
        final JSONArray params = new JSONArray();

        try {
            payload.put(BRConstants.JSONRPC, BRConstants.VERSION_2);
            payload.put(BRConstants.METHOD, BRConstants.ETH_BALANCE);
            params.put(address);
            params.put(BRConstants.LATEST);
            payload.put(BRConstants.PARAMS, params);
            payload.put(BRConstants.ID, requestIdentifier);
        } catch (JSONException e) {
            Log.e(TAG, "getEtherBalance: ", e);
        }

        JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUrl, payload, jsonResult -> {
            try {
                if (!Utils.isNullOrEmpty(jsonResult)) {

                    JSONObject responseObject = new JSONObject(jsonResult);
                    Log.d(TAG, "getBalance response -> " + responseObject.toString());

                    if (responseObject.has(BRConstants.RESULT)) {
                        String balance = responseObject.getString(BRConstants.RESULT);
                        node.announceBalance(walletIdentifier, balance, requestIdentifier);
                    }
                } else {
                    Log.e(TAG, "onRpcRequestCompleted: jsonResult is null");
                }
            } catch (JSONException e) {
                Log.e(TAG, "onRpcRequestCompleted: ", e);
            }

        });
    }


    protected void getTokenBalance(final BREthereumWallet wallet, final long walletIdentifier,
                                   final String contractAddress,
                                   final String address,
                                   final int requestIdentifier) {

        String ethRpcUrl = JsonRpcHelper.createTokenTransactionsUrl(address, contractAddress);

        final JSONObject payload = new JSONObject();
        try {
            payload.put(BRConstants.ID, String.valueOf(requestIdentifier));
        } catch (JSONException e) {
            Log.e(TAG, "getTokenBalance: ", e);
        }

        JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUrl, payload, jsonResult -> {
            try {
                if (!Utils.isNullOrEmpty(jsonResult)) {
                    JSONObject responseObject = new JSONObject(jsonResult);

                    if (responseObject.has(BRConstants.RESULT)) {
                        String balance = responseObject.getString(BRConstants.RESULT);
                        node.announceBalance(walletIdentifier, balance, requestIdentifier);
                    } else {
                        Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "onRpcRequestCompleted: ", e);
            }
        });
    }

    //region BREthereumEWM.Client

    @Override
    public void getGasPrice(final long walletIdentifier, final int requestIdentifier) {
        final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
        Log.d(TAG, "Making rpc request to -> " + ethUrl);

        final JSONObject payload = new JSONObject();
        final JSONArray params = new JSONArray();

        try {
            payload.put(BRConstants.METHOD, BRConstants.ETH_GAS_PRICE);
            payload.put(BRConstants.PARAMS, params);
            payload.put(BRConstants.ID, requestIdentifier);
        } catch (JSONException e) {
            Log.e(TAG, "getGasPrice: ", e);
        }

        JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, jsonResult -> {
            try {
                if (!Utils.isNullOrEmpty(jsonResult)) {
                    JSONObject responseObject = new JSONObject(jsonResult);
                    if (responseObject.has(BRConstants.RESULT)) {
                        String gasPrice = responseObject.getString(BRConstants.RESULT);
                        node.announceGasPrice(walletIdentifier, gasPrice, requestIdentifier);
                    } else {
                        Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "onRpcRequestCompleted: ", e);
            }

        });
    }

    @Override
    public void getGasEstimate(final long walletIdentifier, final long transactionIdentifier, final String from, final String to, final String amount, final String data, final int requestIdentifer) {
        final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
        Log.d(TAG, "Making getGasEstimate rpc request to -> " + ethUrl);

        final JSONObject payload = new JSONObject();
        final JSONArray params = new JSONArray();
        final JSONObject param1 = new JSONObject();

        try {
            param1.put(BRConstants.FROM, from);
            param1.put(BRConstants.TO, to);
            if (!amount.equalsIgnoreCase(HEX_PREFIX)) {
                param1.put(BRConstants.VALUE, amount);
            }
            if (!data.equalsIgnoreCase(HEX_PREFIX)) {
                param1.put(BRConstants.DATA, data);
            }
            params.put(param1);
            payload.put(BRConstants.JSONRPC, BRConstants.VERSION_2);
            payload.put(BRConstants.METHOD, BRConstants.ETH_ESTIMATE_GAS);
            payload.put(BRConstants.PARAMS, params);
            payload.put(BRConstants.ID, requestIdentifer);
        } catch (JSONException e) {
            Log.e(TAG, "getGasEstimate: ", e);
        }
        JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, jsonResult -> {
            try {
                JSONObject responseObject = new JSONObject(jsonResult);

                if (responseObject.has(BRConstants.RESULT)) {
                    String gasEstimate = responseObject.getString(BRConstants.RESULT);
                    Log.d(TAG, "onRpcRequestCompleted: getGasEstimate: " + gasEstimate);
                    node.announceGasEstimate(walletIdentifier, transactionIdentifier, gasEstimate, requestIdentifer);
                } else {
                    Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                }
            } catch (JSONException e) {
                Log.e(TAG, "onRpcRequestCompleted: ", e);
            }
        });
    }

    /**
     * The JSON RPC callbacks
     * Implement JSON RPC methods synchronously
     */

    @Override
    public void getBalance(final long walletIdentifier, final String address, final int requestIdentifier) {
        BREthereumWallet wallet = this.node.getWalletByIdentifier(walletIdentifier);
        BREthereumToken token = wallet.getToken();
        if (null == token)
            getEtherBalance(wallet, walletIdentifier, address, requestIdentifier);
        else
            getTokenBalance(wallet, walletIdentifier, token.getAddress(), address, requestIdentifier);
    }

    @Override
    public void submitTransaction(final long walletIdentifier, final long transactionIdentifier, final String rawTransaction, final int requestIdentifer) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "submitTransaction: walletIdentifier:" + walletIdentifier);
            Log.d(TAG, "submitTransaction: transactionIdentifier:" + transactionIdentifier);
            Log.d(TAG, "submitTransaction: rawTransaction:" + rawTransaction);
            Log.d(TAG, "submitTransaction: requestIdentifer:" + requestIdentifer);
        }

        final String eth_url = JsonRpcHelper.getEthereumRpcUrl();
        Log.d(TAG, "Making rpc request to -> " + eth_url);

        JSONObject payload = new JSONObject();
        JSONArray params = new JSONArray();
        try {
            payload.put(BRConstants.JSONRPC, BRConstants.VERSION_2);
            payload.put(BRConstants.METHOD, BRConstants.ETH_SEND_RAW_TRANSACTION);
            params.put(rawTransaction);
            payload.put(BRConstants.PARAMS, params);
            payload.put(BRConstants.ID, requestIdentifer);
        } catch (JSONException e) {
            Log.e(TAG, "submitTransaction: ", e);
            node.announceSubmitTransaction(walletIdentifier, transactionIdentifier, null, JSON_ERROR_CODE, JSON_ERROR_MESSAGE_PREP, requestIdentifer);
        }

        JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, jsonResult -> {
            String txHash = null;
            int errCode = 0;
            String errMessage = "";
            if (!Utils.isNullOrEmpty(jsonResult)) {
                try {
                    JSONObject responseObject = new JSONObject(jsonResult);
                    Log.d(TAG, "onRpcRequestCompleted: " + responseObject);
                    if (responseObject.has(BRConstants.RESULT)) {
                        txHash = responseObject.getString(BRConstants.RESULT);
                        Log.d(TAG, "onRpcRequestCompleted: " + txHash);
                        node.announceSubmitTransaction(walletIdentifier, transactionIdentifier, txHash, JSON_ERROR_CODE, null, requestIdentifer);
                    } else if (responseObject.has(BRConstants.ERROR)) {
                        JSONObject errObj = responseObject.getJSONObject(BRConstants.ERROR);
                        errCode = errObj.getInt(BRConstants.CODE);
                        errMessage = errObj.getString(BRConstants.MESSAGE);
                        node.announceSubmitTransaction(walletIdentifier, transactionIdentifier, null, errCode, errMessage, requestIdentifer);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "onRpcRequestCompleted: ", e);
                    node.announceSubmitTransaction(walletIdentifier, transactionIdentifier, null, JSON_ERROR_CODE, JSON_ERROR_MESSAGE_REPLY, requestIdentifer);
                }
            }
            final String finalTxHash = txHash;
            final String finalErrMessage = errMessage;
            final int finalErrCode = errCode;
            BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
                final Context context = BreadApp.getBreadContext();
                if (context instanceof Activity) {
                    if (!Utils.isNullOrEmpty(finalTxHash)) {
                        PostAuth.stampMetaData(context, finalTxHash.getBytes());
                        EventUtils.sendTransactionEvent(null);
                        UiUtils.showBreadSignal((Activity) context, context.getString(R.string.Alerts_sendSuccess),
                                context.getString(R.string.Alerts_sendSuccessSubheader), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                                    @Override
                                    public void onComplete() {
                                        UiUtils.killAllFragments((Activity) context);
                                    }
                                });
                    } else {
                        String message = String.format(Locale.getDefault(), "(%d) %s", finalErrCode, finalErrMessage);
                        EventUtils.sendTransactionEvent(message);
                        BRDialog.showSimpleDialog(context, context.getString(R.string.WipeWallet_failedTitle), message);
                    }
                } else {
                    Log.e(TAG, "submitTransaction: app is null or not an activity");
                }
            });
        });
    }

    @Override
    public void getTransactions(final String address, final long fromBlock, final long toBlock, final int id) {
        final String ethRpcUrl = JsonRpcHelper.createEthereumTransactionsUrl(address, fromBlock, toBlock);

        final JSONObject payload = new JSONObject();
        try {
            payload.put(BRConstants.ID, String.valueOf(id));
            payload.put(BRConstants.ACCOUNT, address);
        } catch (JSONException e) {
            Log.e(TAG, "getTransactions: ", e);
        }

        JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUrl, payload, jsonResult -> {
            boolean success = true;
            if (!Utils.isNullOrEmpty(jsonResult)) {
                try {
                    // Convert response into JsonArray of transactions
                    JSONObject transactions = new JSONObject(jsonResult);

                    JSONArray transactionsArray = transactions.getJSONArray(BRConstants.RESULT);

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

                        if (txObject.has(BRConstants.HASH)) {
                            txHash = txObject.getString(BRConstants.HASH);
                        }

                        if (txObject.has(BRConstants.TO)) {
                            txTo = txObject.getString(BRConstants.TO);
                        }

                        if (txObject.has(BRConstants.FROM)) {
                            txFrom = txObject.getString(BRConstants.FROM);
                        }

                        if (txObject.has(BRConstants.CONTRACT_ADDRESS)) {
                            txContract = txObject.getString(BRConstants.CONTRACT_ADDRESS);
                        }

                        if (txObject.has(BRConstants.VALUE)) {
                            txValue = txObject.getString(BRConstants.VALUE);
                        }

                        if (txObject.has(BRConstants.GAS)) {
                            txGas = txObject.getString(BRConstants.GAS);
                        }

                        if (txObject.has(BRConstants.GAS_PRICE)) {
                            txGasPrice = txObject.getString(BRConstants.GAS_PRICE);
                        }

                        if (txObject.has(BRConstants.NONCE)) {
                            txNonce = txObject.getString(BRConstants.NONCE);
                        }

                        if (txObject.has(BRConstants.GAS_USED)) {
                            txGasUsed = txObject.getString(BRConstants.GAS_USED);
                        }

                        if (txObject.has(BRConstants.BLOCK_NUMBER)) {
                            txBlockNumber = txObject.getString(BRConstants.BLOCK_NUMBER);
                        }

                        if (txObject.has(BRConstants.BLOCK_HASH)) {
                            txBlockHash = txObject.getString(BRConstants.BLOCK_HASH);
                        }

                        if (txObject.has(BRConstants.INPUT)) {
                            txData = txObject.getString(BRConstants.INPUT);
                        }

                        if (txObject.has(BRConstants.CONFIRMATIONS)) {
                            txBlockConfirmations = txObject.getString(BRConstants.CONFIRMATIONS);
                        }

                        if (txObject.has(BRConstants.TRANSACTION_INDEX)) {
                            txBlockTransactionIndex = txObject.getString(BRConstants.TRANSACTION_INDEX);
                        }

                        if (txObject.has(BRConstants.TIMESTAMP)) {
                            txBlockTimestamp = txObject.getString(BRConstants.TIMESTAMP);
                        }

                        if (txObject.has(BRConstants.IS_ERROR)) {
                            txIsError = txObject.getString(BRConstants.IS_ERROR);
                        }

                        node.announceTransaction(id, txHash,
                                (mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(txFrom) ? address : txFrom),
                                (mWallet.getAccount().getPrimaryAddress().equalsIgnoreCase(txTo) ? address : txTo),
                                txContract, txValue, txGas, txGasPrice, txData, txNonce, txGasUsed, txBlockNumber, txBlockHash, txBlockConfirmations, txBlockTransactionIndex, txBlockTimestamp, txIsError);
                        Context app = BreadApp.getBreadContext();

                        int blockHeight = (int) node.getBlockHeight();
                        if (app != null && blockHeight != Integer.MAX_VALUE && blockHeight > 0) {
                            BRSharedPrefs.putLastBlockHeight(app, getCurrencyCode(), blockHeight);
                        }
                    }
                    Log.d(TAG, "Rpc Transactions array length -> " + transactionsArray.length());
                } catch (JSONException e) {
                    Log.e(TAG, "onRpcRequestCompleted: ", e);
                    success = false;

                }
            }
            node.announceTransactionComplete(id, success);
        });
    }

    @Override
    public void getLogs(final String contract, final String address, final String event, final long fromBlock, final long toBlock, final int requestIdentifer) {
        final String ethRpcUtl = JsonRpcHelper.createLogsUrl(address, contract, event, fromBlock, toBlock);
        Log.d(TAG, "getLogs: " + ethRpcUtl);
        final JSONObject payload = new JSONObject();
        try {
            payload.put(BRConstants.ID, String.valueOf(requestIdentifer));
            // ?? payload.put("account", address);
        } catch (JSONException e) {
            Log.e(TAG, "getLogs: ", e);
        }

        JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethRpcUtl, payload, jsonResult -> {
            boolean success = true;
            if (!Utils.isNullOrEmpty(jsonResult)) {
                try {
                    // Convert response into JsonArray of logs
                    JSONObject logs = new JSONObject(jsonResult);
                    JSONArray logsArray = logs.getJSONArray(BRConstants.RESULT);

                    // Iterate through the list of transactions and call node.announceTransaction()
                    // to notify the core
                    for (int i = 0; i < logsArray.length(); i++) {
                        JSONObject log = logsArray.getJSONObject(i);

                        Log.d(TAG, "LogObject contains -> " + log.toString());

                        JSONArray topicsArray = log.getJSONArray(BRConstants.TOPICS);
                        List<String> topicsList = new ArrayList<>();
                        for (int dex = 0; dex < topicsArray.length(); dex++) {
                            String topic = topicsArray.getString(dex);
                            // Remove empty topics - assume as only ever the last topic.
                            if (topic == null || topic.equals("")) {
                                break;
                            }
                            topicsList.add(topic);
                        }
                        String[] topics = topicsList.toArray(new String[0]);

                        node.announceLog(requestIdentifer,
                                log.getString(BRConstants.TRANSACTION_HASH),
                                log.getString(BRConstants.ADDRESS), // contract
                                topics,
                                log.getString(BRConstants.DATA),
                                log.getString(BRConstants.GAS_PRICE),
                                log.getString(BRConstants.GAS_USED),
                                log.getString(BRConstants.LOG_INDEX),
                                log.getString(BRConstants.BLOCK_NUMBER),
                                log.getString(BRConstants.TRANSACTION_INDEX),
                                log.getString(BRConstants.TIMESTAMP));
                    }
                } catch (JSONException e) {
                    success = false;
                    Log.e(TAG, "onRpcRequestCompleted: ", e);
                }
            }
            node.announceLogComplete(requestIdentifer, success);
        });
    }

    @Override
    public void getBlocks(String address, int interests, long fromBlock, long toBlock, int requestIdentifer) {
        // Not used for now.
    }

    @Override
    public void getTokens(int requestIdentifer) {
        TokenUtil.fetchTokensFromServer(mContext);
    }

    @Override
    public void getBlockNumber(final int requestIdentifer) {
        final String eth_url = JsonRpcHelper.getEthereumRpcUrl();
        Log.d(TAG, "Making rpc request to -> " + eth_url);

        final JSONObject payload = new JSONObject();
        final JSONArray params = new JSONArray();

        try {
            payload.put(BRConstants.METHOD, BRConstants.ETH_BLOCK_NUMBER);
            payload.put(BRConstants.PARAMS, params);
            payload.put(BRConstants.ID, requestIdentifer);

        } catch (JSONException e) {
            Log.e(TAG, "getBlockNumber: ", e);
        }

        JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), eth_url, payload, jsonResult -> {
            try {
                JSONObject responseObject = new JSONObject(jsonResult);

                if (responseObject.has(BRConstants.RESULT)) {
                    String blockNumber = responseObject.getString(BRConstants.RESULT);
                    node.announceBlockNumber(blockNumber, requestIdentifer);
                } else {
                    Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                }
            } catch (JSONException e) {
                Log.e(TAG, "onRpcRequestCompleted: ", e);
            }
        });
    }

    @Override
    public void getNonce(final String address, final int requestIdentifer) {
        final String ethUrl = JsonRpcHelper.getEthereumRpcUrl();
        Log.d(TAG, "Making rpc request to -> " + ethUrl);

        final JSONObject payload = new JSONObject();
        final JSONArray params = new JSONArray();

        try {
            payload.put(BRConstants.METHOD, BRConstants.ETH_TRANSACTION_COUNT);
            params.put(address);
            params.put(BRConstants.LATEST);  // or "pending" ?
            payload.put(BRConstants.PARAMS, params);
            payload.put(BRConstants.ID, requestIdentifer);

        } catch (JSONException e) {
            Log.e(TAG, "getNonce: ", e);
        }

        JsonRpcHelper.makeRpcRequest(BreadApp.getBreadContext(), ethUrl, payload, jsonResult -> {
            try {
                JSONObject responseObject = new JSONObject(jsonResult);

                if (responseObject.has(BRConstants.RESULT)) {
                    String nonce = responseObject.getString(BRConstants.RESULT);
                    Log.d(TAG, "onRpcRequestCompleted: getNonce: " + nonce);
                    node.announceNonce(address, nonce, requestIdentifer);
                } else {
                    Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                }
            } catch (JSONException e) {
                Log.e(TAG, "onRpcRequestCompleted: ", e);
            }
        });

    }

    @Override
    public void handleWalletEvent(BREthereumWallet wallet, BREthereumEWM.WalletEvent event,
                                  BREthereumEWM.Status status,
                                  String errorDescription) {
        Context context = BreadApp.getBreadContext();

        if (context != null) {
            String currencyCode = (null == wallet.getToken() ? getCurrencyCode() : wallet.getToken().getSymbol());
            switch (event) {
                case CREATED:
                    printInfo("Wallet Created", currencyCode, event.name());
                    break;
                case BALANCE_UPDATED:
                    if (status == BREthereumEWM.Status.SUCCESS) {
                        // mWallet.getBalance returns the balance of the node that is currently syncing
                        BaseWalletManager walletManager = WalletsMaster.getInstance().getWalletByIso(context, currencyCode);
                        BigDecimal balance = walletManager.getBalance();
                        WalletsMaster.getInstance().refreshBalance(currencyCode, balance);
                        printInfo("New Balance: " + wallet.getBalance(), currencyCode, event.name());
                    } else {
                        BRReportsManager.reportBug(new IllegalArgumentException("BALANCE_UPDATED: Failed to update balance: status:"
                                + status + ", err: " + errorDescription));
                    }
                    break;
                case DEFAULT_GAS_LIMIT_UPDATED:
                    printInfo("New Gas Limit: ...", currencyCode, event.name());
                    break;
                case DEFAULT_GAS_PRICE_UPDATED:
                    printInfo("New Gas Price: ...", currencyCode, event.name());
                    break;
                case DELETED:
                    BRReportsManager.reportBug(new NullPointerException("Wallet was deleted:" + event.name()));
                    printInfo("Deleted: ", currencyCode, event.name());
                    break;
            }
        }
    }

    @Override
    public void handleBlockEvent(BREthereumBlock block, BREthereumEWM.BlockEvent event,
                                 BREthereumEWM.Status status,
                                 String errorDescription) {
        Log.d(TAG, "handleBlockEvent: " + block + ", event: " + event);

        switch (event) {
            case CREATED:
                printInfo("Block created: ", "UNK", event.name());
                break;
            case DELETED:
                printInfo("Block deleted: ", "UNK", event.name());
                break;
        }
    }

    @Override
    public void handleTransferEvent(BREthereumWallet wallet,
                                    BREthereumTransfer transaction,
                                    BREthereumEWM.TransactionEvent event,
                                    BREthereumEWM.Status status,
                                    String errorDescription) {

        String currencyCode = (null == wallet.getToken() ? getCurrencyCode() : wallet.getToken().getSymbol());
        for (OnTransactionEventListener listener : mTransactionEventListeners) {
            listener.onTransactionEvent(event, transaction, status);
        }
        switch (event) {
            case CREATED:
                printInfo("Transaction created: " + transaction.getAmount(), currencyCode, event.name());
                break;
            case SIGNED:
                printInfo("Transaction signed: " + transaction.getAmount(), currencyCode, event.name());
                break;
            case SUBMITTED:
                if (mWatchedTransaction != null) {
                    Log.e(TAG, "handleTransactionEvent: mWatchedTransaction: " + mWatchedTransaction.getEtherTx().getNonce()
                            + ", actual: " + transaction.getNonce());
                    if (mWatchedTransaction.getEtherTx().getNonce() == transaction.getNonce()) {
                        String hash = transaction.getOriginationTransactionHash();
                        if (!Utils.isNullOrEmpty(hash)) {
                            if (mWatchListener != null) {
                                mWatchListener.onUpdated(hash);
                            }
                            mWatchListener = null;
                            mWatchedTransaction = null;
                        }
                    }
                } else {
                    Log.e(TAG, "handleTransactionEvent: tx is null");
                }

                // Get the wallet manager helper for this txn's currency (assume ETH)
                // TODO: Refactor logic such that WalletEthManager is not handling non-ETH events
                WalletManagerHelper walletManagerHelper = getWalletManagerHelper();
                if (WalletsMaster.getInstance().isCurrencyCodeErc20(mContext, currencyCode)) {
                    walletManagerHelper = WalletTokenManager.getTokenWalletByIso(mContext, currencyCode).getWalletManagerHelper();
                }
                walletManagerHelper.onTxListModified(transaction.getOriginationTransactionHash());

                Log.d(TAG, "handleTransactionEvent: SUBMITTED: " + transaction.getOriginationTransactionHash());
                printInfo("Transaction submitted: " + transaction.getAmount(), currencyCode, event.name());
                break;
            case INCLUDED:
                printInfo("Transaction blocked: " + transaction.getAmount(), currencyCode, event.name());
                break;
            case ERRORED:
                printInfo("Transaction error: " + transaction.getAmount(), currencyCode, event.name());
                break;
            case GAS_ESTIMATE_UPDATED:
                printInfo("Transaction gas estimate updated: " + transaction.getAmount(), currencyCode, event.name());
                break;
            case BLOCK_CONFIRMATIONS_UPDATED:
                printInfo("Transaction confirmations updated: " + transaction.getBlockConfirmations(), currencyCode, event.name());
                break;
        }
    }

    @Override
    public void handleEWMEvent(BREthereumEWM.EWMEvent event, BREthereumEWM.Status status, String errorDescription) {
        // Not used for now.
    }

    @Override
    public void handlePeerEvent(BREthereumEWM.PeerEvent event, BREthereumEWM.Status status, String errorDescription) {
        // Not used for now.
    }

    @Override
    public void handleTokenEvent(BREthereumToken token, BREthereumEWM.TokenEvent event) {
        Log.d(TAG, "handleTokenEvent: " + token.getName());
        if (event == BREthereumEWM.TokenEvent.CREATED) {
            for (OnTokenLoadedListener listener : mOnTokenLoadedListeners) {
                listener.onTokenLoaded(token.getSymbol());
            }
        }
    }

    //endregion

    public long getBlockHeight() {
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

    /**
     * Interface for listening to transaction events
     */
    public interface OnTransactionEventListener {

        /**
         * Handles a given transaction event
         *
         * @param event an enum indicating what type of transaction event has occurred
         * @param transaction the associated transaction
         * @param status the status of the event
         */
        void onTransactionEvent(BREthereumEWM.TransactionEvent event, BREthereumTransfer transaction, BREthereumEWM.Status status);
    }

    @Override
    protected WalletEthManager getEthereumWallet() {
        return this;
    }

    private void printInfo(String infoText, String walletIso, String eventName) {
        Log.d(TAG, String.format("%s (%s): %s", eventName, walletIso, infoText));
    }

    public void addTokenLoadedListener(OnTokenLoadedListener listener) {
        if (!mOnTokenLoadedListeners.contains(listener)) {
            mOnTokenLoadedListeners.add(listener);
        }
    }

    public void removeTokenLoadedListener(OnTokenLoadedListener listener) {
        mOnTokenLoadedListeners.remove(listener);
    }

    /** Callback for observing loaded tokens. */
    public interface OnTokenLoadedListener {
        /**
         * Called when a new token is available.
         *
         * @param symbol The symbol of the loaded token.
         */
        void onTokenLoaded(String symbol);
    }
}
