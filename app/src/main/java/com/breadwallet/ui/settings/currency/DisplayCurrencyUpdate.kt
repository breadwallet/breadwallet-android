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

import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update

val CurrencyUpdate =
    Update<DisplayCurrency.M, DisplayCurrency.E, DisplayCurrency.F> { model, event ->
        when (event) {
            DisplayCurrency.E.OnBackClicked -> dispatch(effects(DisplayCurrency.F.Nav.GoBack))
            DisplayCurrency.E.OnFaqClicked -> dispatch(effects(DisplayCurrency.F.Nav.GoToFaq))
            is DisplayCurrency.E.OnCurrencySelected -> {
                dispatch(effects(DisplayCurrency.F.SetDisplayCurrency(event.currencyCode)))
            }
            is DisplayCurrency.E.OnCurrenciesLoaded -> {
                next(
                    model.copy(
                        selectedCurrency = event.selectedCurrencyCode,
                        currencies = event.currencies
                    )
                )
            }
            is DisplayCurrency.E.OnSelectedCurrencyUpdated -> {
                next(model.copy(selectedCurrency = event.currencyCode))
            }
            else -> noChange()
        }
    }
