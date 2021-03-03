/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/10/19.
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
package com.breadwallet.ui.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.databinding.ControllerPinBinding
import com.breadwallet.legacy.presenter.customviews.PinLayout
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.DialogChangeHandler

class AuthenticationController(
    args: Bundle
) : BaseController(args) {

    interface Listener {
        /** Called when the user successfully authenticates. */
        fun onAuthenticationSuccess() = Unit

        /**
         * Called when the user exhausts all authentication attempts.
         */
        fun onAuthenticationFailed() = Unit

        /** Called when the user cancels the authentication request. */
        fun onAuthenticationCancelled() = Unit
    }

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_MODE = "mode"
    }

    constructor(
        mode: AuthMode = AuthMode.USER_PREFERRED,
        title: String? = null,
        message: String? = null
    ) : this(
        bundleOf(
            KEY_MODE to mode.name,
            KEY_TITLE to title,
            KEY_MESSAGE to message
        )
    )

    init {
        overridePopHandler(DialogChangeHandler())
        overridePushHandler(DialogChangeHandler())
    }

    private val bindingPin by viewBinding(ControllerPinBinding::inflate)
    private val biometricPrompt by resetOnViewDestroy {
        BiometricPrompt(
            activity as AppCompatActivity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    findListener<Listener>()?.onAuthenticationSuccess()
                    router.popCurrentController()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (mode == AuthMode.USER_PREFERRED) {
                        replaceWithPinAuthentication()
                    } else {
                        if (errorCode == BiometricPrompt.ERROR_CANCELED) {
                            findListener<Listener>()?.onAuthenticationCancelled()
                        } else {
                            findListener<Listener>()?.onAuthenticationFailed()
                        }
                        router.popCurrentController()
                    }
                }

                // Ignored: only handle final events from onAuthenticationError
                override fun onAuthenticationFailed() = Unit
            }
        )
    }

    private val mode by lazy {
        val biometricManager = BiometricManager.from(applicationContext!!)
        val hasBiometrics =
            biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (hasBiometrics == BiometricManager.BIOMETRIC_SUCCESS) {
            AuthMode.valueOf(arg(KEY_MODE))
        } else {
            AuthMode.PIN_REQUIRED
        }
    }

    override fun onCreateView(view: View) {
        super.onCreateView(view)

        when (mode) {
            AuthMode.BIOMETRIC_REQUIRED, AuthMode.USER_PREFERRED -> {
                bindingPin.root.isVisible = false
            }
            AuthMode.PIN_REQUIRED -> {
                with(bindingPin) {
                    title.text = argOptional(KEY_TITLE)
                    message.text = argOptional(KEY_MESSAGE)
                    brkeyboard.setDeleteImage(R.drawable.ic_delete_black)
                }
            }
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        when (mode) {
            AuthMode.PIN_REQUIRED -> {
                bindingPin.pinDigits.setup(
                    bindingPin.brkeyboard,
                    object : PinLayout.PinLayoutListener {
                        override fun onPinInserted(pin: String?, isPinCorrect: Boolean) {
                            if (isPinCorrect) {
                                findListener<Listener>()?.onAuthenticationSuccess()
                                router.popCurrentController()
                            }
                        }

                        override fun onPinLocked() {
                            findListener<Listener>()?.onAuthenticationFailed()
                            router.popCurrentController()
                        }
                    })
            }
            AuthMode.BIOMETRIC_REQUIRED, AuthMode.USER_PREFERRED -> {
                val resources = checkNotNull(resources)
                biometricPrompt.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(resources.getString(R.string.UnlockScreen_touchIdTitle_android))
                        .setNegativeButtonText(resources.getString(R.string.Prompts_TouchId_usePin_android))
                        .build()
                )
            }
        }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        if (mode == AuthMode.PIN_REQUIRED) {
            bindingPin.pinDigits.cleanUp()
        }
    }

    override fun handleBack(): Boolean {
        findListener<Listener>()?.onAuthenticationCancelled()
        return super.handleBack()
    }

    private fun replaceWithPinAuthentication() {
        val pinAuthenticationController = AuthenticationController(
            mode = AuthMode.PIN_REQUIRED,
            title = arg(KEY_TITLE),
            message = arg(KEY_MESSAGE)
        )
        pinAuthenticationController.targetController = targetController
        router.replaceTopController(RouterTransaction.with(pinAuthenticationController))
    }
}
