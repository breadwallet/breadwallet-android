package com.breadwallet.ui.auth

import android.app.Activity
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.legacy.presenter.activities.util.BRActivity
import com.breadwallet.legacy.presenter.customviews.PinLayout
import com.breadwallet.tools.security.FingerprintUiHelper
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import kotlinx.android.synthetic.main.controller_pin.*
import kotlinx.android.synthetic.main.fingerprint_dialog_container.*

class AuthenticationController(
    args: Bundle
) : BaseController(args) {

    enum class Mode {
        /** Attempt biometric auth if configured, otherwise the pin is required. */
        USER_PREFERRED,
        /** Ensures the use of a pin, fails immediately if not set. */
        PIN_REQUIRED,
        /** Ensures the use of biometric auth, fails immediately if not available. */
        BIOMETRIC_REQUIRED
    }

    interface Listener {
        /** Called when the user successfully authenticates. */
        fun onAuthenticationSuccess() = Unit

        /**
         * Called when the user exhausts all authentication attempts.
         *
         * Return true if the failure was consumed to prevent the
         * application from locking itself. The default is false
         * and this return value may be removed in the future.
         */
        fun onAuthenticationFailed(): Boolean = false

        /** Called when the user cancels the authentication request. */
        fun onAuthenticationCancelled() = Unit
    }

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_MODE = "mode"
    }

    constructor(
        mode: Mode = Mode.USER_PREFERRED,
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

    private val mode = Mode.valueOf(arg(KEY_MODE))

    override val layoutId: Int =
        when (mode) {
            Mode.USER_PREFERRED -> R.layout.controller_fingerprint
            Mode.PIN_REQUIRED -> R.layout.controller_pin
            Mode.BIOMETRIC_REQUIRED -> R.layout.controller_fingerprint
        }

    private val listener get() = targetController as? Listener

    private var fingerprintUiHelper: FingerprintUiHelper? = null

    override fun onCreateView(view: View) {
        super.onCreateView(view)

        when (mode) {
            Mode.BIOMETRIC_REQUIRED, Mode.USER_PREFERRED -> {
                showFingerprint()
            }
            Mode.PIN_REQUIRED -> {
                title.text = argOptional(KEY_TITLE)
                message.text = argOptional(KEY_MESSAGE)
                brkeyboard.setDeleteImage(R.drawable.ic_delete_black)
            }
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        when (mode) {
            Mode.PIN_REQUIRED -> {
                pin_digits.setup(brkeyboard, object : PinLayout.PinLayoutListener {
                    override fun onPinInserted(pin: String?, isPinCorrect: Boolean) {
                        if (isPinCorrect) {
                            listener?.onAuthenticationSuccess()
                            router.popCurrentController()
                        }
                    }

                    override fun onPinLocked() {
                        if (listener?.onAuthenticationFailed() == false) {
                            // TODO: This feels hidden in here, we should move this somewhere
                            //  more explicit that wont complicate testing
                            (activity as? BRActivity)?.showWalletDisabled()
                        }
                        router.popCurrentController()
                    }
                })
            }
            Mode.BIOMETRIC_REQUIRED, Mode.USER_PREFERRED -> {
                val mFingerprintManager =
                    activity!!.getSystemService(Activity.FINGERPRINT_SERVICE) as FingerprintManager
                val fingerprintUiHelperBuilder =
                    FingerprintUiHelper.FingerprintUiHelperBuilder(mFingerprintManager)
                val callback = object : FingerprintUiHelper.Callback {
                    override fun onAuthenticated() {
                        listener?.onAuthenticationSuccess()
                        router.popCurrentController()
                    }

                    override fun onError() {
                        listener?.onAuthenticationFailed()
                        router.popCurrentController()
                    }
                }

                fingerprintUiHelper = fingerprintUiHelperBuilder.build(
                    fingerprint_icon as ImageView,
                    fingerprint_status as TextView,
                    callback,
                    activity!!
                )
                fingerprintUiHelper?.startListening(null)
            }
        }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        when (mode) {
            Mode.PIN_REQUIRED -> {
                pin_digits.cleanUp()
            }
            Mode.BIOMETRIC_REQUIRED, Mode.USER_PREFERRED -> {
                fingerprintUiHelper?.stopListening()
            }
        }
    }

    override fun handleBack(): Boolean {
        listener?.onAuthenticationCancelled()
        return super.handleBack()
    }

    private fun showFingerprint() {
        fingerprint_title.text = argOptional(KEY_TITLE)
        cancel_button.setText(R.string.Button_cancel)
        cancel_button.setOnClickListener {
            listener?.onAuthenticationCancelled()
            router.popCurrentController()
        }
        second_dialog_button.setText(R.string.Prompts_TouchId_usePin_android)
        second_dialog_button.setOnClickListener {
            if (mode == Mode.USER_PREFERRED) {
                val pinAuthenticationController = AuthenticationController(
                    mode = Mode.PIN_REQUIRED,
                    title = arg(KEY_TITLE),
                    message = arg(KEY_MESSAGE)
                )
                pinAuthenticationController.targetController = targetController
                router.popCurrentController()
                router.pushController(RouterTransaction.with(pinAuthenticationController))
            } else {
                router.popCurrentController()
            }
        }
    }
}
