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
package com.breadwallet.ui.onboarding

import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.ui.navigation.OnCompleteAction

object OnBoarding {

    /** Displays informational pages and allows initialization of a wallet. */
    data class M(
        /** The currently selected onboarding page. */
        val page: Int = 1,
        /** True when a wallet is being initialized and navigation must be blocked. */
        val isLoading: Boolean = false,
        /** The user's desired location after the wallet is initialized */
        val pendingTarget: Target = Target.NONE
    ) {
        companion object {
            val DEFAULT = M()
        }

        enum class Target {
            NONE, SKIP, BUY, BROWSE
        }

        val isFirstPage = page == 1
    }

    sealed class E {
        data class OnPageChanged(val page: Int) : E()
        object OnSkipClicked : E()
        object OnBackClicked : E()
        object OnBuyClicked : E()
        object OnBrowseClicked : E()

        object OnWalletCreated : E()

        sealed class SetupError : E() {
            object PhraseCreationFailed : SetupError()

            object StoreWalletFailed : SetupError()

            object CryptoSystemBootError : SetupError()
        }
    }

    sealed class F {
        data class TrackEvent(val event: String) : F()

        object CreateWallet : F()

        data class ShowError(val message: String) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                title = "",
                message = message
            )
        }

        object Skip : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.SetPin(onboarding = true)
        }
        object Buy : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.SetPin(
                onboarding = true,
                onComplete = OnCompleteAction.GO_TO_BUY
            )
        }
        object Browse : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.SetPin(onboarding = true)
        }

        object Cancel : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }
    }
}
