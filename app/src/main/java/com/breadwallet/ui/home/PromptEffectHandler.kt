/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 11/15/19.
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
package com.breadwallet.ui.home

import android.content.Context
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BRAccountManager
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.home.HomeScreen.E
import com.breadwallet.ui.home.HomeScreen.F
import com.breadwallet.util.errorHandler
import com.breadwallet.util.usermetrics.UserMetricsUtil
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PromptEffectHandler(
    private val output: Consumer<E>,
    private val context: Context,
    private val accountManager: BRAccountManager
) : Connection<F>, CoroutineScope {

    companion object {
        private const val PROMPT_DISMISSED_FINGERPRINT = "fingerprint"
    }

    override val coroutineContext = SupervisorJob() + Dispatchers.Default + errorHandler()

    override fun accept(effect: F) {
        when (effect) {
            F.LoadPrompt -> launch { loadPrompt() }
            F.StartRescan -> rescan()
            is F.DismissPrompt -> dismissPrompt(effect)
            is F.SaveEmail -> saveEmail(effect)
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun loadPrompt() {
        val promptId = when {
            shouldPrompt(PromptItem.RECOMMEND_RESCAN) -> PromptItem.RECOMMEND_RESCAN
            shouldPrompt(PromptItem.UPGRADE_PIN) -> PromptItem.UPGRADE_PIN
            shouldPrompt(PromptItem.PAPER_KEY) -> PromptItem.PAPER_KEY
            shouldPrompt(PromptItem.FINGER_PRINT) -> PromptItem.FINGER_PRINT
            shouldPrompt(PromptItem.EMAIL_COLLECTION) -> PromptItem.EMAIL_COLLECTION
            else -> null
        }
        if (promptId != null) {
            EventUtils.pushEvent(getPromptName(promptId) + EventUtils.EVENT_PROMPT_SUFFIX_DISPLAYED)
        }
        output.accept(E.OnPromptLoaded(promptId))
    }

    private fun shouldPrompt(promptItem: PromptItem): Boolean {
        return when (promptItem) {
            PromptItem.EMAIL_COLLECTION -> !BRSharedPrefs.getEmailOptIn(context)
                && !BRSharedPrefs.getEmailOptInDismissed(context)
            PromptItem.FINGER_PRINT -> (!BRSharedPrefs.unlockWithFingerprint
                && Utils.isFingerprintAvailable(context)
                && !BRSharedPrefs.getPromptDismissed(context, PROMPT_DISMISSED_FINGERPRINT))
            PromptItem.PAPER_KEY -> !BRSharedPrefs.getPhraseWroteDown()
            PromptItem.UPGRADE_PIN -> accountManager.pinCodeNeedsUpgrade()
            PromptItem.RECOMMEND_RESCAN -> false // BRSharedPrefs.getScanRecommended(iso = "BTC")
        }
    }

    private fun dismissPrompt(effect: F.DismissPrompt) {
        when (effect.promptItem) {
            PromptItem.FINGER_PRINT -> {
                BRSharedPrefs.putPromptDismissed(
                    context,
                    PROMPT_DISMISSED_FINGERPRINT,
                    true
                )
            }
            PromptItem.EMAIL_COLLECTION -> {
                BRSharedPrefs.putEmailOptInDismissed(context, true)
            }
        }
        EventUtils.pushEvent(
            getPromptName(effect.promptItem) + EventUtils.EVENT_PROMPT_SUFFIX_DISMISSED
        )
    }

    private fun rescan() {
        TODO("trigger rescan")
        // BRSharedPrefs.putStartHeight(
        //     act,
        //     BRSharedPrefs.getCurrentWalletCurrencyCode(act),
        //     0
        // )
        // val wallet = WalletsMaster.getInstance().getCurrentWallet(act)
        // wallet.rescan(act)
        // BRSharedPrefs.putScanRecommended(
        //     act,
        //     BRSharedPrefs.getCurrentWalletCurrencyCode(act),
        //     false
        // )
    }

    private fun saveEmail(effect: F.SaveEmail) {
        UserMetricsUtil.makeEmailOptInRequest(context, effect.email)
        BRSharedPrefs.putEmailOptIn(context, true)
    }

    private fun getPromptName(prompt: PromptItem): String {
        return when (prompt) {
            PromptItem.FINGER_PRINT -> EventUtils.PROMPT_TOUCH_ID
            PromptItem.PAPER_KEY -> EventUtils.PROMPT_PAPER_KEY
            PromptItem.UPGRADE_PIN -> EventUtils.PROMPT_UPGRADE_PIN
            PromptItem.RECOMMEND_RESCAN -> EventUtils.PROMPT_RECOMMEND_RESCAN
            PromptItem.EMAIL_COLLECTION -> EventUtils.PROMPT_EMAIL
        }
    }
}
