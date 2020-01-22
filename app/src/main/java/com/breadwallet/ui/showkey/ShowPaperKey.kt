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

import com.breadwallet.ui.navigation.OnCompleteAction
import io.hypno.switchboard.MobiusUpdateSpec

object ShowPaperKey {
    data class M(
        val phrase: List<String>,
        val onComplete: OnCompleteAction,
        val currentWord: Int = 0
    ) {
        companion object {
            fun createDefault(
                phrase: List<String>,
                onComplete: OnCompleteAction
            ) = M(phrase, onComplete)
        }

        override fun toString() =
            "ShowPaperKey.M(phrase=${phrase.size}," +
                " onComplete=$onComplete," +
                " currentWord=$currentWord)"
    }

    @MobiusUpdateSpec(
        prefix = "ShowPaperKey",
        baseModel = M::class,
        baseEffect = F::class
    )
    sealed class E {

        object OnNextClicked : E()
        object OnPreviousClicked : E()
        object OnCloseClicked : E()

        data class OnPageChanged(val position: Int) : E()
    }

    sealed class F {

        object GoToHome : F()
        object GoToBuy : F()
        data class GoToPaperKeyProve(
            val phrase: List<String>,
            val onComplete: OnCompleteAction
        ) : F() {
            override fun toString() = "GoToPaperKeyProve"
        }
    }
}
