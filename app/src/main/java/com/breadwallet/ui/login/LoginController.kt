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
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.databinding.ControllerLoginBinding
import com.breadwallet.legacy.presenter.customviews.PinLayout
import com.breadwallet.tools.animation.SpringAnimator
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.auth.AuthenticationController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.login.LoginScreen.E
import com.breadwallet.ui.login.LoginScreen.F
import com.breadwallet.ui.login.LoginScreen.M
import drewcarlson.mobius.flow.FlowTransformer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.kodein.di.direct
import org.kodein.di.erased.instance

private const val EXTRA_URL = "PENDING_URL"
private const val EXTRA_SHOW_HOME = "SHOW_HOME"
private const val UNLOCKED_DELAY_MS = 200L

class LoginController(args: Bundle? = null) :
    BaseMobiusController<M, E, F>(args),
    AuthenticationController.Listener {

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
    override val flowEffectHandler: FlowTransformer<F, E>
        get() = createLoginScreenHandler(
            checkNotNull(applicationContext),
            direct.instance()
        )

    private val binding by viewBinding(ControllerLoginBinding::inflate)

    private val biometricPrompt by resetOnViewDestroy {
        BiometricPrompt(
            activity as AppCompatActivity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    eventConsumer.accept(E.OnAuthenticationSuccess)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Defer to pin authentication on error
                    if (errorCode != BiometricPrompt.ERROR_CANCELED) {
                        eventConsumer.accept(E.OnAuthenticationFailed)
                    }
                }

                // Ignored: only handle final events from onAuthenticationError
                override fun onAuthenticationFailed() = Unit
            }
        )
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return with(binding) {
            modelFlow.map { it.fingerprintEnable }
                .distinctUntilChanged()
                .onEach { fingerprintEnable ->
                    fingerprintIcon.isVisible = fingerprintEnable
                }
                .launchIn(uiBindScope)
            merge(
                fingerprintIcon.clicks().map { E.OnFingerprintClicked },
                pinDigits.bindInput()
            )
        }
    }

    private fun PinLayout.bindInput() = callbackFlow<E> {
        val channel = channel
        val pinListener = object : PinLayout.PinLayoutListener {
            override fun onPinLocked() {
                channel.offer(E.OnPinLocked)
            }

            override fun onPinInserted(pin: String?, isPinCorrect: Boolean) {
                channel.offer(
                    if (isPinCorrect) {
                        E.OnAuthenticationSuccess
                    } else {
                        E.OnAuthenticationFailed
                    }
                )
            }
        }
        setup(binding.brkeyboard, pinListener)
        awaitClose { cleanUp() }
    }

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        with(binding) {
            brkeyboard.setShowDecimal(false)
            brkeyboard.setDeleteButtonBackgroundColor(resources!!.getColor(android.R.color.transparent))
            brkeyboard.setDeleteImage(R.drawable.ic_delete_dark)

            val pinDigitButtonColors = resources!!.getIntArray(R.array.pin_digit_button_colors)
            brkeyboard.setButtonTextColor(pinDigitButtonColors)
        }
    }

    override fun handleBack() =
        (router.backstackSize > 1 && !currentModel.isUnlocked) ||
            activity?.isTaskRoot == false

    override fun handleViewEffect(effect: ViewEffect) {
        when (effect) {
            F.AuthenticationSuccess -> unlockWallet()
            F.AuthenticationFailed -> showError()
            F.ShowFingerprintController -> {
                biometricPrompt.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(resources!!.getString(R.string.UnlockScreen_touchIdTitle_android))
                        .setNegativeButtonText(resources!!.getString(R.string.Prompts_TouchId_usePin_android))
                        .build()
                )
            }
        }
    }

    override fun onAuthenticationSuccess() {
        super.onAuthenticationSuccess()
        eventConsumer.accept(E.OnAuthenticationSuccess)
    }

    private fun unlockWallet() {
        with(binding) {
            fingerprintIcon.visibility = View.INVISIBLE
            pinDigits.animate().translationY(-com.breadwallet.R.dimen.animation_long.toFloat())
                .setInterpolator(AccelerateInterpolator())
            brkeyboard.animate().translationY(com.breadwallet.R.dimen.animation_long.toFloat())
                .setInterpolator(AccelerateInterpolator())
            unlockedImage.animate().alpha(1f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        controllerScope.launch {
                            delay(UNLOCKED_DELAY_MS)
                            eventConsumer.accept(E.OnUnlockAnimationEnd)
                        }
                    }
                })
        }
    }

    private fun showError() {
        SpringAnimator.failShakeAnimation(applicationContext, binding.pinDigits)
    }
}
