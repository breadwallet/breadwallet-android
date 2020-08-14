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
package com.breadwallet.ui.settings.fingerprint

import com.spotify.mobius.Next

interface FingerprintSettingsUpdateSpec {
    fun patch(model: FingerprintSettings.M, event: FingerprintSettings.E): Next<FingerprintSettings.M, FingerprintSettings.F> = when (event) {
        FingerprintSettings.E.OnBackClicked -> onBackClicked(model)
        FingerprintSettings.E.OnFaqClicked -> onFaqClicked(model)
        is FingerprintSettings.E.OnAppUnlockChanged -> onAppUnlockChanged(model, event)
        is FingerprintSettings.E.OnSendMoneyChanged -> onSendMoneyChanged(model, event)
        is FingerprintSettings.E.OnSettingsLoaded -> onSettingsLoaded(model, event)
    }

    fun onBackClicked(model: FingerprintSettings.M): Next<FingerprintSettings.M, FingerprintSettings.F>

    fun onFaqClicked(model: FingerprintSettings.M): Next<FingerprintSettings.M, FingerprintSettings.F>

    fun onAppUnlockChanged(model: FingerprintSettings.M, event: FingerprintSettings.E.OnAppUnlockChanged): Next<FingerprintSettings.M, FingerprintSettings.F>

    fun onSendMoneyChanged(model: FingerprintSettings.M, event: FingerprintSettings.E.OnSendMoneyChanged): Next<FingerprintSettings.M, FingerprintSettings.F>

    fun onSettingsLoaded(model: FingerprintSettings.M, event: FingerprintSettings.E.OnSettingsLoaded): Next<FingerprintSettings.M, FingerprintSettings.F>
}