package com.breadwallet.wallet.wallets.etherium;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.ethereum.BREthereumAccount;
import com.breadwallet.core.ethereum.BREthereumAmount;
import com.breadwallet.core.ethereum.BREthereumLightNode;
import com.breadwallet.core.ethereum.BREthereumNetwork;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.core.ethereum.BREthereumWallet;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
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
import com.platform.JsonRpcConstants;
import com.platform.JsonRpcRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
    public static final String ETH_SCHEME = null;

    private static final String mName = "Ethereum";

    private List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
    private List<OnTxStatusUpdatedListener> txStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> syncListeners = new ArrayList<>();
    private List<OnTxListModified> txModifiedListeners = new ArrayList<>();

    private static WalletEthManager instance;
    private WalletUiConfiguration uiConfig;
    private final BigDecimal MAX_ETH = new BigDecimal("90000000000000000000000000"); // 90m ETH * 18 (WEI)
    private final BigDecimal WEI_ETH = new BigDecimal("1000000000000000000"); //1ETH = 1000000000000000000 WEI
    private BREthereumWallet mWallet;
    BREthereumLightNode mNode;
    private Context mContext;

    private int mSyncRetryCount = 0;
    private static final int SYNC_MAX_RETRY = 3;

    private Executor listenerExecutor = Executors.newSingleThreadExecutor();


    private WalletEthManager(final Context app, BRCoreMasterPubKey masterPubKey, BREthereumNetwork network) {
        uiConfig = new WalletUiConfiguration("#5e70a3", true, true, false);


        String testPaperKey = "video tiger report bid suspect taxi mail argue naive layer metal surface";
        //todo change the hardcoded priv key to master pub key when done
        mNode = new BREthereumLightNode.JSON_RPC(this, network, testPaperKey);
        BREthereumAccount account = mNode.getAccount();

        mWallet = mNode.getWallet();
        mWallet.setDefaultUnit(BREthereumAmount.Unit.ETHER_WEI);
        mContext = app;

        BREthereumWallet walletToken = mNode.createWallet(BREthereumToken.tokenBRD);
        walletToken.setDefaultUnit(BREthereumAmount.Unit.TOKEN_DECIMAL);


        // Test to make sure that getTransactions fires properly
        mNode.forceTransactionUpdate();


        // Test to make rpc call to eth_estimateGas
        final String amount = "18000000000000000";
        final String eth_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_RPC_ENDPOINT;
        Log.d(TAG, "Making rpc request to -> " + eth_url);
        final JSONObject payload = new JSONObject();
        final JSONArray params = new JSONArray();
        params.put("0x6c0fe9f8f018e68e2f0bee94ab41b75e71df094d");
        params.put("18000000000000000");

        try {
            payload.put("method", "eth_estimateGas");
            payload.put("params", params);
        }catch(JSONException e){
            e.printStackTrace();
        }

        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {

                JsonRpcRequest request = new JsonRpcRequest();
                request.makeRpcRequest(mContext, eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {

                        Log.d(TAG, "Rpc getGasEstimate() Request response -> " + jsonResult);
                    }
                });
            }
        });

    }

    public synchronized static WalletEthManager getInstance(Context app) {
        if (instance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(app);
            if (Utils.isNullOrEmpty(rawPubKey)) {
                Log.e(TAG, "getInstance: rawPubKey is null");
                return null;
            }
            BRCoreMasterPubKey pubKey = new BRCoreMasterPubKey(rawPubKey, false);
//
            instance = new WalletEthManager(app, pubKey, BuildConfig.BITCOIN_TESTNET ? BREthereumNetwork.testnet : BREthereumNetwork.mainnet);

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

    }

    @Override
    public void refreshAddress(Context app) {
        BaseAddress address = getReceiveAddress(app);
        if (Utils.isNullOrEmpty(address.stringify())) {
            Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
        }
        BRSharedPrefs.putReceiveAddress(app, address.stringify(), getIso(app));
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
        return new BigDecimal(0);
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
        BigDecimal cryptoAmount = amount.divide(WEI_ETH, 8, BRConstants.ROUNDING_MODE);
        return cryptoAmount.multiply(new BigDecimal(rate));
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal fiatAmount) {
        if (fiatAmount.doubleValue() == 0) return fiatAmount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), iso);
        if (ent == null) return null;
        double rate = ent.rate;

        return fiatAmount.divide(new BigDecimal(rate), 8, ROUNDING_MODE);

    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        return amount.divide(WEI_ETH, 8, ROUNDING_MODE);
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return amount;
        return amount.multiply(WEI_ETH);
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
        return amount.divide(new BigDecimal(rate), 8, ROUNDING_MODE).multiply(WEI_ETH);
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
    public String getGasEstimate(int id, String to, final String amount, String data) {


        final String eth_url = "https://" + BreadApp.HOST + JsonRpcConstants.BRD_ETH_RPC_ENDPOINT;
        Log.d(TAG, "Making rpc request to -> " + eth_url);
        final JSONObject payload = new JSONObject();
        final JSONArray params = new JSONArray();

        // TODO : Remove and replace with actual address and amount from current wallet
        params.put(to);
        params.put(amount);
        params.put(data);

        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {


                try {
                    payload.put("method", "eth_estimateGas");
                    payload.put("params", params);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonRpcRequest request = new JsonRpcRequest();
                request.makeRpcRequest(mContext, eth_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {

                        Log.d(TAG, "Rpc Request response -> " + jsonResult);
                    }
                });
            }
        });
        return null;
    }

    @Override
    public String submitTransaction(int id, String rawTransaction) {
        return null;
    }

    @Override
    public void getTransactions(int id, final String account) {
        Log.d(TAG, "getTransactions()");
        Log.d(TAG, "account -> " + account);

        final String eth_rpc_url = String.format(JsonRpcConstants.ETH_RPC_TX_LIST, "0xbdfdad139440d2db9ba2aa3b7081c2de39291508");
        Log.d(TAG, "ETH RPC URL -> " + eth_rpc_url);

        final JsonRpcRequest request = new JsonRpcRequest();
        final JSONObject payload = new JSONObject();
        try {
            payload.put("id", String.valueOf(id));
            payload.put("account", account);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {


                request.makeRpcRequest(mContext, eth_rpc_url, payload, new JsonRpcRequest.JsonRpcRequestListener() {
                    @Override
                    public void onRpcRequestCompleted(String jsonResult) {
                        Log.d(TAG, "Rpc response string 3 -> " + jsonResult);


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

                                    String adrians = "0xbdfdad139440d2db9ba2aa3b7081c2de39291508";


                                    mNode.announceTransaction(txHash,
                                            (adrians.equalsIgnoreCase(txFrom) ? account : txFrom),
                                            (adrians.equalsIgnoreCase(txTo) ? account : txTo),
                                            txContract, txValue, txGas, txGasPrice, txData, txNonce, txGasUsed, txBlockNumber, txBlockHash, txBlockConfirmations, txBlockTransactionIndex, txBlockTimestamp, txIsError);
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
}
