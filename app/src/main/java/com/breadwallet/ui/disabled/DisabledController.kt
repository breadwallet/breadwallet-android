/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 3/25/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.ui.disabled

import android.os.Bundle
import android.view.View
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.breadwallet.R
import com.breadwallet.databinding.ControllerDisabledBinding
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.tools.animation.SpringAnimator
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.security.BrdUserState
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.login.LoginController
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.ui.navigation.asSupportUrl
import com.breadwallet.ui.recovery.RecoveryKey
import com.breadwallet.ui.recovery.RecoveryKeyController
import com.breadwallet.ui.web.WebController
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.kodein.di.erased.instance
import java.util.Locale

class DisabledController(args: Bundle? = null) : BaseController(args) {

    private val userManager by instance<BrdUserManager>()
    private val binding by viewBinding(ControllerDisabledBinding::inflate)

    override fun onCreateView(view: View) {
        super.onCreateView(view)

        binding.faqButton.setOnClickListener {
            val url = NavigationTarget.SupportPage(BRConstants.FAQ_WALLET_DISABLE).asSupportUrl()
            router.pushController(
                RouterTransaction.with(WebController(url))
                    .popChangeHandler(BottomSheetChangeHandler())
                    .pushChangeHandler(BottomSheetChangeHandler())
            )
        }

        binding.resetButton.setOnClickListener {
            val controller = RecoveryKeyController(RecoveryKey.Mode.RESET_PIN)
            router.pushController(
                RouterTransaction.with(controller)
                    .popChangeHandler(HorizontalChangeHandler())
                    .pushChangeHandler(HorizontalChangeHandler())
            )
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        userManager.stateChanges(disabledUpdates = true)
            .onEach { state ->
                when (state) {
                    is BrdUserState.Disabled ->
                        walletDisabled(state.seconds)
                    else -> walletEnabled()
                }
            }
            .flowOn(Main)
            .launchIn(viewAttachScope)
    }

    override fun handleBack(): Boolean {
        val isDisabled = userManager.getState() is BrdUserState.Disabled
        if (isDisabled) {
            SpringAnimator.failShakeAnimation(activity, binding.disabled)
        }

        return isDisabled
    }

    private fun walletDisabled(seconds: Int) {
        binding.untilLabel.text = String.format(
            Locale.ROOT,
            "%02d:%02d:%02d",
            seconds / 3600,
            seconds % 3600 / 60,
            seconds % 60
        )
    }

    private fun walletEnabled() {
        logError("Wallet enabled, going to Login.")
        EventUtils.pushEvent(EventUtils.EVENT_LOGIN_UNLOCKED)

        val returnStack = router.backstack
            .takeWhile { it.controller !is DisabledController }

        when {
            returnStack.isEmpty() -> {
                logDebug("Returning to Login for Home screen.")
                router.setRoot(
                    RouterTransaction.with(LoginController(showHome = true))
                        .popChangeHandler(FadeChangeHandler())
                        .pushChangeHandler(FadeChangeHandler())
                )
            }
            returnStack.last().controller !is LoginController -> {
                logDebug("Returning to Login for previous backstack.")
                val loginTransaction = RouterTransaction.with(LoginController(showHome = false))
                    .popChangeHandler(FadeChangeHandler())
                    .pushChangeHandler(FadeChangeHandler())
                router.setBackstack(returnStack + loginTransaction, FadeChangeHandler())
            }
            else -> {
                logDebug("Returning to Login with previous backstack.")
                router.setBackstack(returnStack, FadeChangeHandler())
            }
        }
    }
}
