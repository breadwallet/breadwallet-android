package com.breadwallet.tools.manager;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.adapter.CurrencyListAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Observable;
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

public class CurrencyManager extends Observable {
    private static final String TAG = CurrencyManager.class.getName();

    private static CurrencyManager instance;
    private Timer timer;
    private long BALANCE = 0;
    private TimerTask timerTask;

    private Handler handler;
    //    public static boolean separatorNeedsToBeShown = false;
    private final CurrencyListAdapter currencyListAdapter;
    private static Activity ctx;

    private CurrencyManager() {
        currencyListAdapter = new CurrencyListAdapter(ctx);
        handler = new Handler();
    }

    public static CurrencyManager getInstance(Activity context) {
        ctx = context;

        if (instance == null) {
            instance = new CurrencyManager();
        }
        return instance;
    }


    public void setBalance(long balance) {
        BALANCE = balance;
        setChanged();
        notifyObservers();
    }

    public long getBALANCE() {
        return BALANCE;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context) {
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        if (((BreadWalletApp) ctx.getApplication()).hasInternetAccess()) {
            try {
                JSONArray arr = JsonParser.getJSonArray(context);
                JsonParser.updateFeePerKb(context);
//                Log.e(TAG, "JSONArray arr.length(): " + arr.length());
                if (arr != null) {
                    int length = arr.length();
                    for (int i = 1; i < length; i++) {
                        CurrencyEntity tmp = new CurrencyEntity();
                        try {
                            JSONObject tmpObj = (JSONObject) arr.get(i);
                            tmp.name = tmpObj.getString("name");
                            tmp.code = tmpObj.getString("code");
                            tmp.codeAndName = tmp.code + " - " + tmp.name;
                            tmp.rate = (float) tmpObj.getDouble("rate");
                            String selectedISO = SharedPreferencesManager.getIso(context);
//                        Log.e(TAG,"selectedISO: " + selectedISO);
                            if (tmp.code.equalsIgnoreCase(selectedISO)) {
//                            Log.e(TAG, "theIso : " + theIso);
//                                Log.e(TAG, "Putting the shit in the shared preffs");
                                SharedPreferencesManager.putIso(context, tmp.code);
                                SharedPreferencesManager.putCurrencyListPosition(context, i - 1);
//                            Log.e(TAG,"position set: " + (i - 1));
                                SharedPreferencesManager.putRate(context, tmp.rate);
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
        }
        List tempList = new ArrayList<>(set);
        Collections.reverse(tempList);
        return new LinkedHashSet<>(set);
    }

    public class GetCurrenciesTask extends AsyncTask {
        Set<CurrencyEntity> tmp;

        @Override
        protected Object doInBackground(Object[] params) {
            tmp = getCurrencies(ctx);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (tmp.size() > 0) {
                currencyListAdapter.clear();
                currencyListAdapter.addAll(tmp);
                currencyListAdapter.notifyDataSetChanged();
                SharedPreferencesManager.putExchangeRates(ctx, tmp);
                if (BRAnimator.level <= 2)
                    MiddleViewAdapter.resetMiddleView(ctx, null);
            } else {
                currencyListAdapter.clear();
                Set<CurrencyEntity> currencyEntitySet = SharedPreferencesManager.getExchangeRates(ctx);
                if (currencyEntitySet == null || currencyEntitySet.isEmpty()) return;
                currencyListAdapter.addAll(currencyEntitySet);
                currencyListAdapter.notifyDataSetChanged();
                Log.e(TAG, "Adapter Not Changed, data is empty");
            }
        }
    }

    public CurrencyListAdapter getCurrencyAdapterIfReady() {
//        new GetCurrenciesTask().execute();
        return currencyListAdapter;
    }

    private void initializeTimerTask() {

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

}
