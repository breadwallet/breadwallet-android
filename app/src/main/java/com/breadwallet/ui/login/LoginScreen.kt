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

import com.breadwallet.R
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.auth.AuthMode
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import dev.zacsweers.redacted.annotations.Redacted

object LoginScreen {

    data class M(
        val fingerprintEnable: Boolean = false,
        val showHomeScreen: Boolean = true,
        @Redacted val extraUrl: String,
        val isUnlocked: Boolean = false
    ) {
        companion object {
            fun createDefault(
                extraUrl: String,
                showHomeScreen: Boolean
            ) = M(
                extraUrl = extraUrl,
                showHomeScreen = showHomeScreen
            )
        }
    }

    sealed class E {
        object OnFingerprintClicked : E()
        object OnPinLocked : E()
        object OnUnlockAnimationEnd : E()
        data class OnFingerprintEnabled(val enabled: Boolean) : E()

        object OnAuthenticationSuccess : E()
        object OnAuthenticationFailed : E()
    }

    sealed class F {
        object UnlockBrdUser : F()
        object CheckFingerprintEnable : F()
        object AuthenticationSuccess : F(), ViewEffect
        object AuthenticationFailed : F(), ViewEffect
        data class TrackEvent(
            val eventName: String,
            val attributes: Map<String, String>? = null
        ) : F()

        object GoBack : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }
        object GoToHome : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Home
        }
        object ShowFingerprintController : F(), ViewEffect
        object GoToDisableScreen : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.DisabledScreen
        }
        data class GoToDeepLink(
            @Redacted val url: String
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.DeepLink(url, true)
        }
        data class GoToWallet(
            val currencyCode: String
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Wallet(currencyCode)
        }
    }
}
