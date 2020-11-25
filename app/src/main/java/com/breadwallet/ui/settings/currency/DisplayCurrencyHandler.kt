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
import com.breadwallet.logger.logError
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.BRSharedPrefs.putPreferredFiatIso
import com.breadwallet.tools.manager.COINGECKO_API_URL
import com.breadwallet.ui.settings.currency.DisplayCurrency.E
import com.breadwallet.ui.settings.currency.DisplayCurrency.F
import drewcarlson.mobius.flow.subtypeEffectHandler
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.util.Currency
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.days
import kotlin.time.toDuration

private const val CURRENCIES_URL = "${COINGECKO_API_URL}supported_vs_currencies"
private const val FIAT_CURRENCIES_FILENAME = "fiatcurrencies.json"

fun createDisplayCurrencyHandler(
    context: Context,
    http: OkHttpClient
) = subtypeEffectHandler<F, E> {
    addFunction<F.SetDisplayCurrency> {
        putPreferredFiatIso(iso = it.currencyCode)
        E.OnSelectedCurrencyUpdated(it.currencyCode)
    }
    addTransformer<F.LoadCurrencies> { effects ->
        effects.transform {
            val selectedCurrency = BRSharedPrefs.getPreferredFiatIso()
            val localCacheFile = File(context.filesDir, FIAT_CURRENCIES_FILENAME)
            val fiatCurrencies = if (localCacheFile.exists()) {
                localCacheFile.readText()
            } else {
                context.resources
                    .openRawResource(R.raw.fiatcurrencies)
                    .use { it.reader().readText() }
                    .also { localCacheFile.writeText(it) }
            }.toFiatCurrencies()

            emit(E.OnCurrenciesLoaded(selectedCurrency, fiatCurrencies))

            if (localCacheFile.isExpired()) {
                val selected = BRSharedPrefs.getPreferredFiatIso()
                val updatedCurrencies = http.fetchCurrencies(localCacheFile)
                emit(E.OnCurrenciesLoaded(selected, updatedCurrencies))
            }
        }
    }
}

private fun File.isExpired(): Boolean {
    return (lastModified() - System.currentTimeMillis())
        .toDuration(MILLISECONDS) > 1.days
}

private fun String.toFiatCurrencies(): List<String> {
    val jsonArray = try {
        JSONArray(this)
    } catch (e: JSONException) {
        logError("Failed to parse fiat currencies", e, this)
        JSONArray()
    }
    return List(jsonArray.length(), jsonArray::getString).filter { code ->
        code != null && Currency.getAvailableCurrencies()
            .any { it.currencyCode.equals(code, true) }
    }
}

private suspend fun OkHttpClient.fetchCurrencies(cache: File): List<String> {
    val request = Request.Builder().url(CURRENCIES_URL).build()
    val newCurrencies = try {
        @Suppress("BlockingMethodInNonBlockingContext")
        withContext(IO) {
            newCall(request).execute().use { response ->
                if (response.isSuccessful)
                    response.body?.string()
                else null
            }
        }
    } catch (e: IOException) {
        logError("Failed to fetch fiat currencies", e)
        null
    }
    return newCurrencies?.let { data ->
        cache.writeText(data)
        data.toFiatCurrencies()
    } ?: emptyList()
}