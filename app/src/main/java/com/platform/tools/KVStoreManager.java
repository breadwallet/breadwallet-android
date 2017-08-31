package com.platform.tools;

import android.content.Context;
import android.util.Log;

import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.util.BRCompressor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.APIClient;
import com.platform.entities.TxMetaData;
import com.platform.entities.WalletInfo;
import com.platform.kvstore.CompletionObject;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import static android.R.attr.key;

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
    String walletInfoKey = "wallet-info";

    private KVStoreManager() {
    }

    public static KVStoreManager getInstance() {
        if (instance == null) instance = new KVStoreManager();
        return instance;
    }

    public WalletInfo getWalletInfo(Context app) {
        WalletInfo result = new WalletInfo();
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = new ReplicatedKVStore(app, remoteKVStore);
        long ver = kvStore.localVersion(walletInfoKey);
        CompletionObject obj = kvStore.get(walletInfoKey, ver);
        if (obj.kv == null) {
            Log.e(TAG, "getWalletInfo: value is null for key: " + obj.key);
            return null;
        }

        JSONObject json;

        try {
            byte[] decompressed = BRCompressor.bz2Extract(obj.kv.value);
            if (obj.kv.value == null) {
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
        byte[] compressed = BRCompressor.bz2Compress(result);
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = new ReplicatedKVStore(app, remoteKVStore);
        long localVer = kvStore.localVersion(walletInfoKey);
        long removeVer = kvStore.remoteVersion(walletInfoKey);
        CompletionObject compObj = kvStore.set(localVer, removeVer, walletInfoKey, compressed, System.currentTimeMillis(), 0);
        if (compObj.err != null) {
            Log.e(TAG, "putWalletInfo: Error setting value for key: " + walletInfoKey + ", err: " + compObj.err);
        }

    }

    public TxMetaData getTxMetaData(Context app, byte[] txHash) {
        Log.e(TAG, "txHash hex: " + Utils.bytesToHex(txHash));
        String hex = Utils.bytesToHex(CryptoHelper.sha256(txHash));
        String key = "txn-" + hex;
        Log.e(TAG, "getTxMetaData: txMetaData: " + key);

        TxMetaData result = new TxMetaData();
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = new ReplicatedKVStore(app, remoteKVStore);
        long ver = kvStore.remoteVersion(key);
        CompletionObject obj = kvStore.get(key, ver);
        if (obj.value == null) {
            Log.e(TAG, "getTxMetaData: value is null for key: " + key);
            return null;
        }
        Log.e(TAG, "getTxMetaData: FOUND:" + key);
        JSONObject json;

        try {
//            byte[] decompressed = BRCompressor.bz2Extract(obj.kv.getValue());
//            if (decompressed == null) {
//                Log.e(TAG, "getWalletInfo: decompressed value is null");
//                return null;
//            }
            json = new JSONObject(new String(obj.kv.getValue()));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        try {
            result.classVersion = json.getInt("classVersion");
            result.blockHeight = json.getInt("bh");
            result.exchangeRate = json.getDouble("er");
            result.exchangeCurrency = json.getString("erc");
            result.fee = json.getLong("fr");
            result.txSize = json.getInt("s");
            result.creationTime = json.getInt("c");
            result.deviceId = json.getString("dId");
            result.comment = json.getString("comment");
            result.label = json.getString("label");

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "getWalletInfo: FAILED to get json value");
        }

        Log.e(TAG, "getWalletInfo: " + json);
        return result;
    }

    public void putTxMetaData(Context app, TxMetaData data) {
        //todo finish
    }
}
