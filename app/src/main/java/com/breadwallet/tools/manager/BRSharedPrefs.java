package com.breadwallet.tools.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.breadwallet.tools.util.BRConstants;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
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

public class BRSharedPrefs {
    public static final String TAG = BRSharedPrefs.class.getName();

    public static List<OnIsoChangedListener> isoChangedListeners = new ArrayList<>();

    public interface OnIsoChangedListener {
        void onIsoChanged(String iso);
    }

    public static void addIsoChangedListener(OnIsoChangedListener listener) {
        if (isoChangedListeners == null) {
            isoChangedListeners = new ArrayList<>();
        }
        if (!isoChangedListeners.contains(listener))
            isoChangedListeners.add(listener);
    }

    public static void removeListener(OnIsoChangedListener listener) {
        if (isoChangedListeners == null) {
            isoChangedListeners = new ArrayList<>();
        }
        isoChangedListeners.remove(listener);

    }


    public static String getIso(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getString(BRConstants.CURRENT_CURRENCY, Currency.getInstance(Locale.getDefault()).getCurrencyCode());
    }

    public static void putIso(Context context, String code) {
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(BRConstants.CURRENT_CURRENCY, code.equalsIgnoreCase(Locale.getDefault().getISO3Language()) ? null : code);
        editor.apply();

        for (OnIsoChangedListener listener : isoChangedListeners) {
            if (listener != null) listener.onIsoChanged(code);
        }

    }

