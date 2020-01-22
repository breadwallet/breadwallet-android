/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 12/6/19.
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
package com.breadwallet.ui.settings.fastsync

import com.breadwallet.model.SyncMode
import com.breadwallet.ui.settings.fastsync.FastSync.E
import com.breadwallet.ui.settings.fastsync.FastSync.F
import com.breadwallet.ui.settings.fastsync.FastSync.M
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object FastSyncUpdate : Update<M, E, F>, FastSyncUpdateSpec {

    override fun update(
        model: M,
        event: E
    ): Next<M, F> = patch(model, event)

    override fun onFastSyncChanged(
        model: M,
        event: E.OnFastSyncChanged
    ): Next<M, F> {
        return if (event.enable) {
            next(
                model.copy(fastSyncEnable = event.enable),
                effects(
                    F.MetaData.SetSyncMode(
                        model.currencyId,
                        SyncMode.API_ONLY
                    )
                )
            )
        } else {
            next(
                model.copy(fastSyncEnable = event.enable),
                effects(F.ShowDisableFastSyncDialog)
            )
        }
    }

    override fun onCurrencyIdsUpdated(
        model: M,
        event: E.OnCurrencyIdsUpdated
    ): Next<M, F> {
        val currencyMap = event.currencyMap
        return next(
            model.copy(
                currencyId = currencyMap.entries
                    .single { (currencyCode, _) -> currencyCode.equals(model.currencyCode, true) }
                    .value
            ),
            effects(F.MetaData.LoadSyncModes)
        )
    }

    override fun onSyncModesUpdated(
        model: M,
        event: E.OnSyncModesUpdated
    ): Next<M, F> {
        return next(
            model.copy(
                fastSyncEnable = event.modeMap[model.currencyId] == SyncMode.API_ONLY
            )
        )
    }

    override fun onBackClicked(model: M): Next<M, F> {
        return dispatch(effects(F.Nav.GoBack))
    }

    override fun onLearnMoreClicked(model: M): Next<M, F> {
        return dispatch(effects(F.Nav.GoToFaq))
    }

    override fun onDisableFastSyncConfirmed(model: M): Next<M, F> {
        return dispatch(
            effects(
                F.MetaData.SetSyncMode(
                    model.currencyId,
                    SyncMode.P2P_ONLY
                )
            )
        )
    }

    override fun onDisableFastSyncCanceled(model: M): Next<M, F> {
        return next(model.copy(fastSyncEnable = true))
    }
}
