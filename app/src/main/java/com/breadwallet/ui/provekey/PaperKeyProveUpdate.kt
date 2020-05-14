/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/10/19.
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
package com.breadwallet.ui.provekey

import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.provekey.PaperKeyProve.E
import com.breadwallet.ui.provekey.PaperKeyProve.F
import com.breadwallet.ui.provekey.PaperKeyProve.M
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object PaperKeyProveUpdate : Update<M, E, F>,
    PaperKeyProveUpdateSpec {
    override fun update(
        model: M,
        event: E
    ): Next<M, F> = patch(model, event)

    override fun onSubmitClicked(model: M)
        : Next<M, F> {
        return dispatch(
            setOf(
                if (model.firstWordState == M.WordState.VALID
                    && model.secondWordSate == M.WordState.VALID
                ) {
                    F.StoreWroteDownPhrase
                } else {
                    F.ShakeWords(
                        model.firstWordState != M.WordState.VALID,
                        model.secondWordSate != M.WordState.VALID
                    )
                }
            )
        )
    }

    override fun onFirstWordChanged(
        model: M,
        event: E.OnFirstWordChanged
    ): Next<M, F> {
        val state = when {
            event.word.isEmpty() -> M.WordState.EMPTY
            event.word == model.firstWord -> M.WordState.VALID
            else -> M.WordState.INVALID
        }
        return next(model.copy(firstWordState = state))
    }

    override fun onSecondWordChanged(
        model: M,
        event: E.OnSecondWordChanged
    ): Next<M, F> {
        val state = when {
            event.word.isEmpty() -> M.WordState.EMPTY
            event.word == model.secondWord -> M.WordState.VALID
            else -> M.WordState.INVALID
        }
        return next(model.copy(secondWordSate = state))
    }

    override fun onBreadSignalShown(model: M): Next<M, F> =
        dispatch(
            setOf(
                when (model.onComplete) {
                    OnCompleteAction.GO_TO_BUY -> F.GoToBuy
                    OnCompleteAction.GO_HOME -> F.GoToHome
                } as F
            )
        )

    override fun onWroteDownKeySaved(model: M): Next<M, F> =
        next(model.copy(showBreadSignal = true), setOf(F.ShowStoredSignal))
}
