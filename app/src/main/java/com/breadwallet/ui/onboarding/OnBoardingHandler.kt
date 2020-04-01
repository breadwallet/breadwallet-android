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
import com.breadwallet.tools.security.BRAccountManager
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.onboarding.OnBoarding.E
import com.breadwallet.ui.onboarding.OnBoarding.F
import com.breadwallet.util.errorHandler
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnBoardingHandler(
    coroutineJob: Job,
    private val breadApp: BreadApp,
    private val breadBox: BreadBox,
    private val accountManager: BRAccountManager,
    private val outputProvider: () -> Consumer<E>,
    private val routerProvider: () -> Router
) : Connection<F>, CoroutineScope {

    override val coroutineContext = coroutineJob + Dispatchers.Default + errorHandler()

    override fun accept(effect: F) {
        when (effect) {
            is F.CreateWallet -> launch { createWallet() }
            F.Cancel -> cancelSetup()
            is F.TrackEvent -> trackEvent(effect)
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

    private fun trackEvent(effect: F.TrackEvent) {
        EventUtils.pushEvent(effect.event)
    }

    private suspend fun createWallet() {
        val phrase = generatePhrase() ?: return

        var account: Account
        try {
            account = accountManager.createAccount(phrase)
        } catch (e: Exception) {
            logError("Error storing wallet data.", e)
            outputProvider().accept(E.SetupError.StoreWalletFailed)
            return
        }

        try {
            breadBox.open(account)

            breadApp.startWithInitializedWallet(breadBox, false)

            outputProvider().accept(E.OnWalletCreated)
        } catch (e: IllegalStateException) {
            logError("Error initializing crypto system", e)
            outputProvider().accept(E.SetupError.CryptoSystemBootError)
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
                outputProvider().accept(E.SetupError.PhraseCreationFailed)
                null
            }
        }
    }
}
