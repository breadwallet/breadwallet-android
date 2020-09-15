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

import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import com.breadwallet.app.BreadApp
import com.breadwallet.crypto.Account
import com.breadwallet.crypto.Key
import com.breadwallet.logger.logError
import com.breadwallet.logger.logInfo
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.security.SetupResult
import com.breadwallet.tools.util.Bip39Reader
import com.breadwallet.tools.util.SupportUtils
import com.breadwallet.ui.recovery.RecoveryKey.E
import com.breadwallet.ui.recovery.RecoveryKey.F
import com.breadwallet.ui.recovery.RecoveryKey.M.Companion.RECOVERY_KEY_WORDS_COUNT
import com.breadwallet.util.asNormalizedString
import drewcarlson.mobius.flow.subtypeEffectHandler
import kotlinx.coroutines.delay

private const val LOADING_WATCH_DELAY = 8_000L

fun createRecoveryKeyHandler(
    breadApp: BreadApp,
    userManager: BrdUserManager
) = subtypeEffectHandler<F, E> {
    addFunction<F.Unlink> { effect ->
        val phraseBytes = effect.phrase.asNormalizedString().toByteArray()
        val storedPhrase = try {
            userManager.getPhrase()
        } catch (e: UserNotAuthenticatedException) {
            return@addFunction E.OnWipeWalletCancelled
        }
        if (storedPhrase?.contentEquals(phraseBytes) == true) {
            E.OnRequestWipeWallet
        } else {
            E.OnPhraseInvalid
        }
    }

    addFunction<F.ResetPin> { effect ->
        val phrase = effect.phrase.asNormalizedString()
        try {
            userManager.clearPinCode(phrase.toByteArray())
            E.OnPinCleared
        } catch (e: Exception) {
            E.OnPhraseInvalid
        }
    }

    addFunction<F.ValidateWord> { effect ->
        E.OnWordValidated(
            index = effect.index,
            hasError = !Bip39Reader.isWordValid(breadApp, effect.word)
        )
    }

    addFunction<F.ValidatePhrase> { effect ->
        E.OnPhraseValidated(List(RECOVERY_KEY_WORDS_COUNT) { i ->
            val word = effect.phrase[i]
            !Bip39Reader.isWordValid(breadApp, word)
        })
    }

    addFunction<F.MonitorLoading> {
        delay(LOADING_WATCH_DELAY)
        E.OnLoadingCompleteExpected
    }

    addAction<F.ContactSupport> {
        SupportUtils.submitEmailFromOnboarding(breadApp)
    }

    addFunction<F.RecoverWallet> { effect ->
        val phraseBytes = effect.phrase.asNormalizedString().toByteArray()
        val words = breadApp.findWordsForPhrase(phraseBytes)
        if (words == null) {
            logInfo("Phrase validation failed.")
            E.OnPhraseInvalid
        } else {
            try {
                when (val result = userManager.setupWithPhrase(phraseBytes)) {
                    SetupResult.Success -> {
                        logInfo("Wallet recovered.")
                        BRSharedPrefs.putPhraseWroteDown(check = true)
                        E.OnRecoveryComplete
                    }
                    else -> {
                        logError("Error recovering wallet from phrase: $result")
                        E.OnPhraseInvalid
                    }
                }
            } catch (e: Exception) {
                logError("Error opening BreadBox", e)
                // TODO: Define initialization error
                E.OnPhraseInvalid
            }
        }
    }
}

/**
 * Returns the list of words for the language resulting in
 * a successful [Account.validatePhrase] call or null if
 * the phrase is invalid.
 */
private fun Context.findWordsForPhrase(phraseBytes: ByteArray): List<String>? {
    return Bip39Reader.SupportedLanguage.values()
        .asSequence()
        .mapNotNull { l ->
            val words = Bip39Reader.getBip39Words(this, l.toString())
            if (Account.validatePhrase(phraseBytes, words)) {
                BRSharedPrefs.recoveryKeyLanguage = l.toString()
                Key.setDefaultWordList(words)
                words
            } else null
        }.firstOrNull()
}