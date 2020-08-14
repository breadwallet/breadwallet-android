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
package com.breadwallet.ui.settings.fastsync

import com.spotify.mobius.Next

interface FastSyncUpdateSpec {
    fun patch(model: FastSync.M, event: FastSync.E): Next<FastSync.M, FastSync.F> = when (event) {
        FastSync.E.OnBackClicked -> onBackClicked(model)
        FastSync.E.OnLearnMoreClicked -> onLearnMoreClicked(model)
        FastSync.E.OnDisableFastSyncConfirmed -> onDisableFastSyncConfirmed(model)
        FastSync.E.OnDisableFastSyncCanceled -> onDisableFastSyncCanceled(model)
        is FastSync.E.OnFastSyncChanged -> onFastSyncChanged(model, event)
        is FastSync.E.OnSyncModesUpdated -> onSyncModesUpdated(model, event)
        is FastSync.E.OnCurrencyIdsUpdated -> onCurrencyIdsUpdated(model, event)
    }

    fun onBackClicked(model: FastSync.M): Next<FastSync.M, FastSync.F>

    fun onLearnMoreClicked(model: FastSync.M): Next<FastSync.M, FastSync.F>

    fun onDisableFastSyncConfirmed(model: FastSync.M): Next<FastSync.M, FastSync.F>

    fun onDisableFastSyncCanceled(model: FastSync.M): Next<FastSync.M, FastSync.F>

    fun onFastSyncChanged(model: FastSync.M, event: FastSync.E.OnFastSyncChanged): Next<FastSync.M, FastSync.F>

    fun onSyncModesUpdated(model: FastSync.M, event: FastSync.E.OnSyncModesUpdated): Next<FastSync.M, FastSync.F>

    fun onCurrencyIdsUpdated(model: FastSync.M, event: FastSync.E.OnCurrencyIdsUpdated): Next<FastSync.M, FastSync.F>
}