package com.platform.middlewares.plugins;

import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.util.BRCompressor;
import com.platform.APIClient;
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
            Log.e(TAG, "handling: " + target + " " + baseRequest.getMethod());
            String key = target.replace("/_kv/", "");
            MainActivity app = MainActivity.app;
            if (app == null) {
                try {
                    response.sendError(500, "context is null");
                    baseRequest.setHandled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
            if (key.isEmpty()) {
                Log.e(TAG, "handle: missing key argument");
                try {
                    response.sendError(400);
                    baseRequest.setHandled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            RemoteKVStore remote = RemoteKVStore.getInstance(APIClient.getInstance(app));
            ReplicatedKVStore store = new ReplicatedKVStore(app, remote);
            switch (request.getMethod()) {
                case "GET":
                    Log.e(TAG, "handle: GET: " + key);
                    CompletionObject getObj = store.get(key, 0);
                    KVEntity kv = getObj.kv;

                    if (kv == null) {
                        Log.e(TAG, "handle: kv store does not contain the kv: " + key);
                        try {
                            response.sendError(404);
                            baseRequest.setHandled(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                    byte[] decompressedData = BRCompressor.bz2Extract(kv.getValue());
                    Assert.assertNotNull(decompressedData);
                    try {
                        JSONObject test = new JSONObject(new String(decompressedData)); //just check for validity
                        Log.e(TAG, "handle: " + test.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        try {
                            response.sendError(500);
                            baseRequest.setHandled(true);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        return true;
                    }
                    response.setHeader("ETag", String.valueOf(kv.getVersion()));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS Z", Locale.getDefault());
                    String date = dateFormat.format(kv.getTime());
                    response.setHeader("Last-Modified", date);

                    if (kv.getDeleted() > 0) {
                        try {
                            response.sendError(410, "Gone");
                            baseRequest.setHandled(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                    response.setHeader("Content-Type", "application/json");

                    try {
                        response.setStatus(200);
                        response.getOutputStream().write(decompressedData);
                        baseRequest.setHandled(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                case "PUT":
                    Log.e(TAG, "handle: PUT: " + key);
                    // Read from request
                    byte[] rawData = null;
                    try {
                        InputStream body = request.getInputStream();
                        rawData = IOUtils.toByteArray(body);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (rawData == null) {
                        Log.e(TAG, "handle: missing request body");
                        try {
                            response.sendError(400);
                            baseRequest.setHandled(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }

                    String strVersion = request.getHeader("if-none-match");
                    if (strVersion == null) {
                        Log.e(TAG, "handle: missing If-None-Match header, set to `0` if creating a new key");
                        try {
                            response.sendError(400);
                            baseRequest.setHandled(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                    String ct = request.getHeader("content-type");
                    if (ct == null || !ct.equalsIgnoreCase("application/json")) {
                        Log.e(TAG, "handle: can only set application/json request bodies");
                        try {
                            response.sendError(400);
                            baseRequest.setHandled(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }

                    long version = Long.valueOf(strVersion);

                    byte[] compressedData = BRCompressor.bz2Compress(rawData);
                    assert (compressedData != null);

                    CompletionObject setObj = store.set(new KVEntity(version, 0, key, compressedData, System.currentTimeMillis(), 0));
                    Log.e(TAG, "handle: setObj.err: " + setObj.err);
                    if (setObj.err != null) {
                        int errCode = transformErrorToResponseCode(setObj.err);
                        try {
                            response.sendError(errCode);
                            baseRequest.setHandled(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    response.setHeader("ETag", String.valueOf(setObj.version));
                    dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS Z", Locale.getDefault());
                    date = dateFormat.format(setObj.time);
                    response.setHeader("Last-Modified", date);
                    response.setStatus(204);
                    baseRequest.setHandled(true);
                    return true;
                case "DELETE":
                    Log.e(TAG, "handle: DELETE: " + key);
                    strVersion = request.getHeader("if-none-match");
                    if (strVersion == null) {
                        Log.e(TAG, "handle: missing If-None-Match header");
                        try {
                            response.sendError(400);
                            baseRequest.setHandled(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                    CompletionObject delObj = null;
                    try {
                        delObj = store.delete(key, Long.parseLong(strVersion));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (delObj == null || delObj.err != null) {
                        int err = 500;
                        if (delObj != null)
                            err = transformErrorToResponseCode(delObj.err);
                        try {
                            response.sendError(err);
                            baseRequest.setHandled(true);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        return true;
                    }
                    response.setHeader("ETag", String.valueOf(delObj.version));
                    dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS Z", Locale.getDefault());
                    date = dateFormat.format(delObj.time);
                    response.setHeader("Last-Modified", date);
                    response.setStatus(204);
                    baseRequest.setHandled(true);
                    return true;

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
