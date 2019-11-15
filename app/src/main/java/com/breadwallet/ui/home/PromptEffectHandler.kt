package com.breadwallet.ui.home

import android.content.Context
import com.breadwallet.legacy.presenter.customviews.PinLayout
import com.breadwallet.legacy.wallet.wallets.bitcoin.BaseBitcoinWalletManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BRKeyStore
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.tools.util.Utils
import com.breadwallet.util.usermetrics.UserMetricsUtil
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PromptEffectHandler(
    private val output: Consumer<HomeScreenEvent>,
    private val context: Context
) : Connection<HomeScreenEffect>, CoroutineScope {

    companion object {
        private const val PROMPT_DISMISSED_FINGERPRINT = "fingerprint"
    }

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    override fun accept(effect: HomeScreenEffect) {
        when (effect) {
            HomeScreenEffect.LoadPrompt -> loadPrompt()
            HomeScreenEffect.StartRescan -> rescan()
            is HomeScreenEffect.DismissPrompt -> dismissPrompt(effect)
            is HomeScreenEffect.SaveEmail -> saveEmail(effect)
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
        output.accept(HomeScreenEvent.OnPromptLoaded(promptId))
    }

    private fun shouldPrompt(promptItem: PromptItem): Boolean {
        return when (promptItem) {
            PromptItem.EMAIL_COLLECTION -> !BRSharedPrefs.getEmailOptIn(context)
                && !BRSharedPrefs.getEmailOptInDismissed(context)
            PromptItem.FINGER_PRINT -> (!BRSharedPrefs.unlockWithFingerprint
                && Utils.isFingerprintAvailable(context)
                && !BRSharedPrefs.getPromptDismissed(context, PROMPT_DISMISSED_FINGERPRINT))
            PromptItem.PAPER_KEY -> !BRSharedPrefs.getPhraseWroteDown(context)
            PromptItem.UPGRADE_PIN -> BRKeyStore.getPinCode(context).length != PinLayout.MAX_PIN_DIGITS
            PromptItem.RECOMMEND_RESCAN -> {
                BRSharedPrefs.getScanRecommended(
                    context, BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE
                )
            }
        }
    }

    private fun dismissPrompt(effect: HomeScreenEffect.DismissPrompt) {
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

    private fun saveEmail(effect: HomeScreenEffect.SaveEmail) {
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