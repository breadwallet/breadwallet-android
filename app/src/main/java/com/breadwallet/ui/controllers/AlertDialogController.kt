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
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import kotlinx.android.synthetic.main.controller_alert_dialog.*

/** */
class AlertDialogController(
    args: Bundle
) : BaseController(args) {

    interface Listener {
        fun onPositiveClicked(dialogId: String, controller: AlertDialogController) = Unit
        fun onNegativeClicked(dialogId: String, controller: AlertDialogController) = Unit
        fun onDismissed(dialogId: String, controller: AlertDialogController) = Unit
        fun onHelpClicked(dialogId: String, controller: AlertDialogController) = Unit
    }

    companion object {
        private const val KEY_DIALOG_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_POSITIVE_TEXT = "positive_text"
        private const val KEY_NEGATIVE_TEXT = "negative_text"
        private const val KEY_ICON_RES_ID = "icon_res_id"
        private const val KEY_SHOW_HELP = "show_help"
    }

    constructor(
        message: String,
        title: String? = null,
        positiveText: String? = null,
        negativeText: String? = null,
        iconResId: Int? = null,
        showHelp: Boolean = false,
        dialogId: String = ""
    ) : this(
        bundleOf(
            KEY_DIALOG_ID to dialogId,
            KEY_TITLE to title,
            KEY_MESSAGE to message,
            KEY_POSITIVE_TEXT to positiveText,
            KEY_NEGATIVE_TEXT to negativeText,
            KEY_ICON_RES_ID to iconResId,
            KEY_SHOW_HELP to showHelp
        )
    )

    init {
        overridePushHandler(DialogChangeHandler())
        overridePopHandler(DialogChangeHandler())
    }

    override val layoutId = R.layout.controller_alert_dialog

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        val dialogId = arg<String>(KEY_DIALOG_ID)
        layoutBackground.setOnClickListener {
            findListener<Listener>()?.onDismissed(dialogId, this)
            router.popCurrentController()
        }
        pos_button.setOnClickListener {
            findListener<Listener>()?.onPositiveClicked(dialogId, this)
            router.popCurrentController()
        }
        neg_button.setOnClickListener {
            findListener<Listener>()?.onNegativeClicked(dialogId, this)
            router.popCurrentController()
        }
        help_icon.setOnClickListener {
            findListener<Listener>()?.onHelpClicked(dialogId, this)
        }

        help_icon.isVisible = arg(KEY_SHOW_HELP)
        dialog_text.text = arg(KEY_MESSAGE)

        val title = argOptional<String>(KEY_TITLE)
        dialog_title.isGone = title.isNullOrBlank()
        dialog_title.text = title

        val positiveText = argOptional<String>(KEY_POSITIVE_TEXT)
        pos_button.isGone = positiveText.isNullOrBlank()
        pos_button.text = positiveText

        val negativeText = argOptional<String>(KEY_NEGATIVE_TEXT)
        neg_button.isGone = negativeText.isNullOrBlank()
        neg_button.text = negativeText
    }

    override fun handleBack(): Boolean {
        findListener<Listener>()?.onDismissed(arg(KEY_DIALOG_ID), this)
        return super.handleBack()
    }
}
