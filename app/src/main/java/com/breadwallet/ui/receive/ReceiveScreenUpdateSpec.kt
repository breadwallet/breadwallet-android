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
package com.breadwallet.ui.receive

import com.spotify.mobius.Next

interface ReceiveScreenUpdateSpec {
    fun patch(model: ReceiveScreen.M, event: ReceiveScreen.E): Next<ReceiveScreen.M, ReceiveScreen.F> = when (event) {
        ReceiveScreen.E.OnCloseClicked -> onCloseClicked(model)
        ReceiveScreen.E.OnFaqClicked -> onFaqClicked(model)
        ReceiveScreen.E.OnShareClicked -> onShareClicked(model)
        ReceiveScreen.E.OnCopyAddressClicked -> onCopyAddressClicked(model)
        ReceiveScreen.E.OnAmountClicked -> onAmountClicked(model)
        ReceiveScreen.E.OnToggleCurrencyClicked -> onToggleCurrencyClicked(model)
        is ReceiveScreen.E.OnExchangeRateUpdated -> onExchangeRateUpdated(model, event)
        is ReceiveScreen.E.OnWalletInfoLoaded -> onWalletInfoLoaded(model, event)
        is ReceiveScreen.E.OnAmountChange -> onAmountChange(model, event)
    }

    fun onCloseClicked(model: ReceiveScreen.M): Next<ReceiveScreen.M, ReceiveScreen.F>

    fun onFaqClicked(model: ReceiveScreen.M): Next<ReceiveScreen.M, ReceiveScreen.F>

    fun onShareClicked(model: ReceiveScreen.M): Next<ReceiveScreen.M, ReceiveScreen.F>

    fun onCopyAddressClicked(model: ReceiveScreen.M): Next<ReceiveScreen.M, ReceiveScreen.F>

    fun onAmountClicked(model: ReceiveScreen.M): Next<ReceiveScreen.M, ReceiveScreen.F>

    fun onToggleCurrencyClicked(model: ReceiveScreen.M): Next<ReceiveScreen.M, ReceiveScreen.F>

    fun onExchangeRateUpdated(model: ReceiveScreen.M, event: ReceiveScreen.E.OnExchangeRateUpdated): Next<ReceiveScreen.M, ReceiveScreen.F>

    fun onWalletInfoLoaded(model: ReceiveScreen.M, event: ReceiveScreen.E.OnWalletInfoLoaded): Next<ReceiveScreen.M, ReceiveScreen.F>

    fun onAmountChange(model: ReceiveScreen.M, event: ReceiveScreen.E.OnAmountChange): Next<ReceiveScreen.M, ReceiveScreen.F>
}