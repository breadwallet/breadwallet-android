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

import com.breadwallet.app.BreadApp
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.crypto.Account
import com.breadwallet.crypto.Key
import com.breadwallet.logger.logError
import com.breadwallet.logger.logInfo
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.security.SetupResult
import com.breadwallet.tools.util.Bip39Reader
import com.breadwallet.ui.recovery.RecoveryKey.E
import com.breadwallet.ui.recovery.RecoveryKey.F
import com.breadwallet.util.asNormalizedString
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RecoveryKeyHandler(
    private val output: Consumer<E>,
    private val breadApp: BreadApp,
    private val breadBox: BreadBox,
    private val userManager: BrdUserManager,
    private val retainedScope: CoroutineScope,
    private val retainedProducer: () -> Consumer<E>,
    val goToUnlink: () -> Unit,
    val goToErrorDialog: () -> Unit,
    val errorShake: () -> Unit
) : Connection<F>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    override fun accept(effect: F) {
        when (effect) {
            F.ErrorShake -> launch(Dispatchers.Main) { errorShake() }
            F.GoToPhraseError -> goToErrorDialog()
            is F.ResetPin -> retainedScope.launch { resetPin(effect) }
            is F.Unlink -> retainedScope.launch { unlink(effect) }
            is F.RecoverWallet -> retainedScope.launch { recoverWallet(effect) }
            is F.ValidateWord -> validateWord(effect)
            is F.ValidatePhrase -> validatePhrase(effect)
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun validateWord(effect: F.ValidateWord) {
        val isValid = Bip39Reader.isWordValid(BreadApp.getBreadContext(), effect.word)
        output.accept(E.OnWordValidated(effect.index, !isValid))
    }

    private fun validatePhrase(effect: F.ValidatePhrase) {
        val errors = MutableList(RecoveryKey.M.RECOVERY_KEY_WORDS_COUNT) { false }
        effect.phrase.forEachIndexed { index, word ->
            errors[index] = !Bip39Reader.isWordValid(BreadApp.getBreadContext(), word)
        }
        output.accept(E.OnPhraseValidated(errors))
    }

    private suspend fun resetPin(effect: F.ResetPin) {
        val phrase = effect.phrase.asNormalizedString()
        retainedProducer().accept(
            try {
                userManager.clearPinCode(phrase.toByteArray())
                E.OnPinCleared
            } catch (e: Exception) {
                E.OnPhraseInvalid
            }
        )
    }

    private suspend fun unlink(effect: F.Unlink) {
        val phrase = effect.phrase.asNormalizedString()

        val storedPhrase = try {
            userManager.getPhrase() ?: throw IllegalStateException("null phrase")
        } catch (e: Exception) {
            logError("Error storing phrase", e)
            // TODO: BRAccountManager read error
            retainedProducer().accept(E.OnPhraseInvalid)
            return
        }

        if (phrase.toByteArray().contentEquals(storedPhrase)) {
            goToUnlink()
        } else {
            retainedProducer().accept(E.OnPhraseInvalid)
        }
    }

    private suspend fun recoverWallet(effect: F.RecoverWallet) {
        val phraseBytes = effect.phrase.asNormalizedString().toByteArray()
        val words = findWordsForPhrase(phraseBytes)
        if (words == null) {
            logInfo("Phrase validation failed.")
            retainedProducer().accept(E.OnPhraseInvalid)
            return
        }

        retainedScope.launch(Dispatchers.Main) {
            try {
                when (val result = userManager.setupWithPhrase(phraseBytes)) {
                    SetupResult.Success -> logInfo("Wallet recovered.")
                    else -> {
                        logError("Error recovering wallet from phrase: $result")
                        retainedProducer().accept(E.OnPhraseInvalid)
                        return@launch
                    }
                }

                BRSharedPrefs.putPhraseWroteDown(check = true)
            } catch (e: Exception) {
                logError("Error opening BreadBox", e)
                // TODO: Define initialization error
                retainedProducer().accept(E.OnPhraseInvalid)
                return@launch
            }

            retainedProducer().accept(E.OnRecoveryComplete)
        }
    }

    /**
     * Returns the list of words for the language resulting in
     * a successful [Account.validatePhrase] call or null if
     * the phrase is invalid.
     */
    private fun findWordsForPhrase(phraseBytes: ByteArray): List<String>? {
        val context = BreadApp.getBreadContext()
        return Bip39Reader.SupportedLanguage.values()
            .asSequence()
            .mapNotNull { l ->
                val words = Bip39Reader.getBip39Words(context, l.toString())
                if (Account.validatePhrase(phraseBytes, words)) {
                    BRSharedPrefs.recoveryKeyLanguage = l.toString()
                    Key.setDefaultWordList(words)
                    words
                } else null
            }.firstOrNull()
    }
}
