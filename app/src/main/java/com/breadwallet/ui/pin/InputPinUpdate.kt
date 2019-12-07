/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 9/23/19.
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

import com.spotify.mobius.Next
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object InputPinUpdate : Update<InputPinModel, InputPinEvent, InputPinEffect>,
    InputPinUpdateSpec {

    override fun update(
        model: InputPinModel,
        event: InputPinEvent
    ) = patch(model, event)

    override fun onFaqClicked(model: InputPinModel): Next<InputPinModel, InputPinEffect> =
        Next.dispatch(setOf(InputPinEffect.GoToFaq))

    override fun onPinEntered(
        model: InputPinModel,
        event: InputPinEvent.OnPinEntered
    ): Next<InputPinModel, InputPinEffect> {
        return when (model.mode) {
            InputPinModel.Mode.VERIFY -> if (event.isPinCorrect) {
                next(model.copy(pinUpdateMode = true, mode = InputPinModel.Mode.NEW))
            } else {
                next(model, setOf<InputPinEffect>(InputPinEffect.ErrorShake))
            }
            InputPinModel.Mode.NEW -> {
                next(model.copy(mode = InputPinModel.Mode.CONFIRM, pin = event.pin))
            }
            InputPinModel.Mode.CONFIRM -> if (event.pin == model.pin) {
                next(model, setOf<InputPinEffect>(InputPinEffect.SetupPin(model.pin)))
            } else {
                next(
                    model.copy(mode = InputPinModel.Mode.NEW, pin = ""),
                    setOf<InputPinEffect>(InputPinEffect.ErrorShake)
                )
            }
        }
    }

    override fun onPinLocked(model: InputPinModel): Next<InputPinModel, InputPinEffect> =
        next(model, setOf(InputPinEffect.GoToDisabledScreen))

    override fun onPinSaved(model: InputPinModel): Next<InputPinModel, InputPinEffect> {
        val effect = if (model.pinUpdateMode || model.skipWriteDownKey) {
            InputPinEffect.GoToHome
        } else {
            InputPinEffect.GoToWriteDownKey(model.onComplete)
        }
        return next(model, setOf(effect))
    }

    override fun onPinSaveFailed(model: InputPinModel): Next<InputPinModel, InputPinEffect> {
        return next(model.copy(mode = InputPinModel.Mode.NEW, pin = ""))
    }

    override fun onPinCheck(
        model: InputPinModel,
        event: InputPinEvent.OnPinCheck
    ): Next<InputPinModel, InputPinEffect> {
        val mode = if (event.hasPin) InputPinModel.Mode.VERIFY else InputPinModel.Mode.NEW
        return next(model.copy(mode = mode))
    }
}
