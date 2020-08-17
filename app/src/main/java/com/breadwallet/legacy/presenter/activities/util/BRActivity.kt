/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 5/23/17.
 * Copyright (c) 2017 breadwallet LLC
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
package com.breadwallet.legacy.presenter.activities.util

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.app.BreadApp.Companion.setBreadContext
import com.breadwallet.legacy.presenter.activities.DisabledActivity
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.legacy.wallet.wallets.bitcoin.WalletBitcoinManager
import com.breadwallet.tools.animation.BRDialog
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs.getScreenHeight
import com.breadwallet.tools.manager.BRSharedPrefs.putAppBackgroundedFromHome
import com.breadwallet.tools.manager.BRSharedPrefs.putScreenHeight
import com.breadwallet.tools.manager.BRSharedPrefs.putScreenWidth
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.tools.security.BRKeyStore
import com.breadwallet.tools.security.PostAuth
import com.breadwallet.tools.security.isWalletDisabled
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.MainActivity
import com.breadwallet.ui.recovery.RecoveryKeyActivity
import com.breadwallet.ui.send.SendSheetController
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LOCK_TIMEOUT = 180_000L // 3 minutes in milliseconds
private const val TAG = "BRActivity"

@Suppress("TooManyFunctions")
abstract class BRActivity : AppCompatActivity() {

    private var walletLockJob: Job? = null

    private var locked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveScreenSizesIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        walletLockJob?.cancel()
        (applicationContext as BreadApp).setDelayServerShutdown(false, -1)
    }

    override fun onStart() {
        super.onStart()

        walletLockJob?.cancel()
        walletLockJob = null
        if (locked) {
            lockApp()
            locked = false
        }
    }

    override fun onStop() {
        super.onStop()
        if (this !is MainActivity) {
            walletLockJob = GlobalScope.launch(Main) {
                delay(LOCK_TIMEOUT)
                locked = true
            }
        }
        if (this is MainActivity) putAppBackgroundedFromHome(this, true)
    }

    override fun onResume() {
        super.onResume()
        init()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BRConstants.CAMERA_REQUEST_ID -> {
                // Received permission result for camera permission.
                Log.i(
                    TAG,
                    "Received response for CAMERA_REQUEST_ID permission request."
                )
                // Check if the only required permission has been granted.
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO: Remove this logic
                } else {
                    Log.i(TAG, "CAMERA permission was NOT granted.")
                    BRDialog.showSimpleDialog(
                        this,
                        getString(R.string.Send_cameraUnavailabeTitle_android),
                        getString(R.string.Send_cameraUnavailabeMessage_android)
                    )
                }
            }
            // Check if the only required permission has been granted.
            QRUtils.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_ID ->
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Write storage permission has been granted, preview can be displayed.
                    Log.i(TAG, "WRITE permission has now been granted.")
                    // No longer supported, handle this result in the controller: QRUtils.share(this);
                }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BRConstants.PAY_REQUEST_CODE -> {
                (applicationContext as BreadApp).setDelayServerShutdown(false, requestCode)
                if (resultCode == Activity.RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks()
                        .execute { PostAuth.getInstance().onPublishTxAuth(this@BRActivity, null, true, null) }
                }
            }
            BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) {
                BRExecutor.getInstance().forLightWeightBackgroundTasks()
                    .execute { PostAuth.getInstance().onPaymentProtocolRequest(this@BRActivity, true, null) }
            }
        }
    }

    private fun init() { //show wallet locked if it is and we're not in an illegal activity.
        if (this !is RecoveryKeyActivity) {
            if (isWalletDisabled(this)) {
                showWalletDisabled()
            }
        }
        setBreadContext(this)
    }

    private fun saveScreenSizesIfNeeded() {
        if (getScreenHeight(this) == 0) {
            Log.d(TAG, "saveScreenSizesIfNeeded: saving screen sizes.")
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            putScreenHeight(this, size.y)
            putScreenWidth(this, size.x)
        }
    }

    private fun lockApp() {
        if (this !is DisabledActivity && this !is MainActivity) {
            if (BRKeyStore.getPinCode(this).isNotEmpty()) {
                UiUtils.startBreadActivity(this, true)
            }
        }
    }

    /**
     * Start DisabledActivity.
     */
    fun showWalletDisabled() {
        if (this !is DisabledActivity) {
            UiUtils.showWalletDisabled(this)
        }
    }

    /**
     * Check if there is an overlay view over the screen, if an
     * overlay view is found the event won't be dispatched and
     * a dialog with a warning will be shown.
     *
     * @param event The touch screen event.
     * @return boolean Return true if this event was consumed or if an overlay view was found.
     */
    protected fun checkOverlayAndDispatchTouchEvent(event: MotionEvent): Boolean {
        // Filter obscured touches by consuming them.
        if (event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0) {
            if (event.action == MotionEvent.ACTION_UP) {
                BRDialog.showSimpleDialog(
                    this, getString(R.string.Android_screenAlteringTitle),
                    getString(R.string.Android_screenAlteringMessage)
                )
            }
            return true
        }
        return super.dispatchTouchEvent(event)
    }
}
