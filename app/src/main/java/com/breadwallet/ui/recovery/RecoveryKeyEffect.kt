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

sealed class RecoveryKeyEffect {

    object GoToRecoveryKeyFaq : RecoveryKeyEffect()
    object SetPinForRecovery : RecoveryKeyEffect()
    object GoToLoginForReset : RecoveryKeyEffect()
    object SetPinForReset : RecoveryKeyEffect()
    object GoToPhraseError : RecoveryKeyEffect()

    object ErrorShake : RecoveryKeyEffect()

    data class ValidateWord(
            val index: Int,
            val word: String
    ) : RecoveryKeyEffect() {
        override fun toString() = "ValidateWord()"
    }

    data class ValidatePhrase(
            val phrase: List<String>
    ) : RecoveryKeyEffect() {
        init {
            require(phrase.size == 12) { "phrase must contain 12 words." }
        }

        override fun toString() = "ValidatePhrase()"
    }

    data class Unlink(
            val phrase: List<String>
    ) : RecoveryKeyEffect() {
        init {
            require(phrase.size == 12) { "phrase must contain 12 words." }
            require(phrase.all { it.isNotBlank() }) { "phrase cannot contain blank words." }
        }

        override fun toString() = "Unlink()"
    }

    data class ResetPin(
            val phrase: List<String>
    ) : RecoveryKeyEffect() {
        init {
            require(phrase.size == 12) { "phrase must contain 12 words." }
            require(phrase.all { it.isNotBlank() }) { "phrase cannot contain blank words." }
        }

        override fun toString() = "ResetPin()"
    }

    data class RecoverWallet(
            val phrase: List<String>
    ) : RecoveryKeyEffect() {
        init {
            require(phrase.size == 12) { "phrase must contain 12 words." }
            require(phrase.all { it.isNotBlank() }) { "phrase cannot contain blank words." }
        }

        override fun toString() = "RecoverWallet()"
    }
}