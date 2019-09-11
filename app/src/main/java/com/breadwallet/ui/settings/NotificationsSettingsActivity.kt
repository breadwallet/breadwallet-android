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
package com.breadwallet.ui.settings

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity
import com.breadwallet.ui.util.viewModel
import kotlinx.android.synthetic.main.activity_notifications_settings.*
import android.os.Build
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.util.Log
import android.widget.Toast
import androidx.work.Operation
import com.breadwallet.R
import com.breadwallet.repository.NotificationsState
import com.breadwallet.tools.mvvm.Status
import com.breadwallet.tools.util.EventUtils


/**
 * Activity to turn on and off push notifications.
 */
class NotificationsSettingsActivity : BaseSettingsActivity() {

    companion object {
        private val TAG = NotificationsSettingsActivity::class.java.simpleName

        fun start(caller: Activity) {
            caller.startActivity(Intent(caller, NotificationsSettingsActivity::class.java))
            EventUtils.pushEvent(EventUtils.EVENT_PUSH_NOTIFICATIONS_OPEN_APP_SETTINGS)
        }
    }

    private val viewModel by viewModel { NotificationsSettingsViewModel() }

    override fun getBackButtonId(): Int = R.id.back_button

    override fun getLayoutId(): Int = R.layout.activity_notifications_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.refreshState()
        viewModel.notificationsEnable.observe(this, Observer { state ->
            when (state) {
                NotificationsState.APP_ENABLED -> {
                    toggle_button.isChecked = true
                    toggle_button.isEnabled = true
                    open_settings_btn.post { open_settings_btn.visibility = View.INVISIBLE }
                    current_settings_description.text = getString(R.string.PushNotifications_enabledBody)
                }
                NotificationsState.APP_DISABLED -> {
                    toggle_button.isChecked = false
                    toggle_button.isEnabled = true
                    open_settings_btn.post { open_settings_btn.visibility = View.INVISIBLE }
                    current_settings_description.text = getString(R.string.PushNotifications_disabledBody)
                }
                NotificationsState.SYSTEM_DISABLED -> {
                    toggle_button.isChecked = false
                    toggle_button.isEnabled = false
                    open_settings_btn.post { open_settings_btn.visibility = View.VISIBLE }
                    current_settings_description.text = getString(R.string.PushNotifications_enableInstructions)
                }
            }
        })
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
    }

    private fun setListeners() {
        // setup compound button
        toggle_button.setOnCheckedChangeListener { _, isChecked ->
            val event = if (isChecked) {
                EventUtils.EVENT_PUSH_NOTIFICATIONS_SETTING_TOGGLE_ON
            } else {
                EventUtils.EVENT_PUSH_NOTIFICATIONS_SETTING_TOGGLE_OFF
            }
            EventUtils.pushEvent(event)
            updateNotifications(isChecked)
        }

        // setup android settings button
        open_settings_btn.setOnClickListener {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        EventUtils.pushEvent(EventUtils.EVENT_PUSH_NOTIFICATIONS_OPEN_OS_SETTING)
        val intent = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                Intent().apply {
                    action = ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(EXTRA_APP_PACKAGE, packageName)
                    addCategory(Intent.CATEGORY_DEFAULT)
                }
            }
            else -> {
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            }
        }
        startActivity(intent)
    }

    private fun updateNotifications(notificationsEnabled: Boolean) {
        viewModel.togglePushNotifications(notificationsEnabled).observe(this, Observer { resource ->
            when (resource?.status) {
                Status.LOADING -> {
                    progress_layout.visibility = View.VISIBLE
                    Log.d(TAG, "Updating notification settings")
                }
                Status.ERROR -> {
                    progress_layout.visibility = View.GONE
                    Log.d(TAG, "Failed to update notifications settings")
                    Toast.makeText(this, R.string.PushNotifications_updateFailed, Toast.LENGTH_LONG).show()
                }
                Status.SUCCESS -> {
                    progress_layout.visibility = View.GONE
                    Log.d(TAG, "Settings updated")
                }
            }
        })
    }
}