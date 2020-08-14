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
package com.breadwallet.ui.addwallets

import com.spotify.mobius.Next

interface AddWalletsUpdateSpec {
    fun patch(model: AddWallets.M, event: AddWallets.E): Next<AddWallets.M, AddWallets.F> = when (event) {
        AddWallets.E.OnBackClicked -> onBackClicked(model)
        is AddWallets.E.OnSearchQueryChanged -> onSearchQueryChanged(model, event)
        is AddWallets.E.OnTokensChanged -> onTokensChanged(model, event)
        is AddWallets.E.OnAddWalletClicked -> onAddWalletClicked(model, event)
        is AddWallets.E.OnRemoveWalletClicked -> onRemoveWalletClicked(model, event)
    }

    fun onBackClicked(model: AddWallets.M): Next<AddWallets.M, AddWallets.F>

    fun onSearchQueryChanged(model: AddWallets.M, event: AddWallets.E.OnSearchQueryChanged): Next<AddWallets.M, AddWallets.F>

    fun onTokensChanged(model: AddWallets.M, event: AddWallets.E.OnTokensChanged): Next<AddWallets.M, AddWallets.F>

    fun onAddWalletClicked(model: AddWallets.M, event: AddWallets.E.OnAddWalletClicked): Next<AddWallets.M, AddWallets.F>

    fun onRemoveWalletClicked(model: AddWallets.M, event: AddWallets.E.OnRemoveWalletClicked): Next<AddWallets.M, AddWallets.F>
}