package com.platform.middlewares.plugins;

import android.app.Activity;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.platform.APIClient;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Plugin;
import com.platform.kvstore.CompletionObject;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;
import com.platform.sqlite.KVItem;

import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

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
            Activity app = BreadApp.getCurrentActivity();
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }
            if (key.isEmpty()) {
                Log.e(TAG, "handle: missing key argument: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(400, null, baseRequest, response);
            }

            RemoteKVStore remote = RemoteKVStore.getInstance(APIClient.getInstance());
            ReplicatedKVStore store = new ReplicatedKVStore(remote);
            switch (request.getMethod()) {
                case "GET":
                    Log.i(TAG, "handle: " + target + " " + baseRequest.getMethod() + ", key: " + key);
                    CompletionObject getObj = store.get(getKey(key), 0);
                    KVItem kv = getObj.kv;

                    if (kv == null || kv.deleted > 0) {
                        Log.e(TAG, "handle: kv store does not contain the kv: " + key);
                        return BRHTTPHelper.handleError(404, null, baseRequest, decorateResponse(0, 0, response));
                    }
                    try {
                        JSONObject test = new JSONObject(new String(kv.value)); //just check for validity
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "handle: the json is not valid: for key: " + key + ", " + target + " " + baseRequest.getMethod());
                        store.delete(getKey(key), kv.version);
                        return BRHTTPHelper.handleError(404, null, baseRequest, decorateResponse(kv.version, kv.time, response));
                    }

                    if (kv.deleted > 0) {
                        Log.w(TAG, "handle: the key is gone: " + target + " " + baseRequest.getMethod());
                        return BRHTTPHelper.handleError(410, "Gone", baseRequest, decorateResponse(kv.version, kv.time, response));
                    }
                    return BRHTTPHelper.handleSuccess(200, kv.value, baseRequest, decorateResponse(kv.version, kv.time, response), "application/json");
                case "PUT":
                    Log.i(TAG, "handle:" + target + " " + baseRequest.getMethod() + ", key: " + key);
                    // Read from request
                    byte[] rawData = BRHTTPHelper.getBody(request);

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

                    CompletionObject setObj = store.set(new KVItem(version, 0, getKey(key), rawData, System.currentTimeMillis(), 0));
                    if (setObj.err != null) {
                        Log.e(TAG, "handle: error setting the key: " + key + ", err: " + setObj.err);
                        int errCode = transformErrorToResponseCode(setObj.err);
                        return BRHTTPHelper.handleError(errCode, null, baseRequest, response);
                    }

                    return BRHTTPHelper.handleSuccess(204, null, baseRequest, decorateResponse(setObj.version, setObj.time, response), null);
                case "DELETE":
                    Log.i(TAG, "handle: : " + target + " " + baseRequest.getMethod() + ", key: " + key);
                    strVersion = request.getHeader("if-none-match");
                    Log.e(TAG, "handle: missing If-None-Match header: " + target + " " + baseRequest.getMethod());

                    if (strVersion == null) {
                        Log.e(TAG, "handle: if-none-match is missing, sending 400");
                        return BRHTTPHelper.handleError(400, null, baseRequest, response);
                    }

                    CompletionObject delObj = null;
                    try {
                        delObj = store.delete(getKey(key), Long.parseLong(strVersion));
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
                    response.addHeader("Cache-Control", "max-age=0, must-revalidate");
                    SimpleDateFormat dateFormat = new SimpleDateFormat(
                            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                    String rfc1123 = dateFormat.format(delObj.time);
                    response.setHeader("Last-Modified", rfc1123);
                    return BRHTTPHelper.handleSuccess(204, null, baseRequest, response, null);

            }
        }

        return false;
    }

    private HttpServletResponse decorateResponse(long ver, long time, HttpServletResponse response) {
        response.addHeader("Cache-Control", "max-age=0, must-revalidate");
        response.addHeader("ETag", String.valueOf(ver));

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        String rfc1123 = dateFormat.format(time);
        response.setHeader("Content-Type", "application/json");
        response.addHeader("Last-Modified", rfc1123);
        return response;
    }

    private String getKey(String key) {
        if (key == null) Log.e(TAG, "getKey: key is null");
        return "plat-" + key;
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
