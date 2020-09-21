/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
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
package com.breadwallet.tools.manager

import android.content.Context
import com.breadwallet.BuildConfig
import com.breadwallet.legacy.presenter.entities.CurrencyEntity
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.model.PriceChange
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.wallet.Interval
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.platform.interfaces.AccountMetaDataProvider
import com.platform.util.getDoubleOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Date
import java.util.Locale

/** Delay between each data request. */
private const val REFRESH_DELAY_MS = 60_000L
private const val REFRESH_ERROR_DELAY_MS = 2_000L

/** Default fiat currencies to retrieve exchange rates for. */
private const val USD = "USD"
private const val EUR = "EUR"
private val DEFAULT_FIATS = listOf(USD, EUR)

/** Hard-coded AVM currencies and related remote config keys. */
private const val AVM_CURRENCY_CODE = "avm"
private const val EUR_AVM_CURRENCY_CODE = "eur.avm"
private const val AVM_TO_EUR_KEY = "AVM_TO_EUR"
private const val EUR_AVM_TO_EUR_KEY = "EUR_AVM_TO_EUR"

private const val COINGECKO_API_URL = "https://api.coingecko.com/api/v3/"

class RatesFetcher(
    private val accountMetaData: AccountMetaDataProvider,
    private val okhttp: OkHttpClient,
    private val context: Context
) {
    /** Updates the token and exchange rate data for the current list of currency ids. */
    fun start(scope: CoroutineScope) {
        accountMetaData.enabledWallets()
            .onStart {
                updateRates()
            }
            .filter { it.isNotEmpty() }
            // Ignore emissions from order changes
            .distinctUntilChanged { old, new ->
                old.size == new.size &&
                    old.containsAll(new) &&
                    new.containsAll(old)
            }
            .map { currencyIds ->
                currencyIds.mapNotNull {
                    TokenUtil.tokenForCurrencyId(it)?.symbol
                }
            }
            // Repeat the latest list every 60 seconds
            .transformLatest { codes ->
                while (true) {
                    emit(codes)
                    delay(REFRESH_DELAY_MS)
                }
            }
            .combine(BRSharedPrefs.preferredFiatIsoChanges()) { codes, _ -> codes }
            .onEach { codes ->
                logDebug("Updating currency and rate data", codes)
                updateRates(codes)
            }
            .retry { e ->
                logError("Failed to update currency and rate data", e)
                delay(REFRESH_ERROR_DELAY_MS)
                true
            }
            .launchIn(scope)
    }

    private suspend fun updateRates(codes: List<String>? = null) {
        val ratesRepo = RatesRepository.getInstance(context)
        val codeList = codes ?: ratesRepo.allCurrencyCodesPossible

        val preferredFiat = BRSharedPrefs.getPreferredFiatIso()
        val fetchFiats = (DEFAULT_FIATS + preferredFiat.toUpperCase(Locale.ROOT)).distinct()

        val rates = fetchRates(codeList, fetchFiats)

        val currencyEntities = mutableSetOf<CurrencyEntity>()
        val priceChanges = mutableMapOf<String, PriceChange>()

        rates.forEach { (currencyCode, coinPrice) ->
            if (currencyCode.isEmpty()) return@forEach

            fetchFiats.forEach { fiat ->
                val cryptoSymbol = currencyCode.toUpperCase(Locale.ROOT)
                val rate = coinPrice.optDouble(fiat.toLowerCase(Locale.ROOT), 0.0).toFloat()
                currencyEntities.add(
                    CurrencyEntity(
                        name = fiat,
                        code = fiat.toUpperCase(Locale.ROOT),
                        rate = rate,
                        iso = cryptoSymbol
                    )
                )
                val changePct = coinPrice.optDouble("${fiat}_24h_change".toLowerCase(Locale.ROOT), 0.0)
                val changeAmount = rate * changePct * 0.01
                priceChanges[cryptoSymbol] = PriceChange(changePct, changeAmount)
            }
        }

        if (BuildConfig.USE_REMOTE_CONFIG) {
            currencyEntities.addAll(getFixedRates())
        }

        if (currencyEntities.isNotEmpty()) {
            ratesRepo.putCurrencyRates(currencyEntities)
        }

        if (priceChanges.isNotEmpty()) {
            ratesRepo.updatePriceChanges(priceChanges)
        }
    }

    /** Pull hard-coded rates from remote config. */
    private fun getFixedRates(): List<CurrencyEntity> {
        val remoteConfig = Firebase.remoteConfig
        return mapOf(
            AVM_CURRENCY_CODE to remoteConfig.getDouble(AVM_TO_EUR_KEY),
            EUR_AVM_CURRENCY_CODE to remoteConfig.getDouble(EUR_AVM_TO_EUR_KEY)
        ).entries
            .filter { it.value != 0.0 }
            .map { (currencyCode, price) ->
                CurrencyEntity(
                    name = EUR,
                    code = EUR,
                    rate = price.toFloat(),
                    iso = currencyCode
                )
            }
    }

    private suspend fun fetchRates(
        codes: List<String>,
        fiats: List<String>
    ): Map<String, JSONObject> {
        val ids = codes.mapNotNull { currencyCode ->
            TokenUtil.coingeckoIdForCode(currencyCode)
        }

        val url = buildString {
            append(COINGECKO_API_URL)
            append("simple/price")
            append("?ids=${ids.joinToString(",")}")
            append("&vs_currencies=${fiats.joinToString(",")}")
            append("&include_24hr_change=true")
        }
        val request = Request.Builder()
            .get()
            .url(url)
            .build()
        val res = withContext(Dispatchers.IO) {
            runCatching {
                okhttp.newCall(request).execute()
            }.onFailure { e ->
                logError("Failed to fetch data", e)
            }
        }.getOrNull()

        return if (res == null || !res.isSuccessful) {
            emptyMap()
        } else {
            val json = runCatching {
                val bodyString = checkNotNull(res.body).string()
                JSONObject(bodyString)
            }.onFailure { e ->
                logError("Failed to parse response body", e)
            }.getOrNull()

            if (json == null) {
                emptyMap()
            } else {
                ids.associateWith { json.getJSONObject(it) }
                    .mapKeys { (key, _) ->
                        TokenUtil.tokenForCoingeckoId(key)?.symbol ?: ""
                    }
                    .filterKeys { it.isNotBlank() }
            }
        }
    }

    suspend fun getHistoricalData(
        fromCurrency: String,
        toCurrency: String,
        interval: Interval
    ): List<PriceDataPoint> {
        val id = TokenUtil.coingeckoIdForCode(fromCurrency) ?: return emptyList()

        val url = buildString {
            append(COINGECKO_API_URL)
            append("coins/$id/market_chart")
            append("?vs_currency=$toCurrency")
            append("&days=${interval.days}")
        }
        val request = Request.Builder()
            .get()
            .url(url)
            .build()
        val res = withContext(Dispatchers.IO) {
            runCatching {
                okhttp.newCall(request).execute()
            }.onFailure { e ->
                logError("Failed to fetch data", e)
            }
        }.getOrNull()

        return if (res == null || !res.isSuccessful) {
            emptyList()
        } else {
            val json = runCatching {
                val bodyString = checkNotNull(res.body).string()
                JSONObject(bodyString)
            }.onFailure { e ->
                logError("Failed to parse response body", e)
            }.getOrNull()

            if (json == null) {
                emptyList()
            } else {
                val prices = json.getJSONArray("prices")
                (0 until prices.length())
                    .filter { interval.keepEvery == 0 || it % interval.keepEvery == 0 }
                    .map { i ->
                        val data = prices.getJSONArray(i)
                        PriceDataPoint(
                            time = Date(data.getLong(0)),
                            closePrice = data.getDouble(1)
                        )
                    }
            }
        }
    }

    suspend fun getMarketData(
        fromCurrency: String,
        toCurrency: String
    ): MarketDataResult {
        val id = TokenUtil.coingeckoIdForCode(fromCurrency) ?: return MarketDataResult.Failure.UnsupportedCurrency

        val url = buildString {
            append(COINGECKO_API_URL)
            append("coins/$id/")
            append("?market_data=true")
            append("&localization=false")
            append("&tickers=false")
            append("&community_data=false")
            append("&developer_data=false")
        }
        val request = Request.Builder()
            .get()
            .url(url)
            .build()
        val res = withContext(Dispatchers.IO) {
            runCatching {
                okhttp.newCall(request).execute()
            }.onFailure { e ->
                logError("Failed to fetch data", e)
            }
        }.getOrNull()

        return if (res == null || !res.isSuccessful) {
            MarketDataResult.Failure.Fetch
        } else {
            val json = runCatching {
                val bodyString = checkNotNull(res.body).string()
                JSONObject(bodyString)
            }.onFailure { e ->
                logError("Failed to parse response body", e)
            }.getOrNull()

            if (json == null) {
                MarketDataResult.Failure.BadData
            } else {
                val marketData = json.getJSONObject("market_data")
                val lowerToCurrency = toCurrency.toLowerCase(Locale.ROOT)

                val marketCap = marketData.getJSONObject("market_cap").getDoubleOrNull(lowerToCurrency)?.toBigDecimal()
                val totalVolume = marketData.getJSONObject("total_volume").getDoubleOrNull(lowerToCurrency)?.toBigDecimal()
                val high = marketData.getJSONObject("high_24h").getDoubleOrNull(lowerToCurrency)?.toBigDecimal()
                val low = marketData.getJSONObject("low_24h").getDoubleOrNull(lowerToCurrency)?.toBigDecimal()
                MarketDataResult.Success(marketCap, totalVolume, high, low)
            }
        }
    }

}

sealed class MarketDataResult  {
    data class Success(
        val marketCap: BigDecimal?,
        val totalVolume: BigDecimal?,
        val high24h: BigDecimal?,
        val low24h: BigDecimal?
    ) : MarketDataResult()

    sealed class Failure : MarketDataResult() {
        object Fetch : Failure()
        object BadData : Failure()
        object UnsupportedCurrency : Failure()
    }
}

