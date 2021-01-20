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
package com.breadwallet.ui.showkey

import com.breadwallet.ui.navigation.OnCompleteAction
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object ShowPaperKeyUpdate : Update<ShowPaperKey.M, ShowPaperKey.E, ShowPaperKey.F>,
    ShowPaperKeyUpdateSpec {

    override fun update(
        model: ShowPaperKey.M,
        event: ShowPaperKey.E
    ): Next<ShowPaperKey.M, ShowPaperKey.F> = patch(model, event)

    override fun onNextClicked(
        model: ShowPaperKey.M
    ): Next<ShowPaperKey.M, ShowPaperKey.F> {
        return if (model.currentWord == model.phrase.size - 1) {
            val effect: ShowPaperKey.F = if (model.onComplete == null) {
                if (model.phraseWroteDown) {
                    ShowPaperKey.F.GoBack
                } else {
                    ShowPaperKey.F.GoToPaperKeyProve(model.phrase, OnCompleteAction.GO_HOME)
                }
            } else {
                ShowPaperKey.F.GoToPaperKeyProve(model.phrase, model.onComplete)
            }
            dispatch(setOf<ShowPaperKey.F>(effect))
        } else {
            next(model.copy(currentWord = model.currentWord + 1))
        }
    }

    override fun onPreviousClicked(
        model: ShowPaperKey.M
    ): Next<ShowPaperKey.M, ShowPaperKey.F> {
        check(model.currentWord > 0)
        return next(model.copy(currentWord = model.currentWord - 1))
    }

    override fun onPageChanged(
        model: ShowPaperKey.M,
        event: ShowPaperKey.E.OnPageChanged
    ): Next<ShowPaperKey.M, ShowPaperKey.F> {
        return next(model.copy(currentWord = event.position))
    }

    override fun onCloseClicked(
        model: ShowPaperKey.M
    ): Next<ShowPaperKey.M, ShowPaperKey.F> {
        val effect = when (model.onComplete) {
            OnCompleteAction.GO_HOME -> ShowPaperKey.F.GoToHome
            OnCompleteAction.GO_TO_BUY -> ShowPaperKey.F.GoToBuy
            null -> ShowPaperKey.F.GoBack
        } as ShowPaperKey.F
        return dispatch(setOf(effect))
    }
}
