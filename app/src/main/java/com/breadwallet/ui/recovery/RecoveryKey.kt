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

object RecoveryKey {

    enum class Mode {
        RECOVER, WIPE, RESET_PIN
    }

    /** Represents a screen that allows users to enter a BIP39 mnemonic. */
    data class M(
        /**
         * [Mode] determines the operation to execute on the phrase.
         * [Mode.RECOVER] will initialize the wallet.
         * [Mode.WIPE] will clear all wallet data and reset the app.
         * [Mode.RESET_PIN] will clear the current pin and set a new one.
         */
        val mode: Mode,
        /**
         * A 12 item list of words that make up a BIP39 mnemonic.
         * All 12 items are an empty string by default.
         */
        val phrase: List<String> = List(RECOVERY_KEY_WORDS_COUNT) { "" },
        /**
         * A 12 item list of the validation state of the corresponding word in [phrase].
         * All 12 items are false by default.
         */
        val errors: List<Boolean> = List(RECOVERY_KEY_WORDS_COUNT) { false },
        /** True when user input should be blocked and navigation prevented. */
        val isLoading: Boolean = false,
        /** The list index of the currently selected word input or -1 if none is selected. */
        val focusedWordIndex: Int = 0
    ) {

        companion object {
            const val RECOVERY_KEY_WORDS_COUNT = 12

            fun createDefault(mode: Mode) =
                M(mode = mode)

            fun createWithOptionalPhrase(mode: Mode, phrase: String?) =
                M(
                    mode = mode,
                    phrase = phrase?.split(" ") ?: List(RECOVERY_KEY_WORDS_COUNT) { "" }
                )
        }

        init {
            require(focusedWordIndex in -1..11) {
                "focusedWordIndex must be in -1..11"
            }
            require(phrase.size == RECOVERY_KEY_WORDS_COUNT) {
                "phrase list must contain 12 items"
            }
            require(errors.size == RECOVERY_KEY_WORDS_COUNT) {
                "errors list must contain 12 items"
            }
        }

        /** Redact all phrase information, keep synced with model changes. */
        override fun toString(): String {
            return "RecoveryKeyModel(mode=${mode.name}, isLoading=$isLoading)"
        }
    }

    @MobiusUpdateSpec(
        prefix = "RecoveryKey",
        baseModel = M::class,
        baseEffect = F::class
    )
    sealed class E {
        data class OnWordChanged(val index: Int, val word: String) : E() {
            init {
                require(index in 0..11) { "Word index must be in 0..11" }
            }

            override fun toString() = "OnWordChanged()"
        }

        data class OnWordValidated(val index: Int, val hasError: Boolean) : E() {
            init {
                require(index in 0..11) { "Word index must be in 0..11" }
            }

            override fun toString() = "OnWordValidated()"
        }

        data class OnFocusedWordChanged(val index: Int) : E() {
            init {
                require(index in -1..11) { "Focused word index must be in -1..11" }
            }

            override fun toString() = "OnFocusedWordChanged()"
        }

        data class OnTextPasted(val text: String) : E() {
            override fun toString() = "OnTextPasted()"
        }

        object OnPhraseInvalid : E()
        object OnPhraseSaved : E()
        object OnPhraseSaveFailed : E()

        object OnPinCleared : E()
        object OnPinSet : E()
        object OnPinSetCancelled : E()

        object OnShowPhraseGranted : E()
        object OnShowPhraseFailed : E()
        object OnRecoveryComplete : E()
        object OnFaqClicked : E()
        object OnNextClicked : E()
    }

    sealed class F {

        object GoToRecoveryKeyFaq : F()
        object SetPinForRecovery : F()
        object GoToLoginForReset : F()
        object SetPinForReset : F()
        object GoToPhraseError : F()

        object ErrorShake : F()

        data class ValidateWord(
            val index: Int,
            val word: String
        ) : F() {
            override fun toString() = "ValidateWord()"
        }

        data class ValidatePhrase(
            val phrase: List<String>
        ) : F() {
            init {
                require(phrase.size == 12) { "phrase must contain 12 words." }
            }

            override fun toString() = "ValidatePhrase()"
        }

        data class Unlink(
            val phrase: List<String>
        ) : F() {
            init {
                require(phrase.size == 12) { "phrase must contain 12 words." }
                require(phrase.all { it.isNotBlank() }) { "phrase cannot contain blank words." }
            }

            override fun toString() = "Unlink()"
        }

        data class ResetPin(
            val phrase: List<String>
        ) : F() {
            init {
                require(phrase.size == 12) { "phrase must contain 12 words." }
                require(phrase.all { it.isNotBlank() }) { "phrase cannot contain blank words." }
            }

            override fun toString() = "ResetPin()"
        }

        data class RecoverWallet(
            val phrase: List<String>
        ) : F() {
            init {
                require(phrase.size == 12) { "phrase must contain 12 words." }
                require(phrase.all { it.isNotBlank() }) { "phrase cannot contain blank words." }
            }

            override fun toString() = "RecoverWallet()"
        }
    }
}
