package com.platform.tools;

import android.content.Context;
import android.util.Log;

import com.breadwallet.tools.util.BRCompressor;
import com.platform.APIClient;
import com.platform.entities.TxMetaData;
import com.platform.entities.WalletInfo;
import com.platform.kvstore.CompletionObject;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;

import org.json.JSONException;
import org.json.JSONObject;

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
        long ver = kvStore.remoteVersion(walletInfoKey);
        CompletionObject obj = kvStore.get(walletInfoKey, ver);
        if (obj.value == null) {
            Log.e(TAG, "getWalletInfo: value is null for key: " + obj.key);
            return null;
        }
        byte[] uncompressed = BRCompressor.bz2Extract(obj.value);
        JSONObject json;
        try {
            json = new JSONObject(new String(uncompressed));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        //todo finish
        Log.e(TAG, "getWalletInfo: " + json);
        return result;
    }

    public TxMetaData getTxMetaData(Context app, String txHash) {
        String key = "txn-" + txHash;
        TxMetaData result = new TxMetaData();
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(app));
        ReplicatedKVStore kvStore = new ReplicatedKVStore(app, remoteKVStore);
        long ver = kvStore.remoteVersion(key);
        CompletionObject obj = kvStore.get(key, ver);
        if (obj.value == null) {
            Log.e(TAG, "getTxMetaData: value is null for key: " + obj.key);
            return null;
        }
        byte[] uncompressed = BRCompressor.bz2Extract(obj.value);
        JSONObject json;
        try {
            json = new JSONObject(new String(uncompressed));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        //todo finish
        Log.e(TAG, "getTxMetaData: " + json);
        return result;
    }
}
