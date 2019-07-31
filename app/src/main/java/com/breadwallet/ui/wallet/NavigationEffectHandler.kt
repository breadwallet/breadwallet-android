package com.breadwallet.ui.wallet

import android.os.Bundle
import android.os.Handler
import com.breadwallet.R
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.presenter.entities.CryptoRequest
import com.breadwallet.presenter.fragments.FragmentSend
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.util.EventUtils
import com.platform.util.AppReviewPromptManager
import com.spotify.mobius.Connection
import com.spotify.mobius.android.runners.MainThreadWorkRunner

class NavigationEffectHandler(
        private val activity: BRActivity // TODO: Don't depend on an activity
) : Connection<NavigationEffect> {

    companion object {
        private const val SEND_SHOW_DELAY = 300
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

    override fun dispose() {
        workRunner.dispose()
    }
}