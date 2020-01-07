package com.breadwallet.ui.settings.currency

import com.spotify.mobius.First.first
import com.spotify.mobius.Init

val DisplayCurrencyInit = Init<DisplayCurrency.M, DisplayCurrency.F> { model ->
    first(model, setOf(DisplayCurrency.F.LoadCurrencies))
}