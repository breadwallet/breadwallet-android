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
import com.breadwallet.BuildConfig;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.platform.APIClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

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

    /* Url fields */
    private static final String ENDPOINT_ME_METRICS = "/me/metrics";
    private static final String URL = BRConstants.HTTPS_PROTOCOL + BreadApp.HOST + ENDPOINT_ME_METRICS;

    /* Metric field key */
    private static final String FIELD_METRIC = "metric";
    /* Data field key */
    private static final String FIELD_DATA = "data";

    /* Metric field values */
    private static final String METRIC_LAUNCH = "launch";
    private static final String METRIC_PIGEON_TRANSACTION = "pigeon-transaction";
    private static final String METRIC_SEG_WIT = "segWit";

    /* Various field keys and values for data field of launch metric */
    private static final String FIELD_BUNDLES = "bundles";
    private static final String FIELD_OS_VERSION = "os_version";
    private static final String FIELD_USER_AGENT = "user_agent";
    private static final String SYSTEM_PROPERTY_USER_AGENT = "http.agent";
    private static final String FIELD_DEVICE_TYPE = "device_type";
    private static final String DEVICE_TYPE = Build.MANUFACTURER + " " + Build.MODEL;
    private static final String FIELD_APPLICATION_ID = "application_id";
    private static final String FIELD_ADVERTISING_ID = "advertising_id";

    /* Various field keys and values for data field of pigeon transaction metric */
    private static final String FIELD_STATUS = "status";  // Version 2
    private static final String FIELD_IDENTIFIER = "identifier"; // Version 2
    private static final String FIELD_SERVICE = "service"; // Version 2
    private static final String FIELD_TRANSACTION_HASH = "transactionHash";
    private static final String FIELD_FROM_CURRENCY = "fromCurrency";
    private static final String FIELD_FROM_AMOUNT = "fromAmount";
    private static final String FIELD_FROM_ADDRESS = "fromAddress";
    private static final String FIELD_TO_CURRENCY = "toCurrency";
    private static final String FIELD_TO_AMOUNT = "toAmount";
    private static final String FIELD_TO_ADDRESS = "toAddress";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_ERROR = "error";  // Version 2

    /* Various field keys and values for data field of segWit metric */
    private static final String FIELD_EVENT_TYPE = "eventType";
    public static final String ENABLE_SEG_WIT = "enableSegWit";
    public static final String VIEW_LEGACY_ADDRESS = "viewLegacyAddress";

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
            for (String bundleName : APIClient.BUNDLE_NAMES) {
                bundles.put(bundleName, BRSharedPrefs.getBundleHash(context, bundleName));
            }

            JSONObject data = new JSONObject();
            data.put(FIELD_BUNDLES, bundles);
            data.put(FIELD_OS_VERSION, Build.VERSION.RELEASE);
            data.put(FIELD_USER_AGENT, System.getProperty(SYSTEM_PROPERTY_USER_AGENT));
            data.put(FIELD_DEVICE_TYPE, DEVICE_TYPE);
            data.put(FIELD_APPLICATION_ID, BuildConfig.APPLICATION_ID);
            data.put(FIELD_ADVERTISING_ID, AdvertisingIdClient.getAdvertisingIdInfo(context).getId());

            JSONObject payload = new JSONObject();
            payload.put(FIELD_METRIC, METRIC_LAUNCH);
            payload.put(FIELD_DATA, data);

            sendMetricsRequestWithPayload(context, payload);

        } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException | IOException | JSONException e) {
            Log.e(TAG, "Error constructing JSON payload for user metrics request.", e);
        }
    }

    public static void logCallRequestResponse(Context context, int status, String identifier, String service, String transactionHash,
                                              String fromCurrency, String fromAmount, String fromAddress,
                                              String toCurrency, String toAmount, String toAddress, long timestamp,
                                              Integer errorCode) {
        try {
            JSONObject data = new JSONObject();

            data.put(FIELD_STATUS, status);

            // Only include a field if its value is not null
            if (!Utils.isNullOrEmpty(identifier)) {
                data.put(FIELD_IDENTIFIER, identifier);
            }

            if (!Utils.isNullOrEmpty(service)) {
                data.put(FIELD_SERVICE, service);
            }

            if (!Utils.isNullOrEmpty(transactionHash)) {
                data.put(FIELD_TRANSACTION_HASH, transactionHash);
            }

            if (!Utils.isNullOrEmpty(fromCurrency)) {
                data.put(FIELD_FROM_CURRENCY, fromCurrency);
            }

            if (!Utils.isNullOrEmpty(fromAmount)) {
                data.put(FIELD_FROM_AMOUNT, fromAmount);
            }

            if (!Utils.isNullOrEmpty(fromAddress)) {
                data.put(FIELD_FROM_ADDRESS, fromAddress);
            }

            if (!Utils.isNullOrEmpty(toCurrency)) {
                data.put(FIELD_TO_CURRENCY, toCurrency);

                if (!Utils.isNullOrEmpty(toAmount)) {
                    data.put(FIELD_TO_AMOUNT, toAmount);
                }

                if (!Utils.isNullOrEmpty(toAddress)) {
                    data.put(FIELD_TO_ADDRESS, toAddress);
                }
            }

            data.put(FIELD_TIMESTAMP, timestamp);

            if (errorCode != null) {
                data.put(FIELD_ERROR, errorCode.intValue());
            }

            JSONObject payload = new JSONObject();
            payload.put(FIELD_METRIC, METRIC_PIGEON_TRANSACTION);
            payload.put(FIELD_DATA, data);

            sendMetricsRequestWithPayload(context, payload);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating call request payload! ", e);
        }
    }

    public static void logSegwitEvent(Context context, String eventType) {
        try {
            JSONObject data = new JSONObject();

            data.put(FIELD_EVENT_TYPE, eventType);
            data.put(FIELD_TIMESTAMP, System.currentTimeMillis());
            JSONObject payload = new JSONObject();
            payload.put(FIELD_METRIC, METRIC_SEG_WIT);
            payload.put(FIELD_DATA, data);
            sendMetricsRequestWithPayload(context, payload);
        } catch (JSONException e) {
            Log.e(TAG, "Error constructing JSON payload for user metrics request.", e);
        }
    }

    private static void sendMetricsRequestWithPayload(final Context context, final JSONObject payload) {
        final MediaType JSON = MediaType.parse(BRConstants.CONTENT_TYPE_JSON);
        RequestBody requestBody = RequestBody.create(JSON, payload.toString());
        Request request = new Request.Builder()
                .url(URL)
                .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON)
                .header(BRConstants.HEADER_ACCEPT, BRConstants.HEADER_VALUE_ACCEPT).post(requestBody).build();

        APIClient.getInstance(context).sendRequest(request, true);
    }

}
