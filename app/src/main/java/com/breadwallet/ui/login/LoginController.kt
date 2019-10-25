/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 10/10/19.
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
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_pin.*
import kotlinx.android.synthetic.main.pin_digits.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class LoginController(args: Bundle? = null) :
    BaseMobiusController<LoginModel, LoginEvent, LoginEffect>(args),
    AuthenticationController.Listener {

    companion object {
        private const val EXTRA_URL = "com.breadwallet.ui.login.LoginController.PENDING_URL"
    }

    override val layoutId = R.layout.activity_pin

    constructor(intentUrl: String?) : this(
        bundleOf(EXTRA_URL to intentUrl)
    )

    override val defaultModel = LoginModel.createDefault(
        arg(EXTRA_URL, "")
    )
    override val update = LoginUpdate
    override val init = LoginInit
    override val effectHandler: Connectable<LoginEffect, LoginEvent> = CompositeEffectHandler.from(
        Connectable { output ->
            LoginEffectHandler(
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
        nestedConnectable({ direct.instance<NavigationEffectHandler>() }, { effect ->
            when (effect) {
                LoginEffect.GoToDisableScreen -> NavigationEffect.GoToDisabledScreen
                else -> null
            }
        }),
        nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
            when (effect) {
                LoginEffect.AuthenticationSuccess -> NavigationEffect.GoToHome
                else -> null
            }
        })
    )

    override fun bindView(output: Consumer<LoginEvent>): Disposable {
        val pinListener = object : PinLayout.PinLayoutListener {
            override fun onPinLocked() {
                output.accept(LoginEvent.OnPinLocked)
            }

            override fun onPinInserted(pin: String?, isPinCorrect: Boolean) {
                output.accept(
                    if (isPinCorrect) {
                        LoginEvent.OnAuthenticationSuccess
                    } else {
                        LoginEvent.OnAuthenticationFailed
                    }
                )
            }
        }
        pin_digits.setup(brkeyboard, pinListener)
        fingerprint_icon.setOnClickListener { output.accept(LoginEvent.OnFingerprintClicked) }
        return Disposable {
            pin_digits.cleanUp()
        }
    }

    override fun LoginModel.render() {
        ifChanged(LoginModel::fingerprintEnable) {
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
        router.backstackSize > 1 || activity?.isTaskRoot == false

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
                        eventConsumer.accept(LoginEvent.OnUnlockAnimationEnd)
                    }, DateUtils.SECOND_IN_MILLIS / 2)
                }
            })
    }

    private fun showFingerprintPrompt() {
        val controller = AuthenticationController(
            mode = AuthenticationController.Mode.BIOMETRIC_REQUIRED,
            title = resources!!.getString(R.string.UnlockScreen_touchIdTitle_android)
        )
        controller.targetController = this@LoginController
        router.pushController(RouterTransaction.with(controller))
    }

    override fun onAuthenticationSuccess() {
        super.onAuthenticationSuccess()
        eventConsumer.accept(LoginEvent.OnAuthenticationSuccess)
    }
}
