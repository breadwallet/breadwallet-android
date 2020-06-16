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

import com.breadwallet.ui.addwallets.AddWallets.E
import com.breadwallet.ui.addwallets.AddWallets.F
import com.breadwallet.ui.addwallets.AddWallets.M
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object AddWalletsUpdate : Update<M, E, F>, AddWalletsUpdateSpec {

    override fun update(model: M, event: E) = patch(model, event)

    override fun onBackClicked(model: M): Next<M, F> {
        return dispatch(
            effects(
                F.GoBack
            )
        )
    }

    override fun onSearchQueryChanged(
        model: M,
        event: E.OnSearchQueryChanged
    ): Next<M, F> {
        return next(
            model.copy(searchQuery = event.query),
            effects(
                F.SearchTokens(event.query)
            )
        )
    }

    override fun onTokensChanged(
        model: M,
        event: E.OnTokensChanged
    ): Next<M, F> {
        return next(
            model.copy(
                tokens = event.tokens
            )
        )
    }

    override fun onAddWalletClicked(
        model: M,
        event: E.OnAddWalletClicked
    ): Next<M, F> {
        return dispatch(
            effects(
                F.AddWallet(event.token)
            )
        )
    }

    override fun onRemoveWalletClicked(
        model: M,
        event: E.OnRemoveWalletClicked
    ): Next<M, F> {
        return dispatch(
            effects(
                F.RemoveWallet(event.token)
            )
        )
    }
}
