package com.breadwallet.tools;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.wallet.BRWalletManager;

import java.util.Currency;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 6/13/16.
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
public class SharedPreferencesManager {
    public static final String TAG = SharedPreferencesManager.class.getName();
    private static String RECEIVE_ADDRESS = "receive_address";

    public static String getIso(Activity context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        return settingsToGet.getString(FragmentCurrency.CURRENT_CURRENCY, Currency.getInstance(Locale.getDefault()).getCurrencyCode());
    }


    public static float getRate(Activity context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        return settingsToGet.getFloat(FragmentCurrency.RATE, 1);
    }

    public static boolean getPhraseWroteDown(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(BRWalletManager.PHRASE_WRITTEN, false);

    }

    public static void putCheckBoxRecoveryPhraseFragment(Activity context, boolean check) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(BRWalletManager.PHRASE_WRITTEN, check);
        editor.apply();
    }

    public static int getCurrencyListPosition(Context context) {
        SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        return settings.getInt(FragmentCurrency.POSITION, 0);
    }


    public static void putReceiveAddress(Activity ctx, String tmpAddr) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(RECEIVE_ADDRESS, tmpAddr);
        editor.apply();
    }

    public static void clearAllPrefs(Activity activity) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public static String getReceiveAddress(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(RECEIVE_ADDRESS, "");
    }

    public static void putFirstAddress(Activity context, String firstAddress) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(BRConstants.FIRST_ADDRESS, firstAddress);
        editor.apply();
    }

    public static long getFeePerKb(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(BRConstants.FEE_KB_PREFS, 0);
    }

    public static long getSecureTime(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(BRConstants.SECURE_TIME_PREFS, System.currentTimeMillis() / 1000);
    }

    public static int getLimit(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(BRConstants.LIMIT_PREFS, BRConstants.ONE_BITCOIN);
    }

    public static void putLimit(Activity activity, int limit) {
        SharedPreferences prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(BRConstants.LIMIT_PREFS, limit);
        editor.apply();
    }

    public static void putIso(Activity context, String code) {

        SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(FragmentCurrency.CURRENT_CURRENCY, code.equalsIgnoreCase(Locale.getDefault().getISO3Language()) ? null : code);
        editor.apply();

    }

    public static void putCurrencyListPosition(Activity context, int lastItemsPosition) {
        SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(FragmentCurrency.POSITION, lastItemsPosition);
        editor.apply();
    }

    public static void putRate(Activity context, float rate) {
        SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(FragmentCurrency.RATE, rate);
        editor.apply();
    }

    public static String getFirstAddress(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(BRConstants.FIRST_ADDRESS, "");
    }

    public static void putFeePerKb(Activity context, long fee) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(BRConstants.FEE_KB_PREFS, fee);
        editor.apply();
    }

    public static void putSecureTime(Activity activity, long date) {
        SharedPreferences prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(BRConstants.SECURE_TIME_PREFS, date);
        editor.apply();
    }

    public static void putExchangeRates(Activity activity, Set<CurrencyEntity> rates) {
        Set<String> set = new HashSet<>();
        for(CurrencyEntity s : rates){
            set.add(s.codeAndName);
        }
        SharedPreferences prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(BRConstants.EXCHANGE_RATES, set);
        editor.apply();
    }

    public static Set<String> getExchangeRates(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(BRConstants.EXCHANGE_RATES, new HashSet<String>());
    }
}
