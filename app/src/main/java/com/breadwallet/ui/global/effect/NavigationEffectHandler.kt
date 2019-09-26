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
package com.breadwallet.ui.global.effect

import android.content.Intent
import android.support.v4.app.FragmentActivity
import com.breadwallet.R
import com.breadwallet.presenter.activities.*
import com.breadwallet.presenter.activities.settings.SettingsActivity
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.presenter.customviews.BRDialogView
import com.breadwallet.tools.animation.BRDialog
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.AppEntryPointHandler
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.MainActivity
import com.breadwallet.ui.notification.InAppNotificationActivity
import com.breadwallet.wallet.WalletsMaster
import com.platform.HTTPServer
import com.platform.util.AppReviewPromptManager
import com.spotify.mobius.Connection
import com.spotify.mobius.android.runners.MainThreadWorkRunner
import java.util.HashMap

class NavigationEffectHandler(
        private val activity: BRActivity // TODO: Don't depend on an activity
) : Connection<NavigationEffect>,
        NavigationEffectHandlerSpec {

    companion object {
        // TODO: Find a better place for these constants
        const val BITCOIN_CURRENCY_CODE = "BTC"
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
        val url = String.format(BRConstants.CURRENCY_PARAMETER_STRING_FORMAT,
                HTTPServer.getPlatformUrl(HTTPServer.URL_BUY),
                BITCOIN_CURRENCY_CODE)
        UiUtils.startPlatformBrowser(activity, url)
    }

    override fun goToTrade() {
        UiUtils.startPlatformBrowser(activity, HTTPServer.getPlatformUrl(HTTPServer.URL_TRADE))
    }

    override fun goToMenu() {
        val intent = Intent(activity, SettingsActivity::class.java)
        intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_SETTINGS)
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300)
    }

    override fun goToAddWallet() {
        val intent = Intent(activity, AddWalletsActivity::class.java)
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

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

    override fun goToReceive(effect: NavigationEffect.GoToReceive) {
        UiUtils.showReceiveFragment(activity, true)
    }

    override fun goToTransaction(effect: NavigationEffect.GoToTransaction) = Unit

    override fun goToDeepLink(effect: NavigationEffect.GoToDeepLink) {
        AppEntryPointHandler.processDeepLink(activity, effect.url)
    }

    override fun goToInAppMessage(effect: NavigationEffect.GoToInAppMessage) {
        InAppNotificationActivity.start(activity, effect.inAppMessage)
    }

    override fun goToFaq(effect: NavigationEffect.GoToFaq) {
        val wm = WalletsMaster.getInstance().getCurrentWallet(activity)
        UiUtils.showSupportFragment(activity as FragmentActivity, effect.articleId, wm)
    }

    override fun goToSetPin(effect: NavigationEffect.GoToSetPin) {
        val intent = Intent(activity, InputPinActivity::class.java).apply {
            putExtra(InputPinActivity.EXTRA_PIN_IS_ONBOARDING, effect.onboarding)
            if (effect.buy) {
                putExtra(
                    InputPinActivity.EXTRA_PIN_NEXT_SCREEN,
                    PaperKeyActivity.DoneAction.SHOW_BUY_SCREEN.name
                )
            }
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.apply {
            if (effect.onboarding) {
                finish()
                startActivity(intent)
            } else
                startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE)
        }
    }

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
}
