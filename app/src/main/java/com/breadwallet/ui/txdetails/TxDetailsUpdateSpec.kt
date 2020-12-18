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
package com.breadwallet.ui.txdetails

import com.spotify.mobius.Next

interface TxDetailsUpdateSpec {
    fun patch(model: TxDetails.M, event: TxDetails.E): Next<TxDetails.M, TxDetails.F> = when (event) {
        TxDetails.E.OnTransactionHashClicked -> onTransactionHashClicked(model)
        TxDetails.E.OnAddressClicked -> onAddressClicked(model)
        TxDetails.E.OnClosedClicked -> onClosedClicked(model)
        TxDetails.E.OnShowHideDetailsClicked -> onShowHideDetailsClicked(model)
        TxDetails.E.OnGiftResendClicked -> onGiftResendClicked(model)
        TxDetails.E.OnGiftReclaimClicked -> onGiftReclaimClicked(model)
        is TxDetails.E.OnTransactionUpdated -> onTransactionUpdated(model, event)
        is TxDetails.E.OnFiatAmountNowUpdated -> onFiatAmountNowUpdated(model, event)
        is TxDetails.E.OnMetaDataUpdated -> onMetaDataUpdated(model, event)
        is TxDetails.E.OnMemoChanged -> onMemoChanged(model, event)
    }

    fun onTransactionHashClicked(model: TxDetails.M): Next<TxDetails.M, TxDetails.F>

    fun onAddressClicked(model: TxDetails.M): Next<TxDetails.M, TxDetails.F>

    fun onClosedClicked(model: TxDetails.M): Next<TxDetails.M, TxDetails.F>

    fun onShowHideDetailsClicked(model: TxDetails.M): Next<TxDetails.M, TxDetails.F>

    fun onTransactionUpdated(model: TxDetails.M, event: TxDetails.E.OnTransactionUpdated): Next<TxDetails.M, TxDetails.F>

    fun onFiatAmountNowUpdated(model: TxDetails.M, event: TxDetails.E.OnFiatAmountNowUpdated): Next<TxDetails.M, TxDetails.F>

    fun onMetaDataUpdated(model: TxDetails.M, event: TxDetails.E.OnMetaDataUpdated): Next<TxDetails.M, TxDetails.F>

    fun onMemoChanged(model: TxDetails.M, event: TxDetails.E.OnMemoChanged): Next<TxDetails.M, TxDetails.F>

    fun onGiftResendClicked(model: TxDetails.M): Next<TxDetails.M, TxDetails.F>

    fun onGiftReclaimClicked(model: TxDetails.M): Next<TxDetails.M, TxDetails.F>
}