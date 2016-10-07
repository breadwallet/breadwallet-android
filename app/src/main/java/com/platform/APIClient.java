package com.platform;

import android.app.Activity;

import android.content.Context;
import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRWalletManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.R.attr.name;
import static com.breadwallet.R.string.request;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/29/16.
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
public class APIClient {

    public static final String TAG = APIClient.class.getName();

    // proto is the transport protocol to use for talking to the API (either http or https)
    private static final String PROTO = "https";
    // host is the server(s) on which the API is hosted
    private static final String HOST = "api.breadwallet.com";
    // convenience getter for the API endpoint
    private static final String BASE_URL = PROTO + "://" + HOST;
    //feePerKb url
    private static final String FEE_PER_KB_URL = "/v1/fee-per-kb";
    //token
    private static final String TOKEN = "/token";
    //me
    private static final String ME = "/me";
    //singleton instance
    private static APIClient ourInstance;

    private static final String GET = "GET";
    private static final String POST = "POST";
    private Activity ctx;

    public static synchronized APIClient getInstance(Activity context) {

        if (ourInstance == null) ourInstance = new APIClient(context);
        return ourInstance;
    }

    private APIClient(Activity context) {
        ctx = context;
    }

    public static synchronized APIClient getInstance() {
        if (ourInstance == null) ourInstance = new APIClient();
        return ourInstance;
    }

    private APIClient() {
    }

    //returns the fee per kb or 0 if something went wrong
    public long feePerKb() {

        try {
            String strUtl = BASE_URL + FEE_PER_KB_URL;
            Request request = new Request.Builder().url(strUtl).get().build();
            String response = sendRequest(request, false);

            JSONObject object = new JSONObject(response);
            return (long) object.getInt("fee_per_kb");
        } catch (JSONException e) {
            e.printStackTrace();

        }
        return 0;
    }

    public String buyBitcoinMe() {
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return null;
        String strUtl = BASE_URL + ME;
        Log.e(TAG, "buyBitcoinMe: strUrl: " + strUtl);
        Request request = new Request.Builder()
                .url(strUtl)
                .get()
                .build();
        String response = sendRequest(request, true);
        if (response.equalsIgnoreCase("401")) {
            getToken();
            response = sendRequest(request, true);
        }

        Log.e(TAG, "buyBitcoinMe: response: " + response);
//            if (response.isEmpty()) return null;
//            JSONObject obj = new JSONObject(response);
//            String token = obj.getString("token");
//            KeyStoreManager.putToken(token.getBytes(), ctx);

        return null;
    }

