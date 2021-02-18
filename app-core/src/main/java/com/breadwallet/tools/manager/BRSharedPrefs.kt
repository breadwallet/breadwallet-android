/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/13/16.
 * Copyright (c) 2016 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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
import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.breadwallet.app.Conversion
import com.breadwallet.model.PriceAlert
import com.breadwallet.tools.util.Bip39Reader
import com.breadwallet.tools.util.ServerBundlesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
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
    private const val LAST_GIFT_CHECK_TIME = "lastGiftCheckTime"

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
    private const val DEBUG_HOST = "debug_host"
    private const val DEBUG_SERVER_BUNDLE = "debug_server_bundle"
    private const val DEBUG_WEB_PLATFORM_URL = "debug_web_platform_url"
    private const val HTTP_SERVER_PORT = "http_server_port"
    private const val REWARDS_ANIMATION_SHOWN = "rewardsAnimationShown"
    private const val READ_IN_APP_NOTIFICATIONS = "readInAppNotifications"
    private const val PRICE_ALERTS = "priceAlerts"
    private const val PRICE_ALERTS_INTERVAL = "priceAlertsInterval"
    private const val LANGUAGE = "language"
    private const val UNLOCK_WITH_FINGERPRINT = "unlock-with-fingerprint"
    private const val CONFIRM_SEND_WITH_FINGERPRINT = "confirm-send-with-fingerprint"
    private const val TRACKED_TRANSACTIONS = "tracked-transactions"
    private const val APP_RATE_PROMPT_DONT_ASK_AGAIN = "app-rate-prompt-dont-ask-again"
    private const val APP_RATE_PROMPT_SHOULD_PROMPT = "app-rate-prompt-should-prompt"
    private const val APP_RATE_PROMPT_SHOULD_PROMPT_DEBUG = "app-rate-prompt-should-prompt-debug"
    const val APP_FOREGROUNDED_COUNT = "appForegroundedCount"
    const val APP_RATE_PROMPT_HAS_RATED = "appReviewPromptHasRated"

    private val secureTimeFlow = MutableSharedFlow<Long>(replay = 1)

    /**
     * Call when Application is initialized to setup [brdPrefs].
     * This removes the need for a context parameter.
     */
    fun initialize(context: Context, applicationScope: CoroutineScope) {
        brdPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applicationScope.launch {
            _trackedConversionChanges.value = getTrackedConversions()

            secureTimeFlow.tryEmit(getSecureTime())
        }
    }

    private lateinit var brdPrefs: SharedPreferences
    private val promptChangeChannel = BroadcastChannel<Unit>(Channel.CONFLATED)

    var lastGiftCheckTime: Long
        get() = brdPrefs.getLong(LAST_GIFT_CHECK_TIME, 0L)
        set(value) {
            brdPrefs.edit { putLong(LAST_GIFT_CHECK_TIME, value) }
        }

    var phraseWroteDown: Boolean
        get() = brdPrefs.getBoolean(PAPER_KEY_WRITTEN_DOWN, false)
        set(value) {
            brdPrefs.edit { putBoolean(PAPER_KEY_WRITTEN_DOWN, value) }
        }

    @JvmStatic
    fun getPreferredFiatIso(): String =
        brdPrefs.getString(
            CURRENT_CURRENCY, try {
                Currency.getInstance(Locale.getDefault()).currencyCode
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Currency.getInstance(Locale.US).currencyCode
            }
        )!!

    fun putPreferredFiatIso(iso: String) =
        brdPrefs.edit {
            val default = if (iso.equals(Locale.getDefault().isO3Language, ignoreCase = true)) {
                null
            } else iso
            putString(CURRENT_CURRENCY, default)
        }

    fun getReceiveAddress(iso: String): String? =
        brdPrefs.getString(RECEIVE_ADDRESS + iso.toUpperCase(), "")

    fun putReceiveAddress(tmpAddr: String, iso: String) =
        brdPrefs.edit { putString(RECEIVE_ADDRESS + iso.toUpperCase(), tmpAddr) }

    @JvmStatic
    fun getSecureTime() =
        brdPrefs.getLong(SECURE_TIME, System.currentTimeMillis())

    //secure time from the server
    fun putSecureTime(date: Long) {
        brdPrefs.edit { putLong(SECURE_TIME, date) }
        secureTimeFlow.tryEmit(date)
    }

    fun secureTimeFlow(): Flow<Long> {
        return secureTimeFlow
    }

    fun getLastSyncTime(iso: String) =
        brdPrefs.getLong(LAST_SYNC_TIME_PREFIX + iso.toUpperCase(), 0)

    fun putLastSyncTime(iso: String, time: Long) =
        brdPrefs.edit { putLong(LAST_SYNC_TIME_PREFIX + iso.toUpperCase(), time) }

    fun getLastSendTransactionBlockheight(iso: String) =
        brdPrefs.getLong(LAST_SEND_TRANSACTION_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), 0)

    fun putLastSendTransactionBlockheight(
        iso: String,
        blockHeight: Long
    ) = brdPrefs.edit {
        putLong(LAST_SEND_TRANSACTION_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), blockHeight)
    }

    //if the user prefers all in crypto units, not fiat currencies
    fun isCryptoPreferred(context: Context? = null): Boolean =
        brdPrefs.getBoolean(IS_CRYPTO_PREFERRED, false)

    //if the user prefers all in crypto units, not fiat currencies
    fun setIsCryptoPreferred(b: Boolean) =
        brdPrefs.edit { putBoolean(IS_CRYPTO_PREFERRED, b) }

    fun getUseFingerprint(): Boolean =
        brdPrefs.getBoolean(USE_FINGERPRINT, false)

    fun putUseFingerprint(use: Boolean) =
        brdPrefs.edit { putBoolean(USE_FINGERPRINT, use) }

    fun getFeatureEnabled(feature: String): Boolean =
        brdPrefs.getBoolean(feature, false)

    fun putFeatureEnabled(enabled: Boolean, feature: String) =
        brdPrefs.edit { putBoolean(feature, enabled) }

    @JvmStatic
    fun getWalletRewardId(): String? =
        brdPrefs.getString(WALLET_REWARD_ID, null)

    fun putWalletRewardId(id: String) =
        brdPrefs.edit { putString(WALLET_REWARD_ID, id) }

    fun getGeoPermissionsRequested(): Boolean =
        brdPrefs.getBoolean(GEO_PERMISSIONS_REQUESTED, false)

    fun putGeoPermissionsRequested(requested: Boolean) =
        brdPrefs.edit { putBoolean(GEO_PERMISSIONS_REQUESTED, requested) }

    fun getStartHeight(iso: String): Long =
        brdPrefs.getLong(START_HEIGHT_PREFIX + iso.toUpperCase(), 0)

    fun putStartHeight(iso: String, startHeight: Long) =
        brdPrefs.edit { putLong(START_HEIGHT_PREFIX + iso.toUpperCase(), startHeight) }

    fun getLastRescanTime(iso: String): Long =
        brdPrefs.getLong(RESCAN_TIME_PREFIX + iso.toUpperCase(), 0)

    fun putLastRescanTime(iso: String, time: Long) =
        brdPrefs.edit { putLong(RESCAN_TIME_PREFIX + iso.toUpperCase(), time) }

    fun getLastBlockHeight(iso: String): Int =
        brdPrefs.getInt(LAST_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), 0)

    fun putLastBlockHeight(iso: String, lastHeight: Int) =
        brdPrefs.edit {
            putInt(LAST_BLOCK_HEIGHT_PREFIX + iso.toUpperCase(), lastHeight)
        }

    fun getScanRecommended(iso: String): Boolean =
        brdPrefs.getBoolean(SCAN_RECOMMENDED_PREFIX + iso.toUpperCase(), false)

    fun putScanRecommended(iso: String, recommended: Boolean) =
        brdPrefs.edit {
            putBoolean(SCAN_RECOMMENDED_PREFIX + iso.toUpperCase(), recommended)
        }

    @JvmStatic
    fun getDeviceId(): String =
        brdPrefs.run {
            if (contains(USER_ID)) {
                getString(USER_ID, "")!!
            } else {
                UUID.randomUUID().toString().also {
                    edit { putString(USER_ID, it) }
                }
            }
        }

    fun getDebugHost(): String? =
        brdPrefs.getString(DEBUG_HOST, "")

    fun putDebugHost(host: String) =
        brdPrefs.edit { putString(DEBUG_HOST, host) }

    fun clearAllPrefs() = brdPrefs.edit { clear() }

    @JvmStatic
    fun getShowNotification(): Boolean =
        brdPrefs.getBoolean(SHOW_NOTIFICATION, true)

    @JvmStatic
    fun putShowNotification(show: Boolean) =
        brdPrefs.edit { putBoolean(SHOW_NOTIFICATION, show) }

    fun getShareData(): Boolean =
        brdPrefs.getBoolean(SHARE_DATA, true)

    fun putShareData(show: Boolean) =
        brdPrefs.edit { putBoolean(SHARE_DATA, show) }

    fun getPromptDismissed(promptName: String): Boolean =
        brdPrefs.getBoolean(PROMPT_PREFIX + promptName, false)

    fun putPromptDismissed(promptName: String, dismissed: Boolean) =
        brdPrefs.edit { putBoolean(PROMPT_PREFIX + promptName, dismissed) }

    fun getTrustNode(iso: String): String? =
        brdPrefs.getString(TRUST_NODE_PREFIX + iso.toUpperCase(), "")

    fun putTrustNode(iso: String, trustNode: String) =
        brdPrefs.edit { putString(TRUST_NODE_PREFIX + iso.toUpperCase(), trustNode) }

    fun putFCMRegistrationToken(token: String) =
        brdPrefs.edit { putString(FCM_TOKEN, token) }

    fun getFCMRegistrationToken(): String? =
        brdPrefs.getString(FCM_TOKEN, "")

    fun putNotificationId(notificationId: Int) =
        brdPrefs.edit { putInt(NOTIFICATION_ID, notificationId) }

    fun getNotificationId(): Int =
        brdPrefs.getInt(NOTIFICATION_ID, 0)

    fun putScreenHeight(screenHeight: Int) =
        brdPrefs.edit { putInt(SCREEN_HEIGHT, screenHeight) }

    @JvmStatic
    fun getScreenHeight(): Int =
        brdPrefs.getInt(SCREEN_HEIGHT, 0)

    fun putScreenWidth(screenWidth: Int) =
        brdPrefs.edit { putInt(SCREEN_WIDTH, screenWidth) }

    @JvmStatic
    fun getScreenWidth(): Int =
        brdPrefs.getInt(SCREEN_WIDTH, 0)

    @JvmStatic
    fun putBundleHash(bundleName: String, bundleHash: String) =
        brdPrefs.edit { putString(BUNDLE_HASH_PREFIX + bundleName, bundleHash) }

    @JvmStatic
    fun getBundleHash(bundleName: String): String? =
        brdPrefs.getString(BUNDLE_HASH_PREFIX + bundleName, null)

    fun putIsSegwitEnabled(isEnabled: Boolean) =
        brdPrefs.edit { putBoolean(SEGWIT, isEnabled) }

    fun getIsSegwitEnabled(): Boolean =
        brdPrefs.getBoolean(SEGWIT, false)

    fun putEmailOptIn(hasOpted: Boolean) =
        brdPrefs.edit { putBoolean(EMAIL_OPT_IN, hasOpted) }

    fun getEmailOptIn(): Boolean =
        brdPrefs.getBoolean(EMAIL_OPT_IN, false)

    fun putRewardsAnimationShown(wasShown: Boolean) =
        brdPrefs.edit { putBoolean(REWARDS_ANIMATION_SHOWN, wasShown) }

    fun getRewardsAnimationShown(): Boolean =
        brdPrefs.getBoolean(REWARDS_ANIMATION_SHOWN, false)

    fun putEmailOptInDismissed(dismissed: Boolean) =
        brdPrefs.edit { putBoolean(EMAIL_OPT_IN_DISMISSED, dismissed) }

    fun getEmailOptInDismissed(): Boolean =
        brdPrefs.getBoolean(EMAIL_OPT_IN_DISMISSED, false)

    /**
     * Get the debug bundle from shared preferences or empty if not available.
     *
     * @param bundleType Bundle type.
     * @return Saved debug bundle or empty.
     */
    @JvmStatic
    fun getDebugBundle(bundleType: ServerBundlesHelper.Type): String? =
        brdPrefs.getString(DEBUG_SERVER_BUNDLE + bundleType.name, "")

    /**
     * Save the bundle to use in debug mode.
     *
     * @param context    Execution context.
     * @param bundleType Bundle type.
     * @param bundle     Debug bundle.
     */
    @JvmStatic
    fun putDebugBundle(
        bundleType: ServerBundlesHelper.Type,
        bundle: String
    ) = brdPrefs.edit { putString(DEBUG_SERVER_BUNDLE + bundleType.name, bundle) }

    /**
     * Get the web platform debug URL from shared preferences or empty, if not available.
     *
     * @param context Execution context.
     * @return Returns the web platform debug URL or empty.
     */
    @JvmStatic
    fun getWebPlatformDebugURL(): String =
        brdPrefs.getString(DEBUG_WEB_PLATFORM_URL, "")!!

    /**
     * Saves the web platform debug URL to the shared preferences.
     *
     * @param context Execution context.
     * @param webPlatformDebugURL The web platform debug URL to be persisted.
     */
    @JvmStatic
    fun putWebPlatformDebugURL(webPlatformDebugURL: String) =
        brdPrefs.edit { putString(DEBUG_WEB_PLATFORM_URL, webPlatformDebugURL) }

    /**
     * Get the port that was used to start the HTTPServer.
     *
     * @param context Execution context.
     * @return The last port used to start the HTTPServer.
     */
    @JvmStatic
    fun getHttpServerPort(): Int =
        brdPrefs.getInt(HTTP_SERVER_PORT, 0)

    /**
     * Save the port used to start the HTTPServer.
     *
     * @param context Execution context.
     * @param port    Port used when starting the HTTPServer
     */
    @JvmStatic
    fun putHttpServerPort(port: Int) =
        brdPrefs.edit { putInt(HTTP_SERVER_PORT, port) }

    /**
     * Save the given in-app notification id into the collection of read message.
     *
     * @param context        Execution context.
     * @param notificationId The id of the message that has been read.
     */
    fun putReadInAppNotificationId(notificationId: String) {
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
    fun getReadInAppNotificationIds(): Set<String> =
        brdPrefs.getStringSet(READ_IN_APP_NOTIFICATIONS, emptySet()) ?: emptySet()

    /**
     * Save an int with the given key in the shared preferences.
     *
     * @param context Execution context.
     * @param key     The name of the preference.
     * @param value   The new value for the preference.
     */
    fun putInt(key: String, value: Int) =
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
    fun getInt(key: String, defaultValue: Int): Int =
        brdPrefs.getInt(key, defaultValue)

    /**
     * Save an boolean with the given key in the shared preferences.
     *
     * @param context Execution context.
     * @param key     The name of the preference.
     * @param value   The new value for the preference.
     */
    fun putBoolean(key: String, value: Boolean) =
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
    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        brdPrefs.getBoolean(key, defaultValue)

    /**
     * Gets the set of user defined price alerts.
     */
    fun getPriceAlerts(): Set<PriceAlert> = emptySet()

    /**
     * Save a set of user defined price alerts.
     */
    fun putPriceAlerts(priceAlerts: Set<PriceAlert>) = Unit

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

    /** The user's language string as provided by [Locale.getLanguage]. */
    var recoveryKeyLanguage: String
        get() = brdPrefs.getString(LANGUAGE, Locale.getDefault().language)!!
        set(value) {
            val isLanguageValid = Bip39Reader.SupportedLanguage.values().any { lang ->
                lang.toString() == value
            }

            brdPrefs.edit {
                if (isLanguageValid) {
                    putString(LANGUAGE, value)
                } else {
                    putString(LANGUAGE, Bip39Reader.SupportedLanguage.EN.toString())
                }
            }
        }

    /** Preference to unlock the app using the fingerprint sensor */
    var unlockWithFingerprint: Boolean
        get() = brdPrefs.getBoolean(UNLOCK_WITH_FINGERPRINT, getUseFingerprint())
        set(value) = brdPrefs.edit {
            putBoolean(UNLOCK_WITH_FINGERPRINT, value)
        }

    /** Preference to send money using the fingerprint sensor */
    var sendMoneyWithFingerprint: Boolean
        get() = brdPrefs.getBoolean(CONFIRM_SEND_WITH_FINGERPRINT, getUseFingerprint())
        set(value) = brdPrefs.edit {
            putBoolean(CONFIRM_SEND_WITH_FINGERPRINT, value)
        }

    fun preferredFiatIsoChanges() = callbackFlow<String> {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == CURRENT_CURRENCY) offer(getPreferredFiatIso())
        }
        brdPrefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            brdPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    private val _trackedConversionChanges = MutableStateFlow<Map<String, List<Conversion>>?>(null)
    val trackedConversionChanges: Flow<Map<String, List<Conversion>>>
        get() = _trackedConversionChanges
            .filterNotNull()
            .onStart {
                _trackedConversionChanges.value?.let { emit(it) }
            }

    fun getTrackedConversions(): Map<String, List<Conversion>> =
        brdPrefs.getStringSet(TRACKED_TRANSACTIONS, emptySet())!!
            .map { Conversion.deserialize(it) }
            .groupBy(Conversion::currencyCode)

    fun putTrackedConversion(conversion: Conversion) {
        brdPrefs.edit {
            putStringSet(
                TRACKED_TRANSACTIONS,
                brdPrefs.getStringSet(TRACKED_TRANSACTIONS, emptySet())!! + conversion.serialize()
            )
        }
        _trackedConversionChanges.value = getTrackedConversions()
    }

    fun removeTrackedConversion(conversion: Conversion) {
        brdPrefs.edit {
            val conversionStr = conversion.serialize()
            putStringSet(
                TRACKED_TRANSACTIONS,
                brdPrefs.getStringSet(TRACKED_TRANSACTIONS, emptySet())!! - conversionStr
            )
        }
        _trackedConversionChanges.value = getTrackedConversions()
    }

    var appRatePromptShouldPrompt: Boolean
        get() = brdPrefs.getBoolean(APP_RATE_PROMPT_SHOULD_PROMPT, false)
        set(value) = brdPrefs.edit { putBoolean(APP_RATE_PROMPT_SHOULD_PROMPT, value) }
            .also { promptChangeChannel.offer(Unit) }

    var appRatePromptShouldPromptDebug: Boolean
        get() = brdPrefs.getBoolean(APP_RATE_PROMPT_SHOULD_PROMPT_DEBUG, false)
        set(value) = brdPrefs.edit { putBoolean(APP_RATE_PROMPT_SHOULD_PROMPT_DEBUG, value) }
            .also { promptChangeChannel.offer(Unit) }

    var appRatePromptHasRated: Boolean
        get() = brdPrefs.getBoolean(APP_RATE_PROMPT_HAS_RATED, false)
        set(value) = brdPrefs.edit { putBoolean(APP_RATE_PROMPT_HAS_RATED, value) }
            .also { promptChangeChannel.offer(Unit) }

    var appRatePromptDontAskAgain: Boolean
        get() = brdPrefs.getBoolean(APP_RATE_PROMPT_DONT_ASK_AGAIN, false)
        set(value) = brdPrefs.edit { putBoolean(APP_RATE_PROMPT_DONT_ASK_AGAIN, value) }
            .also { promptChangeChannel.offer(Unit) }

    fun promptChanges(): Flow<Unit> =
        promptChangeChannel.asFlow()
            .onStart { emit(Unit) }
}
