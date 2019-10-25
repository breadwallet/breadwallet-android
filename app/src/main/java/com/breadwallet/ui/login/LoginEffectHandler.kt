/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 10/25/19.
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
package com.breadwallet.ui.login

import android.content.Context
import com.breadwallet.tools.manager.AppEntryPointHandler
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.isFingerPrintAvailableAndSetup
import com.breadwallet.tools.util.EventUtils
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LoginEffectHandler(
    private val output: Consumer<LoginEvent>,
    private val context: Context,
    private val shakeKeyboard: () -> Unit,
    private val unlockWalletAnimation: () -> Unit,
    private val showFingerprintPrompt: () -> Unit
) : Connection<LoginEffect>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    override fun accept(value: LoginEffect) {
        when (value) {
            LoginEffect.AuthenticationFailed -> launch(Dispatchers.Main) { shakeKeyboard() }
            LoginEffect.CheckFingerprintEnable -> checkFingerprintEnable()
            LoginEffect.LoadLoginPreferences -> loadLoginPreferences()
            LoginEffect.AuthenticationSuccess -> launch(Dispatchers.Main) { unlockWalletAnimation() }
            LoginEffect.ShowFingerprintController -> launch(Dispatchers.Main) { showFingerprintPrompt() }
            is LoginEffect.ProcessUrl -> processUrl(value)
            is LoginEffect.TrackEvent -> trackEvent(value)
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun checkFingerprintEnable() {
        val fingerprintEnable =
            isFingerPrintAvailableAndSetup(context) && BRSharedPrefs.unlockWithFingerprint
        if (fingerprintEnable) {
            launch(Dispatchers.Main) { showFingerprintPrompt() }
        }
        output.accept(LoginEvent.OnFingerprintEnabled(fingerprintEnable))
    }

    private fun loadLoginPreferences() {
        output.accept(
            LoginEvent.OnLoginPreferencesLoaded(
                BRSharedPrefs.wasAppBackgroundedFromHome(),
                BRSharedPrefs.getCurrentWalletCurrencyCode()
            )
        )
    }

    private fun processUrl(effect: LoginEffect.ProcessUrl) {
        AppEntryPointHandler.processDeepLink(
            context,
            effect.url
        )
    }

    private fun trackEvent(event: LoginEffect.TrackEvent) {
        EventUtils.pushEvent(
            event.eventName,
            event.attributes
        )
    }
}