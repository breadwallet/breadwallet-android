/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/4/19.
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
package com.breadwallet.ui.controllers

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.breadwallet.databinding.ControllerSignalBinding
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.controllers.SignalController.Companion.CLOSE_DELAY_MS
import com.breadwallet.ui.controllers.SignalController.Listener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Displays a message and image for [CLOSE_DELAY_MS].
 *
 * Callers may set themselves as a target using [setTargetController]
 * and implementing [Listener] to be notified when the signal is complete.
 */
class SignalController(args: Bundle) : BaseController(args) {

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_ICON_RES_ID = "icon_res_id"

        private const val CLOSE_DELAY_MS = 2_000L
    }

    constructor(
        title: String,
        description: String,
        iconResId: Int
    ) : this(
        bundleOf(
            KEY_TITLE to title,
            KEY_DESCRIPTION to description,
            KEY_ICON_RES_ID to iconResId
        )
    )

    init {
        overridePopHandler(BottomSheetChangeHandler())
        overridePushHandler(BottomSheetChangeHandler())
    }

    /**
     * If the [getTargetController] is a [Listener],
     * [onSignalComplete] will be called after the
     * [CLOSE_DELAY_MS] and the controller is popped.
     */
    interface Listener {
        fun onSignalComplete()
    }

    private val binding by viewBinding(ControllerSignalBinding::inflate)

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        with(binding) {
            title.text = arg(KEY_TITLE)
            description.text = arg(KEY_DESCRIPTION)
            qrImage.setImageResource(arg(KEY_ICON_RES_ID))
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        viewAttachScope.launch(Dispatchers.Main) {
            delay(CLOSE_DELAY_MS)
            findListener<Listener>()?.onSignalComplete()
            router.popCurrentController()
        }
    }
}
