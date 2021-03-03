/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 9/23/19.
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
package com.breadwallet.ui.pin

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.breadwallet.R
import com.breadwallet.databinding.ControllerPinInputBinding
import com.breadwallet.legacy.presenter.customviews.PinLayout
import com.breadwallet.legacy.presenter.customviews.PinLayout.PinLayoutListener
import com.breadwallet.tools.animation.SpringAnimator
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.pin.InputPin.E
import com.breadwallet.ui.pin.InputPin.F
import com.breadwallet.ui.pin.InputPin.M
import drewcarlson.mobius.flow.FlowTransformer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance

private const val EXTRA_PIN_MODE_UPDATE = "pin-update"
private const val EXTRA_SKIP_WRITE_DOWN = "skip-write-down"
private const val EXTRA_ON_COMPLETE = "on-complete"

class InputPinController(args: Bundle) : BaseMobiusController<M, E, F>(args) {

    constructor(
        onComplete: OnCompleteAction,
        pinUpdate: Boolean = false,
        skipWriteDown: Boolean = false
    ) : this(
        bundleOf(
            EXTRA_PIN_MODE_UPDATE to pinUpdate,
            EXTRA_ON_COMPLETE to onComplete.name,
            EXTRA_SKIP_WRITE_DOWN to skipWriteDown
        )
    )

    override val defaultModel = M.createDefault(
        pinUpdateMode = arg(EXTRA_PIN_MODE_UPDATE, false),
        onComplete = OnCompleteAction.valueOf(arg(EXTRA_ON_COMPLETE)),
        skipWriteDownKey = arg(EXTRA_SKIP_WRITE_DOWN, false)
    )
    override val init = InputPinInit
    override val update = InputPinUpdate
    override val flowEffectHandler: FlowTransformer<F, E>
        get() = createInputPinHandler(direct.instance())

    private val binding by viewBinding(ControllerPinInputBinding::inflate)

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        val pinDigitButtonColors = resources?.getIntArray(R.array.pin_digit_button_colors)
        binding.brkeyboard.setButtonTextColor(pinDigitButtonColors)
        binding.brkeyboard.setShowDecimal(false)
    }

    override fun handleViewEffect(effect: ViewEffect) {
        when (effect) {
            F.ErrorShake -> SpringAnimator.failShakeAnimation(applicationContext, binding.pinDigits)
            F.ShowPinError -> toastLong(R.string.UpdatePin_setPinError)
        }
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return merge(
            binding.faqButton.clicks().map { E.OnFaqClicked },
            binding.pinDigits.bindInput()
        )
    }

    private fun PinLayout.bindInput() = callbackFlow<E> {
        val channel = channel
        setup(binding.brkeyboard, object : PinLayoutListener {
            override fun onPinInserted(pin: String, isPinCorrect: Boolean) {
                channel.offer(E.OnPinEntered(pin, isPinCorrect))
            }

            override fun onPinLocked() {
                channel.offer(E.OnPinLocked)
            }
        })
        awaitClose { cleanUp() }
    }

    override fun M.render() {
        ifChanged(M::mode) {
            binding.title.setText(
                when (mode) {
                    M.Mode.VERIFY -> R.string.UpdatePin_enterCurrent
                    M.Mode.NEW -> if (pinUpdateMode) {
                        R.string.UpdatePin_enterNew
                    } else {
                        R.string.UpdatePin_createTitle
                    }
                    M.Mode.CONFIRM -> if (pinUpdateMode) {
                        R.string.UpdatePin_reEnterNew
                    } else {
                        R.string.UpdatePin_createTitleConfirm
                    }
                }
            )
        }
        ifChanged(M::pinUpdateMode, binding.pinDigits::setIsPinUpdating)
    }
}
