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
import com.breadwallet.tools.security.BRAccountManager
import com.breadwallet.tools.util.Bip39Reader
import com.breadwallet.ui.recovery.RecoveryKey.E
import com.breadwallet.ui.recovery.RecoveryKey.F
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

class RecoveryKeyHandler(
    private val output: Consumer<E>,
    private val breadBox: BreadBox,
    private val accountManager: BRAccountManager,
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
        val phrase = normalizePhrase(effect.phrase)
        retainedProducer().accept(
            try {
                accountManager.clearPinCode(phrase.toByteArray())
                E.OnPinCleared
            } catch (e: Exception) {
                E.OnPhraseInvalid
            }
        )
    }

    private suspend fun unlink(effect: F.Unlink) {
        val phrase = normalizePhrase(effect.phrase)

        val storedPhrase = try {
            accountManager.getPhrase() ?: throw IllegalStateException("null phrase")
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
        val context = BreadApp.getBreadContext()

        val phraseBytes = normalizePhrase(effect.phrase).toByteArray()
        val words = findWordsForPhrase(phraseBytes)
        if (words == null) {
            logInfo("Phrase validation failed.")
            retainedProducer().accept(E.OnPhraseInvalid)
            return
        }

        retainedScope.launch(Dispatchers.Main) {
            try {
                val account = accountManager.recoverAccount(phraseBytes)
                BRSharedPrefs.putPhraseWroteDown(check = true)
                breadBox.open(account)
            } catch (e: Exception) {
                logError("Error opening BreadBox", e)
                // TODO: Define initialization error
                retainedProducer().accept(E.OnPhraseInvalid)
                return@launch
            }

            (context.applicationContext as BreadApp).startWithInitializedWallet(breadBox, false)

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
        val allLocales = Locale.getAvailableLocales().asSequence()

        return (sequenceOf(Locale.getDefault()) + allLocales)
            .map(Locale::getLanguage)
            .map { it to Bip39Reader.getBip39Words(context, it) }
            .firstOrNull { (language, words) ->
                Account.validatePhrase(phraseBytes, words)
                    .also { matched ->
                        if (matched) {
                            BRSharedPrefs.recoveryKeyLanguage = language
                            Key.setDefaultWordList(words)
                        }
                    }
            }?.second
    }

    private fun normalizePhrase(phrase: List<String>) =
        Normalizer.normalize(
            phrase.joinToString(" ")
                .replace("　", " ")
                .replace("\n", " ")
                .trim()
                .replace(" +".toRegex(), " "), Normalizer.Form.NFKD
        )
}
