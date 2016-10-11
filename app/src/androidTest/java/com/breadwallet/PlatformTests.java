package com.breadwallet;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.platform.APIClient;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okhttp3.Request;
import okhttp3.Response;

import static com.platform.APIClient.BREAD_BUY;
import static com.platform.APIClient.bytesToHex;


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
//    public void bundleUpdateTest() {
//        APIClient apiClient = APIClient.getInstance();
//        Request request = new Request.Builder()
//                .get()
//                .url("https://s3.amazonaws.com/breadwallet-assets/bread-buy/7f5bc5c6cc005df224a6ea4567e508491acaffdc2e4769e5262a52f5b785e261.tar").build();
//        Response response = apiClient.sendRequest(request, false);
//        File bundleFile = new File(mActivityRule.getActivity().getFilesDir().getAbsolutePath() + APIClient.bundleFileName);
//        apiClient.downloadBundle(response, bundleFile);
////        String latestVersion = getLatestVersion();
////        Assert.assertNotNull(latestVersion);
////        String currentTarVersion = getCurrentVersion(bundleFile);
////        Log.e(TAG, "bundleUpdateTest: latestVersion: " + latestVersion + ", currentTarVersion: " + currentTarVersion);
////
////        Assert.assertNotNull(currentTarVersion);
////        Assert.assertNotEquals(latestVersion, currentTarVersion);
////        apiClient.updateBundle(mActivityRule.getActivity());
////        currentTarVersion = getCurrentVersion(bundleFile);
////        Log.e(TAG, "bundleUpdateTest:AFTER UPDATE latestVersion: " + latestVersion + ", currentTarVersion: " + currentTarVersion);
////
////        Assert.assertEquals(latestVersion, currentTarVersion);
//    }

    private String getCurrentVersion(File bundleFile){
        byte[] bFile;
        String currentTarVersion = null;
        try {
            bFile = IOUtils.toByteArray(new FileInputStream(bundleFile));
            Log.e(TAG, "bundleUpdateTest: bFile.length: "+ bFile.length);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bFile);
            currentTarVersion = bytesToHex(hash);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return currentTarVersion;
    }

    private String getLatestVersion(){
        APIClient apiClient = APIClient.getInstance();
        String response = null;
        try {
            response = apiClient.sendRequest(new Request.Builder()
                    .get()
                    .url(String.format("%s/assets/bundles/%s/versions", BASE_URL, BREAD_BUY))
                    .build(), false).body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String respBody = "";
        respBody = response;
        Log.e(TAG, "updateBundle: response: " + respBody);
        String latestVersion = null;
        try {
            JSONObject versionsJson = new JSONObject(respBody);
            JSONArray jsonArray = versionsJson.getJSONArray("versions");
            latestVersion = (String) jsonArray.get(jsonArray.length() - 1);
            Log.e(TAG, "updateBundle: latestVersion: " + latestVersion);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return latestVersion;
    }

    @Test
    public void testGetToken() {
        APIClient apiClient = APIClient.getInstance();
        apiClient.getToken();
    }

    @Test
    public void testMeRequest() {
        APIClient apiClient = APIClient.getInstance();
        String response = apiClient.buyBitcoinMe().toLowerCase();
        String expectedString = "invalid signature";
        Assert.assertNotEquals(response, expectedString);
    }

}
