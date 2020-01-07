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