package com.breadwallet.tools.util;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Locale;

import static android.R.attr.data;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
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

public class JsonParser {
    public static final String TAG = JsonParser.class.getName();

    public static JSONArray getJSonArray(Activity activity) {
        String jsonString = callURL("https://api.breadwallet.com/rates");
        JSONArray jsonArray = null;
        try {
            JSONObject obj = new JSONObject(jsonString);
            jsonArray = obj.getJSONArray("body");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray == null ? getBackUpJSonArray(activity) : jsonArray;
    }

    public static JSONArray getBackUpJSonArray(Activity activity) {
        String jsonString = callURL("https://bitpay.com/rates");

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonString);

            jsonArray = obj.getJSONArray("data");
            JSONObject headers = obj.getJSONObject("headers");
            String secureDate = headers.getString("Date");
            @SuppressWarnings("deprecation") long date = Date.parse(secureDate) / 1000;

            SharedPreferencesManager.putSecureTime(activity, date);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    public static void updateFeePerKb(Activity activity) {
        String jsonString = callURL("https://api.breadwallet.com/fee-per-kb");
        if (jsonString == null || jsonString.isEmpty()) {
            Log.e(TAG, "updateFeePerKb: failed to update fee, response string: " + jsonString);
            return;
        }
        long fee;
        try {
            JSONObject obj = new JSONObject(jsonString);
            fee = obj.getLong("fee_per_kb");
            if (fee != 0 && fee < BRConstants.MAX_FEE_PER_KB) {

                SharedPreferencesManager.putFeePerKb(activity, fee);
                BRWalletManager.getInstance(activity).setFeePerKb(fee);
            }
        } catch (JSONException e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }
    }

    private static String callURL(String myURL) {
//        System.out.println("Requested URL_EA:" + myURL);
        StringBuilder sb = new StringBuilder();
        HttpURLConnection urlConn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(myURL);
            urlConn = (HttpURLConnection) url.openConnection();
            int versionNumber = 0;
            MainActivity app = MainActivity.app;
            if (app != null) {
                try {
                    PackageInfo pInfo = null;
                    pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                    versionNumber = pInfo.versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
//            int stringId = 0;
            String appName = "Bread";
//            if (app != null) {
//                stringId = app.getApplicationInfo().labelRes;
//                appName = app.getString(stringId);
//            }
            String message = String.format(Locale.getDefault(), "%s/%d/%s", appName, versionNumber, System.getProperty("http.agent"));
            urlConn.setRequestProperty("User-agent", message);
            urlConn.setReadTimeout(60 * 1000);


            String strDate = urlConn.getHeaderField("date");

            if (strDate == null || app == null) {
                Log.e(TAG, "callURL: strDate == null!!!");
            } else {
                @SuppressWarnings("deprecation") long date = Date.parse(strDate) / 1000;
                SharedPreferencesManager.putSecureTime(app, date);
                Assert.assertTrue(date != 0);
            }

            if (urlConn.getInputStream() != null) {
                in = new InputStreamReader(urlConn.getInputStream(),
                        Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(in);

                int cp;
                while ((cp = bufferedReader.read()) != -1) {
                    sb.append((char) cp);
                }
                bufferedReader.close();
            }
            assert in != null;
            in.close();
        } catch (Exception e) {
            return null;
        }

        return sb.toString();
    }

}