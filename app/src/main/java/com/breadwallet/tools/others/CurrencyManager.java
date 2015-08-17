package com.breadwallet.tools.others;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.CurrencyListAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/22/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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

public class CurrencyManager {
    private static MainActivity app = MainActivity.app;
    public static final String TAG = "CurrencyManager";
    private static Timer timer;
    private static TimerTask timerTask;
    private static final Handler handler = new Handler();
    private static CurrencyListAdapter currencyListAdapter = new CurrencyListAdapter(MainActivity.app, R.layout.currency_list_item);

    public static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static List<String> getCurrencies() {
        List<String> list = null;
        if (isNetworkAvailable()) {
            list = new ArrayList<>();
            JSONArray arr;
            arr = JsonParser.getJSonArray("https://bitpay.com/rates");
            int length = arr.length();
            for (int i = 1; i < length; i++) {
                String tmp = null;
                try {
                    JSONObject tmpObj = (JSONObject) arr.get(i);
                    tmp = tmpObj.getString("code") + " - " + tmpObj.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                list.add(tmp);
            }
        } else {
            if (list == null) list = new ArrayList<>();
        }
        if (list.size() > 0)
            list.add("TES - testing text extra long text mega long text oh my god that's a long text");
        return list;
    }

    public static class GetCurrenciesTask extends AsyncTask {
        List<String> tmp;

        @Override
        protected Object doInBackground(Object[] params) {
            tmp = getCurrencies();
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if (tmp.size() > 0) {
                currencyListAdapter.clear();
                currencyListAdapter.addAll(tmp);
                currencyListAdapter.notifyDataSetChanged();
                Log.e(TAG, "Adapter changed >> Filled: adapter count: " + currencyListAdapter.getCount());
            } else {
                Log.e(TAG, "Adapter Not Changed, data is empty");
            }

        }
    }

    public static CurrencyListAdapter getCurrencyAddapterIfReady() {
        new GetCurrenciesTask().execute();
        return currencyListAdapter;
    }

    public static void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        new GetCurrenciesTask().execute();
                    }
                });
            }
        };
    }

    public static void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 0, 60000); //
    }

    public static void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
