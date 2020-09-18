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
import com.breadwallet.ui.recovery.RecoveryKey.E
import com.breadwallet.ui.recovery.RecoveryKey.F
import com.breadwallet.ui.recovery.RecoveryKey.M
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update

object RecoveryKeyUpdate : Update<M, E, F>, RecoveryKeyUpdateSpec {

    override fun update(model: M, event: E) = patch(model, event)

    override fun onWordChanged(
        model: M,
        event: E.OnWordChanged
    ): Next<M, F> {
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
        model: M,
        event: E.OnWordValidated
    ): Next<M, F> {
        return when {
            model.errors[event.index] == event.hasError -> noChange()
            else -> next(
                model.copy(
                    errors = model.errors.replaceAt(event.index, event.hasError)
                ), setOf(F.ErrorShake)
            )
        }
    }

    override fun onNextClicked(model: M): Next<M, F> {
        return when {
            model.isLoading -> noChange()
            else -> next(
                model.copy(isLoading = true),
                setOf<F>(F.ValidatePhrase(model.phrase))
            )
        }
    }

    override fun onPhraseValidated(model: M, event: E.OnPhraseValidated): Next<M, F> {
        return when {
            event.errors.none { it } -> {
                val nextEffect = when (model.mode) {
                    RecoveryKey.Mode.RECOVER ->
                        F.RecoverWallet(model.phrase)
                    RecoveryKey.Mode.RESET_PIN ->
                        F.ResetPin(model.phrase)
                    RecoveryKey.Mode.WIPE ->
                        F.Unlink(model.phrase)
                }
                next(
                    model.copy(isLoading = true),
                    setOf(
                        F.MonitorLoading,
                        nextEffect
                    )
                )
            }
            else -> next(
                model.copy(
                    errors = event.errors,
                    isLoading = false,
                    showContactSupport = false
                ), setOf(F.ErrorShake)
            )
        }
    }

    override fun onFocusedWordChanged(
        model: M,
        event: E.OnFocusedWordChanged
    ): Next<M, F> {
        return when {
            model.focusedWordIndex == -1 ->
                next(model.copy(focusedWordIndex = event.index))
            else -> {
                val nextModel = model.copy(focusedWordIndex = event.index)
                if (model.phrase[model.focusedWordIndex].isBlank()) {
                    next(nextModel)
                } else {
                    next(
                        nextModel, setOf<F>(
                            F.ValidateWord(
                                model.focusedWordIndex,
                                model.phrase[model.focusedWordIndex]
                            )
                        )
                    )
                }
            }
        }
    }

    override fun onRecoveryComplete(model: M): Next<M, F> {
        return dispatch(setOf(F.SetPinForRecovery))
    }

    override fun onPhraseSaveFailed(model: M): Next<M, F> {
        return next(model.copy(
            isLoading = false,
            showContactSupport = false
        ))
    }

    override fun onPhraseInvalid(model: M): Next<M, F> {
        return next(
            model.copy(
                isLoading = false,
                showContactSupport = false
            ), setOf<F>(
                F.ErrorShake,
                F.GoToPhraseError
            )
        )
    }

    override fun onPinSet(model: M): Next<M, F> {
        return dispatch(setOf(F.GoToLoginForReset))
    }

    override fun onPinSetCancelled(model: M): Next<M, F> {
        return dispatch(setOf(F.SetPinForReset))
    }

    override fun onPinCleared(model: M): Next<M, F> {
        return dispatch(setOf(F.SetPinForReset))
    }

    override fun onFaqClicked(model: M): Next<M, F> {
        return dispatch(setOf(F.GoToRecoveryKeyFaq))
    }

    override fun onTextPasted(
        model: M,
        event: E.OnTextPasted
    ): Next<M, F> {
        return when {
            event.text.isBlank() -> noChange()
            model.isLoading -> noChange()
            else -> {
                val phrase = event.text.split("\\s+".toRegex())
                when (phrase.size) {
                    0 -> dispatch(setOf<F>(F.ErrorShake))
                    12 -> next(
                        model.copy(
                            isLoading = true,
                            phrase = phrase
                        ),
                        setOf<F>(F.ValidatePhrase(phrase))
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

    override fun onShowPhraseFailed(model: M): Next<M, F> {
        return next(
            model.copy(
                isLoading = false,
                showContactSupport = false
            ), setOf<F>(
                F.ErrorShake,
                F.GoToPhraseError
            )
        )
    }

    override fun onRequestWipeWallet(model: M): Next<M, F> = when {
        !model.isLoading || model.mode != RecoveryKey.Mode.WIPE -> noChange()
        else -> next(
            model.copy(isLoading = true),
            setOf<F>(F.GoToWipeWallet)
        )
    }

    override fun onWipeWalletConfirmed(model: M): Next<M, F> = when {
        !model.isLoading || model.mode != RecoveryKey.Mode.WIPE -> noChange()
        else -> dispatch(setOf<F>(F.WipeWallet))
    }

    override fun onWipeWalletCancelled(model: M): Next<M, F> = when {
        !model.isLoading || model.mode != RecoveryKey.Mode.WIPE -> noChange()
        else -> next(model.copy(isLoading = false))
    }

    override fun onLoadingCompleteExpected(model: M): Next<M, F> = when {
        model.mode == RecoveryKey.Mode.RECOVER && model.isLoading ->
            next(model.copy(showContactSupport = true))
        else -> noChange()
    }

    override fun onContactSupportClicked(model: M): Next<M, F> {
        return dispatch(setOf<F>(F.ContactSupport))
    }
}
