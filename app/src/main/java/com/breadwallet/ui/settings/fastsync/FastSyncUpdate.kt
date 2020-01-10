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
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object FastSyncUpdate : Update<FastSyncModel, FastSyncEvent, FastSyncEffect>, FastSyncUpdateSpec {

    override fun update(
        model: FastSyncModel,
        event: FastSyncEvent
    ): Next<FastSyncModel, FastSyncEffect> = patch(model, event)

    override fun onFastSyncChanged(
        model: FastSyncModel,
        event: FastSyncEvent.OnFastSyncChanged
    ): Next<FastSyncModel, FastSyncEffect> {
        return if (event.enable) {
            next(
                model.copy(fastSyncEnable = event.enable),
                effects(
                    FastSyncEffect.MetaData.SetSyncMode(
                        model.currencyId,
                        SyncMode.API_ONLY
                    )
                )
            )
        } else {
            next(
                model.copy(fastSyncEnable = event.enable),
                effects(FastSyncEffect.ShowDisableFastSyncDialog)
            )
        }
    }

    override fun onCurrencyIdsUpdated(
        model: FastSyncModel,
        event: FastSyncEvent.OnCurrencyIdsUpdated
    ): Next<FastSyncModel, FastSyncEffect> {
        val currencyMap = event.currencyMap
        return next(
            model.copy(
                currencyId = currencyMap.entries
                    .single { (currencyCode, _) -> currencyCode.equals(model.currencyCode, true) }
                    .value
            ),
            effects(FastSyncEffect.MetaData.LoadSyncModes)
        )
    }

    override fun onSyncModesUpdated(
        model: FastSyncModel,
        event: FastSyncEvent.OnSyncModesUpdated
    ): Next<FastSyncModel, FastSyncEffect> {
        return next(
            model.copy(
                fastSyncEnable = event.modeMap[model.currencyId] == SyncMode.API_ONLY
            )
        )
    }

    override fun onBackClicked(model: FastSyncModel): Next<FastSyncModel, FastSyncEffect> {
        return dispatch(effects(FastSyncEffect.Nav.GoBack))
    }

    override fun onLearnMoreClicked(model: FastSyncModel): Next<FastSyncModel, FastSyncEffect> {
        return dispatch(effects(FastSyncEffect.Nav.GoToFaq))
    }

    override fun onDisableFastSyncConfirmed(model: FastSyncModel): Next<FastSyncModel, FastSyncEffect> {
        return dispatch(
            effects(
                FastSyncEffect.MetaData.SetSyncMode(
                    model.currencyId,
                    SyncMode.P2P_ONLY
                )
            )
        )
    }

    override fun onDisableFastSyncCanceled(model: FastSyncModel): Next<FastSyncModel, FastSyncEffect> {
        return next(model.copy(fastSyncEnable = true))
    }
}
