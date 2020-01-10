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

data class InputPinModel(
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
        ) = InputPinModel(
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
