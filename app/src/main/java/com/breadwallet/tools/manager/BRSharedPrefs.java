package com.breadwallet.tools.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.util.Log;

import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.ServerBundlesHelper;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;

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
    private static final String SEGWIT = "segwit";
    private static final String EMAIL_OPT_IN = "emailOptIn";
    private static final String EMAIL_OPT_IN_DISMISSED = "emailOptInDismissed";
    private static final String CURRENT_CURRENCY = "currentCurrency";
    private static final String PAPER_KEY_WRITTEN_DOWN = "phraseWritten";
    private static final String PREFER_STANDARD_FEE = "favorStandardFee";
    private static final String RECEIVE_ADDRESS = "receive_address";
    private static final String FEE_RATE = "feeRate";
    private static final String ECONOMY_FEE_RATE = "economyFeeRate";
    private static final String BALANCE = "balance";
    private static final String SECURE_TIME = "secureTime";
    private static final String LAST_SYNC_TIME_PREFIX = "lastSyncTime_";
    private static final String LAST_RESCAN_MODE_USED_PREFIX = "lastRescanModeUsed_";
    private static final String LAST_SEND_TRANSACTION_BLOCK_HEIGHT_PREFIX = "lastSendTransactionBlockheight_";
    private static final String FEE_TIME_PREFIX = "feeTime_";
    private static final String ALLOW_SPEND_PREFIX = "allowSpend_";
    private static final String IS_CRYPTO_PREFERRED = "priceInCrypto";
    private static final String USE_FINGERPRINT = "useFingerprint";
    private static final String CURRENT_WALLET_CURRENCY_CODE = "currentWalletIso";
    private static final String WALLET_REWARD_ID = "walletRewardId";
    private static final String GEO_PERMISSIONS_REQUESTED = "geoPermissionsRequested";
    private static final String START_HEIGHT_PREFIX = "startHeight_";
    private static final String RESCAN_TIME_PREFIX = "rescanTime_";
    private static final String LAST_BLOCK_HEIGHT_PREFIX = "lastBlockHeight_";
    private static final String SCAN_RECOMMENDED_PREFIX = "scanRecommended_";
    private static final String PREFORK_SYNCED = "preforkSynced";
    private static final String CURRENCY_UNIT = "currencyUnit";
    private static final String USER_ID = "userId";
    private static final String SHOW_NOTIFICATION = "showNotification";
    private static final String SHARE_DATA = "shareData";
    private static final String NEW_WALLET = "newWallet";
    private static final String PROMPT_PREFIX = "prompt_";
    private static final String TRUST_NODE_PREFIX = "trustNode_";
    private static final String APP_BACKGROUNDED_FROM_HOME = "appBackgroundedFromHome";
    private static final String DEBUG_HOST = "debug_host";
    private static final String DEBUG_SERVER_BUNDLE = "debug_server_bundle";
    private static final String HTTP_SERVER_PORT = "http_server_port";
    private static final String REWARDS_ANIMATION_SHOWN = "rewardsAnimationShown";

    public static String getPreferredFiatIso(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        String defIso;
        try {
            defIso = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            defIso = Currency.getInstance(Locale.US).getCurrencyCode();
        }
        return settingsToGet.getString(CURRENT_CURRENCY, defIso);
    }

    public static void putPreferredFiatIso(Context context, String iso) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(CURRENT_CURRENCY, iso.equalsIgnoreCase(Locale.getDefault().getISO3Language()) ? null : iso);
        editor.apply();

    }

    public static boolean getPhraseWroteDown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PAPER_KEY_WRITTEN_DOWN, false);

    }

    public static void putPhraseWroteDown(Context context, boolean check) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PAPER_KEY_WRITTEN_DOWN, check);
        editor.apply();
    }

    public static boolean getFavorStandardFee(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREFER_STANDARD_FEE + iso.toUpperCase(), true);

    }

    public static void putFavorStandardFee(Context context, String iso, boolean favorStandardFee) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREFER_STANDARD_FEE + iso.toUpperCase(), favorStandardFee);
        editor.apply();
    }

    public static String getReceiveAddress(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(RECEIVE_ADDRESS + iso.toUpperCase(), "");
    }

    public static void putReceiveAddress(Context ctx, String tmpAddr, String iso) {
        SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(RECEIVE_ADDRESS + iso.toUpperCase(), tmpAddr);
        editor.apply();
    }

    public static BigDecimal getFeeRate(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new BigDecimal(prefs.getString(FEE_RATE + iso.toUpperCase(), "0"));
    }

    public static void putFeeRate(Context context, String iso, BigDecimal fee) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(FEE_RATE + iso.toUpperCase(), fee.toPlainString());
        editor.apply();
    }

    public static BigDecimal getEconomyFeeRate(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new BigDecimal(prefs.getString(ECONOMY_FEE_RATE + iso.toUpperCase(), "0"));
    }

    public static void putEconomyFeeRate(Context context, String iso, BigDecimal fee) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(ECONOMY_FEE_RATE + iso.toUpperCase(), fee.toPlainString());
        editor.apply();
    }

    public static long getSecureTime(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(SECURE_TIME, System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS);
    }

    //secure time from the server
    static void putSecureTime(Context activity, long date) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(BRSharedPrefs.SECURE_TIME, date);
        editor.apply();
    }

    public static long getLastSyncTime(Context activity, String iso) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(LAST_SYNC_TIME_PREFIX + iso.toUpperCase(), 0);
    }

    public static void putLastSyncTime(Context activity, String iso, long time) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_SYNC_TIME_PREFIX + iso.toUpperCase(), time);
        editor.apply();
    }

    public static String getLastRescanModeUsed(Context activity, String iso) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LAST_RESCAN_MODE_USED_PREFIX + iso.toUpperCase(), null);
    }

    public static void putLastRescanModeUsed(Context activity, String iso, String mode) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LAST_RESCAN_MODE_USED_PREFIX + iso.toUpperCase(), mode);
        editor.apply();
    }

    public static long getLastSendTransactionBlockheight(Context activity, String iso) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(LAST_SEND_TRANSACTION_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), 0);
    }

    public static void putLastSendTransactionBlockheight(Context activity, String iso, long blockHeight) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_SEND_TRANSACTION_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), blockHeight);
        editor.apply();
    }

    public static long getFeeTime(Context activity, String iso) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(FEE_TIME_PREFIX + iso.toUpperCase(), 0);
    }

    public static void putFeeTime(Context activity, String iso, long feeTime) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(FEE_TIME_PREFIX + iso.toUpperCase(), feeTime);
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
        return prefs.getBoolean(ALLOW_SPEND_PREFIX + iso.toUpperCase(), true);
    }

    public static void putAllowSpend(Context activity, String iso, boolean allow) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ALLOW_SPEND_PREFIX + iso.toUpperCase(), allow);
        editor.apply();
    }

    //if the user prefers all in crypto units, not fiat currencies
    public static boolean isCryptoPreferred(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(IS_CRYPTO_PREFERRED, false);
    }

    //if the user prefers all in crypto units, not fiat currencies
    public static void setIsCryptoPreferred(Context activity, boolean b) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(IS_CRYPTO_PREFERRED, b);
        editor.apply();
    }

    public static boolean getUseFingerprint(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(USE_FINGERPRINT, false);
    }

    public static void putUseFingerprint(Context activity, boolean use) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(USE_FINGERPRINT, use);
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

    public static String getCurrentWalletCurrencyCode(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(CURRENT_WALLET_CURRENCY_CODE, WalletBitcoinManager.BITCOIN_CURRENCY_CODE);
    }

    public static void putCurrentWalletCurrencyCode(Context activity, String currencyCode) {
        if (currencyCode == null) {
            throw new NullPointerException("cannot be null");
        }
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(CURRENT_WALLET_CURRENCY_CODE, currencyCode);
        editor.apply();
    }

    public static String getWalletRewardId(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(WALLET_REWARD_ID, null);
    }

    public static void putWalletRewardId(Context app, String id) {
        if (id == null) throw new NullPointerException("cannot be null");
        SharedPreferences prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(WALLET_REWARD_ID, id);
        editor.apply();
    }

    public static boolean getGeoPermissionsRequested(Context activity) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(GEO_PERMISSIONS_REQUESTED, false);
    }

    public static void putGeoPermissionsRequested(Context activity, boolean requested) {
        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(GEO_PERMISSIONS_REQUESTED, requested);
        editor.apply();
    }

    public static long getStartHeight(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getLong(START_HEIGHT_PREFIX + iso.toUpperCase(), 0);
    }

    public static void putStartHeight(Context context, String iso, long startHeight) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(START_HEIGHT_PREFIX + iso.toUpperCase(), startHeight);
        editor.apply();
    }

    public static long getLastRescanTime(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getLong(RESCAN_TIME_PREFIX + iso.toUpperCase(), 0);
    }

    public static void putLastRescanTime(Context context, String iso, long time) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(RESCAN_TIME_PREFIX + iso.toUpperCase(), time);
        editor.apply();
    }

    public static int getLastBlockHeight(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getInt(LAST_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), 0);
    }

    public static void putLastBlockHeight(Context context, String iso, int lastHeight) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(LAST_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), lastHeight);
        editor.apply();
    }

    public static boolean getScanRecommended(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getBoolean(SCAN_RECOMMENDED_PREFIX + iso.toUpperCase(), false);
    }

    public static void putScanRecommended(Context context, String iso, boolean recommended) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SCAN_RECOMMENDED_PREFIX + iso.toUpperCase(), recommended);
        editor.apply();
    }

    public static boolean getBchPreForkSynced(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getBoolean(PREFORK_SYNCED, false);
    }

    public static void putBchPreForkSynced(Context context, boolean synced) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREFORK_SYNCED, synced);
        editor.apply();
    }

    // BTC, mBTC, Bits
    //ignore iso, using same denomination for both for now
    public static int getCryptoDenomination(Context context, String iso) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getInt(CURRENCY_UNIT, BRConstants.CURRENT_UNIT_BITCOINS);
    }

    // BTC, mBTC, Bits
    //ignore iso, using same denomination for both for now
    public static void putCryptoDenomination(Context context, String iso, int unit) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(CURRENCY_UNIT, unit);
        editor.apply();
    }

    public static String getDeviceId(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        String deviceId = settingsToGet.getString(USER_ID, "");
        if (deviceId.isEmpty()) setDeviceId(context, UUID.randomUUID().toString());
        return (settingsToGet.getString(USER_ID, ""));
    }

    private static void setDeviceId(Context context, String uuid) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(USER_ID, uuid);
        editor.apply();
    }

    public static String getDebugHost(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, 0);
        return settingsToGet.getString(DEBUG_HOST, "");
    }

    public static void putDebugHost(Context context, String host) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(DEBUG_HOST, host);
        editor.apply();
    }

    public static void clearAllPrefs(Context activity) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    public static boolean getShowNotification(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean(SHOW_NOTIFICATION, false);
    }

    public static void putShowNotification(Context context, boolean show) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SHOW_NOTIFICATION, show);
        editor.apply();
    }

    public static boolean getShareData(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean(SHARE_DATA, true);
    }

    public static void putShareData(Context context, boolean show) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SHARE_DATA, show);
        editor.apply();
    }

    public static boolean isNewWallet(Context context) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean(NEW_WALLET, true);
    }

    public static void putIsNewWallet(Context context, boolean newWallet) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(NEW_WALLET, newWallet);
        editor.apply();
    }

    public static boolean getPromptDismissed(Context context, String promptName) {
        SharedPreferences settingsToGet = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settingsToGet.getBoolean(PROMPT_PREFIX + promptName, false);
    }

    public static void putPromptDismissed(Context context, String promptName, boolean dismissed) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PROMPT_PREFIX + promptName, dismissed);
        editor.apply();
    }

    public static String getTrustNode(Context context, String iso) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(TRUST_NODE_PREFIX + iso.toUpperCase(), "");
    }

    public static void putTrustNode(Context context, String iso, String trustNode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(TRUST_NODE_PREFIX + iso.toUpperCase(), trustNode);
        editor.apply();
    }


    public static boolean wasAppBackgroundedFromHome(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(APP_BACKGROUNDED_FROM_HOME, true);

    }

    public static void putAppBackgroundedFromHome(Context context, boolean fromHome) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(APP_BACKGROUNDED_FROM_HOME, fromHome);
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

    public static void putIsSegwitEnabled(Context context, boolean isEnabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SEGWIT, isEnabled);
        editor.apply();
    }

    public static boolean getIsSegwitEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(SEGWIT, false);
    }

    public static void putEmailOptIn(Context context, boolean hasOpted) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(EMAIL_OPT_IN, hasOpted);
        editor.apply();
    }

    public static boolean getEmailOptIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(EMAIL_OPT_IN, false);
    }

    public static void putRewardsAnimationShown(Context context, boolean wasShown) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(REWARDS_ANIMATION_SHOWN, wasShown);
        editor.apply();
    }

    public static boolean getRewardsAnimationShown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(REWARDS_ANIMATION_SHOWN, false);
    }

    public static void putEmailOptInDismissed(Context context, boolean dismissed) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(EMAIL_OPT_IN_DISMISSED, dismissed);
        editor.apply();
    }

    public static boolean getEmailOptInDismissed(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(EMAIL_OPT_IN_DISMISSED, false);
    }

    /**
     * Get the debug bundle from shared preferences or empty if not available.
     *
     * @param context    Execution context.
     * @param bundleType Bundle type.
     * @return Saved debug bundle or empty.
     */
    public static String getDebugBundle(Context context, ServerBundlesHelper.Type bundleType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(DEBUG_SERVER_BUNDLE + bundleType.name(), "");
    }

    /**
     * Save the bundle to use in debug mode.
     *
     * @param context    Execution context.
     * @param bundleType Bundle type.
     * @param bundle     Debug bundle.
     */
    public static void putDebugBundle(Context context, ServerBundlesHelper.Type bundleType, String bundle) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(DEBUG_SERVER_BUNDLE + bundleType.name(), bundle).apply();
    }

    /**
     * Get the port that was used to start the HTTPServer.
     *
     * @param context Execution context.
     * @return The last port used to start the HTTPServer.
     */
    public static int getHttpServerPort(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(HTTP_SERVER_PORT, 0);
    }

    /**
     * Save the port used to start the HTTPServer.
     *
     * @param context Execution context.
     * @param port    Port used when starting the HTTPServer
     */
    public static void putHttpServerPort(Context context, int port) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(HTTP_SERVER_PORT, port).apply();
    }

}
