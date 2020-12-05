/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 1/7/20.
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
package com.breadwallet.ui.settings.currency

import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget

object DisplayCurrency {

    data class M(val currencies: List<String>, val selectedCurrency: String) {
        companion object {
            fun createDefault(): M = M(emptyList(), "")
        }
    }

    sealed class E {
        object OnBackClicked : E()
        object OnFaqClicked : E()
        data class OnCurrencySelected(val currencyCode: String) : E()
        data class OnCurrenciesLoaded(
            val selectedCurrencyCode: String,
            val currencies: List<String>
        ) : E()

        data class OnSelectedCurrencyUpdated(val currencyCode: String) : E()
    }

    sealed class F {
        object LoadCurrencies : F()
        data class SetDisplayCurrency(val currencyCode: String) : F()
        sealed class Nav(
            override val navigationTarget: NavigationTarget
        ) : F(), NavigationEffect {
            object GoBack : Nav(NavigationTarget.Back)
            object GoToFaq : Nav(
                NavigationTarget.SupportPage(BRConstants.FAQ_DISPLAY_CURRENCY)
            )
        }
    }
}
