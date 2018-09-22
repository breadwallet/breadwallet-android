/**
 * BreadWallet
 * <p/>
 * Created by Jade Byfield on <jade@brd.com> 6/7/18.
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
package com.breadwallet.app.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.platform.APIClient;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * This class seeks to accomplish a couple things
 * <p>
 * 1) Be reusable, since we need to make the call to the user metrics endpoint in a few different places
 * 2) Construct the authenticated request to the me/metrics endpoint, and passing in the appropriate JSON data
 */
public final class UserMetricsUtil {
    private static final String TAG = UserMetricsUtil.class.getSimpleName();

    private static final String ENDPOINT_ME_METRICS = "/me/metrics";
    private static final String URL = BRConstants.HTTPS_PROTOCOL + BreadApp.HOST + ENDPOINT_ME_METRICS;

    private static final String FIELD_METRIC = "metric";
    private static final String METRIC_LAUNCH = "launch";
    private static final String FIELD_DATA = "data";

    // Data field names and some values
    private static final String FIELD_BUNDLES = "bundles";
    private static final String BUNDLE_WEB = "brd-web";
    private static final String FIELD_OS_VERSION = "os_version";
    private static final String FIELD_USER_AGENT = "user_agent";
    private static final String SYSTEM_PROPERTY_USER_AGENT = "http.agent";
    private static final String FIELD_DEVICE_TYPE = "device_type";
    private static final String DEVICE_TYPE = Build.MANUFACTURER + " " + Build.MODEL;

    private static byte[] bundleHash;

    private UserMetricsUtil() {
    }

    public static void sendUserMetricsRequest() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                UserMetricsUtil.makeUserMetricsRequest(BreadApp.getBreadContext());
            }
        });
    }

    private static void makeUserMetricsRequest(Context context) {
        try {
            JSONObject bundles = new JSONObject();
            bundles.put(BUNDLE_WEB, bundleHash);

            JSONObject data = new JSONObject();
            data.put(FIELD_BUNDLES, bundles);
            data.put(FIELD_OS_VERSION, Build.VERSION.RELEASE);
            data.put(FIELD_USER_AGENT,  System.getProperty(SYSTEM_PROPERTY_USER_AGENT));
            data.put(FIELD_DEVICE_TYPE, DEVICE_TYPE);

            JSONObject payload = new JSONObject();
            payload.put(FIELD_METRIC, METRIC_LAUNCH);
            payload.put(FIELD_DATA, data);

            final MediaType JSON = MediaType.parse(BRConstants.CONTENT_TYPE_JSON);
            RequestBody requestBody = RequestBody.create(JSON, payload.toString());
            Request request = new Request.Builder()
                    .url(URL)
                    .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON)
                    .header(BRConstants.HEADER_ACCEPT, BRConstants.HEADER_VALUE_ACCEPT).post(requestBody).build();

            APIClient.getInstance(context).sendRequest(request, true);

        } catch (JSONException e) {
            Log.e(TAG, "Error constructing JSON payload for user metrics request.");
        }
    }

    public static void setBundleHash(byte[] hash) {
        bundleHash = hash;
    }
}
