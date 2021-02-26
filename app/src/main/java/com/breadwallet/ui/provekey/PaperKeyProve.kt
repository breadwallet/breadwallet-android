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
package com.breadwallet.ui.provekey

import com.breadwallet.R
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.util.normalize
import dev.zacsweers.redacted.annotations.Redacted

object PaperKeyProve {

    data class M(
        /** Paper key */
        @Redacted val phrase: List<String>,
        /** Position of the first word to be verified */
        val firstWordIndex: Int,
        /** Position of the second word to be verified */
        val secondWordIndex: Int,
        /** Action to be done when dismissing the flow */
        val onComplete: OnCompleteAction,
        /** Current state of the first input field */
        val firstWordState: WordState = WordState.EMPTY,
        /** Current state of the second input field */
        val secondWordSate: WordState = WordState.EMPTY,
        /** Flag to show animation when the words were validated */
        val showBreadSignal: Boolean = false
    ) {
        companion object {
            private const val WORD_COUNT = 12
            fun createDefault(phrase: List<String>, onComplete: OnCompleteAction): M {
                check(phrase.size == WORD_COUNT) { "Paper key must contain $WORD_COUNT words" }
                val indices = (0 until WORD_COUNT).shuffled()
                val firstWord = indices[0]
                val secondWord = indices[1]
                return M(phrase, firstWord, secondWord, onComplete)
            }
        }

        /** Used to represent the state of the content of an input field */
        enum class WordState { EMPTY, INVALID, VALID }

        val firstWord: String = phrase[firstWordIndex].normalize()
        val secondWord: String = phrase[secondWordIndex].normalize()
    }

    sealed class E {
        object OnSubmitClicked : E()
        object OnBreadSignalShown : E()
        object OnWroteDownKeySaved : E()

        data class OnFirstWordChanged(@Redacted val word: String) : E()
        data class OnSecondWordChanged(@Redacted val word: String) : E()
    }

    sealed class F {
        object GoToHome : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Home
        }
        object GoToBuy : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Buy
        }
        object ShowStoredSignal : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Signal(
                titleResId = R.string.Alerts_paperKeySet,
                messageResId = R.string.Alerts_paperKeySetSubheader,
                iconResId = R.drawable.ic_check_mark_white
            )
        }

        object StoreWroteDownPhrase : F()

        data class ShakeWords(
            val first: Boolean,
            val second: Boolean
        ) : F(), ViewEffect
    }
}
