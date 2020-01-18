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
package com.breadwallet.legacy.presenter.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.breadwallet.repository.NotificationsState
import com.breadwallet.repository.PushNotificationsSettingsRepositoryImpl
import com.breadwallet.tools.mvvm.Resource
import com.breadwallet.tools.threads.executor.BRExecutor

class NotificationSettingsViewModel : ViewModel() {

    val notificationsEnable = MutableLiveData<NotificationsState>()

    fun togglePushNotifications(enable: Boolean): LiveData<Resource<Void>> {
        val state = PushNotificationsSettingsRepositoryImpl.getNotificationsState()
        val resource = MutableLiveData<Resource<Void>>().apply { value = Resource.loading() }

        // Check if we need to update the settings.
        if ((state == NotificationsState.APP_ENABLED && !enable)
                || (state == NotificationsState.APP_DISABLED && enable)) {

            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
                val updated = PushNotificationsSettingsRepositoryImpl.togglePushNotifications(enable)
                if (updated) {
                    resource.postValue(Resource.success())
                } else {
                    resource.postValue(Resource.error())
                }
                notificationsEnable.postValue(PushNotificationsSettingsRepositoryImpl.getNotificationsState())
            }

        } else {
            resource.value = Resource.success()
        }
        return resource
    }

    /**
     * Check what is the current state of the notification settings. This is intended to be called
     * when we return to the settings activity to verify if the notifications are enabled on the
     * OS settings.
     */
    fun refreshState() {
        notificationsEnable.value = PushNotificationsSettingsRepositoryImpl.getNotificationsState()
    }

}
