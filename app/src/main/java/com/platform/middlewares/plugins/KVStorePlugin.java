package com.platform.middlewares.plugins;

import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.util.BRCompressor;
import com.platform.APIClient;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Plugin;
import com.platform.kvstore.CompletionObject;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;
import com.platform.sqlite.KVEntity;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/1/16.
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
public class KVStorePlugin implements Plugin {
    public static final String TAG = KVStorePlugin.class.getName();

    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (target.startsWith("/_kv/")) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            String key = target.replace("/_kv/", "");
            MainActivity app = MainActivity.app;
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }
            if (key.isEmpty()) {
                Log.e(TAG, "handle: missing key argument: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(400, null, baseRequest, response);
            }

            RemoteKVStore remote = RemoteKVStore.getInstance(APIClient.getInstance(app));
            ReplicatedKVStore store = new ReplicatedKVStore(app, remote);
            switch (request.getMethod()) {
                case "GET":
                    Log.i(TAG, "handle: " + target + " " + baseRequest.getMethod() + ", key: " + key);
                    CompletionObject getObj = store.get(key, 0);
                    KVEntity kv = getObj.kv;

                    if (kv == null) {
                        Log.e(TAG, "handle: kv store does not contain the kv: " + key);
                        return BRHTTPHelper.handleError(400, null, baseRequest, response);
                    }
                    byte[] decompressedData = BRCompressor.bz2Extract(kv.getValue());
                    Assert.assertNotNull(decompressedData);
                    try {
                        JSONObject test = new JSONObject(new String(decompressedData)); //just check for validity
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "handle: the json is not valid: " + target + " " + baseRequest.getMethod());

                        return BRHTTPHelper.handleError(500, null, baseRequest, response);
                    }
                    response.setHeader("ETag", String.valueOf(kv.getVersion()));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS Z", Locale.getDefault());
                    String date = dateFormat.format(kv.getTime());
                    response.setHeader("Last-Modified", date);

                    if (kv.getDeleted() > 0) {
                        Log.w(TAG, "handle: the key is gone: " + target + " " + baseRequest.getMethod());
                        return BRHTTPHelper.handleError(410, "Gone", baseRequest, response);
                    }
                    return BRHTTPHelper.handleSuccess(200, decompressedData, baseRequest, response, "application/json");
                case "PUT":
                    Log.i(TAG, "handle:" + target + " " + baseRequest.getMethod() + ", key: " + key);
                    // Read from request
                    byte[] rawData = null;
                    try {
                        InputStream body = request.getInputStream();
                        rawData = IOUtils.toByteArray(body);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (rawData == null) {
                        Log.e(TAG, "handle: missing request body: " + target + " " + baseRequest.getMethod());
                        return BRHTTPHelper.handleError(400, null, baseRequest, response);
                    }

                    String strVersion = request.getHeader("if-none-match");
                    if (strVersion == null) {
                        Log.e(TAG, "handle: missing If-None-Match header, set to `0` if creating a new key: " + target + " " + baseRequest.getMethod());
                        return BRHTTPHelper.handleError(400, null, baseRequest, response);
                    }
                    String ct = request.getHeader("content-type");
                    if (ct == null || !ct.equalsIgnoreCase("application/json")) {
                        Log.e(TAG, "handle: can only set application/json request bodies: " + target + " " + baseRequest.getMethod());
                        return BRHTTPHelper.handleError(400, null, baseRequest, response);
                    }

                    long version = Long.valueOf(strVersion);

                    byte[] compressedData = BRCompressor.bz2Compress(rawData);
                    assert (compressedData != null);

                    CompletionObject setObj = store.set(new KVEntity(version, 0, key, compressedData, System.currentTimeMillis(), 0));
                    if (setObj.err != null) {
                        Log.e(TAG, "handle: error setting the key: " + key + ", err: " + setObj.err);
                        int errCode = transformErrorToResponseCode(setObj.err);
                        return BRHTTPHelper.handleError(errCode, null, baseRequest, response);
                    }

                    response.setHeader("ETag", String.valueOf(setObj.version));
                    dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS Z", Locale.getDefault());
                    date = dateFormat.format(setObj.time);
                    response.setHeader("Last-Modified", date);
                    return BRHTTPHelper.handleSuccess(204, null, baseRequest, response, null);
                case "DELETE":
                    Log.i(TAG, "handle: : " + target + " " + baseRequest.getMethod() + ", key: " + key);
                    strVersion = request.getHeader("if-none-match");
                    Log.e(TAG, "handle: missing If-None-Match header: " + target + " " + baseRequest.getMethod());

                    if (strVersion == null) {
                        return BRHTTPHelper.handleError(400, null, baseRequest, response);
                    }

                    CompletionObject delObj = null;
                    try {
                        delObj = store.delete(key, Long.parseLong(strVersion));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        return BRHTTPHelper.handleError(500, null, baseRequest, response);
                    }
                    if (delObj == null || delObj.err != null) {
                        int err = 500;

                        if (delObj != null) {
                            Log.e(TAG, "handle: error deleting key: " + key + ", err: " + delObj.err);
                            err = transformErrorToResponseCode(delObj.err);
                        } else {
                            Log.e(TAG, "handle: error deleting key: " + key + ", delObj is null");
                        }
                        return BRHTTPHelper.handleError(err, null, baseRequest, response);
                    }
                    response.setHeader("ETag", String.valueOf(delObj.version));
                    dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS Z", Locale.getDefault());
                    date = dateFormat.format(delObj.time);
                    response.setHeader("Last-Modified", date);
                    return BRHTTPHelper.handleSuccess(204, null, baseRequest, response, null);

            }
        }

        return false;
    }

    private int transformErrorToResponseCode(CompletionObject.RemoteKVStoreError err) {
        switch (err) {
            case notFound:
                return 404;
            case conflict:
                return 409;
            default:
                Log.e(TAG, "transformErrorToResponseCode: unexpected error: " + err.name());
                return 500;
        }
    }

}
