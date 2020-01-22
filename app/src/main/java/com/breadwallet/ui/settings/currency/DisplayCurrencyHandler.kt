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

import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.BRSharedPrefs.putPreferredFiatIso
import com.breadwallet.ui.navigation.NavEffectTransformer
import com.spotify.mobius.flow.subtypeEffectHandler
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

object DisplayCurrencyHandler {
    fun create(
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
