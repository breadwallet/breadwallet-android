package com.breadwallet.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.tools.adapter.AmountAdapter;
import com.breadwallet.tools.adapter.CurrencyListAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
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
    private static CurrencyManager instance;
    public static final String TAG = "CurrencyManager";
    private Timer timer;
    private long BALANCE = 0;
    private TimerTask timerTask;
    public final String bitcoinLowercase = "\u0180";
    private final Handler handler = new Handler();
    public static boolean separatorNeedsToBeShown = false;
    private CurrencyListAdapter currencyListAdapter;
    private Context ctx;

    private CurrencyManager(Context ctx){
        this.ctx = ctx;
        currencyListAdapter = new CurrencyListAdapter(ctx, R.layout.currency_list_item);
    }

    public static synchronized CurrencyManager getInstance(Context ctx) {

        if (instance == null) {
            instance = new CurrencyManager(ctx);
        }
        return instance;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void setBalance(long balance) {
        BALANCE = balance;
        String tmp = getCurrentBalanceText();
        Log.e(TAG, "In the setBalance, app: " + ctx);
        ((BreadWalletApp) ctx.getApplicationContext()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, tmp);

    }

    public long getBALANCE() {
        return BALANCE;
    }

    public List<CurrencyEntity> getCurrencies() {
        List<CurrencyEntity> list = new ArrayList<>();
        if (isNetworkAvailable()) {
            try {
                JSONArray arr;
                arr = JsonParser.getJSonArray("https://bitpay.com/rates");
                int length = arr.length();
                for (int i = 1; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");
                        tmp.codeAndName = tmp.code + " - " + tmp.name;
                        tmp.rate = (float) tmpObj.getDouble("rate");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    list.add(tmp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (list.size() > 0) {
            CurrencyEntity test = new CurrencyEntity();
            test.name = "testing text extra long text mega long text oh my god that's a long text";
            test.code = "TES";
            test.codeAndName = test.code + " - " + test.name;
            test.rate = 0.999f;
            list.add(test);
        }
        return list;
    }

    public class GetCurrenciesTask extends AsyncTask {
        List<CurrencyEntity> tmp;

        @Override
        protected Object doInBackground(Object[] params) {
            tmp = getCurrencies();
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (tmp.size() > 0) {
                currencyListAdapter.clear();
                currencyListAdapter.addAll(tmp);
                currencyListAdapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, "Adapter Not Changed, data is empty");
            }

        }
    }

    public CurrencyListAdapter getCurrencyAdapterIfReady() {
        new GetCurrenciesTask().execute();
        return currencyListAdapter;
    }

    public void initializeTimerTask() {

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

    public String getMiddleTextExchangeString(long target, String iso) {
        double result = getBitsFromBitcoin(target);
//        Log.e(TAG, "result of the exchange rate calculation: " + result);
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String finalResult = getFormattedCurrencyString(iso, "1") + " = " + bitcoinLowercase +
                decimalFormat.format(result);
        return finalResult;
    }

    public long getBitsFromBitcoin(long target) {
        return target * 1000000;
    }

    public long getBitsFromSatoshi(long target) {
        return target / 100;
    }

    public String getCurrentBalanceText() {
        SharedPreferences settings;
//        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        settings = MainActivity.app.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");

        float rate = settings.getFloat(FragmentCurrency.RATE, 0f);
        long balance = getBitsFromSatoshi(getBALANCE());
        double exchange = (balance * rate / 1000000);
        CustomLogger.LogThis("rate", String.valueOf(rate), "exchange", String.valueOf(exchange));
        String result = getFormattedCurrencyString("BTC", String.valueOf(balance)) + "(" +
                getFormattedCurrencyString(iso, String.valueOf(exchange)) + ")";
        return result;
    }

    public String getFormattedCurrencyString(String isoCurrencyCode, String amount) {
        DecimalFormat currencyFormat;

        // This formats currency values as the user expects to read them (default locale).
        currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        // This specifies the actual currency that the value is in, and provide
        // s the currency symbol.
        DecimalFormatSymbols decimalFormatSymbols;
        Currency currency;
        if (isoCurrencyCode == "BTC") {
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
        return currencyFormat.format(new BigDecimal(amount).doubleValue());
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

}
