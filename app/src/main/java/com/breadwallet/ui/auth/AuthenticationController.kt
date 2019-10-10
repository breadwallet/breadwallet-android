package com.breadwallet.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.breadwallet.R
import com.breadwallet.legacy.presenter.activities.util.BRActivity
import com.breadwallet.legacy.presenter.customviews.PinLayout
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import kotlinx.android.synthetic.main.controller_pin.*

// TODO: Currently only supports pin authentication
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
            Mode.USER_PREFERRED -> R.layout.controller_pin // TODO: fingerprint layout
            Mode.PIN_REQUIRED -> R.layout.controller_pin
            Mode.BIOMETRIC_REQUIRED -> R.layout.fingerprint_dialog_container
        }

    private val listener get() = targetController as? Listener

    override fun onCreateView(view: View) {
        super.onCreateView(view)

        title.text = argOptional(KEY_TITLE)
        message.text = argOptional(KEY_MESSAGE)

        brkeyboard.setDeleteImage(R.drawable.ic_delete_black)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
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

    override fun onDetach(view: View) {
        super.onDetach(view)
        pin_digits.cleanUp()
    }

    override fun handleBack(): Boolean {
        listener?.onAuthenticationCancelled()
        return super.handleBack()
    }
}
