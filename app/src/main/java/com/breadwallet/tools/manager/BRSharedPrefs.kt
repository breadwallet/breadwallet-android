/**
 * BreadWallet
 *
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/13/16.
 * Copyright (c) 2016 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.tools.manager

import android.content.Context
import android.content.SharedPreferences
import android.support.annotation.VisibleForTesting
import android.text.format.DateUtils
import android.util.Log
import androidx.core.content.edit
import com.breadwallet.model.FeeOption
import com.breadwallet.model.PriceAlert
import com.breadwallet.repository.asJsonArrayString
import com.breadwallet.repository.fromJsonArrayString

import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.ServerBundlesHelper
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager

import org.json.JSONArray

import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.UUID

// Suppress warnings about context usage, it remains to support legacy coe.
@Suppress("UNUSED_PARAMETER")
object BRSharedPrefs {
    val TAG: String = BRSharedPrefs::class.java.name

    const val PREFS_NAME = "MyPrefsFile"
    private const val FCM_TOKEN = "fcmToken"
    private const val NOTIFICATION_ID = "notificationId"
    private const val SCREEN_HEIGHT = "screenHeight"
    private const val SCREEN_WIDTH = "screenWidth"
    private const val BUNDLE_HASH_PREFIX = "bundleHash_"
    private const val SEGWIT = "segwit"
    private const val EMAIL_OPT_IN = "emailOptIn"
    private const val EMAIL_OPT_IN_DISMISSED = "emailOptInDismissed"
    private const val CURRENT_CURRENCY = "currentCurrency"
    private const val PAPER_KEY_WRITTEN_DOWN = "phraseWritten"
    private const val PREFER_STANDARD_FEE = "favorStandardFee"
    private const val FEE_PREFERENCE = "feePreference"
    @VisibleForTesting
    const val RECEIVE_ADDRESS = "receive_address"
    private const val FEE_RATE = "feeRate"
    private const val ECONOMY_FEE_RATE = "economyFeeRate"
    private const val BALANCE = "balance"
    private const val SECURE_TIME = "secureTime"
    private const val LAST_SYNC_TIME_PREFIX = "lastSyncTime_"
    private const val LAST_RESCAN_MODE_USED_PREFIX = "lastRescanModeUsed_"
    private const val LAST_SEND_TRANSACTION_BLOCK_HEIGHT_PREFIX = "lastSendTransactionBlockheight_"
    private const val FEE_TIME_PREFIX = "feeTime_"
    private const val ALLOW_SPEND_PREFIX = "allowSpend_"
    private const val IS_CRYPTO_PREFERRED = "priceInCrypto"
    private const val USE_FINGERPRINT = "useFingerprint"
    private const val CURRENT_WALLET_CURRENCY_CODE = "currentWalletIso"
    private const val WALLET_REWARD_ID = "walletRewardId"
    private const val GEO_PERMISSIONS_REQUESTED = "geoPermissionsRequested"
    private const val START_HEIGHT_PREFIX = "startHeight_"
    private const val RESCAN_TIME_PREFIX = "rescanTime_"
    private const val LAST_BLOCK_HEIGHT_PREFIX = "lastBlockHeight_"
    private const val SCAN_RECOMMENDED_PREFIX = "scanRecommended_"
    private const val PREFORK_SYNCED = "preforkSynced"
    private const val CURRENCY_UNIT = "currencyUnit"
    private const val USER_ID = "userId"
    private const val SHOW_NOTIFICATION = "showNotification"
    private const val SHARE_DATA = "shareData"
    private const val NEW_WALLET = "newWallet"
    private const val PROMPT_PREFIX = "prompt_"
    private const val TRUST_NODE_PREFIX = "trustNode_"
    private const val APP_BACKGROUNDED_FROM_HOME = "appBackgroundedFromHome"
    private const val DEBUG_HOST = "debug_host"
    private const val DEBUG_SERVER_BUNDLE = "debug_server_bundle"
    private const val DEBUG_WEB_PLATFORM_URL = "debug_web_platform_url"
    private const val HTTP_SERVER_PORT = "http_server_port"
    private const val REWARDS_ANIMATION_SHOWN = "rewardsAnimationShown"
    private const val READ_IN_APP_NOTIFICATIONS = "readInAppNotifications"
    private const val PRICE_ALERTS = "priceAlerts"
    private const val PRICE_ALERTS_INTERVAL = "priceAlertsInterval"
    const val APP_FOREGROUNDED_COUNT = "appForegroundedCount"
    const val APP_RATE_PROMPT_HAS_RATED = "appReviewPromptHasRated"
    const val APP_RATE_PROMPT_HAS_DISMISSED = "appReviewPromptHasDismissed"

    /**
     * Call when Application is initialized to setup [brdPrefs].
     * This removes the need for a context parameter.
     */
    @JvmStatic
    fun provideContext(context: Context) {
        brdPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private lateinit var brdPrefs: SharedPreferences

    @JvmStatic @JvmOverloads
    fun getPreferredFiatIso(context: Context? = null): String? =
            brdPrefs.getString(CURRENT_CURRENCY, try {
                Currency.getInstance(Locale.getDefault()).currencyCode
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Currency.getInstance(Locale.US).currencyCode
            })

    @JvmStatic
    fun putPreferredFiatIso(context: Context? = null, iso: String) =
            brdPrefs.edit {
                val default = if (iso.equals(Locale.getDefault().isO3Language, ignoreCase = true)) {
                    null
                } else iso
                putString(CURRENT_CURRENCY, default)
            }

    @JvmStatic
    fun getPhraseWroteDown(context: Context? = null): Boolean =
            brdPrefs.getBoolean(PAPER_KEY_WRITTEN_DOWN, false)

    @JvmStatic
    fun putPhraseWroteDown(context: Context, check: Boolean) =
            brdPrefs.edit { putBoolean(PAPER_KEY_WRITTEN_DOWN, check) }

    @JvmStatic
    fun getPreferredFeeOption(context: Context, iso: String): String =
            brdPrefs.getString(FEE_PREFERENCE + iso.toUpperCase(), FeeOption.REGULAR.toString())!!

    @JvmStatic
    fun putPreferredFeeOption(context: Context, iso: String, feeOption: FeeOption) =
            brdPrefs.edit {
                putString(FEE_PREFERENCE + iso.toUpperCase(), feeOption.toString())
            }

    @JvmStatic
    fun getReceiveAddress(context: Context? = null, iso: String): String? =
            brdPrefs.getString(RECEIVE_ADDRESS + iso.toUpperCase(), "")

    @JvmStatic
    fun putReceiveAddress(context: Context? = null, tmpAddr: String, iso: String) =
            brdPrefs.edit { putString(RECEIVE_ADDRESS + iso.toUpperCase(), tmpAddr) }

    @JvmStatic
    fun getFeeRate(context: Context, iso: String, feeOption: FeeOption): BigDecimal =
            BigDecimal(brdPrefs.getString(FEE_RATE + iso.toUpperCase() + feeOption.toString(), "0"))

    @JvmStatic
    fun putFeeRate(context: Context, iso: String, fee: BigDecimal, feeOption: FeeOption) =
            brdPrefs.edit {
                putString(FEE_RATE + iso.toUpperCase() + feeOption.toString(), fee.toPlainString())
            }

    @JvmStatic
    fun getSecureTime(context: Context? = null) =
            brdPrefs.getLong(SECURE_TIME, System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS)

    //secure time from the server
    @JvmStatic
    fun putSecureTime(context: Context? = null, date: Long) =
            brdPrefs.edit { putLong(SECURE_TIME, date) }

    @JvmStatic
    fun getLastSyncTime(context: Context? = null, iso: String) =
            brdPrefs.getLong(LAST_SYNC_TIME_PREFIX + iso.toUpperCase(), 0)

    @JvmStatic
    fun putLastSyncTime(context: Context? = null, iso: String, time: Long) =
            brdPrefs.edit { putLong(LAST_SYNC_TIME_PREFIX + iso.toUpperCase(), time) }

    @JvmStatic
    fun getLastRescanModeUsed(context: Context? = null, iso: String): String? =
            brdPrefs.getString(LAST_RESCAN_MODE_USED_PREFIX + iso.toUpperCase(), null)

    @JvmStatic
    fun putLastRescanModeUsed(context: Context? = null, iso: String, mode: String) =
            brdPrefs.edit {
                putString(LAST_RESCAN_MODE_USED_PREFIX + iso.toUpperCase(), mode)
            }

    @JvmStatic
    fun getLastSendTransactionBlockheight(context: Context? = null, iso: String) =
            brdPrefs.getLong(LAST_SEND_TRANSACTION_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), 0)

    @JvmStatic
    fun putLastSendTransactionBlockheight(context: Context? = null, iso: String, blockHeight: Long) =
            brdPrefs.edit {
                putLong(LAST_SEND_TRANSACTION_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), blockHeight)
            }

    @JvmStatic
    fun getFeeTime(context: Context? = null, iso: String): Long =
            brdPrefs.getLong(FEE_TIME_PREFIX + iso.toUpperCase(), 0)

    @JvmStatic
    fun putFeeTime(context: Context? = null, iso: String, feeTime: Long) =
            brdPrefs.edit { putLong(FEE_TIME_PREFIX + iso.toUpperCase(), feeTime) }

    @JvmStatic
    fun getBitIdNonces(context: Context? = null, key: String): List<Int> =
            try {
                val results = JSONArray(brdPrefs.getString(key, null))
                List(results.length()) {
                    results.getInt(it).also { nonce ->
                        Log.d(TAG, "found a nonce: $nonce")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }

    @JvmStatic
    fun putBitIdNonces(context: Context? = null, nonces: List<Int>, key: String) =
            brdPrefs.edit { putString(key, JSONArray().put(nonces).toString()) }

    @JvmStatic
    fun getAllowSpend(context: Context? = null, iso: String): Boolean =
            brdPrefs.getBoolean(ALLOW_SPEND_PREFIX + iso.toUpperCase(), true)

    @JvmStatic
    fun putAllowSpend(context: Context? = null, iso: String, allow: Boolean) =
            brdPrefs.edit { putBoolean(ALLOW_SPEND_PREFIX + iso.toUpperCase(), allow) }

    //if the user prefers all in crypto units, not fiat currencies
    @JvmStatic
    fun isCryptoPreferred(context: Context? = null): Boolean =
            brdPrefs.getBoolean(IS_CRYPTO_PREFERRED, false)

    //if the user prefers all in crypto units, not fiat currencies
    @JvmStatic
    fun setIsCryptoPreferred(context: Context? = null, b: Boolean) =
            brdPrefs.edit { putBoolean(IS_CRYPTO_PREFERRED, b) }

    @JvmStatic
    fun getUseFingerprint(context: Context? = null): Boolean =
            brdPrefs.getBoolean(USE_FINGERPRINT, false)

    @JvmStatic
    fun putUseFingerprint(context: Context? = null, use: Boolean) =
            brdPrefs.edit { putBoolean(USE_FINGERPRINT, use) }

    @JvmStatic
    fun getFeatureEnabled(context: Context? = null, feature: String): Boolean =
            brdPrefs.getBoolean(feature, false)

    @JvmStatic
    fun putFeatureEnabled(context: Context? = null, enabled: Boolean, feature: String) =
            brdPrefs.edit { putBoolean(feature, enabled) }

    @JvmStatic
    fun getCurrentWalletCurrencyCode(context: Context? = null): String =
            brdPrefs.getString(CURRENT_WALLET_CURRENCY_CODE, WalletBitcoinManager.BITCOIN_CURRENCY_CODE)!!

    @JvmStatic
    fun putCurrentWalletCurrencyCode(context: Context? = null, currencyCode: String) =
            brdPrefs.edit { putString(CURRENT_WALLET_CURRENCY_CODE, currencyCode) }

    @JvmStatic
    fun getWalletRewardId(context: Context? = null): String? =
            brdPrefs.getString(WALLET_REWARD_ID, null)

    @JvmStatic
    fun putWalletRewardId(context: Context? = null, id: String) =
            brdPrefs.edit { putString(WALLET_REWARD_ID, id) }

    @JvmStatic
    fun getGeoPermissionsRequested(context: Context? = null): Boolean =
            brdPrefs.getBoolean(GEO_PERMISSIONS_REQUESTED, false)

    @JvmStatic
    fun putGeoPermissionsRequested(context: Context? = null, requested: Boolean) =
            brdPrefs.edit { putBoolean(GEO_PERMISSIONS_REQUESTED, requested) }

    @JvmStatic
    fun getStartHeight(context: Context? = null, iso: String): Long =
            brdPrefs.getLong(START_HEIGHT_PREFIX + iso.toUpperCase(), 0)

    @JvmStatic
    fun putStartHeight(context: Context? = null, iso: String, startHeight: Long) =
            brdPrefs.edit { putLong(START_HEIGHT_PREFIX + iso.toUpperCase(), startHeight) }

    @JvmStatic
    fun getLastRescanTime(context: Context? = null, iso: String): Long =
            brdPrefs.getLong(RESCAN_TIME_PREFIX + iso.toUpperCase(), 0)

    @JvmStatic
    fun putLastRescanTime(context: Context? = null, iso: String, time: Long) =
            brdPrefs.edit { putLong(RESCAN_TIME_PREFIX + iso.toUpperCase(), time) }

    @JvmStatic
    fun getLastBlockHeight(context: Context? = null, iso: String): Int =
            brdPrefs.getInt(LAST_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), 0)

    @JvmStatic
    fun putLastBlockHeight(context: Context? = null, iso: String, lastHeight: Int) =
            brdPrefs.edit {
                putInt(LAST_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), lastHeight)
            }

    @JvmStatic
    fun getScanRecommended(context: Context? = null, iso: String): Boolean =
            brdPrefs.getBoolean(SCAN_RECOMMENDED_PREFIX + iso.toUpperCase(), false)

    @JvmStatic
    fun putScanRecommended(context: Context? = null, iso: String, recommended: Boolean) =
            brdPrefs.edit {
                putBoolean(SCAN_RECOMMENDED_PREFIX + iso.toUpperCase(), recommended)
            }

    @JvmStatic
    fun getBchPreForkSynced(context: Context? = null): Boolean =
            brdPrefs.getBoolean(PREFORK_SYNCED, false)

    @JvmStatic
    fun putBchPreForkSynced(context: Context? = null, synced: Boolean) =
            brdPrefs.edit { putBoolean(PREFORK_SYNCED, synced) }

    // BTC, mBTC, Bits
    //ignore iso, using same denomination for both for now
    @JvmStatic
    fun getCryptoDenomination(context: Context? = null, iso: String): Int =
            brdPrefs.getInt(CURRENCY_UNIT, BRConstants.CURRENT_UNIT_BITCOINS)

    // BTC, mBTC, Bits
    //ignore iso, using same denomination for both for now
    @JvmStatic
    fun putCryptoDenomination(context: Context? = null, iso: String, unit: Int) =
            brdPrefs.edit { putInt(CURRENCY_UNIT, unit) }

    @JvmStatic
    fun getDeviceId(context: Context? = null): String =
            brdPrefs.run {
                if (contains(USER_ID)) {
                    getString(USER_ID, "")!!
                } else {
                    UUID.randomUUID().toString().also {
                        edit { putString(USER_ID, it) }
                    }
                }
            }

    @JvmStatic
    fun getDebugHost(context: Context? = null): String? =
            brdPrefs.getString(DEBUG_HOST, "")

    @JvmStatic
    fun putDebugHost(context: Context? = null, host: String) =
            brdPrefs.edit { putString(DEBUG_HOST, host) }

    @JvmStatic
    fun clearAllPrefs(context: Context? = null) = brdPrefs.edit { clear() }

    @JvmStatic
    fun getShowNotification(context: Context? = null): Boolean =
            brdPrefs.getBoolean(SHOW_NOTIFICATION, true)

    @JvmStatic
    fun putShowNotification(context: Context? = null, show: Boolean) =
            brdPrefs.edit { putBoolean(SHOW_NOTIFICATION, show) }

    @JvmStatic
    fun getShareData(context: Context? = null): Boolean =
            brdPrefs.getBoolean(SHARE_DATA, true)

    @JvmStatic
    fun putShareData(context: Context? = null, show: Boolean) =
            brdPrefs.edit { putBoolean(SHARE_DATA, show) }

    @JvmStatic
    fun isNewWallet(context: Context? = null): Boolean =
            brdPrefs.getBoolean(NEW_WALLET, true)

    @JvmStatic
    fun putIsNewWallet(context: Context? = null, newWallet: Boolean) =
            brdPrefs.edit { putBoolean(NEW_WALLET, newWallet) }

    @JvmStatic
    fun getPromptDismissed(context: Context? = null, promptName: String): Boolean =
            brdPrefs.getBoolean(PROMPT_PREFIX + promptName, false)

    @JvmStatic
    fun putPromptDismissed(context: Context? = null, promptName: String, dismissed: Boolean) =
            brdPrefs.edit { putBoolean(PROMPT_PREFIX + promptName, dismissed) }

    @JvmStatic
    fun getTrustNode(context: Context? = null, iso: String): String? =
            brdPrefs.getString(TRUST_NODE_PREFIX + iso.toUpperCase(), "")

    @JvmStatic
    fun putTrustNode(context: Context? = null, iso: String, trustNode: String) =
            brdPrefs.edit { putString(TRUST_NODE_PREFIX + iso.toUpperCase(), trustNode) }


    @JvmStatic
    fun wasAppBackgroundedFromHome(context: Context? = null): Boolean =
            brdPrefs.getBoolean(APP_BACKGROUNDED_FROM_HOME, true)

    @JvmStatic
    fun putAppBackgroundedFromHome(context: Context? = null, fromHome: Boolean) =
            brdPrefs.edit { putBoolean(APP_BACKGROUNDED_FROM_HOME, fromHome) }

    @JvmStatic
    fun putFCMRegistrationToken(context: Context? = null, token: String) =
            brdPrefs.edit { putString(FCM_TOKEN, token) }

    @JvmStatic
    fun getFCMRegistrationToken(context: Context? = null): String? =
            brdPrefs.getString(FCM_TOKEN, "")

    @JvmStatic
    fun putNotificationId(context: Context? = null, notificationId: Int) =
            brdPrefs.edit { putInt(NOTIFICATION_ID, notificationId) }

    @JvmStatic
    fun getNotificationId(context: Context? = null): Int =
            brdPrefs.getInt(NOTIFICATION_ID, 0)

    @JvmStatic
    fun putScreenHeight(context: Context? = null, screenHeight: Int) =
            brdPrefs.edit { putInt(SCREEN_HEIGHT, screenHeight) }

    @JvmStatic
    fun getScreenHeight(context: Context? = null): Int =
            brdPrefs.getInt(SCREEN_HEIGHT, 0)

    @JvmStatic
    fun putScreenWidth(context: Context? = null, screenWidth: Int) =
            brdPrefs.edit { putInt(SCREEN_WIDTH, screenWidth) }

    @JvmStatic
    fun getScreenWidth(context: Context? = null): Int =
            brdPrefs.getInt(SCREEN_WIDTH, 0)

    @JvmStatic
    fun putBundleHash(context: Context? = null, bundleName: String, bundleHash: String) =
            brdPrefs.edit { putString(BUNDLE_HASH_PREFIX + bundleName, bundleHash) }

    @JvmStatic
    fun getBundleHash(context: Context? = null, bundleName: String): String? =
            brdPrefs.getString(BUNDLE_HASH_PREFIX + bundleName, null)

    @JvmStatic
    fun putIsSegwitEnabled(context: Context? = null, isEnabled: Boolean) =
            brdPrefs.edit { putBoolean(SEGWIT, isEnabled) }

    @JvmStatic
    fun getIsSegwitEnabled(context: Context? = null): Boolean =
            brdPrefs.getBoolean(SEGWIT, false)

    @JvmStatic
    fun putEmailOptIn(context: Context? = null, hasOpted: Boolean) =
            brdPrefs.edit { putBoolean(EMAIL_OPT_IN, hasOpted) }

    @JvmStatic
    fun getEmailOptIn(context: Context? = null): Boolean =
            brdPrefs.getBoolean(EMAIL_OPT_IN, false)

    @JvmStatic
    fun putRewardsAnimationShown(context: Context? = null, wasShown: Boolean) =
            brdPrefs.edit { putBoolean(REWARDS_ANIMATION_SHOWN, wasShown) }

    @JvmStatic
    fun getRewardsAnimationShown(context: Context? = null): Boolean =
            brdPrefs.getBoolean(REWARDS_ANIMATION_SHOWN, false)

    @JvmStatic
    fun putEmailOptInDismissed(context: Context? = null, dismissed: Boolean) =
            brdPrefs.edit { putBoolean(EMAIL_OPT_IN_DISMISSED, dismissed) }

    @JvmStatic
    fun getEmailOptInDismissed(context: Context? = null): Boolean =
            brdPrefs.getBoolean(EMAIL_OPT_IN_DISMISSED, false)

    /**
     * Get the debug bundle from shared preferences or empty if not available.
     *
     * @param context    Execution context.
     * @param bundleType Bundle type.
     * @return Saved debug bundle or empty.
     */
    @JvmStatic
    fun getDebugBundle(context: Context? = null, bundleType: ServerBundlesHelper.Type): String? =
            brdPrefs.getString(DEBUG_SERVER_BUNDLE + bundleType.name, "")

    /**
     * Save the bundle to use in debug mode.
     *
     * @param context    Execution context.
     * @param bundleType Bundle type.
     * @param bundle     Debug bundle.
     */
    @JvmStatic
    fun putDebugBundle(context: Context? = null, bundleType: ServerBundlesHelper.Type, bundle: String) =
            brdPrefs.edit { putString(DEBUG_SERVER_BUNDLE + bundleType.name, bundle) }

    /**
     * Get the web platform debug URL from shared preferences or empty, if not available.
     *
     * @param context Execution context.
     * @return Returns the web platform debug URL or empty.
     */
    @JvmStatic
    fun getWebPlatformDebugURL(context: Context? = null): String =
            brdPrefs.getString(DEBUG_WEB_PLATFORM_URL, "")!!

    /**
     * Saves the web platform debug URL to the shared preferences.
     *
     * @param context Execution context.
     * @param webPlatformDebugURL The web platform debug URL to be persisted.
     */
    @JvmStatic
    fun putWebPlatformDebugURL(context: Context? = null, webPlatformDebugURL: String) =
            brdPrefs.edit { putString(DEBUG_WEB_PLATFORM_URL, webPlatformDebugURL) }

    /**
     * Get the port that was used to start the HTTPServer.
     *
     * @param context Execution context.
     * @return The last port used to start the HTTPServer.
     */
    @JvmStatic
    fun getHttpServerPort(context: Context? = null): Int =
            brdPrefs.getInt(HTTP_SERVER_PORT, 0)

    /**
     * Save the port used to start the HTTPServer.
     *
     * @param context Execution context.
     * @param port    Port used when starting the HTTPServer
     */
    @JvmStatic
    fun putHttpServerPort(context: Context? = null, port: Int) =
            brdPrefs.edit { putInt(HTTP_SERVER_PORT, port) }

    /**
     * Save the given in-app notification id into the collection of read message.
     *
     * @param context        Execution context.
     * @param notificationId The id of the message that has been read.
     */
    @JvmStatic
    fun putReadInAppNotificationId(context: Context? = null, notificationId: String) {
        val readIds = getReadInAppNotificationIds()
        brdPrefs.edit {
            if (!readIds.contains(notificationId)) {
                putStringSet(READ_IN_APP_NOTIFICATIONS, readIds + notificationId)
            }
        }
    }

    /**
     * Get the ids of the in-app notification that has been read.
     *
     * @param context Execution context.
     * @return A set with the ids of the messages that has been read.
     */
    @JvmStatic
    fun getReadInAppNotificationIds(context: Context? = null): Set<String> =
            brdPrefs.getStringSet(READ_IN_APP_NOTIFICATIONS, emptySet()) ?: emptySet()

    /**
     * Save an int with the given key in the shared preferences.
     *
     * @param context Execution context.
     * @param key     The name of the preference.
     * @param value   The new value for the preference.
     */
    @JvmStatic
    fun putInt(context: Context? = null, key: String, value: Int) =
            brdPrefs.edit { putInt(key, value) }

    /**
     * Retrieve an int value from the preferences.
     *
     * @param key          The name of the preference to retrieve.
     * @param defaultValue Value to return if this preference does not exist.
     * @param context      Execution context.
     * @param key          The name of the preference.
     * @param defaultValue The default value to return if not present.
     * @return Returns the preference value if it exists, or defValue.
     */
    @JvmStatic
    fun getInt(context: Context? = null, key: String, defaultValue: Int): Int =
            brdPrefs.getInt(key, defaultValue)

    /**
     * Save an boolean with the given key in the shared preferences.
     *
     * @param context Execution context.
     * @param key     The name of the preference.
     * @param value   The new value for the preference.
     */
    @JvmStatic
    fun putBoolean(context: Context? = null, key: String, value: Boolean) =
            brdPrefs.edit { putBoolean(key, value) }

    /**
     * Retrieve an boolean value from the preferences.
     *
     * @param key          The name of the preference to retrieve.
     * @param defaultValue Value to return if this preference does not exist.
     * @param context      Execution context.
     * @param key          The name of the preference.
     * @param defaultValue The default value to return if not present.
     * @return Returns the preference value if it exists, or defValue.
     */
    @JvmStatic
    fun getBoolean(context: Context? = null, key: String, defaultValue: Boolean): Boolean =
            brdPrefs.getBoolean(key, defaultValue)

    /**
     * Gets the set of user defined price alerts.
     */
    fun getPriceAlerts(): Set<PriceAlert> =
            brdPrefs.getStringSet(PRICE_ALERTS, emptySet())!!
                    .map { PriceAlert.fromJsonArrayString(it) }
                    .toSet()

    /**
     * Save a set of user defined price alerts.
     */
    fun putPriceAlerts(priceAlerts: Set<PriceAlert>) =
            brdPrefs.edit {
                putStringSet(PRICE_ALERTS, priceAlerts.map(PriceAlert::asJsonArrayString).toSet())
            }

    /**
     * Gets the user defined interval in minutes between price
     * alert checks.
     */
    fun getPriceAlertsInterval() =
            brdPrefs.getInt(PRICE_ALERTS_INTERVAL, 15)

    /**
     * Sets the user defined interval in minutes between price
     * alert checks.
     */
    fun putPriceAlertsInterval(interval: Int) =
            brdPrefs.edit { putInt(PRICE_ALERTS_INTERVAL, interval) }
}
