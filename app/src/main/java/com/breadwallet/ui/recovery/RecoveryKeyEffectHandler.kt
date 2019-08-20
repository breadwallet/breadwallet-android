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

import android.security.keystore.UserNotAuthenticatedException
import com.breadwallet.BreadApp
import com.breadwallet.core.BRCoreKey
import com.breadwallet.core.BRCoreMasterPubKey
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.AuthManager
import com.breadwallet.tools.security.BRKeyStore
import com.breadwallet.tools.security.SmartValidator
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.util.logError
import com.breadwallet.ui.util.logInfo
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import java.text.Normalizer

class RecoveryKeyEffectHandler(
        private val output: Consumer<RecoveryKeyEvent>,
        val goToUnlink: () -> Unit,
        val goToErrorDialog: () -> Unit,
        val errorShake: () -> Unit
) : Connection<RecoveryKeyEffect> {

    override fun accept(effect: RecoveryKeyEffect) {
        when (effect) {
            RecoveryKeyEffect.ErrorShake -> errorShake()
            RecoveryKeyEffect.GoToPhraseError -> goToErrorDialog()
            is RecoveryKeyEffect.ResetPin -> resetPin(effect)
            is RecoveryKeyEffect.Unlink -> unlink(effect)
            is RecoveryKeyEffect.RecoverWallet -> recoverWallet(effect)
            is RecoveryKeyEffect.ValidateWord -> validateWord(effect)
        }
    }

    override fun dispose() {

    }

    private fun validateWord(effect: RecoveryKeyEffect.ValidateWord) {
        val isValid = SmartValidator.isWordValid(BreadApp.getBreadContext(), effect.word)
        output.accept(RecoveryKeyEvent.OnWordValidated(effect.index, !isValid))
    }

    private fun resetPin(effect: RecoveryKeyEffect.ResetPin) {
        val context = BreadApp.getBreadContext()
        val phrase = normalizePhrase(effect.phrase)

        output.accept(when {
            SmartValidator.isPaperKeyCorrect(phrase, context) -> {
                AuthManager.getInstance().setPinCode(context, "")
                RecoveryKeyEvent.OnPinCleared
            }
            else -> RecoveryKeyEvent.OnPhraseInvalid
        })
    }

    private fun unlink(effect: RecoveryKeyEffect.Unlink) {
        val context = BreadApp.getBreadContext()
        val phrase = normalizePhrase(effect.phrase)

        if (SmartValidator.isPaperKeyCorrect(phrase, context)) {
            goToUnlink()
        } else {
            output.accept(RecoveryKeyEvent.OnPhraseInvalid)
        }
    }

    // TODO: This method serves as a collection point for all
    //  of the necessary steps in creating a wallet from a phrase.
    //  After all operations have been collected and reviewed,
    //  this function can be broken down into smaller pieces.
    private fun recoverWallet(effect: RecoveryKeyEffect.RecoverWallet) {
        val context = BreadApp.getBreadContext()
        val phrase = normalizePhrase(effect.phrase)

        if (!SmartValidator.isPaperKeyValid(context, phrase)) {
            logInfo("Paper key validation failed.")
            output.accept(RecoveryKeyEvent.OnPhraseInvalid)
            return
        }

        val phraseBytes = phrase.toByteArray()

        val storePhraseSuccess = try {
            BRKeyStore.putPhrase(
                    phraseBytes,
                    context,
                    BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE
            )
        } catch (e: UserNotAuthenticatedException) {
            // TODO: BRKeyStore.putPhrase launches an activity for result to authenticate the user.
            //   The host Controller for this effect handler, will turn the result into a successful
            //   event that repeats this operation.
            logInfo("User not authenticated, attempting authentication before restoring wallet.", e)
            return
        } catch (e: Exception) {
            logError("Error storing phrase", e)
            // TODO: use non-phrase related error
            output.accept(RecoveryKeyEvent.OnPhraseInvalid)
            return
        }
        if (!storePhraseSuccess) {
            logError("Failed to store phrase.")
            // TODO: use non-phrase related error
            output.accept(RecoveryKeyEvent.OnPhraseInvalid)
            return
        }
        BreadApp.initializeCryptoSystem(phrase)

        try {
            val seed = BRCoreKey.getSeedFromPhrase(phraseBytes)
            val authKey = BRCoreKey.getAuthPrivKeyForAPI(seed)
            BRKeyStore.putAuthKey(authKey, context)

            BRSharedPrefs.putPhraseWroteDown(check = true)

            // Recover wallet-info and token list before starting to sync wallets.
            // TODO: KVStoreManager.syncWalletInfo(context)
            // TODO: KVStoreManager.syncTokenList(context)

            val mpk = BRCoreMasterPubKey(phraseBytes, true)
            BRKeyStore.putMasterPublicKey(mpk.serialize(), context)

            // TODO: APIClient.getInstance(getApplication()).updatePlatform()

            output.accept(RecoveryKeyEvent.OnRecoveryComplete)
        } catch (e: Exception) {
            logError("Error creating wallet", e)
            //TODO: Define specific error event
            output.accept(RecoveryKeyEvent.OnPhraseInvalid)
        }
    }

    private fun normalizePhrase(phrase: List<String>) =
            Normalizer.normalize(phrase.joinToString(" ")
                    .replace("　", " ")
                    .replace("\n", " ")
                    .trim()
                    .replace(" +".toRegex(), " "), Normalizer.Form.NFKD)
}