    public static boolean getPhraseWroteDown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(BRConstants.PHRASE_WRITTEN, false);

    }

    public static void putPhraseWroteDown(Context context, boolean check) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(BRConstants.PHRASE_WRITTEN, check);
        editor.apply();
    }

    public static int getCurrencyListPosition(Context context) {
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settings.getInt(BRConstants.POSITION, 0);
    }

    public static void putCurrencyListPosition(Context context, int lastItemsPosition) {
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BRConstants.POSITION, lastItemsPosition);
        editor.apply();
    }

    public static String getReceiveAddress(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(BRConstants.RECEIVE_ADDRESS, "");
    }

    public static void putReceiveAddress(Context ctx, String tmpAddr) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(BRConstants.RECEIVE_ADDRESS, tmpAddr);
        editor.apply();
    }

    public static String getWalletName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(BRConstants.WALLET_NAME, "My Loaf");
    }

    public static void putWalletName(Context ctx, String name) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(BRConstants.WALLET_NAME, name);
        editor.apply();
    }

    public static String getFirstAddress(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(BRConstants.FIRST_ADDRESS, "");
    }

    public static void putFirstAddress(Context context, String firstAddress) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(BRConstants.FIRST_ADDRESS, firstAddress);
        editor.apply();
    }

    public static long getFeePerKb(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(BRConstants.FEE_KB_PREFS, 0);
    }

    public static void putFeePerKb(Context context, long fee) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(BRConstants.FEE_KB_PREFS, fee);
        editor.apply();
    }

    public static long getEconomyFeePerKb(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(BRConstants.ECONOMY_FEE_KB_PREFS, 0);
    }

    public static void putEconomyFeePerKb(Context context, long fee) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(BRConstants.ECONOMY_FEE_KB_PREFS, fee);
        editor.apply();
    }

    public static long getCatchedBalance(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong("balance", 0);
    }

    public static void putCatchedBalance(Context context, long fee) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("balance", fee);
        editor.apply();
    }

    public static long getSecureTime(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(BRConstants.SECURE_TIME_PREFS, System.currentTimeMillis() / 1000);
    }

    //secure time from the server
    public static void putSecureTime(Context activity, long date) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(BRConstants.SECURE_TIME_PREFS, date);
        editor.apply();
    }

    public static long getLastSyncTime(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong("lastSyncTime", 0);
    }

    public static void putLastSyncTime(Context activity, long time) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastSyncTime", time);
        editor.apply();
    }

    public static List<Integer> getBitIdNonces(Context activity, String key) {
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

    public static void putBitIdNonces(Context activity, List<Integer> nonces, String key) {
        JSONArray arr = new JSONArray();
        arr.put(nonces);
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, arr.toString());
        editor.apply();
    }

    public static boolean getAllowSpend(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(BRConstants.ALLOW_SPEND, true);
    }

    public static void putAllowSpend(Context activity, boolean allow) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(BRConstants.ALLOW_SPEND, allow);
        editor.apply();
    }

    //if the user prefers all in bitcoin units, not other currencies
    public static boolean getPreferredBTC(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("priceSetToLitecoin", true);
    }

    //if the user prefers all in bitcoin units, not other currencies
    public static void putPreferredBTC(Context activity, boolean b) {
        Log.e(TAG, "putPreferredBTC: " + b);
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("priceSetToLitecoin", b);
        editor.apply();
    }

    //if the user prefers all in bitcoin units, not other currencies
    public static boolean getUseFingerprint(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("useFingerprint", false);
    }

    //if the user prefers all in bitcoin units, not other currencies
    public static void putUseFingerprint(Context activity, boolean use) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("useFingerprint", use);
        editor.apply();
    }

    public static boolean getFeatureEnabled(Context activity, String feature) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(feature, false);
    }

    public static void putFeatureEnabled(Context activity, boolean enabled, String feature) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(feature, enabled);
        editor.apply();
    }

    public static boolean getGeoPermissionsRequested(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(GEO_PERMISSIONS_REQUESTED, false);
    }

    public static void putGeoPermissionsRequested(Context activity, boolean requested) {
        SharedPreferences prefs = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(GEO_PERMISSIONS_REQUESTED, requested);
        editor.apply();
    }

    public static int getStartHeight(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getInt(BRConstants.START_HEIGHT, 0);
    }

    public static void putStartHeight(Context context, int startHeight) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BRConstants.START_HEIGHT, startHeight);
        editor.apply();
    }

    public static int getLastBlockHeight(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getInt(BRConstants.LAST_BLOCK_HEIGHT, 0);
    }

    public static void putLastBlockHeight(Context context, int lastHeight) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BRConstants.LAST_BLOCK_HEIGHT, lastHeight);
        editor.apply();
    }

    public static boolean getScanRecommended(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getBoolean("scanRecommended", false);
    }

    public static void putScanRecommended(Context context, boolean recommended) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("scanRecommended", recommended);
        editor.apply();
    }

    public static int getCurrencyUnit(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        return settingsToGet.getInt(BRConstants.CURRENT_UNIT, BRConstants.CURRENT_UNIT_LITES);
    }

    public static void putCurrencyUnit(Context context, int unit) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(BRConstants.CURRENT_UNIT, unit);
        editor.apply();
    }

    public static String getDeviceId(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        String deviceId = settingsToGet.getString(BRConstants.USER_ID, "");
        if (deviceId.isEmpty()) setDeviceId(context, UUID.randomUUID().toString());
        return (settingsToGet.getString(BRConstants.USER_ID, ""));
    }

    private static void setDeviceId(Context context, String uuid) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(BRConstants.USER_ID, uuid);
        editor.apply();
    }

    public static void clearAllPrefs(Context activity) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public static boolean getShowNotification(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean("showNotification", false);
    }

    public static void putShowNotification(Context context, boolean show) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("showNotification", show);
        editor.apply();
    }

    public static boolean getShareData(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean("shareData", false);
    }

    public static void putShareData(Context context, boolean show) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("shareData", show);
        editor.apply();
    }

    public static boolean getShareDataDismissed(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean("shareDataDismissed", false);
    }

    public static void putShareDataDismissed(Context context, boolean dismissed) {
        if (context == null) return;
        SharedPreferences settings = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("shareDataDismissed", dismissed);
        editor.apply();
    }

    public static String getBCHTxId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("bchTxId", "");
    }

    public static void putBCHTxId(Context context, String txId) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("bchTxId", txId);
        editor.apply();
    }

    public static String getTrustNode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("trustNode", "");
    }

    public static void putTrustNode(Context context, String trustNode) {
        SharedPreferences prefs = context.getSharedPreferences(BRConstants.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("trustNode", trustNode);
        editor.apply();
    }
}