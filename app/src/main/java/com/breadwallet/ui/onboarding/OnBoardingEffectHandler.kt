/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/12/19.
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
package com.breadwallet.ui.onboarding

import com.bluelinelabs.conductor.Router
import com.breadwallet.app.BreadApp
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.crypto.Account
import com.breadwallet.crypto.Key
import com.breadwallet.logger.logError
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.KeyStore
import com.breadwallet.tools.util.EventUtils
import com.platform.interfaces.AccountMetaDataProvider
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class OnBoardingEffectHandler(
    coroutineJob: Job,
    private val breadApp: BreadApp,
    private val breadBox: BreadBox,
    private val keyStore: KeyStore,
    private val metadataProvider: AccountMetaDataProvider,
    private val outputProvider: () -> Consumer<OnBoardingEvent>,
    private val routerProvider: () -> Router
) : Connection<OnBoardingEffect>, CoroutineScope {

    override val coroutineContext = coroutineJob + Dispatchers.Default

    override fun accept(effect: OnBoardingEffect) {
        when (effect) {
            is OnBoardingEffect.CreateWallet -> launch { createWallet() }
            OnBoardingEffect.Cancel -> cancelSetup()
            is OnBoardingEffect.TrackEvent -> trackEvent(effect)
        }
    }

    override fun dispose() {
        // NOTE: We cancel coroutineJob in OnBoardingController
    }

    private fun cancelSetup() {
        launch(Dispatchers.Main) {
            routerProvider().popCurrentController()
        }
    }

    private fun trackEvent(effect: OnBoardingEffect.TrackEvent) {
        EventUtils.pushEvent(effect.event)
    }

    private suspend fun createWallet() {
        val phrase = generatePhrase() ?: return

        // Save and load the phrase to ensure everything works
        if (!putPhrase(phrase)) return
        val storedPhrase = loadPhrase() ?: return

        // Create and store account and wallet-info
        val uids = BRSharedPrefs.getDeviceId()
        val creationDate = Date()

        val account = Account.createFromPhrase(storedPhrase, creationDate, uids)

        // Create signing keys
        val words = Key.getDefaultWordList()

        val apiKey = createApiAuthKey(phrase, words) ?: return

        try {
            setupWallet(account, apiKey, creationDate)
        } catch (e: Exception) {
            logError("Error storing wallet data.", e)
            outputProvider().accept(OnBoardingEvent.SetupError.StoreWalletFailed)
        }

        // Note: Ignore write failures
        createAccountMetaData(creationDate)

        try {
            breadBox.open(account)

            breadApp.startWithInitializedWallet(breadBox, false)

            outputProvider().accept(OnBoardingEvent.OnWalletCreated)
        } catch (e: IllegalStateException) {
            logError("Error initializing crypto system", e)
            outputProvider().accept(OnBoardingEvent.SetupError.CryptoSystemBootError)
        }
    }

    private suspend fun generatePhrase(): ByteArray? {
        return withContext(Dispatchers.Default) {
            val words = Key.getDefaultWordList()
            try {
                Account.generatePhrase(words)
                    .also { phrase ->
                        check(Account.validatePhrase(phrase, words)) {
                            "Invalid phrase generated."
                        }
                    }
            } catch (e: Exception) {
                logError("Failed to generate phrase.", e)
                outputProvider().accept(OnBoardingEvent.SetupError.PhraseCreationFailed)
                null
            }
        }
    }

    private suspend fun putPhrase(phrase: ByteArray): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                keyStore.putPhrase(phrase)
                true
            } catch (e: Exception) {
                logError("Failed to store phrase.", e)
                outputProvider().accept(OnBoardingEvent.SetupError.PhraseStoreFailed)
                false
            }
        }
    }

    private suspend fun loadPhrase(): ByteArray? {
        return withContext(Dispatchers.Main) {
            try {
                keyStore.getPhrase()
            } catch (e: Exception) {
                logError("Failed to load phrase.", e)
                outputProvider().accept(OnBoardingEvent.SetupError.PhraseLoadFailed)
                null
            }
        }
    }

    private suspend fun createApiAuthKey(phrase: ByteArray, words: List<String>): Key? {
        return withContext(Dispatchers.Default) {
            Key.createForBIP32ApiAuth(phrase, words).run {
                when {
                    isPresent -> get()
                    else -> {
                        logError("Failed to create api auth key from phrase.")
                        outputProvider().accept(OnBoardingEvent.SetupError.ApiKeyCreationFailed)
                        null
                    }
                }
            }
        }
    }

    /** Stores [account], [apiKey], and [creationDate] in [KeyStore]. */
    private suspend fun setupWallet(account: Account, apiKey: Key, creationDate: Date) {
        keyStore.putAccount(account)
        keyStore.putAuthKey(apiKey.encodeAsPrivate())
        keyStore.putWalletCreationTime(creationDate.time)
    }

    private suspend fun createAccountMetaData(creationDate: Date): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                metadataProvider.create(creationDate)
                true
            } catch (e: Exception) {
                // Note: If we fail to set WalletInfo, let the whole
                // operation be considered a success.
                logError("Failed to store WalletInfoData", e)
                false
            }
        }
    }
}
