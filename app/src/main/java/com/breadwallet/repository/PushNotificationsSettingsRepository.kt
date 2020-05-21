/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 7/11/19.
 * Copyright (c) 2019 breadwallet LLC
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
package com.breadwallet.repository

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.breadwallet.app.BreadApp
import com.breadwallet.repository.NotificationsState.APP_DISABLED
import com.breadwallet.repository.NotificationsState.APP_ENABLED
import com.breadwallet.repository.NotificationsState.SYSTEM_DISABLED
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
        val token = BRSharedPrefs.getFCMRegistrationToken()
        val remoteUpdated = when {
            token.isNullOrBlank() -> // We don't have a token yet, we will update or ignore once we receive one.
                true
            notificationsEnable -> NotificationsSettingsClientImpl.registerToken(context, token)
            else -> NotificationsSettingsClientImpl.unregisterToken(context, token)
        }
        if (remoteUpdated) BRSharedPrefs.putShowNotification(notificationsEnable)
        return remoteUpdated
    }

    override fun getNotificationsState(): NotificationsState {
        val appPreferences = BRSharedPrefs.getShowNotification()
        val systemPreferences = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return when {
            !systemPreferences -> SYSTEM_DISABLED
            appPreferences -> APP_ENABLED
            else -> APP_DISABLED
        }
    }
}
