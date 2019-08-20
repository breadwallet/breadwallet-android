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
import com.breadwallet.presenter.activities.LoginActivity
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.onboarding.IntroController

/**
 * The main user entrypoint into the app.
 *
 * This activity serves as a Conductor router host and translates
 * platform events into Mobius events.
 */
class MainActivity : BRActivity() {

    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The view of this activity is nothing more than a Controller host with animation support
        setContentView(ChangeHandlerFrameLayout(this).also { view ->
            router = Conductor.attachRouter(this, view, savedInstanceState)
        })

        if (!(application as BreadApp).isDeviceStateValid) {
            // Note: Calling isDeviceStateValid will handle user resolution
            //   making it safe to terminate here.
            return
        }

        // The app is launched, no screen to be restored
        if (!router.hasRootController()) {
            if (BreadApp.hasWallet(applicationContext)) {
                // Wallet exists, launch login activity as normal
                startActivity(Intent(applicationContext, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
                })
                finish()
            } else {
                // No wallet, launch IntroController
                val rootController = when {
                    // TODO: We would display home controller instead of the old activity
                    //   BreadApp.hasWallet(applicationContext) -> LoginController()
                    else -> IntroController()
                }
                router.setRoot(RouterTransaction.with(rootController)
                        .popChangeHandler(FadeChangeHandler())
                        .pushChangeHandler(FadeChangeHandler()))
            }
        }

        // TODO: Move this: Print once when app is first launched in debug mode.
        if (BuildConfig.DEBUG) {
            Utils.printPhoneSpecs(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        router.onActivityResult(requestCode, resultCode, data)
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
}
