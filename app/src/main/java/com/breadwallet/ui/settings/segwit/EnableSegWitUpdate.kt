/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 11/05/19.
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
package com.breadwallet.ui.settings.segwit

import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object EnableSegWitUpdate : Update<EnableSegWitModel, EnableSegWitEvent, EnableSegWitEffect>,
    EnableSegWitUpdateSpec {
    override fun update(
        model: EnableSegWitModel,
        event: EnableSegWitEvent
    ): Next<EnableSegWitModel, EnableSegWitEffect> = patch(model, event)

    override fun onEnableClick(model: EnableSegWitModel): Next<EnableSegWitModel, EnableSegWitEffect> =
        next(model.copy(state = EnableSegWitModel.State.CONFIRMATION))

    override fun onContinueClicked(model: EnableSegWitModel): Next<EnableSegWitModel, EnableSegWitEffect> =
        next(
            model.copy(state = EnableSegWitModel.State.DONE),
            setOf(EnableSegWitEffect.EnableSegWit)
        )

    override fun onCancelClicked(model: EnableSegWitModel): Next<EnableSegWitModel, EnableSegWitEffect> =
        next(model.copy(state = EnableSegWitModel.State.ENABLE))

    override fun onBackClicked(model: EnableSegWitModel): Next<EnableSegWitModel, EnableSegWitEffect> =
        dispatch(setOf(EnableSegWitEffect.GoBack))

    override fun onDoneClicked(model: EnableSegWitModel): Next<EnableSegWitModel, EnableSegWitEffect> =
        dispatch(setOf(EnableSegWitEffect.GoToHome))
}
