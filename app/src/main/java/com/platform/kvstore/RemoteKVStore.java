package com.platform.kvstore;

import android.util.Log;

import com.platform.APIClient;
import com.platform.interfaces.KVStoreAdaptor;
import com.platform.sqlite.KVItem;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import okhttp3.RequestBody;
import okhttp3.Response;

import static android.R.attr.key;
import static com.platform.kvstore.CompletionObject.RemoteKVStoreError.unknown;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/13/16.
 * Copyright (c) 2016 breadwallet LLC
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

public class RemoteKVStore implements KVStoreAdaptor {
    public static final String TAG = RemoteKVStore.class.getName();

    private static RemoteKVStore instance;
    private APIClient apiClient;
    private int retryCount = 0;

    public static RemoteKVStore getInstance(APIClient apiClient) {
        if (instance == null)
            instance = new RemoteKVStore(apiClient);
        return instance;
    }

    private RemoteKVStore(APIClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public CompletionObject ver(String key) {
        String url = String.format("%s/kv/1/%s", APIClient.BASE_URL, key);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .head()
                .build();
        Response res = null;
        res = apiClient.sendRequest(request, true, retryCount);
        if (res == null) {
            Log.d(TAG, "ver: [KV] PUT key=" + key + ", err= response is null (maybe auth challenge)");
            return new CompletionObject(0, 0, unknown);
        }

        if (!res.isSuccessful()) {
            Log.e(TAG, "ver: [KV] PUT key=" + key + ", err=" + (res.code()));
            return new CompletionObject(0, 0, unknown);
        }
        long v = extractVersion(res);
        long t = extractDate(res);
        return new CompletionObject(v, t, extractErr(res));
    }

    @Override
    public CompletionObject put(String key, byte[] value, long version) {
        String url = String.format("%s/kv/1/%s", APIClient.BASE_URL, key);
        RequestBody requestBody = RequestBody.create(null, value);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .put(requestBody)
                .addHeader("If-None-Match", String.valueOf(version))
                .addHeader("Content-Type", "application/octet-stream")
                .addHeader("Content-Length", String.valueOf(value.length))
                .build();
        Response res = apiClient.sendRequest(request, true, retryCount);
        if (res == null) {
            Log.d(TAG, "ver: [KV] PUT key=" + key + ", err= response is null (maybe auth challenge)");
            return new CompletionObject(0, 0, unknown);
        }

        if (!res.isSuccessful()) {
            Log.e(TAG, "ver: [KV] PUT key=" + key + ", err=" + (res.code()));
            return new CompletionObject(0, 0, unknown);
        }
        long v = extractVersion(res);
        long t = extractDate(res);
        return new CompletionObject(v, t, extractErr(res));
    }

    @Override
    public CompletionObject del(String key, long version) {
        String url = String.format("%s/kv/1/%s", APIClient.BASE_URL, key);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .delete()
                .addHeader("If-None-Match", String.valueOf(version))
                .build();
        Response res = null;
        res = apiClient.sendRequest(request, true, retryCount);
        if (res == null) {
            Log.d(TAG, "ver: [KV] PUT key=" + key + ", err= response is null (maybe auth challenge)");
            return new CompletionObject(0, 0, unknown);
        }

        if (!res.isSuccessful()) {
            Log.e(TAG, "ver: [KV] PUT key=" + key + ", err=" + (res.code()));
            return new CompletionObject(0, 0, unknown);
        }
        long v = extractVersion(res);
        long t = extractDate(res);
        return new CompletionObject(v, t, extractErr(res));
    }

    @Override
    public CompletionObject get(String key, long version) {
        String url = String.format("%s/kv/1/%s", APIClient.BASE_URL, key);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .addHeader("If-None-Match", String.valueOf(version))
                .build();
        Response res = null;
        res = apiClient.sendRequest(request, true, retryCount);
        if (res == null) {
            Log.d(TAG, "ver: [KV] PUT key=" + key + ", err= response is null (maybe auth challenge)");
            return new CompletionObject(0, 0, unknown);
        }

        if (!res.isSuccessful()) {
            Log.e(TAG, "ver: [KV] PUT key=" + key + ", err=" + (res.code()));
            return new CompletionObject(0, 0, unknown);
        }
        long v = extractVersion(res);
        long t = extractDate(res);
        byte[] value = new byte[0];
        try {
            value = res.body().bytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CompletionObject(v, t, value, extractErr(res));
    }

    @Override
    public CompletionObject keys() {
        String url = String.format("%s/kv/_all_keys", APIClient.BASE_URL);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();
        Response res = null;
        res = apiClient.sendRequest(request, true, retryCount);
        if (res == null) {
            Log.d(TAG, "ver: [KV] PUT key=" + key + ", err= response is null (maybe auth challenge)");
            return new CompletionObject(0, 0, unknown);
        }

        if (!res.isSuccessful()) {
            Log.e(TAG, "ver: [KV] PUT key=" + key + ", err=" + (res.code()));
            return new CompletionObject(0, 0, unknown);
        }
        byte[] reqData = null;
        try {
            reqData = res.body().bytes();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (reqData == null) return new CompletionObject(0, 0, unknown);
        ByteBuffer buffer = ByteBuffer.wrap(reqData).order(java.nio.ByteOrder.LITTLE_ENDIAN);

        List<KVItem> keys = new ArrayList<>();
        try {
            int count = buffer.getInt();

            for (int i = 0; i < count; i++) {
                String key = null;
                long version = 0;
                long time = 0;
                byte deleted = 0;

                int keyLen = buffer.getInt();

                byte[] keyBytes = new byte[keyLen];
                buffer.get(keyBytes, 0, keyLen);
                try {
                    key = new String(keyBytes, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                version = buffer.getLong();
                time = buffer.getLong();
                deleted = buffer.get();
                if (key == null || key.isEmpty()) return new CompletionObject(0, 0, unknown);
                keys.add(new KVItem(0, version, key, null, time, deleted));

            }
        } catch (Exception e) {
            e.printStackTrace();
            return new CompletionObject(0, 0, unknown);
        }
        return new CompletionObject(keys, null);
    }

    private long extractVersion(Response res) {
        long remoteVersion = 0;
        try {
            if (res != null) {
                remoteVersion = Long.valueOf(res.header("ETag"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return remoteVersion;
    }

    private long extractDate(Response res) {
        long remoteTime = 0;
        try {
            if (res != null) {
                String lastModified = res.header("Last-Modified");
                remoteTime = Date.parse(lastModified);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return remoteTime;
    }

    private CompletionObject.RemoteKVStoreError extractErr(Response res) {
        int code = res.code();
        if (code <= 399 && code >= 200) code = 999;
        switch (code) {
            case 404:
                return CompletionObject.RemoteKVStoreError.notFound;
            case 409:
                return CompletionObject.RemoteKVStoreError.conflict;
            case 410:
                return CompletionObject.RemoteKVStoreError.tombstone;
            case 999:
                return null;
            default:
                return CompletionObject.RemoteKVStoreError.unknown;
        }
    }
}
