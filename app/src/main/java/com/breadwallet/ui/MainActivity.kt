/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/12/19.
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
package com.breadwallet.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import com.bluelinelabs.conductor.ChangeHandlerFrameLayout
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.breadwallet.BreadApp

import com.breadwallet.BuildConfig
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.presenter.entities.CryptoRequest
import com.breadwallet.tools.manager.AppEntryPointHandler
import com.breadwallet.tools.security.BRKeyStore
import com.breadwallet.tools.security.KeyStore
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.login.LoginController
import com.breadwallet.ui.onboarding.IntroController
import com.breadwallet.ui.pin.InputPinController
import com.breadwallet.ui.util.logError

/**
 * The main user entrypoint into the app.
 *
 * This activity serves as a Conductor router host and translates
 * platform events into Mobius events.
 */
class MainActivity : BRActivity() {

    companion object {
        const val EXTRA_DATA = "com.breadwallet.ui.MainActivity.EXTRA_DATA"
        const val EXTRA_CRYPTO_REQUEST = "com.breadwallet.ui.MainActivity.EXTRA_CRYPTO_REQUEST"
        const val EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID =
            "com.breadwallet.ui.MainActivity.EXTRA_PUSH_CAMPAIGN_ID"

        // TODO Remove after refactoring settings into controllers
        private const val EXTRA_OPEN_PIN_UPDATE =
            "com.breadwallet.ui.MainActivity.EXTRA_OPEN_PIN_UPDATE"

        fun openPinUpdate(context: Context) {
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_OPEN_PIN_UPDATE, true)
            })
        }
    }

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!(application as BreadApp).isDeviceStateValid) {
            // Note: Calling isDeviceStateValid will handle user resolution
            //   making it safe to terminate here.
            logError("Device state is invalid.")
            return
        }

        // The view of this activity is nothing more than a Controller host with animation support
        setContentView(ChangeHandlerFrameLayout(this).also { view ->
            router = Conductor.attachRouter(this, view, savedInstanceState)
        })

        // The app is launched, no screen to be restored
        if (!router.hasRootController()) {
            val rootController = when {
                BreadApp.hasWallet() -> {
                    if (BRKeyStore.getPinCode(this).isNotBlank()) {
                        val intentUrl = processIntentData(intent)
                        LoginController(intentUrl)
                    } else {
                        InputPinController()
                    }
                }
                else -> IntroController()
            }
            router.setRoot(
                RouterTransaction.with(rootController)
                    .popChangeHandler(FadeChangeHandler())
                    .pushChangeHandler(FadeChangeHandler())
            )
        } else {
            // TODO: open browser for the new intent
        }

        if (BuildConfig.DEBUG) {
            Utils.printPhoneSpecs(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        router.onActivityResult(requestCode, resultCode, data)
        KeyStore.onActivityResult(requestCode, resultCode)
    }

    override fun onBackPressed() {
        // Defer to controller back-press control before exiting.
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return checkOverlayAndDispatchTouchEvent(ev)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return

        if (intent.hasExtra(EXTRA_OPEN_PIN_UPDATE)) {
            router.pushController(
                RouterTransaction.with(InputPinController(pinUpdate = true))
                    .popChangeHandler(FadeChangeHandler())
                    .pushChangeHandler(FadeChangeHandler())
            )
            return
        }

        val request = intent.getSerializableExtra(EXTRA_CRYPTO_REQUEST)
        if (request is CryptoRequest) {
            intent.removeExtra(EXTRA_CRYPTO_REQUEST)
            // TODO: Create backstack and handle request when Send
            //   fragment is converted to a controller.
        }

        val url = processIntentData(intent)
        if (!url.isNullOrBlank()) {
            AppEntryPointHandler.processDeepLink(this, url)
        }
    }

    /** Process the new intent and return the url to browse if available */
    private fun processIntentData(intent: Intent): String? {
        if (intent.hasExtra(EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID)) {
            val campaignId = intent.getStringExtra(EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID)
            val attributes =
                mapOf<String, String>(EventUtils.EVENT_ATTRIBUTE_CAMPAIGN_ID to campaignId)
            EventUtils.pushEvent(EventUtils.EVENT_MIXPANEL_APP_OPEN, attributes)
            EventUtils.pushEvent(EventUtils.EVENT_PUSH_NOTIFICATION_OPEN)
        }

        var data: String? = intent.getStringExtra(EXTRA_DATA)
        if (data.isNullOrBlank()) {
            data = intent.dataString
        }
        return data
    }
}
