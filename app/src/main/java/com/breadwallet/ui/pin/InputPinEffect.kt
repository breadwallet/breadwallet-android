/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 9/23/19.
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
package com.breadwallet.ui.pin

import com.breadwallet.ui.navigation.OnCompleteAction

private const val PIN_LENGTH = 6

sealed class InputPinEffect {

    data class SetupPin(
        val pin: String
    ) : InputPinEffect() {
        init {
            require(pin.length == PIN_LENGTH) {
                "pin must contain $PIN_LENGTH digits"
            }
        }

        override fun toString() = "SetupPin()"
    }

    object GoToHome : InputPinEffect()
    object GoToFaq : InputPinEffect()
    object GoToDisabledScreen : InputPinEffect()
    object ErrorShake : InputPinEffect()
    object CheckIfPinExists : InputPinEffect()

    data class GoToWriteDownKey(val onComplete: OnCompleteAction) : InputPinEffect()
}
