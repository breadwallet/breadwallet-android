/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 10/11/19.
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
package com.breadwallet.ui.addwallets

import com.spotify.mobius.Update

import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next

object AddWalletsUpdate :
    Update<AddWalletsModel, AddWalletsEvent, AddWalletsEffect>,
    AddWalletsUpdateSpec {

    override fun update(model: AddWalletsModel, event: AddWalletsEvent) = patch(model, event)

    override fun onBackClicked(model: AddWalletsModel): Next<AddWalletsModel, AddWalletsEffect> {
        return dispatch(
            effects(
                AddWalletsEffect.GoBack
            )
        )
    }

    override fun onSearchQueryChanged(
        model: AddWalletsModel,
        event: AddWalletsEvent.OnSearchQueryChanged
    ): Next<AddWalletsModel, AddWalletsEffect> {
        return next(
            model.copy(searchQuery = event.query),
            effects(
                AddWalletsEffect.SearchTokens(event.query)
            )
        )
    }

    override fun onTokensChanged(
        model: AddWalletsModel,
        event: AddWalletsEvent.OnTokensChanged
    ): Next<AddWalletsModel, AddWalletsEffect> {
        return next(
            model.copy(
                tokens = event.tokens.toMutableList().apply { sortBy { it.currencyCode } }
            )
        )
    }

    override fun onAddWalletClicked(
        model: AddWalletsModel,
        event: AddWalletsEvent.OnAddWalletClicked
    ): Next<AddWalletsModel, AddWalletsEffect> {
        return dispatch(
            effects(
                AddWalletsEffect.AddWallet(event.token)
            )
        )
    }

    override fun onRemoveWalletClicked(
        model: AddWalletsModel,
        event: AddWalletsEvent.OnRemoveWalletClicked
    ): Next<AddWalletsModel, AddWalletsEffect> {
        return dispatch(
            effects(
                AddWalletsEffect.RemoveWallet(event.token)
            )
        )
    }
}
