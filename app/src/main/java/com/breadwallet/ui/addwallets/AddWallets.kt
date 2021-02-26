/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 10/11/19.
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
package com.breadwallet.ui.addwallets

import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import dev.zacsweers.redacted.annotations.Redacted

object AddWallets {

    data class M(
        @Redacted val tokens: List<Token> = emptyList(),
        val searchQuery: String = ""
    ) {
        companion object {
            fun createDefault() = M()
        }
    }

    sealed class E {
        data class OnSearchQueryChanged(@Redacted val query: String) : E()
        data class OnTokensChanged(@Redacted val tokens: List<Token>) : E()

        data class OnAddWalletClicked(val token: Token) : E()
        data class OnRemoveWalletClicked(val token: Token) : E()
        object OnBackClicked : E()
    }

    sealed class F {
        data class SearchTokens(@Redacted val query: String) : F()
        data class AddWallet(val token: Token) : F()
        data class RemoveWallet(val token: Token) : F()

        object GoBack : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }
    }
}

data class Token(
    val name: String,
    val currencyCode: String,
    val currencyId: String,
    val startColor: String,
    val enabled: Boolean,
    val removable: Boolean
)
