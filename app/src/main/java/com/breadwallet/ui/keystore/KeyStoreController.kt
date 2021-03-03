/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 5/20/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.ui.keystore

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.app.ShareCompat
import androidx.core.content.getSystemService
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.databinding.ControllerKeystoreBinding
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.security.BrdUserState.KeyStoreInvalid
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.controllers.AlertDialogController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.kodein.di.erased.instance

private const val DIALOG_WIPE = "keystore_wipe"
private const val DIALOG_UNINSTALL = "keystore_uninstall"
private const val DIALOG_LOCK = "keystore_lock"
private const val BRD_SUPPORT_EMAIL = "support@brd.com"
private const val BRD_EMAIL_SUBJECT = "Android Key Store Error"
private const val PACKAGE_PREFIX = "package:"
private const val SET_AUTH_REQ_CODE = 5713

class KeyStoreController(
    args: Bundle? = null
) : BaseController(args), AlertDialogController.Listener {

    private val brdUser by instance<BrdUserManager>()

    @Suppress("unused")
    private val binding by viewBinding(ControllerKeystoreBinding::inflate)

    override fun onAttach(view: View) {
        super.onAttach(view)
        brdUser.stateChanges()
            .onEach { state ->
                when (state) {
                    is KeyStoreInvalid -> showKeyStoreDialog(state)
                    else -> restartApp()
                }
            }
            .flowOn(Dispatchers.Main)
            .launchIn(viewAttachScope)
    }

    override fun onPositiveClicked(
        dialogId: String,
        controller: AlertDialogController,
        result: AlertDialogController.DialogInputResult
    ) {
        when (dialogId) {
            DIALOG_WIPE -> wipeDevice()
            DIALOG_LOCK -> devicePassword()
            DIALOG_UNINSTALL -> uninstall()
        }
    }

    override fun onNegativeClicked(
        dialogId: String,
        controller: AlertDialogController,
        result: AlertDialogController.DialogInputResult
    ) {
        when (dialogId) {
            DIALOG_UNINSTALL, DIALOG_WIPE -> contactSupport()
            DIALOG_LOCK -> checkNotNull(activity).finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SET_AUTH_REQ_CODE && resultCode == Activity.RESULT_OK) {
            checkNotNull(activity).recreate()
        }
    }

    private fun showKeyStoreDialog(state: KeyStoreInvalid) {
        val topController = router.backstack.lastOrNull()?.controller
        val currentDialog = (topController as? AlertDialogController)?.dialogId
        val res = checkNotNull(resources)
        val controller = when (state) {
            KeyStoreInvalid.Wipe -> {
                if (currentDialog == DIALOG_WIPE) return
                AlertDialogController(
                    dialogId = DIALOG_WIPE,
                    dismissible = false,
                    message = res.getString(R.string.Alert_keystore_invalidated_wipe_android),
                    title = res.getString(R.string.Alert_keystore_title_android),
                    positiveText = res.getString(R.string.Button_wipe_android),
                    negativeText = res.getString(R.string.Button_contactSupport_android)
                )
            }
            KeyStoreInvalid.Uninstall -> {
                if (currentDialog == DIALOG_UNINSTALL) return
                AlertDialogController(
                    dialogId = DIALOG_UNINSTALL,
                    dismissible = false,
                    title = res.getString(R.string.Alert_keystore_title_android),
                    message = res.getString(R.string.Alert_keystore_invalidated_uninstall_android),
                    positiveText = res.getString(R.string.Button_uninstall_android),
                    negativeText = res.getString(R.string.Button_contactSupport_android)
                )
            }
            KeyStoreInvalid.Lock -> {
                if (currentDialog == DIALOG_LOCK) return
                AlertDialogController(
                    dialogId = DIALOG_LOCK,
                    dismissible = false,
                    title = res.getString(R.string.JailbreakWarnings_title),
                    message = res.getString(R.string.Prompts_NoScreenLock_body_android),
                    positiveText = res.getString(R.string.Button_securitySettings_android),
                    negativeText = res.getString(R.string.AccessibilityLabels_close)
                )
            }
        }
        val transaction = RouterTransaction.with(controller)
        if (currentDialog.isNullOrBlank()) {
            router.pushController(transaction)
        } else {
            router.replaceTopController(transaction)
        }
    }

    private fun devicePassword() {
        val activity = checkNotNull(activity)
        val intent = Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
        if (intent.resolveActivity(activity.packageManager) == null) {
            logError("showEnableDevicePasswordDialog: Security Settings button failed.")
        } else {
            startActivityForResult(intent, SET_AUTH_REQ_CODE)
        }
    }

    private fun wipeDevice() {
        logDebug("showKeyStoreInvalidDialogAndWipe: Clearing app data.")
        val activity = checkNotNull(activity)
        activity.getSystemService<ActivityManager>()?.clearApplicationUserData()
    }

    private fun uninstall() {
        logError("showKeyStoreInvalidDialogAndUninstall: Uninstalling")
        val activity = checkNotNull(activity)
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse(PACKAGE_PREFIX + BuildConfig.APPLICATION_ID)
        }

        if (intent.resolveActivity(activity.packageManager) == null) {
            logError("showKeyStoreInvalidDialogAndUninstall: Uninstall button failed.")
        } else {
            startActivity(intent)
        }
    }

    private fun contactSupport() {
        val activity = checkNotNull(activity)
        try {
            ShareCompat.IntentBuilder.from(activity)
                .setType("message/rfc822")
                .addEmailTo(BRD_SUPPORT_EMAIL)
                .setSubject(BRD_EMAIL_SUBJECT)
                .startChooser()
        } catch (e: ActivityNotFoundException) {
            logError("No email clients found", e)
            toast(R.string.ErrorMessages_emailUnavailableTitle)
        }
    }

    private fun restartApp() {
        router.setBackstack(emptyList(), null)
        checkNotNull(activity).recreate()
    }
}
