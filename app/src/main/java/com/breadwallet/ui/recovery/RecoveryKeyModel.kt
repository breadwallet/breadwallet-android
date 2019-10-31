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

/** Represents a screen that allows users to enter a BIP39 mnemonic. */
data class RecoveryKeyModel(
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
    val phrase: List<String> = List(12) { "" },
    /**
     * A 12 item list of the validation state of the corresponding word in [phrase].
     * All 12 items are false by default.
     */
    val errors: List<Boolean> = List(12) { false },
    /** True when user input should be blocked and navigation prevented. */
    val isLoading: Boolean = false,
    /** The list index of the currently selected word input or -1 if none is selected. */
    val focusedWordIndex: Int = 0
) {
    enum class Mode {
        RECOVER, WIPE, RESET_PIN
    }

    companion object {
        fun createDefault(mode: Mode) =
            RecoveryKeyModel(mode = mode)

        fun createWithOptionalPhrase(mode: Mode, phrase: String?) =
            RecoveryKeyModel(
                mode = mode,
                phrase = phrase?.split(" ") ?: List(12) { "" }
            )
    }

    init {
        require(focusedWordIndex in -1..11) {
            "focusedWordIndex must be in -1..11"
        }
        require(phrase.size == 12) {
            "phrase list must contain 12 items"
        }
        require(errors.size == 12) {
            "errors list must contain 12 items"
        }
    }

    /** Redact all phrase information, keep synced with model changes. */
    override fun toString(): String {
        return "RecoveryKeyModel(mode=${mode.name}, isLoading=$isLoading)"
    }
}
