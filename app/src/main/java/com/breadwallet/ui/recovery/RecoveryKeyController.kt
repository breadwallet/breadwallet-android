/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/13/19.
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
package com.breadwallet.ui.recovery

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.effecthandler.metadata.MetaDataEffect
import com.breadwallet.effecthandler.metadata.MetaDataEffectHandler
import com.breadwallet.legacy.presenter.customviews.BRDialogView
import com.breadwallet.legacy.presenter.customviews.BREdit
import com.breadwallet.logger.logError
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.animation.BRDialog
import com.breadwallet.tools.animation.SpringAnimator
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.util.DefaultTextWatcher
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_input_words.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class RecoveryKeyController(
    args: Bundle? = null
) : BaseMobiusController<RecoveryKeyModel, RecoveryKeyEvent, RecoveryKeyEffect>(args) {

    constructor(
        mode: RecoveryKeyModel.Mode,
        phrase: String? = null
    ) : this(
        bundleOf("mode" to mode.name)
    ) {
        launchPhrase = phrase
        if (launchPhrase != null) {
            eventConsumer.accept(RecoveryKeyEvent.OnNextClicked)
        }
    }

    private var launchPhrase: String? = null
    private val mode = arg("mode", RecoveryKeyModel.Mode.RECOVER.name)

    init {
        // TODO: This request code is used in RecoveryKeyEffectHandler without calling
        //  Controller.startActivityForResult so we must register our interest manually.
        registerForActivityResult(BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE)
        registerForActivityResult(BRConstants.SHOW_PHRASE_REQUEST_CODE)
    }

    override val layoutId: Int = R.layout.activity_input_words

    override val defaultModel
        get() = RecoveryKeyModel.createWithOptionalPhrase(
            mode = RecoveryKeyModel.Mode.valueOf(mode),
            phrase = launchPhrase
        )
    override val update = RecoveryKeyUpdate
    override val effectHandler = CompositeEffectHandler.from<RecoveryKeyEffect, RecoveryKeyEvent>(
        Connectable { output ->
            val resources = resources!!
            RecoveryKeyEffectHandler(output, direct.instance(), direct.instance(), {
                // unlink
                BRDialog.showCustomDialog(
                    activity!!,
                    resources.getString(R.string.WipeWallet_alertTitle),
                    resources.getString(R.string.WipeWallet_alertMessage),
                    resources.getString(R.string.WipeWallet_wipe),
                    resources.getString(R.string.Button_cancel),
                    {
                        it.context
                            .getSystemService(ActivityManager::class.java)
                            .clearApplicationUserData()
                    },
                    { brDialogView -> brDialogView.dismissWithAnimation() },
                    { eventConsumer.accept(RecoveryKeyEvent.OnPhraseSaveFailed) },
                    0
                )
            }, {
                // error dialog
                BRDialog.showCustomDialog(
                    activity!!,
                    "",
                    resources.getString(R.string.RecoverWallet_invalid),
                    resources.getString(R.string.AccessibilityLabels_close),
                    null,
                    BRDialogView.BROnClickListener { brDialogView -> brDialogView.dismissWithAnimation() },
                    null,
                    DialogInterface.OnDismissListener {
                        eventConsumer.accept(RecoveryKeyEvent.OnPhraseSaveFailed)
                    },
                    0
                )
            }, {
                SpringAnimator.failShakeAnimation(applicationContext, view)
            })
        },
        nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
            when (effect) {
                RecoveryKeyEffect.GoToRecoveryKeyFaq -> NavigationEffect.GoToFaq(BRConstants.FAQ_PAPER_KEY)
                RecoveryKeyEffect.SetPinForRecovery -> NavigationEffect.GoToSetPin(true)
                RecoveryKeyEffect.GoToLoginForReset -> NavigationEffect.GoToLogin
                RecoveryKeyEffect.SetPinForReset -> NavigationEffect.GoToSetPin()
                else -> null
            }
        })
    )

    private val wordInputs: List<BREdit>
        get() = listOf(
            word1, word2, word3,
            word4, word5, word6,
            word7, word8, word9,
            word10, word11, word12
        )

    private val inputTextColorValue = TypedValue()
    private var errorTextColor: Int = -1
    private var normalTextColor: Int = -1

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        val theme = view.context.theme
        val resources = resources!!

        theme.resolveAttribute(R.attr.input_words_text_color, inputTextColorValue, true)
        errorTextColor = resources.getColor(R.color.red_text, theme)
        normalTextColor = resources.getColor(inputTextColorValue.resourceId, theme)

        // TODO: This needs a better home
        if (Utils.isUsingCustomInputMethod(applicationContext)) {
            BRDialog.showCustomDialog(
                applicationContext!!,
                resources.getString(R.string.JailbreakWarnings_title),
                resources.getString(R.string.Alert_customKeyboard_android),
                resources.getString(R.string.Button_ok),
                resources.getString(R.string.JailbreakWarnings_close),
                BRDialogView.BROnClickListener { brDialogView ->
                    val imeManager =
                        applicationContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imeManager.showInputMethodPicker()
                    brDialogView.dismissWithAnimation()
                },
                BRDialogView.BROnClickListener { brDialogView -> brDialogView.dismissWithAnimation() },
                null,
                0
            )
        }
    }

    override fun bindView(output: Consumer<RecoveryKeyEvent>): Disposable {
        val resources = resources!!
        when (currentModel.mode) {
            RecoveryKeyModel.Mode.WIPE -> {
                title.text = resources.getString(R.string.RecoveryKeyFlow_enterRecoveryKey)
                description.text = resources.getString(R.string.WipeWallet_instruction)
            }
            RecoveryKeyModel.Mode.RESET_PIN -> {
                title.text = resources.getString(R.string.RecoverWallet_header_reset_pin)
                description.text =
                    resources.getString(R.string.RecoverWallet_subheader_reset_pin)
            }
            RecoveryKeyModel.Mode.RECOVER -> Unit
        }

        faq_button.setOnClickListener {
            output.accept(RecoveryKeyEvent.OnFaqClicked)
        }
        send_button.setOnClickListener {
            output.accept(RecoveryKeyEvent.OnNextClicked)
        }

        // Bind paste event
        wordInputs.first().addEditTextEventListener { event ->
            if (event == BREdit.EditTextEvent.PASTE) {
                val clipboardText = BRClipboardManager.getClipboard(activity)
                output.accept(RecoveryKeyEvent.OnTextPasted(clipboardText))
            }
        }

        // Bind keyboard enter event
        wordInputs.last().setOnEditorActionListener { _, actionId, event ->
            if (event?.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                output.accept(RecoveryKeyEvent.OnNextClicked)
            }
            false
        }

        // Bind word input focus event
        wordInputs.forEachIndexed { index, input ->
            input.setOnFocusChangeListener { _, focused ->
                if (focused)
                    output.accept(RecoveryKeyEvent.OnFocusedWordChanged(index))
            }
        }

        // Bind word input text event
        val watchers = wordInputs.mapIndexed { index, input ->
            createTextWatcher(output, index, input)
        }

        return Disposable {
            wordInputs.zip(watchers)
                .forEach { (input, watcher) ->
                    input.removeTextChangedListener(watcher)
                }
        }
    }

    override fun RecoveryKeyModel.render() {
        ifChanged(RecoveryKeyModel::phrase) { phrase ->
            wordInputs.zip(phrase)
                .filter { (input, word) ->
                    input.text.toString() != word
                }
                .forEach { (input, word) ->
                    input.setText(word, TextView.BufferType.EDITABLE)
                }
        }

        ifChanged(RecoveryKeyModel::isLoading) {
            // TODO: Show loading msg
            loading_view.isVisible = it
        }

        ifChanged(RecoveryKeyModel::errors) { errors ->
            wordInputs.zip(errors)
                .forEach { (input, error) ->
                    if (error) {
                        if (input.currentTextColor != errorTextColor)
                            input.setTextColor(errorTextColor)
                    } else {
                        if (input.currentTextColor != normalTextColor)
                            input.setTextColor(normalTextColor)
                    }
                }
        }
    }

    /** Creates a recovery word input text watcher and attaches it to [input]. */
    private fun createTextWatcher(
        output: Consumer<RecoveryKeyEvent>,
        index: Int,
        input: EditText
    ) = object : DefaultTextWatcher() {
        override fun afterTextChanged(s: Editable?) {
            val word = s?.toString() ?: ""
            output.accept(RecoveryKeyEvent.OnWordChanged(index, word))
        }
    }.also(input::addTextChangedListener)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE ->
                eventConsumer.accept(handlePutPhraseResult(resultCode))
            BRConstants.SHOW_PHRASE_REQUEST_CODE ->
                eventConsumer.accept(handleShowPhraseResult(resultCode))
            else ->
                logError(
                    "Registered for onActivityResult of $requestCode but was unhandled.",
                    resultCode,
                    data
                )
        }
    }

    override fun handleBack(): Boolean = currentModel.isLoading

    private fun handlePutPhraseResult(resultCode: Int): RecoveryKeyEvent =
        when (resultCode) {
            Activity.RESULT_OK -> RecoveryKeyEvent.OnPhraseSaved
            else -> RecoveryKeyEvent.OnPhraseSaveFailed
        }

    private fun handleShowPhraseResult(resultCode: Int): RecoveryKeyEvent =
        when (resultCode) {
            Activity.RESULT_OK -> RecoveryKeyEvent.OnShowPhraseGranted
            else -> RecoveryKeyEvent.OnShowPhraseFailed
        }
}
