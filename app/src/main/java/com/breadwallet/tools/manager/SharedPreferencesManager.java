package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static com.breadwallet.tools.util.BRConstants.GEO_PERMISSIONS_REQUESTED;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/13/16.
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

public class SharedPreferencesManager {
    public static final String TAG = SharedPreferencesManager.class.getName();

    public static String getIso(Activity context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        Currency currency;
        try {
            currency = Currency.getInstance(Locale.getDefault());
        } catch (IllegalArgumentException e) {
            currency = Currency.getInstance(Locale.ENGLISH);
        }
        String defaultIso = currency.getCurrencyCode();
        return settingsToGet.getString(BRConstants.CURRENT_CURRENCY, defaultIso);
    }

    public static void putIso(Activity context, String code) {
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(BRConstants.CURRENT_CURRENCY, code.equalsIgnoreCase(Locale.getDefault().getISO3Language()) ? null : code);
        editor.apply();

    }

    public static float getRate(Activity context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getFloat(BRConstants.RATE, 1);
    }

    public static void putRate(Activity context, float rate) {
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(BRConstants.RATE, rate);
        editor.apply();
    }

    public static boolean getPhraseWroteDown(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(BRConstants.PHRASE_WRITTEN, false);

    }

    public static void putPhraseWroteDown(Activity context, boolean check) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(BRConstants.PHRASE_WRITTEN, check);
        editor.apply();
    }

    public static int getCurrencyListPosition(Context context) {
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settings.getInt(BRConstants.POSITION, 0);
    }

    public static void putCurrencyListPosition(Activity context, int lastItemsPosition) {
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BRConstants.POSITION, lastItemsPosition);
        editor.apply();
    }

    public static String getReceiveAddress(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        String address = prefs.getString(BRConstants.RECEIVE_ADDRESS, "");
        if (Utils.isNullOrEmpty(address)) address = BRWalletManager.getReceiveAddress();
        if (Utils.isNullOrEmpty(address)) {
            FirebaseCrash.report(new RuntimeException("Address is null"));
        } else {
            putReceiveAddress(context, address);
        }
        return address;
    }

    public static void putReceiveAddress(Activity ctx, String tmpAddr) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(BRConstants.RECEIVE_ADDRESS, tmpAddr);
        editor.apply();
    }

    public static String getFirstAddress(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(BRConstants.FIRST_ADDRESS, "");
    }

    public static void putFirstAddress(Activity context, String firstAddress) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(BRConstants.FIRST_ADDRESS, firstAddress);
        editor.apply();
    }
    public static String getBCHTxId(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("bchTxId", "");
    }

    public static void putBCHTxId(Activity context, String txId) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("bchTxId", txId);
        editor.apply();
    }

    public static String getTrustNode(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("trustNode", "");
    }

    public static void putTrustNode(Activity context, String trustNode) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("trustNode", trustNode);
        editor.apply();
    }

    public static long getFeePerKb(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(BRConstants.FEE_KB_PREFS, 0);
    }

    public static void putFeePerKb(Activity context, long fee) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(BRConstants.FEE_KB_PREFS, fee);
        editor.apply();
    }

    public static long getSecureTime(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(BRConstants.SECURE_TIME_PREFS, System.currentTimeMillis() / 1000);
    }

    //secure time from the server
    public static void putSecureTime(Activity activity, long date) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(BRConstants.SECURE_TIME_PREFS, date);
        editor.apply();
    }

    public static long getPhraseWarningTime(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(BRConstants.PHRASE_WARNING_TIME, 0);
    }


    public static void putPhraseWarningTime(Activity activity, long phraseWarningTime) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(BRConstants.PHRASE_WARNING_TIME, phraseWarningTime);
        editor.apply();
    }

    public static int getLimit(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(BRConstants.LIMIT_PREFS, BRConstants.HUNDRED_BITS);
    }

    public static void putLimit(Activity activity, int limit) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(BRConstants.LIMIT_PREFS, limit);
        editor.apply();
    }

    public static List<Integer> getBitIdNonces(Activity activity, String key) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        String result = prefs.getString(key, null);
        List<Integer> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(result);
            for (int i = 0; i < arr.length(); i++) {
                int a = arr.getInt(i);
                Log.d("found a nonce: ", a + "");
                list.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void putBitIdNonces(Activity activity, List<Integer> nonces, String key) {
        JSONArray arr = new JSONArray();
        arr.put(nonces);
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, arr.toString());
        editor.apply();
    }

    public static boolean getAllowSpend(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(BRConstants.ALLOW_SPEND, true);
    }

    public static void putAllowSpend(Activity activity, boolean allow) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(BRConstants.ALLOW_SPEND, allow);
        editor.apply();
    }

    public static boolean getFeatureEnabled(Activity activity, String feature) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(feature, false);
    }

    public static void putFeatureEnabled(Activity activity, boolean enabled, String feature) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(feature, enabled);
        editor.apply();
    }

    public static boolean getGeoPermissionsRequested(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(GEO_PERMISSIONS_REQUESTED, false);
    }

    public static void putGeoPermissionsRequested(Activity activity, boolean requested) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(GEO_PERMISSIONS_REQUESTED, requested);
        editor.apply();
    }


    @SuppressWarnings("unchecked")
    public static Set<CurrencyEntity> getExchangeRates(Activity context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);

        byte[] bytes = prefs.getString(BRConstants.EXCHANGE_RATES, "{}").getBytes();
        if (bytes.length == 0) {
            return null;
        }
        Set<CurrencyEntity> result = null;
        ByteArrayInputStream byteArray = new ByteArrayInputStream(bytes);
        Base64InputStream base64InputStream = new Base64InputStream(byteArray, Base64.NO_WRAP);
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(base64InputStream);

            result = (Set<CurrencyEntity>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void putExchangeRates(Activity activity, Set<CurrencyEntity> rates) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(BRConstants.EXCHANGE_RATES);
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();

        ObjectOutputStream objectOutput;
        try {
            objectOutput = new ObjectOutputStream(arrayOutputStream);
            objectOutput.writeObject(rates);
            byte[] data = arrayOutputStream.toByteArray();
            objectOutput.close();
            arrayOutputStream.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Base64OutputStream b64 = new Base64OutputStream(out, Base64.NO_WRAP);
            b64.write(data);
            b64.close();
            out.close();
            editor.putString(BRConstants.EXCHANGE_RATES, new String(out.toByteArray()));

            editor.apply();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static int getStartHeight(Activity context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getInt(BRConstants.START_HEIGHT, 0);
    }

    public static void putStartHeight(Activity context, int startHeight) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BRConstants.START_HEIGHT, startHeight);
        editor.apply();
    }

    public static int getLastBlockHeight(Activity context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getInt(BRConstants.LAST_BLOCK_HEIGHT, 0);
    }

    public static void putLastBlockHeight(Activity context, int lastHeight) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BRConstants.LAST_BLOCK_HEIGHT, lastHeight);
        editor.apply();
    }

    public static boolean getTipsShown(Activity context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getBoolean(BRConstants.TIPS_SHOWN, false);
    }

    public static void putTipsShown(Activity context, boolean tipsShown) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(BRConstants.TIPS_SHOWN, tipsShown);
        editor.apply();
    }

    public static int getCurrencyUnit(Activity context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getInt(BRConstants.CURRENT_UNIT, BRConstants.CURRENT_UNIT_BITS);
    }

    public static void putCurrencyUnit(Activity context, int unit) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BRConstants.CURRENT_UNIT, unit);
        editor.apply();
    }

    public static String getDeviceId(Activity context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        String deviceId = settingsToGet.getString(BRConstants.USER_ID, "");
        if (deviceId.isEmpty()) setDeviceId(context, UUID.randomUUID().toString());
        return (settingsToGet.getString(BRConstants.USER_ID, ""));
    }

    private static void setDeviceId(Activity context, String uuid) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(BRConstants.USER_ID, uuid);
        editor.apply();
    }

    public static void clearAllPrefs(Activity activity) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }
}
