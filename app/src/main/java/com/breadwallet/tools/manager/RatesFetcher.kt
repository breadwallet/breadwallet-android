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
import com.breadwallet.legacy.presenter.entities.CurrencyEntity
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.model.PriceChange
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.model.TokenItem
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.wallet.Interval
import com.platform.interfaces.AccountMetaDataProvider
import drewcarlson.coingecko.CoinGeckoService
import drewcarlson.coingecko.models.coins.CoinPrice
import kotlinx.coroutines.CoroutineScope
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
import java.util.Date
import java.util.Locale

/** Delay between each data request. */
private const val REFRESH_DELAY_MS = 60_000L
private const val REFRESH_ERROR_DELAY_MS = 2_000L

/** Default fiat currencies to retrieve exchange rates for. */
private val DEFAULT_FIATS = listOf("USD", "EUR")

class RatesFetcher(
    private val accountMetaData: AccountMetaDataProvider,
    private val coinGeckoService: CoinGeckoService,
    private val context: Context
) {
    private var currencyCodeToGeckoIdMap: Map<String, String>? = null

    /** Updates the token and exchange rate data for the current list of currency ids. */
    fun start(scope: CoroutineScope) {
        accountMetaData.enabledWallets()
            .onStart {
                getCoinGeckoIdMap()
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
                    TokenUtil.getTokenItemForCurrencyId(it)?.symbol
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

    private suspend fun getCoinGeckoIdMap(): Map<String, String> {
        val coinList = coinGeckoService.getCoinList()
        return currencyCodeToGeckoIdMap
            ?: TokenUtil.getTokenItems()
                .filter(TokenItem::isSupported)
                .map(TokenItem::symbol)
                .associateWith { symbol ->
                    coinList.firstOrNull { coin ->
                        coin.symbol.equals(symbol, true)
                    }?.id ?: ""
                }
                .filter { (_, id) -> id.isNotBlank() }
                .also { currencyCodeToGeckoIdMap = it }
    }

    private suspend fun updateRates(codes: List<String>? = null) {
        val ratesRepo = RatesRepository.getInstance(context)
        val codeList = codes ?: ratesRepo.allCurrencyCodesPossible

        val preferredFiat = BRSharedPrefs.getPreferredFiatIso()
        val fetchFiats = if (DEFAULT_FIATS.contains(preferredFiat.toUpperCase(Locale.ROOT))) {
            DEFAULT_FIATS
        } else {
            DEFAULT_FIATS + preferredFiat
        }

        val rates = fetchRates(codeList, fetchFiats)

        val currencyEntities = mutableSetOf<CurrencyEntity>()
        val priceChanges = mutableMapOf<String, PriceChange>()

        rates.entries.forEach { (currencyCode, coinPrice) ->
            if (currencyCode.isEmpty()) return@forEach

            fetchFiats.forEach { fiat ->
                currencyEntities.add(
                    CurrencyEntity(
                        name = fiat,
                        code = fiat,
                        rate = coinPrice.getPrice(fiat).toFloat(),
                        iso = currencyCode
                    )
                )
                val changePct = coinPrice.get24hrChangeOrNull(fiat)

                if (changePct != null) {
                    val changeAmount = coinPrice.getPrice(fiat) * changePct * 0.01
                    priceChanges[currencyCode] = PriceChange(changePct, changeAmount)
                }
            }
        }

        if (currencyEntities.isNotEmpty()) {
            ratesRepo.putCurrencyRates(currencyEntities)
        }

        if (priceChanges.isNotEmpty()) {
            ratesRepo.updatePriceChanges(priceChanges)
        }
    }

    private suspend fun fetchRates(
        codes: List<String>,
        fiats: List<String>
    ): Map<String, CoinPrice> {
        val coinGeckoIds = getCoinGeckoIdMap()
        val geckoIdToCurrencyCodeMap =
            coinGeckoIds.entries.associate { it.value to it.key }

        val ids = codes.mapNotNull { coinGeckoIds[it.toUpperCase(Locale.ROOT)] }
        val prices = coinGeckoService.getPrice(
            ids.joinToString(","),
            fiats.joinToString(","),
            include24hrChange = true
        )

        check(prices.isNotEmpty() && prices.entries.isNotEmpty())

        return prices.entries.associate { geckoIdToCurrencyCodeMap[it.key].orEmpty() to it.value }
    }

    suspend fun getHistoricalData(
        fromCurrency: String,
        toCurrency: String,
        interval: Interval
    ): List<PriceDataPoint> {
        val id = getCoinGeckoIdMap()
            .filterKeys { it.equals(fromCurrency, true) }
            .values
            .single()
        return coinGeckoService.getCoinMarketChartById(id, toCurrency, interval.days)
            .prices
            .map { p ->
                PriceDataPoint(
                    time = Date(p.first().toLong()),
                    closePrice = p.last().toDouble()
                )
            }
            .filterIndexed { index, _ ->
                interval.keepEvery == 0 || index % interval.keepEvery == 0
            }
    }
}

