/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 7/11/19.
 * Copyright (c) 2019 breadwallet LLC
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
package com.breadwallet.repository

import android.content.Context
import android.support.v4.app.NotificationManagerCompat
import com.breadwallet.BreadApp
import com.breadwallet.tools.manager.BRSharedPrefs
import com.platform.network.NotificationsSettingsClientImpl

/**
 * Enum used to represent the current state of the notifications settings.
 *  [APP_ENABLED] if receiving push notifications is enabled.
 *  [APP_DISABLED] if receiving push notifications has been disabled on to app.
 *  [SYSTEM_DISABLED] if push notifications has been disabled from the android settings.
 */
enum class NotificationsState { APP_ENABLED, APP_DISABLED, SYSTEM_DISABLED }

/**
 * Repository responsible of push notifications settings.
 */
interface PushNotificationsSettingsRepository {

    /**
     * Enable/Disable receiving push notifications.
     * Return True if the notification setting was successfully updated.
     */
    fun togglePushNotifications(notificationsEnable: Boolean): Boolean

    /**
     * Return the current state of the notifications settings as [NotificationsState]
     */
    fun getNotificationsState(): NotificationsState
}

/**
 * Implementation of [PushNotificationsSettingsRepository] that stores the user preferences in the
 * shared preferences and register/unregister the device token from the backend.
 */
object PushNotificationsSettingsRepositoryImpl : PushNotificationsSettingsRepository {

    private val context: Context get() = BreadApp.getBreadContext()

    override fun togglePushNotifications(notificationsEnable: Boolean): Boolean {
        val token = BRSharedPrefs.getFCMRegistrationToken(context)
        val remoteUpdated = when {
            token.isNullOrBlank() -> // We don't have a token yet, we will update or ignore once we receive one.
                true
            notificationsEnable -> NotificationsSettingsClientImpl.registerToken(context, token)
            else -> NotificationsSettingsClientImpl.unregisterToken(context, token)
        }
        if (remoteUpdated) BRSharedPrefs.putShowNotification(context, notificationsEnable)
        return remoteUpdated
    }

    override fun getNotificationsState(): NotificationsState {
        val appPreferences = BRSharedPrefs.getShowNotification(context)
        val systemPreferences = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return when {
            !systemPreferences -> NotificationsState.SYSTEM_DISABLED
            appPreferences -> NotificationsState.APP_ENABLED
            else -> NotificationsState.APP_DISABLED
        }
    }
}