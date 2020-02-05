/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/10/19.
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
package com.breadwallet.ui.login

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.legacy.presenter.customviews.PinLayout
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.animation.SpringAnimator
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.auth.AuthenticationController
import com.breadwallet.ui.login.LoginScreen.E
import com.breadwallet.ui.login.LoginScreen.F
import com.breadwallet.ui.login.LoginScreen.M
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.controller_login.*
import kotlinx.android.synthetic.main.pin_digits.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

private const val EXTRA_URL = "PENDING_URL"
private const val EXTRA_SHOW_HOME = "SHOW_HOME"

class LoginController(args: Bundle? = null) :
    BaseMobiusController<M, E, F>(args),
    AuthenticationController.Listener {

    override val layoutId = R.layout.controller_login

    constructor(intentUrl: String?) : this(
        bundleOf(EXTRA_URL to intentUrl)
    )

    constructor(showHome: Boolean) : this(
        bundleOf(EXTRA_SHOW_HOME to showHome)
    )

    override val defaultModel = M.createDefault(
        arg(EXTRA_URL, ""),
        arg(EXTRA_SHOW_HOME, true)
    )
    override val update = LoginUpdate
    override val init = LoginInit
    override val effectHandler: Connectable<F, E> = CompositeEffectHandler.from(
        Connectable { output ->
            LoginScreenHandler(
                output,
                direct.instance(),
                shakeKeyboard = {
                    SpringAnimator.failShakeAnimation(
                        activity,
                        pinLayout
                    )
                },
                unlockWalletAnimation = ::unlockWallet,
                showFingerprintPrompt = ::showFingerprintPrompt
            )
        },
        nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
            when (effect) {
                is F.GoToDeepLink -> NavigationEffect.GoToDeepLink(effect.url)
                F.GoToDisableScreen -> NavigationEffect.GoToDisabledScreen
                F.GoToHome -> NavigationEffect.GoToHome
                F.GoBack -> NavigationEffect.GoBack
                else -> null
            }
        })
    )

    override fun bindView(output: Consumer<E>): Disposable {
        val pinListener = object : PinLayout.PinLayoutListener {
            override fun onPinLocked() {
                output.accept(E.OnPinLocked)
            }

            override fun onPinInserted(pin: String?, isPinCorrect: Boolean) {
                output.accept(
                    if (isPinCorrect) {
                        E.OnAuthenticationSuccess
                    } else {
                        E.OnAuthenticationFailed
                    }
                )
            }
        }
        pin_digits.setup(brkeyboard, pinListener)
        fingerprint_icon.setOnClickListener { output.accept(E.OnFingerprintClicked) }
        return Disposable {
            pin_digits.cleanUp()
        }
    }

    override fun M.render() {
        ifChanged(M::fingerprintEnable) {
            fingerprint_icon.isVisible = fingerprintEnable
        }
    }

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        brkeyboard.setShowDecimal(false)
        brkeyboard.setDeleteButtonBackgroundColor(resources!!.getColor(android.R.color.transparent))
        brkeyboard.setDeleteImage(R.drawable.ic_delete_dark)

        val pinDigitButtonColors = resources!!.getIntArray(R.array.pin_digit_button_colors)
        brkeyboard.setButtonTextColor(pinDigitButtonColors)
    }

    override fun handleBack() =
        (router.backstackSize > 1 && !currentModel.isUnlocked) ||
            activity?.isTaskRoot == false

    private fun unlockWallet() {
        fingerprint_icon.visibility = View.INVISIBLE
        pinLayout.animate().translationY(-R.dimen.animation_long.toFloat())
            .setInterpolator(AccelerateInterpolator())
        brkeyboard.animate().translationY(R.dimen.animation_long.toFloat())
            .setInterpolator(AccelerateInterpolator())
        unlocked_image.animate().alpha(1f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    Handler().postDelayed({
                        eventConsumer.accept(E.OnUnlockAnimationEnd)
                    }, DateUtils.SECOND_IN_MILLIS / 2)
                }
            })
    }

    private fun showFingerprintPrompt() {
        if (router.backstack.last().controller() is AuthenticationController) {
            return
        }
        val controller = AuthenticationController(
            mode = AuthenticationController.Mode.BIOMETRIC_REQUIRED,
            title = resources!!.getString(R.string.UnlockScreen_touchIdTitle_android)
        )
        controller.targetController = this@LoginController
        router.pushController(RouterTransaction.with(controller))
    }

    override fun onAuthenticationSuccess() {
        super.onAuthenticationSuccess()
        eventConsumer.accept(E.OnAuthenticationSuccess)
    }
}
