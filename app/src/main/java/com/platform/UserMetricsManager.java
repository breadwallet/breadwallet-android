package com.platform;


import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * This class seeks to accomplish a couple things
 *
 * 1) Be reusable, since we need to make the call to the user metrics endpoint in a few different places
 * 2) Construct the authenticated request to the me/metrics endpoint, and passing in the appropriate JSON data
 */
public final class UserMetricsManager {
    private static final String TAG = UserMetricsManager.class.getSimpleName();

    private static final String ENDPOINT_ME_METRICS = "/me/metrics";
    public static final String METRIC_LAUNCH = "launch";
    private static final String FIELD_METRIC = "metric";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_BUNDLES = "bundles";
    private static byte[] bundleHash;
    private static final String BUNDLE_WEB = "brd-web";

    private UserMetricsManager() {}

    public static void sendUserMetricsRequest() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {

                //First, check if we have a wallet ID already stored in SharedPrefs
                String walletId = BRSharedPrefs.getWalletRewardId(BreadApp.getBreadContext());

                // Only make this request if we have a valid wallet ID
                if (walletId != null && !walletId.isEmpty()) {
                    UserMetricsManager.makeUserMetricsRequest(BreadApp.getBreadContext(), UserMetricsManager.METRIC_LAUNCH);
                }
            }
        });
    }

    private static void makeUserMetricsRequest(Context context, String metricName) {

        String url = BRConstants.HTTPS_PROTOCOL + BreadApp.HOST + ENDPOINT_ME_METRICS;

        JSONObject payload = new JSONObject();


        try {
            JSONObject data = new JSONObject();
            JSONObject bundles = new JSONObject();
            bundles.put(BUNDLE_WEB, bundleHash);
            data.put(FIELD_BUNDLES, bundles);
            payload.put(FIELD_METRIC, metricName);
            payload.put(FIELD_DATA, data);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Error constructing json payload for request!");
        }

        final MediaType JSON
                = MediaType.parse(BRConstants.CONTENT_TYPE_JSON);

        RequestBody requestBody = RequestBody.create(JSON, payload.toString());

        Request request = new Request.Builder()
                .url(url)
                .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON)
                .header(BRConstants.HEADER_ACCEPT, BRConstants.HEADER_VALUE_ACCEPT).post(requestBody).build();

        APIClient.getInstance(context).sendRequest(request, true);

    }

    public static void setBundleHash(byte[] hash) {
        bundleHash = hash;
    }
}
