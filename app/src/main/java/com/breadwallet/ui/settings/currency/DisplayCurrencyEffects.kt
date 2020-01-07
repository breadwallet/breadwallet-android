package com.breadwallet.ui.settings.currency

import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.BRSharedPrefs.putPreferredFiatIso
import com.breadwallet.ui.navigation.NavEffectTransformer
import com.spotify.mobius.flow.subtypeEffectHandler
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

object DisplayCurrencyEffects {
    fun createEffectHandler(
        navEffectHandler: NavEffectTransformer
    ) = subtypeEffectHandler<DisplayCurrency.F, DisplayCurrency.E> {
        addTransformer<DisplayCurrency.F.Nav>(navEffectHandler)
        addFunction(loadCurrencies())
        addFunction(setDisplayCurrency())
    }

    private fun loadCurrencies()
        : suspend (DisplayCurrency.F.LoadCurrencies) -> DisplayCurrency.E.OnCurrenciesLoaded = {
        val selectedCurrency = BRSharedPrefs.getPreferredFiatIso()
        val inputStream = BreadApp.getBreadContext().resources.openRawResource(R.raw.fiatcurrencies)
        val fiatCurrenciesString = inputStream.bufferedReader().use { it.readText() }
        val adapter = Moshi.Builder()
            .build()
            .adapter<List<FiatCurrency>>(
                Types.newParameterizedType(List::class.java, FiatCurrency::class.java)
            )
        val fiatCurrencies = adapter.fromJson(fiatCurrenciesString).orEmpty()

        DisplayCurrency.E.OnCurrenciesLoaded(
            selectedCurrency, fiatCurrencies
        )
    }

    private fun setDisplayCurrency()
        : suspend (DisplayCurrency.F.SetDisplayCurrency) -> DisplayCurrency.E.OnSelectedCurrencyUpdated =
        {
            putPreferredFiatIso(iso = it.currencyCode)
            DisplayCurrency.E.OnSelectedCurrencyUpdated(it.currencyCode)
        }
}