/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 1/21/20.
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
package com.breadwallet.ui.writedownkey

import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.ui.navigation.OnCompleteAction
import dev.zacsweers.redacted.annotations.Redacted

object WriteDownKey {

    data class M(
        val onComplete: OnCompleteAction,
        val requestAuth: Boolean,
        @Redacted val phrase: List<String> = listOf()
    ) {
        companion object {
            fun createDefault(doneAction: OnCompleteAction, requestAuth: Boolean) =
                M(doneAction, requestAuth)
        }
    }

    sealed class E {

        object OnCloseClicked : E()
        object OnFaqClicked : E()
        object OnWriteDownClicked : E()
        object OnGetPhraseFailed : E()
        object OnUserAuthenticated : E()

        data class OnPhraseRecovered(@Redacted val phrase: List<String>) : E()
    }

    sealed class F {
        object GoToFaq : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.SupportPage(BRConstants.FAQ_PAPER_KEY)
        }

        object GoToHome : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Home
        }

        object GoBack : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }

        object GoToBuy : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Buy
        }
        object ShowAuthPrompt : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Authentication()
        }
        object GetPhrase : F()

        data class GoToPaperKey(
            @Redacted val phrase: List<String>,
            val onComplete: OnCompleteAction
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.PaperKey(
                phrase,
                onComplete
            )
        }
    }
}
