/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 9/23/19.
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
import io.hypno.switchboard.MobiusUpdateSpec

private const val PIN_LENGTH = 6

object InputPin {

    data class M(
        val mode: Mode = Mode.NEW,
        val pin: String = "",
        val pinConfirmation: String = "",
        val pinUpdateMode: Boolean = false,
        val skipWriteDownKey: Boolean = false,
        val onComplete: OnCompleteAction
    ) {

        companion object {
            fun createDefault(
                pinUpdateMode: Boolean,
                onComplete: OnCompleteAction,
                skipWriteDownKey: Boolean
            ) = M(
                pinUpdateMode = pinUpdateMode,
                onComplete = onComplete,
                skipWriteDownKey = skipWriteDownKey
            )
        }

        enum class Mode {
            VERIFY,  // Verify the old pin
            NEW,     // Chose a new pin
            CONFIRM  // Confirm the new pin
        }

        override fun toString(): String {
            return "InputPinModel(mode=$mode, " +
                "pin='***', " +
                "pinConfirmation='***', " +
                "pinUpdateMode=$pinUpdateMode, " +
                "skipWriteDownKey=$skipWriteDownKey, " +
                "onComplete=$onComplete)"
        }
    }

    @MobiusUpdateSpec(
        prefix = "InputPin",
        baseModel = M::class,
        baseEffect = F::class
    )
    sealed class E {

        object OnFaqClicked : E()
        object OnPinLocked : E()
        object OnPinSaved : E()
        object OnPinSaveFailed : E()

        data class OnPinEntered(
            val pin: String,
            val isPinCorrect: Boolean
        ) : E() {

            override fun toString() = "OnPinEntered()"
        }

        data class OnPinCheck(
            val hasPin: Boolean
        ) : E() {
            override fun toString() = "OnPinCheck()"
        }
    }

    sealed class F {

        data class SetupPin(
            val pin: String
        ) : F() {
            init {
                require(pin.length == PIN_LENGTH) {
                    "pin must contain $PIN_LENGTH digits"
                }
            }

            override fun toString() = "SetupPin()"
        }

        object GoToHome : F()
        object GoToFaq : F()
        object GoToDisabledScreen : F()
        object ErrorShake : F()
        object CheckIfPinExists : F()

        data class GoToWriteDownKey(val onComplete: OnCompleteAction) : F()
    }
}
