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
import android.os.Bundle
import android.os.Handler
import com.breadwallet.R
import com.breadwallet.model.InAppMessage
import com.breadwallet.presenter.activities.AddWalletsActivity
import com.breadwallet.presenter.activities.BrdWalletActivity
import com.breadwallet.presenter.activities.settings.SettingsActivity
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.presenter.entities.CryptoRequest
import com.breadwallet.presenter.fragments.FragmentSend
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.AppEntryPointHandler
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.notification.InAppNotificationActivity
import com.breadwallet.ui.wallet.WalletActivity
import com.platform.HTTPServer
import com.platform.util.AppReviewPromptManager
import com.spotify.mobius.Connection
import com.spotify.mobius.android.runners.MainThreadWorkRunner
import java.util.HashMap

class NavigationEffectHandler(
        private val activity: BRActivity // TODO: Don't depend on an activity
) : Connection<NavigationEffect> {

    companion object {
        private const val SEND_SHOW_DELAY = 300
        // TODO: Find a better place for these constants
        private const val BITCOIN_CURRENCY_CODE = "BTC"
        private const val BRD_CURRENCY_CODE = "BRD"
    }

    private val workRunner = MainThreadWorkRunner.create()

    override fun accept(value: NavigationEffect) {
        // We must run the navigation code on the MainThread.
        // Being an implementation detail we internalize the
        // MainThread usage and ignore the EffectRunner thread.
        workRunner.post {
            when (value) {
                is NavigationEffect.GoToSend -> goToSend(value.cryptoRequest)
                is NavigationEffect.GoToReceive -> UiUtils.showReceiveFragment(activity, true)
                is NavigationEffect.GoToTransaction -> UiUtils.showTransactionDetails(activity, value.txHash)
                NavigationEffect.GoBack -> activity.onBackPressed()
                NavigationEffect.GoToBrdRewards -> TODO("Go to brd rewards")
                NavigationEffect.GoToReview -> {
                    EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_GOOGLE_PLAY_TRIGGERED)
                    AppReviewPromptManager.openGooglePlay(activity)
                }
                is NavigationEffect.GoToDeepLink -> AppEntryPointHandler.processDeepLink(activity, value.url)
                is NavigationEffect.GoToInAppMessage -> goToInAppMessage(value.inAppMessage)
                is NavigationEffect.GoToWallet -> goToWallet(value.currencyCode)
                NavigationEffect.GoToBuy -> {
                    val url = String.format(BRConstants.CURRENCY_PARAMETER_STRING_FORMAT,
                            HTTPServer.getPlatformUrl(HTTPServer.URL_BUY),
                            BITCOIN_CURRENCY_CODE)
                    UiUtils.startPlatformBrowser(activity, url)
                }
                NavigationEffect.GoToTrade -> UiUtils.startPlatformBrowser(activity, HTTPServer.getPlatformUrl(HTTPServer.URL_TRADE))
                NavigationEffect.GoToMenu -> {
                    val intent = Intent(activity, SettingsActivity::class.java)
                    intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_SETTINGS)
                    activity.startActivity(intent)
                    activity.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300)
                }
                NavigationEffect.GoToAddWallet -> {
                    val intent = Intent(activity, AddWalletsActivity::class.java)
                    activity.startActivity(intent)
                    activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
                }
            }
        }
    }

    private fun goToSend(cryptoRequest: CryptoRequest?) {
        // TODO: Find a better solution.
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
                    arguments.putSerializable(WalletActivity.EXTRA_CRYPTO_REQUEST, cryptoRequest)
                    fragmentSend.arguments = arguments
                }

                if (!fragmentSend.isAdded) {
                    activity.supportFragmentManager.beginTransaction()
                            .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                            .add(android.R.id.content, fragmentSend, FragmentSend::class.java.name)
                            .addToBackStack(FragmentSend::class.java.name).commit()
                }
            }, SEND_SHOW_DELAY.toLong())
        }
    }

    private fun goToWallet(currencyCode: String) {
        BRSharedPrefs.putCurrentWalletCurrencyCode(activity, currencyCode)
        // Use BrdWalletActivity to show rewards view and animation if BRD and not shown yet.
        if (BRD_CURRENCY_CODE.equals(currencyCode, ignoreCase = true)) {
            if (!BRSharedPrefs.getRewardsAnimationShown(activity)) {
                val attributes = HashMap<String, String>()
                attributes[EventUtils.EVENT_ATTRIBUTE_CURRENCY] = BRD_CURRENCY_CODE
                EventUtils.pushEvent(EventUtils.EVENT_REWARDS_OPEN_WALLET, attributes)
            }
            BrdWalletActivity.start(activity, currencyCode)
        } else {
            WalletActivity.start(activity, currencyCode)
        }
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }

    private fun goToInAppMessage(inAppMessage : InAppMessage) {
        InAppNotificationActivity.start(activity, inAppMessage)
    }

    override fun dispose() {
        workRunner.dispose()
    }
}