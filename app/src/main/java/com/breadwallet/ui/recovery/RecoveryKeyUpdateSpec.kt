/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/14/20.
 * Copyright (c) 2020 breadwallet LLC
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

import com.spotify.mobius.Next

interface RecoveryKeyUpdateSpec {
    fun patch(model: RecoveryKey.M, event: RecoveryKey.E): Next<RecoveryKey.M, RecoveryKey.F> = when (event) {
        RecoveryKey.E.OnPhraseInvalid -> onPhraseInvalid(model)
        RecoveryKey.E.OnPhraseSaveFailed -> onPhraseSaveFailed(model)
        RecoveryKey.E.OnPinCleared -> onPinCleared(model)
        RecoveryKey.E.OnPinSet -> onPinSet(model)
        RecoveryKey.E.OnPinSetCancelled -> onPinSetCancelled(model)
        RecoveryKey.E.OnShowPhraseFailed -> onShowPhraseFailed(model)
        RecoveryKey.E.OnRecoveryComplete -> onRecoveryComplete(model)
        RecoveryKey.E.OnFaqClicked -> onFaqClicked(model)
        RecoveryKey.E.OnNextClicked -> onNextClicked(model)
        RecoveryKey.E.OnRequestWipeWallet -> onRequestWipeWallet(model)
        RecoveryKey.E.OnWipeWalletConfirmed -> onWipeWalletConfirmed(model)
        RecoveryKey.E.OnWipeWalletCancelled -> onWipeWalletCancelled(model)
        RecoveryKey.E.OnLoadingCompleteExpected -> onLoadingCompleteExpected(model)
        RecoveryKey.E.OnContactSupportClicked -> onContactSupportClicked(model)
        is RecoveryKey.E.OnWordChanged -> onWordChanged(model, event)
        is RecoveryKey.E.OnWordValidated -> onWordValidated(model, event)
        is RecoveryKey.E.OnPhraseValidated -> onPhraseValidated(model, event)
        is RecoveryKey.E.OnFocusedWordChanged -> onFocusedWordChanged(model, event)
        is RecoveryKey.E.OnTextPasted -> onTextPasted(model, event)
    }

    fun onPhraseInvalid(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onPhraseSaveFailed(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onPinCleared(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onPinSet(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onPinSetCancelled(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onShowPhraseFailed(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onRecoveryComplete(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onFaqClicked(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onNextClicked(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onRequestWipeWallet(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onWipeWalletConfirmed(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onWipeWalletCancelled(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onLoadingCompleteExpected(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onContactSupportClicked(model: RecoveryKey.M): Next<RecoveryKey.M, RecoveryKey.F>

    fun onWordChanged(model: RecoveryKey.M, event: RecoveryKey.E.OnWordChanged): Next<RecoveryKey.M, RecoveryKey.F>

    fun onWordValidated(model: RecoveryKey.M, event: RecoveryKey.E.OnWordValidated): Next<RecoveryKey.M, RecoveryKey.F>

    fun onPhraseValidated(model: RecoveryKey.M, event: RecoveryKey.E.OnPhraseValidated): Next<RecoveryKey.M, RecoveryKey.F>

    fun onFocusedWordChanged(model: RecoveryKey.M, event: RecoveryKey.E.OnFocusedWordChanged): Next<RecoveryKey.M, RecoveryKey.F>

    fun onTextPasted(model: RecoveryKey.M, event: RecoveryKey.E.OnTextPasted): Next<RecoveryKey.M, RecoveryKey.F>
}