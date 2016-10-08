package com.breadwallet;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

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
    public void testGetToken(){
        APIClient apiClient = APIClient.getInstance();
        apiClient.getToken();
    }

    @Test
    public void testMeRequest(){
        APIClient apiClient = APIClient.getInstance();
        String response = apiClient.buyBitcoinMe().toLowerCase().toString();
        String expectedString = "invalid signature".toString();
        Assert.assertNotEquals(response, expectedString);

    }

}
