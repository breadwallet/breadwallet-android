/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/13/19.
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
package com.breadwallet.ui.recovery

import io.hypno.switchboard.MobiusUpdateSpec


@MobiusUpdateSpec(
        baseModel = RecoveryKeyModel::class,
        baseEffect = RecoveryKeyEffect::class)
sealed class RecoveryKeyEvent {
    data class OnWordChanged(val index: Int, val word: String) : RecoveryKeyEvent() {
        init {
            require(index in 0..11) { "Word index must be in 0..11" }
        }

        override fun toString() = "OnWordChanged()"
    }

    data class OnWordValidated(val index: Int, val hasError: Boolean) : RecoveryKeyEvent() {
        init {
            require(index in 0..11) { "Word index must be in 0..11" }
        }

        override fun toString() = "OnWordValidated()"
    }

    data class OnFocusedWordChanged(val index: Int) : RecoveryKeyEvent() {
        init {
            require(index in -1..11) { "Focused word index must be in -1..11" }
        }

        override fun toString() = "OnFocusedWordChanged()"
    }

    data class OnTextPasted(val text: String) : RecoveryKeyEvent() {
        override fun toString() = "OnTextPasted()"
    }

    object OnPhraseInvalid : RecoveryKeyEvent()
    object OnPhraseSaved : RecoveryKeyEvent()
    object OnPhraseSaveFailed : RecoveryKeyEvent()

    object OnPinCleared : RecoveryKeyEvent()
    object OnPinSet : RecoveryKeyEvent()
    object OnPinSetCancelled : RecoveryKeyEvent()

    object OnShowPhraseGranted : RecoveryKeyEvent()
    object OnShowPhraseFailed : RecoveryKeyEvent()
    object OnRecoveryComplete : RecoveryKeyEvent()
    object OnFaqClicked : RecoveryKeyEvent()
    object OnNextClicked : RecoveryKeyEvent()
}