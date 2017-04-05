package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRConstants;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class CurrencyFetchManager {
    private static final String TAG = CurrencyFetchManager.class.getName();

    private static CurrencyFetchManager instance;
    private Timer timer;

    private TimerTask timerTask;
    private Context context;

    private Handler handler;
    //    public static boolean separatorNeedsToBeShown = false;
//    private final CurrencyListAdapter currencyListAdapter;

    private CurrencyFetchManager(Context ctx) {
        this.context = ctx;
//        currencyListAdapter = new CurrencyListAdapter(ctx);
        handler = new Handler();
    }

    public static CurrencyFetchManager getInstance(Context context) {

        if (instance == null) {
            instance = new CurrencyFetchManager(context);
        }
        return instance;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context) {
        Set<CurrencyEntity> set = new LinkedHashSet<>();
//        if (((BreadWalletApp) context.getApplication()).hasInternetAccess()) {
        try {
            JSONArray arr = getJSonArray(context);
            updateFeePerKb(context);
//                Log.e(TAG, "JSONArray arr.length(): " + arr.length());
            if (arr != null) {
                int length = arr.length();
                for (int i = 1; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");
                        tmp.rate = (float) tmpObj.getDouble("rate");
                        String selectedISO = SharedPreferencesManager.getIso(context);
//                        Log.e(TAG,"selectedISO: " + selectedISO);
                        if (tmp.code.equalsIgnoreCase(selectedISO)) {
//                            Log.e(TAG, "theIso : " + theIso);
//                                Log.e(TAG, "Putting the shit in the shared preffs");
                            SharedPreferencesManager.putIso(context, tmp.code);
                            SharedPreferencesManager.putCurrencyListPosition(context, i - 1);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    set.add(tmp);
                }
            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies, response string: " + arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        }
        List tempList = new ArrayList<>(set);
        Collections.reverse(tempList);
        return new LinkedHashSet<>(set);
    }

    public class GetCurrenciesTask extends AsyncTask {
        Set<CurrencyEntity> tmp;
        private Context context;

        public GetCurrenciesTask(Context ctx) {
            this.context = ctx;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            tmp = getCurrencies((Activity) context);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (tmp.size() > 0) {
                CurrencyDataSource.getInstance(context).putCurrencies(tmp);
            }
        }
    }

//    public CurrencyListAdapter getCurrencyAdapterIfReady() {
////        new GetCurrenciesTask().execute();
//        return currencyListAdapter;
//    }

    private void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        new GetCurrenciesTask(context).execute();
                    }
                });
            }
        };
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 0, 60000); //
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    public static JSONArray getJSonArray(Activity activity) {
        String jsonString = callURL(activity, "https://api.breadwallet.com/rates");
        JSONArray jsonArray = null;
        try {
            JSONObject obj = new JSONObject(jsonString);
            jsonArray = obj.getJSONArray("body");

        } catch (JSONException ignored) {
        }
        return jsonArray == null ? getBackUpJSonArray(activity) : jsonArray;
    }

    public static JSONArray getBackUpJSonArray(Activity activity) {
        String jsonString = callURL(activity, "https://bitpay.com/rates");

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
        String jsonString = callURL(activity, "https://api.breadwallet.com/fee-per-kb");
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
                BRWalletManager.getInstance().setFeePerKb(fee);
            }
        } catch (JSONException e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }
    }

    private static String callURL(Context app, String myURL) {
//        System.out.println("Requested URL_EA:" + myURL);
        StringBuilder sb = new StringBuilder();
        HttpURLConnection urlConn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(myURL);
            urlConn = (HttpURLConnection) url.openConnection();
            int versionNumber = 0;
            if (app != null) {
                try {
                    PackageInfo pInfo = null;
                    pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                    versionNumber = pInfo.versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            int stringId = 0;
            String appName = "";
            if (app != null) {
                stringId = app.getApplicationInfo().labelRes;
                appName = app.getString(stringId);
            }
            String message = String.format(Locale.getDefault(), "%s/%d/%s", appName.isEmpty() ? "breadwallet" : appName, versionNumber, System.getProperty("http.agent"));
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
