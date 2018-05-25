package com.platform.tools;

import android.content.Context;
import android.util.Log;

import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.util.BRCompressor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.platform.APIClient;
import com.platform.entities.TokenListMetaData;
import com.platform.entities.TxMetaData;
import com.platform.entities.WalletInfo;
import com.platform.kvstore.CompletionObject;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;
import com.platform.sqlite.KVItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 6/22/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class KVStoreManager {
    private static final String TAG = KVStoreManager.class.getName();

    private static KVStoreManager instance;
    private static final String WALLET_INFO_KEY = "wallet-info";
    private static final String TOKEN_LIST_META_DATA = "token-list-metadata";

    private KVStoreManager() {
    }

    public static KVStoreManager getInstance() {
        if (instance == null) instance = new KVStoreManager();
        return instance;
    }

    public WalletInfo getWalletInfo(Context app) {
        WalletInfo result = new WalletInfo();
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = ReplicatedKVStore.getInstance(app, remoteKVStore);
        long ver = kvStore.localVersion(WALLET_INFO_KEY).version;
        CompletionObject obj = kvStore.get(WALLET_INFO_KEY, ver);
        if (obj.kv == null) {
            Log.e(TAG, "getWalletInfo: value is null for key: " + WALLET_INFO_KEY);
            return null;
        }

        JSONObject json;

        try {
            byte[] decompressed = BRCompressor.bz2Extract(obj.kv.value);
            if (decompressed == null) {
                Log.e(TAG, "getWalletInfo: decompressed value is null");
                return null;
            }
            json = new JSONObject(new String(decompressed));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        try {
            result.classVersion = json.getInt("classVersion");
            result.creationDate = json.getInt("creationDate");
            result.name = json.getString("name");
//            result.currentCurrency = json.getString("currentCurrency");
            Log.d(TAG, "getWalletInfo: " + result.creationDate + ", name: " + result.name);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "getWalletInfo: FAILED to get json value");
        }

        Log.e(TAG, "getWalletInfo: " + json);
        return result;
    }

    public void putWalletInfo(Context app, WalletInfo info) {
        WalletInfo old = getWalletInfo(app);
        if (old == null) old = new WalletInfo(); //create new one if it's null

        //add all the params that we want to change
        if (info.classVersion != 0) old.classVersion = info.classVersion;
        if (info.creationDate != 0) old.creationDate = info.creationDate;
        if (info.name != null) old.name = info.name;

        //sanity check
        if (old.classVersion == 0) old.classVersion = 1;
        if (old.name != null) old.name = "My Bread";

        JSONObject obj = new JSONObject();
        byte[] result;
        try {
            obj.put("classVersion", old.classVersion);
            obj.put("creationDate", old.creationDate);
            obj.put("name", old.name);
            result = obj.toString().getBytes();

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "putWalletInfo: FAILED to create json");
            return;
        }

        if (result.length == 0) {
            Log.e(TAG, "putWalletInfo: FAILED: result is empty");
            return;
        }
        byte[] compressed;
        try {
            compressed = BRCompressor.bz2Compress(result);
        } catch (IOException e) {
            BRReportsManager.reportBug(e);
            return;
        }
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = ReplicatedKVStore.getInstance(app, remoteKVStore);
        long localVer = kvStore.localVersion(WALLET_INFO_KEY).version;
        long removeVer = kvStore.remoteVersion(WALLET_INFO_KEY);
        CompletionObject compObj = kvStore.set(localVer, removeVer, WALLET_INFO_KEY, compressed, System.currentTimeMillis(), 0);
        if (compObj.err != null) {
            Log.e(TAG, "putWalletInfo: Error setting value for key: " + WALLET_INFO_KEY + ", err: " + compObj.err);
        }

    }

    public synchronized void putTokenListMetaData(Context app, TokenListMetaData md) {
        TokenListMetaData old = getTokenListMetaData(app);
        if (old == null) old = new TokenListMetaData(null, null); //create new one if it's null

        //add all the params that we want to change
        if (md.enabledCurrencies.size() > 0) old.enabledCurrencies = md.enabledCurrencies;
        old.hiddenCurrencies = md.hiddenCurrencies;

        JSONObject obj = new JSONObject();
        byte[] result;
        try {
            obj.put("classVersion", TokenListMetaData.CLASS_VERSION);
            JSONArray enabledArr = new JSONArray();
            JSONArray hiddenArr = new JSONArray();
            for (TokenListMetaData.TokenInfo item : old.enabledCurrencies) {
                enabledArr.put(item.erc20 ? item.symbol + ":" + item.contractAddress : item.symbol);
            }
            for (TokenListMetaData.TokenInfo item : old.hiddenCurrencies) {
                hiddenArr.put(item.erc20 ? item.symbol + ":" + item.contractAddress : item.symbol);
            }

            obj.put("enabledCurrencies", enabledArr);
            obj.put("hiddenCurrencies", hiddenArr);
            result = obj.toString().getBytes();

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "putTokenListMetaData: FAILED to create json");
            return;
        }

        if (result.length == 0) {
            Log.e(TAG, "putTokenListMetaData: FAILED: result is empty");
            return;
        }
        byte[] compressed;
        try {
            compressed = BRCompressor.bz2Compress(result);
        } catch (IOException e) {
            BRReportsManager.reportBug(e);
            return;
        }
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = ReplicatedKVStore.getInstance(app, remoteKVStore);
        long localVer = kvStore.localVersion(TOKEN_LIST_META_DATA).version;
        long removeVer = kvStore.remoteVersion(TOKEN_LIST_META_DATA);
        CompletionObject compObj = kvStore.set(localVer, removeVer, TOKEN_LIST_META_DATA, compressed, System.currentTimeMillis(), 0);
        if (compObj.err != null) {
            Log.e(TAG, "putTokenListMetaData: Error setting value for key: " + TOKEN_LIST_META_DATA + ", err: " + compObj.err);
        }

    }

    //expensive, takes ~ 20 milliseconds
    public synchronized TokenListMetaData getTokenListMetaData(Context app) {

        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = ReplicatedKVStore.getInstance(app, remoteKVStore);
        long ver = kvStore.localVersion(TOKEN_LIST_META_DATA).version;
        CompletionObject obj = kvStore.get(TOKEN_LIST_META_DATA, ver);
        if (obj.kv == null) {
            Log.e(TAG, "getTokenListMetaData: value is null for key: " + TOKEN_LIST_META_DATA);
            return null;
        }

        JSONObject json;

        try {
            byte[] decompressed = BRCompressor.bz2Extract(obj.kv.value);
            if (decompressed == null) {
                Log.e(TAG, "getTokenListMetaData: decompressed value is null");
                return null;
            }
            json = new JSONObject(new String(decompressed));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }


        TokenListMetaData result = null;
        try {
            int classVersion = json.getInt("classVersion"); //not using yet
            List<TokenListMetaData.TokenInfo> enabledCurrencies = jsonToMetaData(json.getJSONArray("enabledCurrencies"));
            List<TokenListMetaData.TokenInfo> hiddenCurrencies = jsonToMetaData(json.getJSONArray("hiddenCurrencies"));
            result = new TokenListMetaData(enabledCurrencies, hiddenCurrencies);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "getWalletInfo: FAILED to get json value");
        }

        return result;
    }

    private List<TokenListMetaData.TokenInfo> jsonToMetaData(JSONArray json) throws JSONException {
        List<TokenListMetaData.TokenInfo> result = new ArrayList<>();
        if (json == null) {
            Log.e(TAG, "jsonToMetaData: JSONArray is null");
            return result;
        }
        for (int i = 0; i < json.length(); i++) {
            String s = json.getString(i);
            boolean isErc20 = s.contains(":");
            String symbol = s;
            String address = null;
            if (isErc20) {
                symbol = s.split(":")[0];
                address = s.split(":")[1];
            }
            result.add(new TokenListMetaData.TokenInfo(symbol, isErc20, address));
        }

        return result;
    }

    public TxMetaData getTxMetaData(Context app, byte[] txHash) {
        String key = txKey(txHash);

        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = ReplicatedKVStore.getInstance(app, remoteKVStore);
        long ver = kvStore.localVersion(key).version;

        CompletionObject obj = kvStore.get(key, ver);

        if (obj.kv == null) {
//            Log.e(TAG, "getTxMetaData: kv is null for key: " + key);
            return null;
        }

        return valueToMetaData(obj.kv.value);
    }

    public Map<String, TxMetaData> getAllTxMD(Context app) {
        Map<String, TxMetaData> mds = new HashMap<>();
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = ReplicatedKVStore.getInstance(app, remoteKVStore);
        List<KVItem> list = kvStore.getAllTxMdKv();
        for (int i = 0; i < list.size(); i++) {
            TxMetaData md = valueToMetaData(list.get(i).value);
            if (md != null) mds.put(list.get(i).key, md);
        }

        return mds;
    }

    public TxMetaData valueToMetaData(byte[] value) {
        TxMetaData result = new TxMetaData();
        JSONObject json;
        if (value == null) {
            Log.e(TAG, "valueToMetaData: value is null!");
            return null;
        }
        try {
            byte[] decompressed = BRCompressor.bz2Extract(value);
            if (decompressed == null) {
                Log.e(TAG, "getTxMetaData: decompressed value is null");
                return null;
            }
            json = new JSONObject(new String(decompressed));
        } catch (JSONException e) {

            Log.e(TAG, "valueToMetaData: " + new String(value) + ":", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "valueToMetaData: ", e);
            return null;
        }

        try {
            if (json.has("classVersion"))
                result.classVersion = json.getInt("classVersion");
            if (json.has("bh"))
                result.blockHeight = json.getInt("bh");
            if (json.has("er"))
            result.exchangeRate = json.getDouble("er");
            if (json.has("erc"))
            result.exchangeCurrency = json.getString("erc");
            if (json.has("comment"))
            result.comment = json.getString("comment");
            if (json.has("fr"))
            result.fee = json.getString("fr");
            if (json.has("s"))
            result.txSize = json.getInt("s");
            if (json.has("c"))
            result.creationTime = json.getInt("c");
            if (json.has("dId"))
            result.deviceId = json.getString("dId");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "getTxMetaData: FAILED to get json value");
        }
        return result;
    }

    public void putTxMetaData(Context app, TxMetaData data, byte[] txHash) {
//        if(ActivityUTILS.isMainThread()) throw new NetworkOnMainThreadException();
        String key = txKey(txHash);
        TxMetaData old = getTxMetaData(app, txHash);

        boolean needsUpdate = false;
        if (old == null) {
            needsUpdate = true;
            old = data;
        } else if (data != null) {
            String finalExchangeCurrency = getFinalValue(data.exchangeCurrency, old.exchangeCurrency);
            if (finalExchangeCurrency != null) {
                Log.e(TAG, "putTxMetaData: finalExchangeCurrency:" + finalExchangeCurrency);
                old.exchangeCurrency = finalExchangeCurrency;
                needsUpdate = true;
            }
            String finalDeviceId = getFinalValue(data.deviceId, old.deviceId);
            if (finalDeviceId != null) {
                Log.e(TAG, "putTxMetaData: finalDeviceId:" + finalDeviceId);
                old.deviceId = finalDeviceId;
                needsUpdate = true;
            }
            String finalComment = getFinalValue(data.comment, old.comment);
            if (finalComment != null) {
                Log.e(TAG, "putTxMetaData: comment:" + finalComment);
                old.comment = finalComment;
                needsUpdate = true;
            }
            int finalClassVersion = getFinalValue(data.classVersion, old.classVersion);
            if (finalClassVersion != -1) {
                old.classVersion = finalClassVersion;
                needsUpdate = true;
            }
            int finalCreationTime = getFinalValue(data.creationTime, old.creationTime);
            if (finalCreationTime != -1) {
                old.creationTime = finalCreationTime;
                needsUpdate = true;
            }
            double finalExchangeRate = getFinalValue(data.exchangeRate, old.exchangeRate);
            if (finalExchangeRate != -1) {
                old.exchangeRate = finalExchangeRate;
                needsUpdate = true;
            }
            int finalBlockHeight = getFinalValue(data.blockHeight, old.blockHeight);
            if (finalBlockHeight != -1) {
                old.blockHeight = finalBlockHeight;
                needsUpdate = true;
            }
            int finalTxSize = getFinalValue(data.txSize, old.txSize);
            if (finalTxSize != -1) {
                old.txSize = finalTxSize;
                needsUpdate = true;
            }
            String finalFee = getFinalValue(data.fee, old.fee);
            if (finalFee != null) {
                old.fee = finalFee;
                needsUpdate = true;
            }
        }

        if (!needsUpdate) return;

        Log.d(TAG, "putTxMetaData: updating txMetadata for : " + key);

        JSONObject obj = new JSONObject();
        byte[] result;
        try {
            obj.put("classVersion", old.classVersion);
            obj.put("bh", old.blockHeight);
            obj.put("er", old.exchangeRate);
            obj.put("erc", old.exchangeCurrency == null ? "" : old.exchangeCurrency);
            obj.put("fr", old.fee);
            obj.put("s", old.txSize);
            obj.put("c", old.creationTime);
            obj.put("dId", old.deviceId == null ? "" : old.deviceId);
            obj.put("comment", old.comment == null ? "" : old.comment);
            result = obj.toString().getBytes();

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "putTxMetaData: FAILED to create json");
            return;
        }

        if (result.length == 0) {
            Log.e(TAG, "putTxMetaData: FAILED: result is empty");
            return;
        }
        byte[] compressed;
        try {
            compressed = BRCompressor.bz2Compress(result);
        } catch (IOException e) {
            BRReportsManager.reportBug(e);
            return;
        }
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = ReplicatedKVStore.getInstance(app, remoteKVStore);
        long localVer = kvStore.localVersion(key).version;
        long removeVer = kvStore.remoteVersion(key);
        CompletionObject compObj = kvStore.set(localVer, removeVer, key, compressed, System.currentTimeMillis(), 0);
        if (compObj.err != null) {
            Log.e(TAG, "putTxMetaData: Error setting value for key: " + key + ", err: " + compObj.err);
        }

    }

    //null means no change
    private String getFinalValue(String newVal, String oldVal) {
        if (newVal == null) return null;
        if (oldVal == null) return newVal;
        if (newVal.equals(oldVal)) {
            return null;
        } else {
            return newVal;
        }
    }

    // -1 means no change
    private int getFinalValue(int newVal, int oldVal) {
        if (newVal <= 0) return -1;
        if (oldVal <= 0) return newVal;
        if (newVal == oldVal) {
            return -1;
        } else {
            return newVal;
        }
    }

    public TxMetaData createMetadata(Context app, BaseWalletManager wm, CryptoTransaction tx) {
        TxMetaData txMetaData = new TxMetaData();
        txMetaData.exchangeCurrency = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = RatesDataSource.getInstance(app).getCurrencyByCode(app, wm.getIso(), txMetaData.exchangeCurrency);
        txMetaData.exchangeRate = ent == null ? 0 : new BigDecimal(ent.rate).setScale(8, BRConstants.ROUNDING_MODE).stripTrailingZeros().doubleValue();
        txMetaData.fee = wm.getTxFee(tx).toPlainString();
        txMetaData.txSize = tx.getTxSize().intValue();
        txMetaData.blockHeight = BRSharedPrefs.getLastBlockHeight(app, wm.getIso());
        txMetaData.creationTime = (int) (System.currentTimeMillis() / 1000);//seconds
        txMetaData.deviceId = BRSharedPrefs.getDeviceId(app);
        txMetaData.classVersion = 1;

        return txMetaData;

    }

    // -1 means no change
    private long getFinalValue(long newVal, long oldVal) {
        if (newVal <= 0) return -1;
        if (oldVal <= 0) return newVal;
        if (newVal == oldVal) {
            return -1;
        } else {
            return newVal;
        }
    }

    // -1 means no change
    private double getFinalValue(double newVal, double oldVal) {
        if (newVal <= 0) return -1;
        if (oldVal <= 0) return newVal;
        if (newVal == oldVal) {
            return -1;
        } else {
            return newVal;
        }
    }


    public static String txKey(byte[] txHash) {
        if (Utils.isNullOrEmpty(txHash)) return null;
        String hex = Utils.bytesToHex(CryptoHelper.sha256(txHash));
        if (Utils.isNullOrEmpty(hex)) return null;
        return "txn2-" + hex;
    }
}
