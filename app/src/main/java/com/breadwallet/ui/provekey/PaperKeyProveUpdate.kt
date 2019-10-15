/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 10/10/19.
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
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object PaperKeyProveUpdate : Update<PaperKeyProveModel, PaperKeyProveEvent, PaperKeyProveEffect>,
    PaperKeyProveUpdateSpec {
    override fun update(
        model: PaperKeyProveModel,
        event: PaperKeyProveEvent
    ): Next<PaperKeyProveModel, PaperKeyProveEffect> = patch(model, event)

    override fun onSubmitClicked(model: PaperKeyProveModel)
        : Next<PaperKeyProveModel, PaperKeyProveEffect> {
        return dispatch(
            setOf(
                if (model.firstWordState == PaperKeyProveModel.WordState.VALID
                    && model.secondWordSate == PaperKeyProveModel.WordState.VALID
                ) {
                    PaperKeyProveEffect.StoreWroteDownPhrase
                } else {
                    PaperKeyProveEffect.ShakeWords(
                        model.firstWordState != PaperKeyProveModel.WordState.VALID,
                        model.secondWordSate != PaperKeyProveModel.WordState.VALID
                    )
                }
            )
        )
    }

    override fun onFirstWordChanged(
        model: PaperKeyProveModel,
        event: PaperKeyProveEvent.OnFirstWordChanged
    ): Next<PaperKeyProveModel, PaperKeyProveEffect> {
        val state = when {
            event.word.isEmpty() -> PaperKeyProveModel.WordState.EMPTY
            event.word == model.firstWord -> PaperKeyProveModel.WordState.VALID
            else -> PaperKeyProveModel.WordState.INVALID
        }
        return next(model.copy(firstWordState = state))
    }

    override fun onSecondWordChanged(
        model: PaperKeyProveModel,
        event: PaperKeyProveEvent.OnSecondWordChanged
    ): Next<PaperKeyProveModel, PaperKeyProveEffect> {
        val state = when {
            event.word.isEmpty() -> PaperKeyProveModel.WordState.EMPTY
            event.word == model.secondWord -> PaperKeyProveModel.WordState.VALID
            else -> PaperKeyProveModel.WordState.INVALID
        }
        return next(model.copy(secondWordSate = state))
    }

    override fun onBreadSignalShown(model: PaperKeyProveModel)
        : Next<PaperKeyProveModel, PaperKeyProveEffect> =
        dispatch(
            setOf(
                when (model.onComplete) {
                    OnCompleteAction.GO_TO_BUY -> PaperKeyProveEffect.GoToBuy
                    OnCompleteAction.GO_HOME -> PaperKeyProveEffect.GoToHome
                }
            )
        )

    override fun onWroteDownKeySaved(model: PaperKeyProveModel)
        : Next<PaperKeyProveModel, PaperKeyProveEffect> =
        next(model.copy(showBreadSignal = true))
}