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
package com.breadwallet.ui.settings.fingerprint

import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget

object FingerprintSettings {

    data class M(
        val unlockApp: Boolean = false,
        val sendMoney: Boolean = false,
        val sendMoneyEnable: Boolean = false
    )

    sealed class E {
        object OnBackClicked : E()
        object OnFaqClicked : E()
        data class OnAppUnlockChanged(val enable: Boolean) : E()
        data class OnSendMoneyChanged(val enable: Boolean) : E()
        data class OnSettingsLoaded(
            val unlockApp: Boolean,
            val sendMoney: Boolean
        ) : E()
    }

    sealed class F {
        object LoadCurrentSettings : F()
        data class UpdateFingerprintSetting(
            val unlockApp: Boolean,
            val sendMoney: Boolean
        ) : F()

        object GoBack : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }
        object GoToFaq : F(), NavigationEffect {
            override val navigationTarget =
                NavigationTarget.SupportPage(BRConstants.FAQ_ENABLE_FINGERPRINT)
        }
    }
}
