package com.breadwallet.tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.tools.adapter.AmountAdapter;
import com.breadwallet.tools.adapter.CurrencyListAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.wallet.BRWalletManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 7/22/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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

public class CurrencyManager extends Observable {
    private static final String TAG = CurrencyManager.class.getName();

    private static CurrencyManager instance;
    private Timer timer;
    private long BALANCE = 0;
    private TimerTask timerTask;
    public final String bitcoinLowercase = "\u0180";
    private Handler handler;
    public static boolean separatorNeedsToBeShown = false;
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

    public boolean isNetworkAvailable(Activity context) {
        if (context == null) return false;
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void setBalance(long balance) {
        Log.e(TAG, "in the setBalance, BALANCE:  " + BALANCE);

        BALANCE = balance;
        setChanged();
        notifyObservers();
    }

    public long getBALANCE() {
        return BALANCE;
    }

    private List<CurrencyEntity> getCurrencies(Activity context) {
        List<CurrencyEntity> list = new ArrayList<>();
        if (isNetworkAvailable(context)) {
            try {
                JSONArray arr;
                arr = JsonParser.getJSonArray(context);
                JsonParser.updateFeePerKb(context);
//                Log.e(TAG, "JSONArray arr.length(): " + arr.length());

                int length = arr.length();
                for (int i = 1; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");
                        tmp.codeAndName = tmp.code + " - " + tmp.name;
                        tmp.rate = (float) tmpObj.getDouble("rate");
                        if (tmp.code.equals("USD")) {
                            String theIso = getISOFromPrefs();
//                            Log.e(TAG, "theIso : " + theIso);
                            if (theIso.equals("USD")) {
//                                Log.e(TAG, "Putting the shit in the shared preffs");
                                SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString(FragmentCurrency.CURRENT_CURRENCY, tmp.code);
                                editor.putInt(FragmentCurrency.POSITION, FragmentCurrency.lastItemsPosition);
                                editor.putFloat(FragmentCurrency.RATE, tmp.rate);
                                editor.apply();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    list.add(tmp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public class GetCurrenciesTask extends AsyncTask {
        List<CurrencyEntity> tmp;

        @Override
        protected Object doInBackground(Object[] params) {
            tmp = getCurrencies(ctx);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (tmp.size() > 0) {
//                Log.e(TAG, "inside the adapter changing shit");
                currencyListAdapter.clear();
                currencyListAdapter.addAll(tmp);
                currencyListAdapter.notifyDataSetChanged();
                if (FragmentAnimator.level <= 2)
                    MiddleViewAdapter.resetMiddleView( ctx, null);
            } else {
                Log.e(TAG, "Adapter Not Changed, data is empty");
            }
        }
    }

    public CurrencyListAdapter getCurrencyAdapterIfReady() {
        new GetCurrenciesTask().execute();
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

    public String getMiddleTextExchangeString(double rate, String iso) {
//        Log.e(TAG, "result of the exchange rate calculation: " + result);
        if (rate == 0) rate = 1;
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return null;
        long result = BRWalletManager.getInstance(ctx).bitcoinAmount(100, new BigDecimal(String.valueOf(rate)).multiply(new BigDecimal("100")).doubleValue());
        return getFormattedCurrencyString(iso, 100) + " = " +
                getFormattedCurrencyString("BTC", result);
    }

    public String getBitsAndExchangeString(double rate, String iso, BigDecimal target) {
        Log.e(TAG, "target: " + target);
//        Log.e(TAG, "result of the exchange rate calculation: " + result);
        if (rate == 0) rate = 1;
        long exchange = BRWalletManager.getInstance(ctx).localAmount(target.longValue(),
                new BigDecimal(String.valueOf(rate)).multiply(new BigDecimal("100")).doubleValue());
        return getFormattedCurrencyString("BTC", target.longValue()) + " = " +
                getFormattedCurrencyString(iso, exchange);
    }

    public String getExchangeForAmount(double rate, String iso, BigDecimal target) {
        if (rate == 0) rate = 1;
        long exchange = BRWalletManager.getInstance(ctx).localAmount(target.longValue(),
                new BigDecimal(String.valueOf(rate)).multiply(new BigDecimal("100")).doubleValue());
        if (ctx != null) {
//            long exchangeFromCore = BRWalletManager.getInstance(ctx).localAmount(new Double(target).longValue(),rate);
            Log.e(TAG, "exchange: " + exchange);
//            Log.e(TAG,"exchangeFromCore: " + exchangeFromCore);
        }
        return getFormattedCurrencyString(iso, exchange);
    }

    public String getCurrentBalanceText() {
        String iso = getISOFromPrefs();
        double rate = getRateFromPrefs();
        long exchange = BRWalletManager.getInstance(ctx).localAmount(getBALANCE(),new BigDecimal(String.valueOf(rate)).multiply(new BigDecimal("100")).doubleValue());
        Log.e(TAG, "getCurrentBalanceText, exchange: " + exchange);

        return getFormattedCurrencyString("BTC", getBALANCE()) + " (" +
                getFormattedCurrencyString(iso, exchange) + ")";
    }

    public String getFormattedCurrencyString(String isoCurrencyCode, long amount) {
        DecimalFormat currencyFormat;

        // This formats currency values as the user expects to read them (default locale).
        currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        // This specifies the actual currency that the value is in, and provide
        // s the currency symbol.
        DecimalFormatSymbols decimalFormatSymbols;
        Currency currency;
        if (Objects.equals(isoCurrencyCode, "BTC")) {
            decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
            decimalFormatSymbols.setCurrencySymbol(bitcoinLowercase);
        } else {
            try {
                currency = Currency.getInstance(isoCurrencyCode);
//                Log.e(TAG, "Currency.getInstance succeeded: " + currency.getSymbol());
            } catch (IllegalArgumentException e) {
//                Log.e(TAG, "Currency.getInstance did not succeed, going with the default", e);
                currency = Currency.getInstance(Locale.getDefault());
            }
            decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
            String symbol = currency.getSymbol();
            decimalFormatSymbols.setCurrencySymbol(symbol);
        }
        currencyFormat.setDecimalSeparatorAlwaysShown(separatorNeedsToBeShown);
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(AmountAdapter.digitsInserted);
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
//        Log.e(TAG, "Returning the formatted string with separatorVisibility: " +
// currencyFormat.isDecimalSeparatorAlwaysShown());
        return currencyFormat.format(new BigDecimal(String.valueOf(amount)).divide(new BigDecimal("100")).doubleValue());
    }

    public String getFormattedCurrencyStringForLocale(Locale locale, String isoCurrencyCode, double amount) {
        // This formats currency values as the user expects to read them (default locale).
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);

        // This specifies the actual currency that the value is in, and provides the currency symbol.
        Currency currency = Currency.getInstance(isoCurrencyCode);

        // Note we don't supply a locale to this method - uses default locale to format the currency symbol.
        String symbol = currency.getSymbol(locale);

        // We then tell our formatter to use this symbol.
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(symbol);
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        return currencyFormat.format(amount);
    }

    public String getFormattedCurrencyStringFixed(Locale locale, String isoCurrencyCode, double amount) {
        // This formats currency values as the user expects to read them in the supplied locale.
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);

        // This specifies the actual currency that the value is in, and provides
        // the currency symbol that is used
        Currency currency = Currency.getInstance(isoCurrencyCode);

        // Our fix is to use the US locale as default for the symbol, unless the currency is USD
        // and the locale is NOT the US, in which case we know it should be US$.
        String symbol;
        if (isoCurrencyCode.equalsIgnoreCase("usd") && !locale.equals(Locale.US)) {
            symbol = "US$";// currency.getSymbol(Locale.UK);
        } else {
            symbol = currency.getSymbol(Locale.US); // US locale has the best symbol formatting table.
        }

        // We tell our formatter to use this symbol
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(symbol);
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        return currencyFormat.format(amount);
    }

    public String getISOFromPrefs() {
        SharedPreferences settings = MainActivity.app.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        return settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
    }

    public double getRateFromPrefs() {
        SharedPreferences settings = MainActivity.app.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        return settings.getFloat(FragmentCurrency.RATE, 0f);
    }

}
