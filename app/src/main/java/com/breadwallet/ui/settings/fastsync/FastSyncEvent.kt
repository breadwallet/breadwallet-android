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
import io.hypno.switchboard.MobiusUpdateSpec

@MobiusUpdateSpec(
    baseModel = FastSyncModel::class,
    baseEffect = FastSyncEffect::class
)
sealed class FastSyncEvent {
    object OnBackClicked : FastSyncEvent()
    object OnLearnMoreClicked : FastSyncEvent()
    object OnDisableFastSyncConfirmed : FastSyncEvent()
    object OnDisableFastSyncCanceled : FastSyncEvent()
    data class OnFastSyncChanged(val enable: Boolean) : FastSyncEvent()
    data class OnSyncModesUpdated(val modeMap: Map<String, SyncMode>) : FastSyncEvent()
    data class OnCurrencyIdsUpdated(val currencyMap: Map<String, String>) : FastSyncEvent()
}
