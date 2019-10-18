/**
 * BreadWallet
 *
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> 8/1/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.navigation

import android.content.Intent
import android.support.v4.app.FragmentActivity
import com.breadwallet.R
import com.breadwallet.legacy.presenter.activities.settings.AboutActivity
import com.breadwallet.legacy.presenter.activities.settings.DisplayCurrencyActivity
import com.breadwallet.legacy.presenter.activities.settings.FingerprintActivity
import com.breadwallet.legacy.presenter.activities.settings.ImportActivity
import com.breadwallet.legacy.presenter.activities.settings.NodesActivity
import com.breadwallet.legacy.presenter.activities.settings.SegWitActivity
import com.breadwallet.legacy.presenter.activities.settings.ShareDataActivity
import com.breadwallet.legacy.presenter.activities.settings.SyncBlockchainActivity
import com.breadwallet.legacy.presenter.activities.settings.UnlinkActivity
import com.breadwallet.legacy.presenter.activities.util.BRActivity
import com.breadwallet.legacy.presenter.customviews.BRDialogView
import com.breadwallet.legacy.presenter.settings.NotificationsSettingsActivity
import com.breadwallet.legacy.wallet.WalletsMaster
import com.breadwallet.tools.animation.BRDialog
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.AppEntryPointHandler
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.MainActivity
import com.breadwallet.ui.notification.InAppNotificationActivity
import com.breadwallet.ui.send.SendSheetController
import com.platform.HTTPServer
import com.platform.util.AppReviewPromptManager
import com.spotify.mobius.Connection
import com.spotify.mobius.android.runners.MainThreadWorkRunner

class NavigationEffectHandler(
    private val activity: BRActivity // TODO: Don't depend on an activity
) : Connection<NavigationEffect>,
    NavigationEffectHandlerSpec {

    companion object {
        // TODO: Find a better place for these constants
        const val BITCOIN_CURRENCY_CODE = "BTC"
        const val BITCOIN_CASH_CURRENCY_CODE = "BCH"
    }

    private val workRunner = MainThreadWorkRunner.create()

    override fun accept(value: NavigationEffect) {
        // We must run the navigation code on the MainThread.
        // Being an implementation detail we internalize the
        // MainThread usage and ignore the EffectRunner thread.
        workRunner.post {
            patch(value)
        }
    }

    override fun dispose() {
        workRunner.dispose()
    }

    override fun goToWallet(effect: NavigationEffect.GoToWallet) = Unit

    override fun goBack() {
        activity.onBackPressed()
    }

    override fun goToBrdRewards() {
        TODO("Go to brd rewards")
    }

    override fun goToReview() {
        EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_GOOGLE_PLAY_TRIGGERED)
        AppReviewPromptManager.openGooglePlay(activity)
    }

    override fun goToBuy() {
        val url = String.format(
            BRConstants.CURRENCY_PARAMETER_STRING_FORMAT,
            HTTPServer.getPlatformUrl(HTTPServer.URL_BUY),
            BITCOIN_CURRENCY_CODE
        )
        UiUtils.startPlatformBrowser(activity, url)
    }

    override fun goToTrade() {
        UiUtils.startPlatformBrowser(activity, HTTPServer.getPlatformUrl(HTTPServer.URL_TRADE))
    }

    override fun goToMenu(effect: NavigationEffect.GoToMenu) = Unit

    override fun goToSend(effect: NavigationEffect.GoToSend) {
        /* TODO: This will be moved to RouterNavigationEffectHandler when send becomes a controller
        val cryptoRequest = effect.cryptoRequest
        if (!FragmentSend.isIsSendShown()) {
            FragmentSend.setIsSendShown(true)
            Handler().postDelayed({
                var fragmentSend = activity.supportFragmentManager
                        .findFragmentByTag(FragmentSend::class.java.name) as? FragmentSend
                if (fragmentSend == null) {
                    fragmentSend = FragmentSend()
                }

                if (cryptoRequest != null) {
                    val arguments = Bundle()
                    arguments.putSerializable(WalletController.EXTRA_CRYPTO_REQUEST, cryptoRequest)
                    fragmentSend.arguments = arguments
                }

                if (!fragmentSend.isAdded) {
                    activity.supportFragmentManager.beginTransaction()
                            .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                            .add(android.R.id.content, fragmentSend, FragmentSend::class.java.name)
                            .addToBackStack(FragmentSend::class.java.name).commit()
                }
            }, SEND_SHOW_DELAY.toLong())
        }*/
    }

    override fun goToReceive(effect: NavigationEffect.GoToReceive) = Unit

    override fun goToTransaction(effect: NavigationEffect.GoToTransaction) = Unit

    override fun goToDeepLink(effect: NavigationEffect.GoToDeepLink) {
        AppEntryPointHandler.processDeepLink(activity, effect.url)
    }

    override fun goToInAppMessage(effect: NavigationEffect.GoToInAppMessage) {
        InAppNotificationActivity.start(activity, effect.inAppMessage)
    }

    override fun goToFaq(effect: NavigationEffect.GoToFaq) {
        // TODO: Replace with a controller and remove WalletsMaster usage
        val wm = WalletsMaster.getInstance().getCurrentWallet(activity)
        UiUtils.showSupportFragment(activity as FragmentActivity, effect.articleId, wm)
    }

    override fun goToSetPin(effect: NavigationEffect.GoToSetPin) = Unit

    override fun goToHome() = Unit

    override fun goToLogin() {
        activity.startActivity(Intent(activity, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
    }

    override fun goToErrorDialog(effect: NavigationEffect.GoToErrorDialog) {
        BRDialog.showCustomDialog(
            activity,
            effect.title,
            effect.message,
            activity.getString(R.string.AccessibilityLabels_close),
            null,
            BRDialogView.BROnClickListener { brDialogView -> brDialogView.dismissWithAnimation() },
            null,
            null,
            0
        )
    }

    override fun goToAddWallet() = Unit

    override fun goToManageWallets() = Unit

    override fun goToDisabledScreen() {
        UiUtils.showWalletDisabled(activity)
    }

    override fun goToQrScan() {
        UiUtils.openScanner(activity, SendSheetController.QR_SCAN_RC) // TODO: use controller
    }

    override fun goToWriteDownKey(effect: NavigationEffect.GoToWriteDownKey) = Unit

    override fun goToPaperKey(effect: NavigationEffect.GoToPaperKey) = Unit

    override fun goToPaperKeyProve(effect: NavigationEffect.GoToPaperKeyProve) = Unit

    override fun goToGooglePlay() {
        AppReviewPromptManager.openGooglePlay(activity)
    }

    override fun goToAbout() {
        activity.startActivity(Intent(activity, AboutActivity::class.java))
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun goToDisplayCurrency() {
        activity.startActivity(Intent(activity, DisplayCurrencyActivity::class.java))
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun goToNotificationsSettings() {
        NotificationsSettingsActivity.start(activity)
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun goToShareData() {
        activity.startActivity(Intent(activity, ShareDataActivity::class.java))
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun goToFingerprintAuth() {
        activity.startActivity(Intent(activity, FingerprintActivity::class.java))
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun goToWipeWallet() {
        activity.startActivity(Intent(activity, UnlinkActivity::class.java))
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun goToOnboarding() = Unit

    override fun goToImportWallet() {
        activity.startActivity(Intent(activity, ImportActivity::class.java))
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun goToSyncBlockchain() {
        activity.startActivity(Intent(activity, SyncBlockchainActivity::class.java))
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun goToBitcoinNodeSelector() {
        activity.startActivity(Intent(activity, NodesActivity::class.java))
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    override fun goToEnableSegWit() {
        activity.startActivity(Intent(activity, SegWitActivity::class.java))
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }
}
