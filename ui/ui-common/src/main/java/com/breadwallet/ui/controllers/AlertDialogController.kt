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
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import com.breadwallet.ui.uicommon.databinding.ControllerAlertDialogBinding

/** */
class AlertDialogController(
    args: Bundle
) : BaseController(args) {

    interface Listener {
        fun onPositiveClicked(
            dialogId: String,
            controller: AlertDialogController,
            result: DialogInputResult
        ) = Unit

        fun onNegativeClicked(
            dialogId: String,
            controller: AlertDialogController,
            result: DialogInputResult
        ) = Unit

        fun onDismissed(
            dialogId: String,
            controller: AlertDialogController,
            result: DialogInputResult
        ) = Unit

        fun onHelpClicked(dialogId: String, controller: AlertDialogController) = Unit
    }

    data class DialogInputResult(val inputText: String)

    companion object {
        private const val KEY_DIALOG_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_POSITIVE_TEXT = "positive_text"
        private const val KEY_NEGATIVE_TEXT = "negative_text"
        private const val KEY_ICON_RES_ID = "icon_res_id"
        private const val KEY_SHOW_HELP = "show_help"
        private const val KEY_DISMISSIBLE = "dismissible"
        private const val KEY_TEXT_INPUT_PLACEHOLDER = "text_input_placeholder"
    }

    constructor(
        message: String,
        title: String? = null,
        positiveText: String? = null,
        negativeText: String? = null,
        iconResId: Int? = null,
        showHelp: Boolean = false,
        dialogId: String = "",
        dismissible: Boolean = true,
        textInputPlaceholder: String? = null
    ) : this(
        bundleOf(
            KEY_DIALOG_ID to dialogId,
            KEY_TITLE to title,
            KEY_MESSAGE to message,
            KEY_POSITIVE_TEXT to positiveText,
            KEY_NEGATIVE_TEXT to negativeText,
            KEY_ICON_RES_ID to iconResId,
            KEY_SHOW_HELP to showHelp,
            KEY_DISMISSIBLE to dismissible,
            KEY_TEXT_INPUT_PLACEHOLDER to textInputPlaceholder
        )
    )

    init {
        overridePushHandler(DialogChangeHandler())
        overridePopHandler(DialogChangeHandler())
    }

    private val binding by viewBinding(ControllerAlertDialogBinding::inflate)

    val dialogId = arg<String>(KEY_DIALOG_ID)

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        val dismissible = arg<Boolean>(KEY_DISMISSIBLE)
        with(binding) {
            layoutBackground.setOnClickListener {
                if (dismissible) {
                    findListener<Listener>()?.onDismissed(
                        dialogId,
                        this@AlertDialogController,
                        DialogInputResult(textInput.text.toString())
                    )
                    router.popCurrentController()
                }
            }
            posButton.setOnClickListener {
                findListener<Listener>()?.onPositiveClicked(
                    dialogId,
                    this@AlertDialogController,
                    DialogInputResult(textInput.text.toString())
                )
                if (dismissible) {
                    router.popCurrentController()
                }
            }
            negButton.setOnClickListener {
                findListener<Listener>()?.onNegativeClicked(
                    dialogId,
                    this@AlertDialogController,
                    DialogInputResult(textInput.text.toString())
                )
                if (dismissible) {
                    router.popCurrentController()
                }
            }
            helpIcon.setOnClickListener {
                findListener<Listener>()?.onHelpClicked(dialogId, this@AlertDialogController)
            }

            helpIcon.isVisible = arg(KEY_SHOW_HELP)
            dialogText.text = arg(KEY_MESSAGE)

            val title = argOptional<String>(KEY_TITLE)
            dialogTitle.isGone = title.isNullOrBlank()
            dialogTitle.text = title

            val positiveText = argOptional<String>(KEY_POSITIVE_TEXT)
            posButton.isGone = positiveText.isNullOrBlank()
            posButton.text = positiveText

            val negativeText = argOptional<String>(KEY_NEGATIVE_TEXT)
            negButton.isGone = negativeText.isNullOrBlank()
            negButton.text = negativeText

            val textInputPlaceholder = argOptional<String>(KEY_TEXT_INPUT_PLACEHOLDER)
            textInput.isGone = textInputPlaceholder.isNullOrBlank()
            textInput.hint = textInputPlaceholder
        }
    }

    override fun handleBack(): Boolean {
        return if (arg(KEY_DISMISSIBLE)) {
            findListener<Listener>()?.onDismissed(
                arg(KEY_DIALOG_ID),
                this,
                DialogInputResult(binding.textInput.text.toString())
            )
            super.handleBack()
        } else true
    }
}
