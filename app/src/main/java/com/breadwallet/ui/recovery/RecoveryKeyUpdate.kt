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

import com.breadwallet.ext.replaceAt
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update

object RecoveryKeyUpdate : Update<RecoveryKeyModel, RecoveryKeyEvent, RecoveryKeyEffect>,
    RecoveryKeyUpdateSpec {

    override fun update(model: RecoveryKeyModel, event: RecoveryKeyEvent) = patch(model, event)

    override fun onWordChanged(
        model: RecoveryKeyModel,
        event: RecoveryKeyEvent.OnWordChanged
    ): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return when {
            model.phrase[event.index] == event.word ||
                model.isLoading -> noChange()
            else -> next(
                model.copy(
                    phrase = model.phrase.replaceAt(event.index, event.word),
                    errors = model.errors.replaceAt(event.index, false)
                )
            )
        }
    }

    override fun onWordValidated(
        model: RecoveryKeyModel,
        event: RecoveryKeyEvent.OnWordValidated
    ): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return when {
            model.errors[event.index] == event.hasError -> noChange()
            else -> next(
                model.copy(
                    errors = model.errors.replaceAt(event.index, event.hasError)
                ), setOf(RecoveryKeyEffect.ErrorShake)
            )
        }
    }

    override fun onNextClicked(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return when {
            model.phrase.all { it.isNotBlank() } &&
                model.errors.none { it } -> {
                val nextEffect = when (model.mode) {
                    RecoveryKeyModel.Mode.RECOVER ->
                        RecoveryKeyEffect.RecoverWallet(model.phrase)
                    RecoveryKeyModel.Mode.RESET_PIN ->
                        RecoveryKeyEffect.ResetPin(model.phrase)
                    RecoveryKeyModel.Mode.WIPE ->
                        RecoveryKeyEffect.Unlink(model.phrase)
                }
                next(model.copy(isLoading = true), setOf(nextEffect))
            }
            else -> dispatch(setOf(RecoveryKeyEffect.ErrorShake))
        }
    }

    override fun onFocusedWordChanged(
        model: RecoveryKeyModel,
        event: RecoveryKeyEvent.OnFocusedWordChanged
    ): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return when {
            model.focusedWordIndex == -1 ->
                next(model.copy(focusedWordIndex = event.index))
            else -> {
                val nextModel = model.copy(focusedWordIndex = event.index)
                if (model.phrase[model.focusedWordIndex].isBlank()) {
                    next(nextModel)
                } else {
                    next(
                        nextModel, setOf<RecoveryKeyEffect>(
                            RecoveryKeyEffect.ValidateWord(
                                model.focusedWordIndex,
                                model.phrase[model.focusedWordIndex]
                            )
                        )
                    )
                }
            }
        }
    }

    override fun onRecoveryComplete(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return dispatch(
            setOf(
                RecoveryKeyEffect.SetPinForRecovery,
                RecoveryKeyEffect.RecoverMetaData
            )
        )
    }

    override fun onPhraseSaved(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return dispatch(setOf(RecoveryKeyEffect.RecoverWallet(model.phrase)))
    }

    override fun onPhraseSaveFailed(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return next(model.copy(isLoading = false))
    }

    override fun onPhraseInvalid(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return next(
            model.copy(isLoading = false), setOf(
                RecoveryKeyEffect.ErrorShake,
                RecoveryKeyEffect.GoToPhraseError
            )
        )
    }

    override fun onPinSet(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return dispatch(setOf(RecoveryKeyEffect.GoToLoginForReset))
    }

    override fun onPinSetCancelled(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return dispatch(setOf(RecoveryKeyEffect.SetPinForReset))
    }

    override fun onPinCleared(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return dispatch(setOf(RecoveryKeyEffect.SetPinForReset))
    }

    override fun onFaqClicked(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return dispatch(setOf(RecoveryKeyEffect.GoToRecoveryKeyFaq))
    }

    override fun onTextPasted(
        model: RecoveryKeyModel,
        event: RecoveryKeyEvent.OnTextPasted
    ): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return when {
            event.text.isBlank() -> noChange()
            else -> {
                val phrase = event.text.split("\\s+".toRegex())
                when (phrase.size) {
                    0 -> dispatch(setOf<RecoveryKeyEffect>(RecoveryKeyEffect.ErrorShake))
                    12 -> next(
                        model.copy(phrase = phrase),
                        setOf<RecoveryKeyEffect>(RecoveryKeyEffect.ValidatePhrase(phrase))
                    )
                    else -> next(model.copy(
                        phrase = List(12) { index ->
                            phrase.getOrElse(index) { "" }
                        }
                    ))
                }
            }
        }
    }

    override fun onShowPhraseGranted(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return dispatch(setOf(RecoveryKeyEffect.Unlink(model.phrase)))
    }

    override fun onShowPhraseFailed(model: RecoveryKeyModel): Next<RecoveryKeyModel, RecoveryKeyEffect> {
        return next(
            model.copy(isLoading = false), setOf(
                RecoveryKeyEffect.ErrorShake,
                RecoveryKeyEffect.GoToPhraseError
            )
        )
    }
}