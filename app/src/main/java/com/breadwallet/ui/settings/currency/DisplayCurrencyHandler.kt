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

import android.content.Context
import com.breadwallet.R
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.BRSharedPrefs.putPreferredFiatIso
import com.breadwallet.ui.settings.currency.DisplayCurrency.E
import com.breadwallet.ui.settings.currency.DisplayCurrency.F
import drewcarlson.mobius.flow.subtypeEffectHandler
import org.json.JSONArray

private const val NAME = "name"
private const val CODE = "code"

fun createDisplayCurrencyHandler(
    context: Context
) = subtypeEffectHandler<F, E> {
    addFunction<F.SetDisplayCurrency> {
        putPreferredFiatIso(iso = it.currencyCode)
        E.OnSelectedCurrencyUpdated(it.currencyCode)
    }
    addFunction<F.LoadCurrencies> {
        val selectedCurrency = BRSharedPrefs.getPreferredFiatIso()
        val inputStream = context.resources.openRawResource(R.raw.fiatcurrencies)
        val fiatCurrenciesString = inputStream.bufferedReader().use { it.readText() }

        val jsonArray = runCatching { JSONArray(fiatCurrenciesString) }
            .getOrElse { JSONArray() }
        val fiatCurrencies = List(jsonArray.length()) { i ->
            runCatching {
                val obj = jsonArray.getJSONObject(i)
                FiatCurrency(
                    obj.getString(CODE),
                    obj.getString(NAME)
                )
            }.getOrNull()
        }.filterNotNull()

        E.OnCurrenciesLoaded(selectedCurrency, fiatCurrencies)
    }
}