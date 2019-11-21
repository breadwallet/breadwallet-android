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

package com.breadwallet.legacy.wallet.wallets.ethereum;

import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.app.BreadApp;
import com.breadwallet.legacy.presenter.entities.CurrencyEntity;
import com.breadwallet.legacy.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.legacy.wallet.configs.WalletUiConfiguration;
import com.breadwallet.legacy.wallet.util.JsonRpcHelper;
import com.breadwallet.legacy.wallet.wallets.CryptoAddress;
import com.breadwallet.legacy.wallet.wallets.CryptoTransaction;
import com.breadwallet.legacy.wallet.wallets.WalletManagerHelper;
import com.breadwallet.legacy.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.repository.RatesRepository;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.tools.util.Utils;

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
public class WalletEthManager extends BaseEthereumWalletManager {
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


    private static WalletEthManager mInstance;
    private WalletUiConfiguration mUiConfig;
    private WalletSettingsConfiguration mSettingsConfig;

    private Context mContext;

    private List<OnTokenLoadedListener> mOnTokenLoadedListeners = new ArrayList<>();

    private WalletEthManager(final Context context, byte[] ethPubKey) {
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

        } else {
        }
        mAddress = getReceiveAddress(context).stringify();
        if (Utils.isNullOrEmpty(mAddress)) {
            BRReportsManager.reportBug(new IllegalArgumentException("Eth address missing!"), true);
        }

        estimateGasPrice();
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
        }
        return mInstance;
    }

    public void estimateGasPrice() {
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
    public byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] phrase) {
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
    }

    @Override
    public void disconnect(Context app) {
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
        return new CryptoTransaction[0];
    }

    @Override
    public BigDecimal getTxFee(CryptoTransaction tx) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
            return null;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return null;
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
        return new BigDecimal(1); //1 WEI
    }

    @Override
    public BigDecimal getTransactionAmount(CryptoTransaction tx) {
        return BigDecimal.ZERO;
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
        return false;
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
        return null;
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address) {
        return null;
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
        return BigDecimal.ZERO;
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
                .getFiatForCrypto(BigDecimal.ONE, getCurrencyCode(), BRSharedPrefs.getPreferredFiatIso(app));
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

    protected void getEtherBalance(final long walletIdentifier, final String address, final int requestIdentifier) {
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
                    }
                } else {
                    Log.e(TAG, "onRpcRequestCompleted: jsonResult is null");
                }
            } catch (JSONException e) {
                Log.e(TAG, "onRpcRequestCompleted: ", e);
            }

        });
    }


    protected void getTokenBalance(final long walletIdentifier,
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
                } else {
                    Log.e(TAG, "onRpcRequestCompleted: Error: " + jsonResult);
                }
            } catch (JSONException e) {
                Log.e(TAG, "onRpcRequestCompleted: ", e);
            }
        });

    }

    public long getBlockHeight() {
        return 0L;
    }


    @Override
    public Object getWallet() {
        return null;
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

    /**
     * Callback for observing loaded tokens.
     */
    public interface OnTokenLoadedListener {
        /**
         * Called when a new token is available.
         *
         * @param symbol The symbol of the loaded token.
         */
        void onTokenLoaded(String symbol);
    }
}
