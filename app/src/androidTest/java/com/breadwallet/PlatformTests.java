package com.breadwallet;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.APIClient;
import com.platform.HTTPRequestEntity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/30/16.
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

@RunWith(AndroidJUnit4.class)
public class PlatformTests {
    public static final String TAG = PlatformTests.class.getName();
    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

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

    private static final String GET = "GET";
    private static final String POST = "POST";

    //loading the native library
    static {
        System.loadLibrary("core");
    }

    @Test
    public void testFeePerKbFetch() {
        long fee = APIClient.getInstance().feePerKb();
        System.out.println("testFeePerKbFetch: fee: " + fee);
        Assert.assertNotSame(fee, (long) 0);

    }



//    @Test
//    public void bundleUpdateTest(){
//        APIClient apiClient = APIClient.getInstance();
//        apiClient.updateBundle(mActivityRule.getActivity(), "bread-buy");
//    }

    @Test
    public void testMeRequest(){
        APIClient apiClient = APIClient.getInstance();
        apiClient.buyBitcoinMe();
    }

    @Test
    public void testGetToken(){
        APIClient apiClient = APIClient.getInstance();
        apiClient.getToken();
    }

//    @Test
//    public void testMeRequest() {
//        String strUtl = BASE_URL + ME;
//        System.out.println("getToken: strUrl: " + strUtl);
//        HTTPRequestEntity request = new HTTPRequestEntity(strUtl, HTTPRequestEntity.GET, true, true);
//        Map<String, String> properties = new HashMap<>();
//        request.setHeaders(properties);
//        SecureRandom sr = new SecureRandom();
//        byte[] keyBytes = sr.generateSeed(16);
//        if (keyBytes.length == 0) throw new NullPointerException("failed to create the seed");
//        byte[] authKey = BRWalletManager.getAuthPrivKeyForAPI(keyBytes);
//        String token = getToken(authKey);
//        String response = sendRequest(request, true, token, authKey);
//
//        System.out.println("getToken: response: " + response);
//        if (response.isEmpty()) {
//            System.out.println("response.isEmpty()");
//        }
//    }
//
//    public String getToken(byte[] authKey) {
//        try {
//            String strUtl = BASE_URL + TOKEN;
//            System.out.println("getToken: strUrl: " + strUtl);
//            HTTPRequestEntity request = new HTTPRequestEntity(strUtl, HTTPRequestEntity.POST, true, true);
//            Map<String, String> properties = new HashMap<>();
//            properties.put("Content-Type", "application/json");
//            properties.put("Accept", "application/json");
//            request.setHeaders(properties);
//            JSONObject requestMessageJSON = new JSONObject();
//
//            String base58PubKey = BRWalletManager.getAuthPublicKeyForAPI(authKey);
//            System.out.println("getToken: base58PubKey: " + base58PubKey);
//            requestMessageJSON.put("pubKey", base58PubKey);
//            String uuid = UUID.randomUUID().toString();
//            requestMessageJSON.put("deviceID", uuid);
//            request.setMessage(requestMessageJSON.toString());
//            System.out.println("getToken: message: " + requestMessageJSON.toString());
//            String response = sendRequest(request, false, null, null);
//            System.out.println("getToken: response: " + response);
//            if (response.isEmpty()) return null;
//            JSONObject obj = new JSONObject(response);
//            return obj.getString("token");
//        } catch (JSONException e) {
//            e.printStackTrace();
//
//        }
//        return null;
//
//    }
//
//    public String sendRequest(HTTPRequestEntity req, boolean needsAuth, String token, byte[] authKey) {
//        StringBuilder builder = new StringBuilder();
//        String result = "";
//        int responseCode = 0;
//        BufferedReader bufferedReader = null;
//        HttpURLConnection conn = null;
//        if (needsAuth) {
//            if (token == null) {
//                System.out.println("TOKEN IS NULL");
//                return null;
//            }
//            String base58Body = "";
//            if (req.getMessage() != null) {
//                base58Body = BRWalletManager.base58ofSha256(req.getMessage());
//            }
//            SimpleDateFormat sdf =
//                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
//            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
//            String httpDate = sdf.format(new Date(System.currentTimeMillis()));
//
//            req.getHeaders().put("Date", httpDate.substring(0, httpDate.length() - 6));
//            String requestString = createRequest(req.getMethod(), base58Body,
//                    req.getHeaders().get("Content-Type"), req.getHeaders().get("Date"), "/me");
//
//            System.out.println("sendRequest: requestString: " + requestString);
//            String signedRequest = signRequest(requestString, authKey);
//            String authValue = "bread " + token + ":" + signedRequest;
//            req.getHeaders().put("Authorization", authValue);
//            System.out.println("sendRequest: authValue: " + authValue);
//        }
//        try {
//            URL url = new URL(req.getUrl());
//
//            conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod(req.getMethod());
//            conn.setDoOutput(req.isDoOutput());
//            conn.setDoInput(req.isDoInput());
//            conn.setConnectTimeout(5000);
//            conn.setReadTimeout(10000);
//            if (req.getHeaders() != null) {
//                Set set = req.getHeaders().entrySet();
//                Iterator iterator = set.iterator();
//                while (iterator.hasNext()) {
//                    Map.Entry entries = (Map.Entry) iterator.next();
//                    conn.setRequestProperty(entries.getKey().toString(), entries.getValue().toString());
//                    System.out.println("sendRequest: SetHeader: " + entries.getKey().toString() + ":" + entries.getValue().toString());
//                }
//            }
//            if (req.getMessage() != null) {
//                OutputStream os = conn.getOutputStream();
//                os.write(req.getMessage().getBytes());
//                os.flush();
//            }
//            System.out.println("sendRequest: getResponseMessage: " + conn.getResponseMessage());
//            System.out.println("sendRequest: getResponseCode: " + conn.getResponseCode());
//            responseCode = conn.getResponseCode();
//            String aux = null;
//            bufferedReader = new BufferedReader(new InputStreamReader(
//                    (conn.getInputStream())));
//
//            while ((aux = bufferedReader.readLine()) != null) {
//                builder.append(aux);
//            }
//            System.out.println(conn.getURL());
//            result = builder.toString();
//        } catch (IOException e) {
//            e.printStackTrace();
//            BufferedReader bufferedReaderErr = new BufferedReader(new InputStreamReader(
//                    (conn.getErrorStream())));
//            StringBuilder err = new StringBuilder();
//            String temp;
//            try {
//                while ((temp = bufferedReaderErr.readLine()) != null) {
//                    err.append(temp);
//                }
//                System.out.println("sendRequest: ERROR STREAM: " + err.toString());
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//        } finally {
//            if (conn != null)
//                conn.disconnect();
//        }
//
//        return result.isEmpty() ? String.valueOf(responseCode) : result;
//    }
//
//    private String createRequest(String reqMethod, String base58Body, String contentType, String dateHeader, String url) {
//        return (reqMethod == null ? "" : reqMethod) + "\n" +
//                (base58Body == null ? "" : base58Body) + "\n" +
//                (contentType == null ? "" : contentType) + "\n" +
//                (dateHeader == null ? "" : dateHeader) + "\n" +
//                (url == null ? "" : url) + "\n";
//    }
//
//    public String signRequest(String request, byte[] authKey) {
//        return BRWalletManager.signString(request, authKey);
//    }


}