    public String getToken() {
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return null;
        try {
            String strUtl = BASE_URL + TOKEN;
            Log.e(TAG, "getToken: strUrl: " + strUtl);

            JSONObject requestMessageJSON = new JSONObject();
            String base58PubKey = BRWalletManager.getAuthPublicKeyForAPI(KeyStoreManager.getAuthKey(ctx));
            Log.e(TAG, "getToken: base58PubKey: " + base58PubKey);
            requestMessageJSON.put("pubKey", base58PubKey);
            requestMessageJSON.put("deviceID", SharedPreferencesManager.getDeviceId(ctx));
            Log.e(TAG, "getToken: message: " + requestMessageJSON.toString());

            final MediaType JSON
                    = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, requestMessageJSON.toString());
            Request request = new Request.Builder()
                    .url(strUtl)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(requestBody).build();
            String response = sendRequest(request, false);
            Log.e(TAG, "getToken: response: " + response);
            if (response.isEmpty()) return null;
            JSONObject obj = new JSONObject(response);
            String token = obj.getString("token");
            KeyStoreManager.putToken(token.getBytes(), ctx);

            return token;
        } catch (JSONException e) {
            e.printStackTrace();

        }
        return null;

    }

    private String createRequest(String reqMethod, String base58Body, String contentType, String dateHeader, String url) {
        return (reqMethod == null ? "" : reqMethod) + "\n" +
                (base58Body == null ? "" : base58Body) + "\n" +
                (contentType == null ? "" : contentType) + "\n" +
                (dateHeader == null ? "" : dateHeader) + "\n" +
                (url == null ? "" : url) + "\n";
    }

    public String signRequest(String request) {
        return BRWalletManager.signString(request, KeyStoreManager.getAuthKey(ctx));
    }

    public String sendRequest(Request request, boolean needsAuth) {
        String result = "";
        int responseCode = 0;

        if (needsAuth) {
            Request.Builder modifiedRequest = request.newBuilder();
            String base58Body = "";
            if (request.body() != null) {
                base58Body = BRWalletManager.base58ofSha256(request.body().toString());
            }
            SimpleDateFormat sdf =
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String httpDate = sdf.format(new Date(System.currentTimeMillis()));

            request = modifiedRequest.header("Date", httpDate.substring(0, httpDate.length() - 6)).build();
            String requestString = createRequest(request.method(), base58Body, request.header("Content-Type"), request.header("Date"), "/me");

            Log.e(TAG, "sendRequest: requestString: " + requestString);
            String signedRequest = signRequest(requestString);
            String token = new String(KeyStoreManager.getToken(ctx));
            if (token.isEmpty()) token = getToken();
            if (token == null || token.isEmpty()) {
                Log.e(TAG, "sendRequest: failed to retrieve token");
                return null;
            }
            String authValue = "bread " + token + ":" + signedRequest;
            Log.e(TAG, "sendRequest: authValue: " + authValue);
            modifiedRequest = request.newBuilder();
            request = modifiedRequest.header("Authorization", authValue).build();

        }
        try {
            OkHttpClient client = new OkHttpClient();
            Log.e(TAG, "sendRequest: dateHeader: " + request.header("Date"));
            Log.e(TAG, "sendRequest: Authorization: " + request.header("Authorization"));
            Response response = client.newCall(request).execute();
            System.out.println("sendRequest: getResponseCode : " + response.code());
            System.out.println("sendRequest: getResponseMessage: " + response.message());
            responseCode = response.code();
            result = response.body().string();
            Log.e(TAG, "sendRequest: result: " + result);
            Log.e(TAG, "sendRequest: server date: " + response.header("Date"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.isEmpty() ? String.valueOf(responseCode) : result;
    }

    public void updateBundle(Context context, String name) {
        if (name == null) {
            Log.e(TAG, "updateBundle: name is null");
            return;
        }
        String bundles = "bundles";

        String bundlesFileName = String.format("/%s", bundles);
        String bundleFileName = String.format("/%s/%s.tar", bundles, name);
        String bundleFileNameExtracted = String.format("/%s/%s-extracted", bundles, name);

//        Log.e(TAG, "updateBundle: bundlesFileName: " + bundlesFileName);
//        Log.e(TAG, "updateBundle: bundleFileName: " + bundleFileName);
//        Log.e(TAG, "updateBundle: bundleFileNameExtracted: " + bundleFileNameExtracted);

        File bundlesFolder = new File(context.getFilesDir().getAbsolutePath() + bundlesFileName);
        File bundleFile = new File(context.getFilesDir().getAbsolutePath() + bundleFileName);
        File bundleExtractedFolder = new File(context.getFilesDir().getAbsolutePath() + bundleFileNameExtracted);
//        Log.e(TAG, "updateBundle: bundlesFolder: " + bundlesFolder.toString());
//        Log.e(TAG, "updateBundle: bundleFile: " + bundleFile.toString());
//        Log.e(TAG, "updateBundle: bundleExtractedFolder: " + bundleExtractedFolder.toString());
//
//        Log.e(TAG, "updateBundle: bundlesFolder.exists: " + bundlesFolder.exists());
//        Log.e(TAG, "updateBundle: bundleFile.exists: " + bundleFile.exists());
//        Log.e(TAG, "updateBundle: bundleExtractedFolder.exists: " + bundleExtractedFolder.exists());

        FileOutputStream bundlesOutStream = null;
        FileOutputStream extractedOutStream = null;

        try {
            bundlesOutStream = new FileOutputStream(bundlesFolder);
            extractedOutStream = new FileOutputStream(bundleExtractedFolder);
//            outputStream.write(string.getBytes());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bundlesOutStream.close();
                extractedOutStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bundleFile.exists()) {
            Log.e(TAG, "updateBundle: exists, fetching diff for most recent version");

        }

    }

}
