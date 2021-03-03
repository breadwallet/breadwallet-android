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

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.breadwallet.R
import com.breadwallet.databinding.ControllerNotificationSettingsBinding
import com.breadwallet.logger.logDebug
import com.breadwallet.repository.NotificationsState
import com.breadwallet.tools.mvvm.Status
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.BaseController

/**
 * Activity to turn on and off push notifications.
 */
class NotificationSettingsController : BaseController() {

    private val viewModel by lazy { NotificationSettingsViewModel() }

    private val binding by viewBinding(ControllerNotificationSettingsBinding::inflate)

    init {
        EventUtils.pushEvent(EventUtils.EVENT_PUSH_NOTIFICATIONS_OPEN_APP_SETTINGS)
    }

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        binding.backButton.setOnClickListener {
            router.popCurrentController()
        }
        setListeners()
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        val owner = (activity as AppCompatActivity)
        viewModel.refreshState()
        with(binding) {
            viewModel.notificationsEnable.observe(owner) { state ->
                when (state) {
                    NotificationsState.APP_ENABLED -> {
                        toggleButton.isChecked = true
                        toggleButton.isEnabled = true
                        openSettingsBtn.post { openSettingsBtn.visibility = View.INVISIBLE }
                        currentSettingsDescription.setText(R.string.PushNotifications_enabledBody)
                    }
                    NotificationsState.APP_DISABLED -> {
                        toggleButton.isChecked = false
                        toggleButton.isEnabled = true
                        openSettingsBtn.post { openSettingsBtn.visibility = View.INVISIBLE }
                        currentSettingsDescription.setText(R.string.PushNotifications_disabledBody)
                    }
                    NotificationsState.SYSTEM_DISABLED -> {
                        toggleButton.isChecked = false
                        toggleButton.isEnabled = false
                        openSettingsBtn.post { openSettingsBtn.visibility = View.VISIBLE }
                        currentSettingsDescription.setText(R.string.PushNotifications_enableInstructions)
                    }
                    null -> Unit
                }
            }
        }
    }

    private fun setListeners() {
        // setup compound button
        binding.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            val event = if (isChecked) {
                EventUtils.EVENT_PUSH_NOTIFICATIONS_SETTING_TOGGLE_ON
            } else {
                EventUtils.EVENT_PUSH_NOTIFICATIONS_SETTING_TOGGLE_OFF
            }
            EventUtils.pushEvent(event)
            updateNotifications(isChecked)
        }

        // setup android settings button
        binding.openSettingsBtn.setOnClickListener {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        EventUtils.pushEvent(EventUtils.EVENT_PUSH_NOTIFICATIONS_OPEN_OS_SETTING)
        val packageName = checkNotNull(applicationContext).packageName
        val intent = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                Intent().apply {
                    action = ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(EXTRA_APP_PACKAGE, packageName)
                    addCategory(Intent.CATEGORY_DEFAULT)
                }
            }
            else -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            }
        }
        startActivity(intent)
    }

    private fun updateNotifications(notificationsEnabled: Boolean) {
        val owner = (activity as AppCompatActivity)
        viewModel.togglePushNotifications(notificationsEnabled)
            .observe(owner) { resource ->
                when (resource?.status) {
                    Status.LOADING -> {
                        binding.progressLayout.root.visibility = View.VISIBLE
                        logDebug("Updating notification settings")
                    }
                    Status.ERROR -> {
                        binding.progressLayout.root.visibility = View.GONE
                        logDebug("Failed to update notifications settings")
                        toastLong(R.string.PushNotifications_updateFailed)
                    }
                    Status.SUCCESS -> {
                        binding.progressLayout.root.visibility = View.GONE
                        logDebug("Settings updated")
                    }
                }
            }
    }
}
