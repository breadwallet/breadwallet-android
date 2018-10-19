package com.breadwallet.tools.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.breadwallet.tools.util.BRConstants;

import org.json.JSONArray;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


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

    public static final String PREFS_NAME = "MyPrefsFile";
    private static final String FCM_TOKEN = "fcmToken";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String SCREEN_HEIGHT = "screenHeight";
    private static final String SCREEN_WIDTH = "screenWidth";
    private static final String BUNDLE_HASH_PREFIX = "bundleHash_";

    public static String getPreferredFiatIso(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        String defIso;
        try {
            defIso = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            defIso = Currency.getInstance(Locale.US).getCurrencyCode();
        }
        return settingsToGet.getString("currentCurrency", defIso);
    }

    public static void putPreferredFiatIso(Context context, String iso) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("currentCurrency", iso.equalsIgnoreCase(Locale.getDefault().getISO3Language()) ? null : iso);
        editor.apply();

    }

    public static boolean getPhraseWroteDown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("phraseWritten", false);

    }

    public static void putPhraseWroteDown(Context context, boolean check) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("phraseWritten", check);
        editor.apply();
    }

    public static boolean getFavorStandardFee(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("favorStandardFee" + iso.toUpperCase(), true);

    }

    public static void putFavorStandardFee(Context context, String iso, boolean favorStandardFee) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("favorStandardFee" + iso.toUpperCase(), favorStandardFee);
        editor.apply();
    }

    public static String getReceiveAddress(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("receive_address" + iso.toUpperCase(), "");
    }

    public static void putReceiveAddress(Context ctx, String tmpAddr, String iso) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString("receive_address" + iso.toUpperCase(), tmpAddr);
        editor.apply();
    }

    public static String getFirstAddress(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("firstAddress", "");
    }

    public static void putFirstAddress(Context context, String firstAddress) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("firstAddress", firstAddress);
        editor.apply();
    }

    public static BigDecimal getFeeRate(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new BigDecimal(prefs.getString("feeRate" + iso.toUpperCase(), "0"));
    }

    public static void putFeeRate(Context context, String iso, BigDecimal fee) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("feeRate" + iso.toUpperCase(), fee.toPlainString());
        editor.apply();
    }

    public static BigDecimal getEconomyFeeRate(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new BigDecimal(prefs.getString("economyFeeRate" + iso.toUpperCase(), "0"));
    }

    public static void putEconomyFeeRate(Context context, String iso, BigDecimal fee) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("economyFeeRate" + iso.toUpperCase(), fee.toPlainString());
        editor.apply();
    }

    public static BigDecimal getCachedBalance(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new BigDecimal(prefs.getString("balance" + iso.toUpperCase(),
                String.valueOf(prefs.getLong("balance_" + iso.toUpperCase(), 0))));
    }

    public static void putCachedBalance(Context context, String iso, BigDecimal balance) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("balance" + iso.toUpperCase(), balance.toPlainString());
        editor.apply();
    }

    public static long getSecureTime(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong("secureTime", System.currentTimeMillis() / 1000);
    }

    //secure time from the server
    public static void putSecureTime(Context activity, long date) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("secureTime", date);
        editor.apply();
    }

    public static long getLastSyncTime(Context activity, String iso) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong("lastSyncTime_" + iso.toUpperCase(), 0);
    }

    public static void putLastSyncTime(Context activity, String iso, long time) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastSyncTime_" + iso.toUpperCase(), time);
        editor.apply();
    }

    public static String getLastRescanModeUsed(Context activity, String iso) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("lastRescanModeUsed_" + iso.toUpperCase(), null);
    }

    public static void putLastRescanModeUsed(Context activity, String iso, String mode) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastRescanModeUsed_" + iso.toUpperCase(), mode);
        editor.apply();
    }

    public static long getLastSendTransactionBlockheight(Context activity, String iso) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong("lastSendTransactionBlockheight_" + iso.toUpperCase(), 0);
    }

    public static void putLastSendTransactionBlockheight(Context activity, String iso, long blockHeight) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastSendTransactionBlockheight_" + iso.toUpperCase(), blockHeight);
        editor.apply();
    }

    public static long getFeeTime(Context activity, String iso) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong("feeTime_" + iso.toUpperCase(), 0);
    }

    public static void putFeeTime(Context activity, String iso, long feeTime) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("feeTime_" + iso.toUpperCase(), feeTime);
        editor.apply();
    }

    public static List<Integer> getBitIdNonces(Context activity, String key) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, arr.toString());
        editor.apply();
    }

    public static boolean getAllowSpend(Context activity, String iso) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("allowSpend_" + iso.toUpperCase(), true);
    }

    public static void putAllowSpend(Context activity, String iso, boolean allow) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("allowSpend_" + iso.toUpperCase(), allow);
        editor.apply();
    }

    //if the user prefers all in crypto units, not fiat currencies
    public static boolean isCryptoPreferred(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("priceInCrypto", false);
    }

    //if the user prefers all in crypto units, not fiat currencies
    public static void setIsCryptoPreferred(Context activity, boolean b) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("priceInCrypto", b);
        editor.apply();
    }

    public static boolean getUseFingerprint(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("useFingerprint", false);
    }

    public static void putUseFingerprint(Context activity, boolean use) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("useFingerprint", use);
        editor.apply();
    }

    public static boolean getFeatureEnabled(Context activity, String feature) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(feature, false);
    }

    public static void putFeatureEnabled(Context activity, boolean enabled, String feature) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(feature, enabled);
        editor.apply();
    }

    public static String getCurrentWalletIso(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("currentWalletIso", "BTC");
    }

    public static void putCurrentWalletIso(Context activity, String iso) {
        if (iso == null) throw new NullPointerException("cannot be null");
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("currentWalletIso", iso);
        editor.apply();
    }

    public static String getWalletRewardId(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("walletRewardId", null);
    }

    public static void putWalletRewardId(Context app, String id) {
        if (id == null) throw new NullPointerException("cannot be null");
        SharedPreferences prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("walletRewardId", id);
        editor.apply();
    }

    public static boolean getGeoPermissionsRequested(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("geoPermissionsRequested", false);
    }

    public static void putGeoPermissionsRequested(Context activity, boolean requested) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("geoPermissionsRequested", requested);
        editor.apply();
    }

    public static long getStartHeight(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getLong("startHeight_" + iso.toUpperCase(), 0);
    }

    public static void putStartHeight(Context context, String iso, long startHeight) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("startHeight_" + iso.toUpperCase(), startHeight);
        editor.apply();
    }

    public static long getLastRescanTime(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getLong("rescanTime_" + iso.toUpperCase(), 0);
    }

    public static void putLastRescanTime(Context context, String iso, long time) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("rescanTime_" + iso.toUpperCase(), time);
        editor.apply();
    }

    public static int getLastBlockHeight(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getInt("lastBlockHeight" + iso.toUpperCase(), 0);
    }

    public static void putLastBlockHeight(Context context, String iso, int lastHeight) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("lastBlockHeight" + iso.toUpperCase(), lastHeight);
        editor.apply();
    }

    public static boolean getScanRecommended(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getBoolean("scanRecommended_" + iso.toUpperCase(), false);
    }

    public static void putScanRecommended(Context context, String iso, boolean recommended) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("scanRecommended_" + iso.toUpperCase(), recommended);
        editor.apply();
    }

    public static boolean getBchPreforkSynced(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getBoolean("preforkSynced", false);
    }

    public static void putBchPreforkSynced(Context context, boolean synced) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("preforkSynced", synced);
        editor.apply();
    }

    // BTC, mBTC, Bits
    //ignore iso, using same denomination for both for now
    public static int getCryptoDenomination(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getInt("currencyUnit", BRConstants.CURRENT_UNIT_BITCOINS);

    }

    // BTC, mBTC, Bits
    //ignore iso, using same denomination for both for now
    public static void putCryptoDenomination(Context context, String iso, int unit) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("currencyUnit", unit);
        editor.apply();
    }

    public static String getDeviceId(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        String deviceId = settingsToGet.getString("userId", "");
        if (deviceId.isEmpty()) setDeviceId(context, UUID.randomUUID().toString());
        return (settingsToGet.getString("userId", ""));
    }

    private static void setDeviceId(Context context, String uuid) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("userId", uuid);
        editor.apply();
    }

    public static void clearAllPrefs(Context activity) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public static boolean getShowNotification(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean("showNotification", false);
    }

    public static void putShowNotification(Context context, boolean show) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("showNotification", show);
        editor.apply();
    }

    public static boolean getShareData(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean("shareData", false);
    }

    public static void putShareData(Context context, boolean show) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("shareData", show);
        editor.apply();
    }

    public static boolean isNewWallet(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean("newWallet", true);
    }

    public static void putIsNewWallet(Context context, boolean newWallet) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("newWallet", newWallet);
        editor.apply();
    }

    public static boolean getPromptDismissed(Context context, String promptName) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean("prompt_" + promptName, false);
    }

    public static void putPromptDismissed(Context context, String promptName, boolean dismissed) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("prompt_" + promptName, dismissed);
        editor.apply();
    }


    public static String getTrustNode(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("trustNode_" + iso.toUpperCase(), "");
    }

    public static void putTrustNode(Context context, String iso, String trustNode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("trustNode_" + iso.toUpperCase(), trustNode);
        editor.apply();
    }


    public static boolean wasAppBackgroundedFromHome(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("appBackgroundedFromHome", true);

    }

    public static void putAppBackgroundedFromHome(Context context, boolean fromHome) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("appBackgroundedFromHome", fromHome);
        editor.apply();

    }

    public static void putFCMRegistrationToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(FCM_TOKEN, token);
        editor.apply();
    }

    public static String getFCMRegistrationToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(FCM_TOKEN, "");
    }

    public static void putNotificationId(Context context, int notificationId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(NOTIFICATION_ID, notificationId);
        editor.apply();
    }

    public static int getNotificationId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(NOTIFICATION_ID, 0);
    }

    public static void putScreenHeight(Context context, int screenHeight) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SCREEN_HEIGHT, screenHeight);
        editor.apply();
    }

    public static int getScreenHeight(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(SCREEN_HEIGHT, 0);
    }

    public static void putScreenWidth(Context context, int screenWidth) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SCREEN_WIDTH, screenWidth);
        editor.apply();
    }

    public static int getScreenWidth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(SCREEN_WIDTH, 0);
    }

    public static void putBundleHash(Context context, String bundleName, String bundleHash) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(BUNDLE_HASH_PREFIX + bundleName, bundleHash);
        editor.apply();
    }

    public static String getBundleHash(Context context, String bundleName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(BUNDLE_HASH_PREFIX + bundleName, null);
    }

}