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
import com.breadwallet.BreadApp
import com.breadwallet.crypto.Account
import com.breadwallet.crypto.Key
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.AuthManager
import com.breadwallet.tools.security.BRKeyStore
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.Bip39Reader
import com.breadwallet.ui.util.logError
import com.breadwallet.ui.util.logInfo
import com.platform.APIClient
import com.platform.entities.WalletInfoData
import com.platform.tools.KVStoreManager
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.*
import java.text.Normalizer
import java.util.*
import java.util.concurrent.TimeUnit

class RecoveryKeyEffectHandler(
        private val output: Consumer<RecoveryKeyEvent>,
        val goToUnlink: () -> Unit,
        val goToErrorDialog: () -> Unit,
        val errorShake: () -> Unit
) : Connection<RecoveryKeyEffect>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

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
        coroutineContext.cancel()
    }

    private fun validateWord(effect: RecoveryKeyEffect.ValidateWord) {
        val isValid = Bip39Reader.isWordValid(BreadApp.getBreadContext(), effect.word)
        output.accept(RecoveryKeyEvent.OnWordValidated(effect.index, !isValid))
    }

    private fun resetPin(effect: RecoveryKeyEffect.ResetPin) {
        val context = BreadApp.getBreadContext()
        val phrase = normalizePhrase(effect.phrase)

        val storedPhrase = try {
            BRKeyStore.getPhrase(context, BRConstants.SHOW_PHRASE_REQUEST_CODE)
        } catch (e: UserNotAuthenticatedException) {
            logInfo("User not authenticated, attempting authentication before restoring wallet.", e)
            return
        } catch (e: Exception) {
            logError("Error storing phrase", e)
            // TODO: KeyStore read error
            output.accept(RecoveryKeyEvent.OnPhraseInvalid)
            return
        }

        output.accept(when {
            phrase.toByteArray().contentEquals(storedPhrase) -> {
                AuthManager.getInstance().setPinCode(context, "")
                RecoveryKeyEvent.OnPinCleared
            }
            else -> RecoveryKeyEvent.OnPhraseInvalid
        })
    }

    private fun unlink(effect: RecoveryKeyEffect.Unlink) {
        val context = BreadApp.getBreadContext()
        val phrase = normalizePhrase(effect.phrase)

        val storedPhrase = try {
            BRKeyStore.getPhrase(context, BRConstants.SHOW_PHRASE_REQUEST_CODE)
        } catch (e: UserNotAuthenticatedException) {
            logInfo("User not authenticated, attempting authentication before restoring wallet.", e)
            return
        } catch (e: Exception) {
            logError("Error storing phrase", e)
            // TODO: KeyStore read error
            output.accept(RecoveryKeyEvent.OnPhraseInvalid)
            return
        }

        if (phrase.toByteArray().contentEquals(storedPhrase)) {
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

        val phraseBytes = normalizePhrase(effect.phrase).toByteArray()

        val words = findWordsForPhrase(phraseBytes)
        if (words == null) {
            logInfo("Phrase validation failed.")
            output.accept(RecoveryKeyEvent.OnPhraseInvalid)
            return
        }

        val storePhraseSuccess = try {
            BRKeyStore.putPhrase(
                    phraseBytes,
                    context,
                    BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE
            )
        } catch (e: UserNotAuthenticatedException) {
            logInfo("User not authenticated, attempting auth before restoring wallet.", e)
            return
        }

        if (!storePhraseSuccess) {
            logError("Failed to store phrase.")
            output.accept(RecoveryKeyEvent.OnPhraseInvalid) // TODO: Define phrase write error
            return
        }

        BRSharedPrefs.putPhraseWroteDown(check = true)

        val apiKey = Key.createForBIP32ApiAuth(phraseBytes, words).apply {
            if (!isPresent) {
                logError("Failed to create api auth key from phrase.")
                output.accept(RecoveryKeyEvent.OnPhraseInvalid) // TODO: Define unexpected error dialog
                return
            }
        }.get()

        launch(Dispatchers.Main) {
            val creationDate = getWalletCreationDate()

            val uids = BRSharedPrefs.getDeviceId()
            val account = Account.createFromPhrase(phraseBytes, creationDate, uids)
            val accountKey = Key.createFromPhrase(phraseBytes, words).run {
                when {
                    isPresent -> get()
                    else -> {
                        logError("Failed to create key from phrase.")
                        // TODO: Define generic initialization error with retry
                        output.accept(RecoveryKeyEvent.OnPhraseInvalid)
                        return@launch
                    }
                }
            }

            try {
                setupWallet(account, accountKey, apiKey, creationDate, context)
            } catch (e: Exception) {
                logError("Error setting up wallet", e)
                // TODO: Define generic initialization error with retry
                output.accept(RecoveryKeyEvent.OnPhraseInvalid)
                return@launch
            }

            try {
                // TODO: This will be replaced with a cryptosystem initialization handler
                BreadApp.initializeCryptoSystem(context, account)
            } catch (e: Exception) {
                logError("Error initializing crypto system", e)
                output.accept(RecoveryKeyEvent.OnPhraseInvalid) // TODO: To be removed
                return@launch
            }

            updateTokensAndPlatform(context)

            output.accept(RecoveryKeyEvent.OnRecoveryComplete)
        }
    }

    /** Stores [account], [accountKey], [apiKey], and [creationDate] in [BRKeyStore]. */
    private fun setupWallet(account: Account, accountKey: Key, apiKey: Key, creationDate: Date, context: Context) {
        BRKeyStore.putAccount(account, context)
        BRKeyStore.putAuthKey(apiKey.encodeAsPrivate(), context)
        BRKeyStore.putMasterPublicKey(accountKey.encodeAsPublic(), context)
        BRKeyStore.putWalletCreationTime(creationDate.time.toInt(), context)
    }

    // TODO: These operations should occur elsewhere as a side-effect
    //   of the crypto system being initialized.
    private suspend fun updateTokensAndPlatform(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                KVStoreManager.syncTokenList(context)
                APIClient.getInstance(context).updatePlatform()
            } catch (e: Exception) {
                logError("Failed to sync token list or update platform.", e)
            }
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

    /**
     * Sync and return [WalletInfoData] from [KVStoreManager]
     * or null if anything goes wrong.
     * TODO: Signing requests is not supported, we will always fail
     *  here because of APIClient usage.
     */
    private suspend fun fetchWalletInfo(): WalletInfoData? {
        val context = BreadApp.getBreadContext()
        return withContext(Dispatchers.IO) {
            try {
                KVStoreManager.syncWalletInfo(context)
                KVStoreManager.getWalletInfo(context)
            } catch (e: Exception) {
                logError("Failed to sync wallet info", e)
                null
            }
        }
    }

    /**
     * Returns the [Date] of [WalletInfoData.creationDate] or 0
     * when [fetchWalletInfo] returns null.
     */
    private suspend fun getWalletCreationDate() =
            Date(fetchWalletInfo()
                    ?.creationDate
                    ?.toLong()
                    ?.let(TimeUnit.SECONDS::toMillis) ?: 0L)

    private fun normalizePhrase(phrase: List<String>) =
            Normalizer.normalize(phrase.joinToString(" ")
                    .replace("　", " ")
                    .replace("\n", " ")
                    .trim()
                    .replace(" +".toRegex(), " "), Normalizer.Form.NFKD)
}