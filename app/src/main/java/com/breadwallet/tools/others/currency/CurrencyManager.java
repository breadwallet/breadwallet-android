package com.breadwallet.tools.others.currency;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.others.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Mihail on 7/22/15.
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
