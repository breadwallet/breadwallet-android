/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/10/19.
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
package com.breadwallet.ui.showkey

import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.ui.navigation.OnCompleteAction
import dev.zacsweers.redacted.annotations.Redacted

object ShowPaperKey {
    data class M(
        @Redacted val phrase: List<String>,
        val onComplete: OnCompleteAction?,
        val currentWord: Int = 0,
        val phraseWroteDown: Boolean = false
    ) {
        companion object {
            fun createDefault(
                phrase: List<String>,
                onComplete: OnCompleteAction?,
                phraseWroteDown: Boolean
            ) = M(phrase, onComplete, phraseWroteDown = phraseWroteDown)
        }
    }

    sealed class E {

        object OnNextClicked : E()
        object OnPreviousClicked : E()
        object OnCloseClicked : E()

        data class OnPageChanged(val position: Int) : E()
    }

    sealed class F {

        object GoToHome : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Home
        }
        object GoToBuy : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Buy
        }
        object GoBack : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }
        data class GoToPaperKeyProve(
            @Redacted val phrase: List<String>,
            val onComplete: OnCompleteAction
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.PaperKeyProve(
                phrase,
                onComplete
            )
        }
    }
}
