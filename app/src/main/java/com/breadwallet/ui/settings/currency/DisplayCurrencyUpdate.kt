/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 1/7/20.
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
package com.breadwallet.ui.settings.currency

import com.breadwallet.ui.settings.currency.DisplayCurrency.E
import com.breadwallet.ui.settings.currency.DisplayCurrency.F
import com.breadwallet.ui.settings.currency.DisplayCurrency.M
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update

val CurrencyUpdate = Update<M, E, F> { model, event ->
    when (event) {
        E.OnBackClicked -> dispatch(effects(F.Nav.GoBack))
        E.OnFaqClicked -> dispatch(effects(F.Nav.GoToFaq))
        is E.OnCurrencySelected -> {
            dispatch(effects(F.SetDisplayCurrency(event.currencyCode)))
        }
        is E.OnCurrenciesLoaded -> {
            next(
                model.copy(
                    selectedCurrency = event.selectedCurrencyCode,
                    currencies = event.currencies
                )
            )
        }
        is E.OnSelectedCurrencyUpdated -> {
            next(model.copy(selectedCurrency = event.currencyCode))
        }
        else -> noChange()
    }
}
