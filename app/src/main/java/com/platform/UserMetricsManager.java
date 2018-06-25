package com.platform;


import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.util.BRConstants;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/*
This class seeks to accomplish a couple things

1) Be reusable, since we need to make the call to the user metrics endpoint in a few different places
2) Construct the authenticated request to the me/metrics endpoint, and passing in the appropriate JSON data
 */
public class UserMetricsManager {

    private static final String ENDPOINT_ME_METRICS = "/me/metrics";
    public static final String METRIC_LAUNCH = "launch";
    private static final String FIELD_METRIC = "metric";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_BUNDLES = "bundles";
    private static byte[] bundleHash;
    private static final String TAG = "UserMetricsManager";
    private static final String BUNDLE_WEB = "brd-web";


    public static void makeUserMetricsRequest(Context context, String metricName) {

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
                = MediaType.parse(BRConstants.HEADER_VALUE_CONTENT_TYPE);

        RequestBody requestBody = RequestBody.create(JSON, payload.toString());

        Request request = new Request.Builder()
                .url(url)
                .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.HEADER_VALUE_CONTENT_TYPE)
                .header(BRConstants.HEADER_ACCEPT, BRConstants.HEADER_VALUE_ACCEPT).post(requestBody).build();

        APIClient.getInstance(context).sendRequest(request, true);

    }

    public static void setBundleHash(byte[] hash) {
        bundleHash = hash;
    }
}
