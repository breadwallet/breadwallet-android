/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/25/19.
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

import io.hypno.switchboard.MobiusUpdateSpec

object LoginScreen {

    data class M(
        val fingerprintEnable: Boolean = false,
        val showHomeScreen: Boolean = true,
        val currentCurrencyCode: String = "",
        val extraUrl: String
    ) {
        companion object {
            fun createDefault(extraUrl: String) = M(extraUrl = extraUrl)
        }
    }

    @MobiusUpdateSpec(
        prefix = "LoginScreen",
        baseModel = M::class,
        baseEffect = F::class
    )
    sealed class E {
        object OnFingerprintClicked : E()
        object OnPinLocked : E()
        object OnUnlockAnimationEnd : E()
        data class OnFingerprintEnabled(val enabled: Boolean) : E()
        data class OnLoginPreferencesLoaded(
            val showHomeScreen: Boolean,
            val currentCurrencyCode: String
        ) : E()

        object OnAuthenticationSuccess : E()
        object OnAuthenticationFailed : E()
    }

    sealed class F {
        object GoToDisableScreen : F()
        object GoToHome : F()
        object CheckFingerprintEnable : F()
        object LoadLoginPreferences : F()
        object ShowFingerprintController : F()
        object AuthenticationSuccess : F()
        object AuthenticationFailed : F()
        data class GoToWallet(val currencyCode: String) : F()
        data class GoToDeepLink(val url: String) : F()
        data class TrackEvent(
            val eventName: String,
            val attributes: Map<String, String>? = null
        ) : F()
    }
}
