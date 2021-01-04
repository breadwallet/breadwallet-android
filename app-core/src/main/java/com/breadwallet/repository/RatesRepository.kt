/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 4/15/19.
 * Copyright (c) 2019 breadwallet LLC
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
package com.breadwallet.repository

import android.content.Context
import android.util.Log
import com.breadwallet.legacy.presenter.entities.CurrencyEntity
import com.breadwallet.model.PriceChange
import com.breadwallet.model.TokenItem
import com.breadwallet.tools.sqlite.RatesDataSource
import com.breadwallet.tools.util.TokenUtil
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.math.BigDecimal
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for Currency Rates data. Contains methods for reading and writing rates data,
 * and abstracts away the underlying data source.
 */
class RatesRepository private constructor(private val mContext: Context) {

    private val dataSource = RatesDataSource.getInstance(mContext)
    private val changeEventChannel = BroadcastChannel<Unit>(CONFLATED)

    private val mCache = ConcurrentHashMap<String, CurrencyEntity>()
    private val mPriceChanges = ConcurrentHashMap<String, PriceChange>()

    /**
     * Retrieves the currency rate between two currencies.
     *
     * @param fromCurrency the 'from' currency
     * @param toCurrency   the 'to' currency
     * @return a currency entity encapsulating the two currencies and the rate between them, returns
     * null if either the currencies are empty or the rate wasn't found
     */
    private fun getCurrencyByCode(fromCurrency: String?, toCurrency: String?): CurrencyEntity? {
        if (fromCurrency.isNullOrBlank() || toCurrency.isNullOrBlank()) {
            return null
        }
        val cacheKey = getCacheKey(fromCurrency, toCurrency)
        var currencyEntity = mCache[cacheKey]
        if (currencyEntity == null && cacheKey != null) {
            currencyEntity = dataSource.getCurrencyByCode(fromCurrency, toCurrency)
            if (currencyEntity != null) {
                mCache[cacheKey] = currencyEntity
            }
        }
        return currencyEntity
    }

    /**
     * Persists the given set of currency entities, which map rates between currencies.
     *
     * @param currencyEntities the list of currency pairs and their rates to persist
     */
    fun putCurrencyRates(currencyEntities: Collection<CurrencyEntity>) {
        if (currencyEntities.isNullOrEmpty()) return
        val isRatesUpdateSuccessful = dataSource.putCurrencies(currencyEntities)
        if (isRatesUpdateSuccessful) {
            updateCache(currencyEntities)
        }
        changeEventChannel.offer(Unit)
    }

    /**
     * Retrieves all rates for a given currency (e.g., BTC -> USD etc.).
     *
     * @param currencyCode the currency code for which rates are being retrieved
     * @return a list of currency entities, which encapsulate two currencies and the rate between
     * them, returns an empty list if no rates are found for the specified currency
     */
    fun getAllRatesForCurrency(currencyCode: String): List<CurrencyEntity> {
        return dataSource.getAllCurrencies(currencyCode)
    }

    /**
     * Get the fiat value for a given amount of crypto. Because we only store the
     * fiat exchange rate for BTC we have to calculate the crypto to fiat rate from
     * crypto to BTC and BTC to fiat rates.
     *
     * @param cryptoAmount Amount of crypto we want to calculate.
     * @param cryptoCode   Code of the crypto we want to calculate.
     * @param fiatCode     Code of the fiat we want to get the exchange for.
     * @return The fiat value of the given crypto.
     */
    fun getFiatForCrypto(
        cryptoAmount: BigDecimal,
        cryptoCode: String,
        fiatCode: String
    ): BigDecimal? {
        val cryptoRate = getCurrencyByCode(cryptoCode, fiatCode)
        if (cryptoRate == null) {
            Log.e(TAG, "getFiatForCrypto: No fiat rate for $cryptoCode")
            return null
        }
        return cryptoAmount * cryptoRate.rate.toBigDecimal()
    }

    fun getFiatPerCryptoUnit(cryptoCode: String, fiatCode: String): BigDecimal {
        return getFiatForCrypto(BigDecimal.ONE, cryptoCode, fiatCode) ?: BigDecimal.ZERO
    }

    /**
     * Generates the cache key that is or would be used to store the rate between the two given
     * currencies.
     *
     * @param fromCurrency the 'from' currency
     * @param toCurrency   the 'to' currency
     * @return the cache key
     */
    private fun getCacheKey(fromCurrency: String?, toCurrency: String?): String? {
        return if (fromCurrency.isNullOrBlank() || toCurrency.isNullOrBlank()) {
            null
        } else {
            "$fromCurrency$CACHE_KEY_DELIMITER$toCurrency".toLowerCase(Locale.ROOT)
        }
    }

    /**
     * Parses a cache key to return the 'from' currency. For example, given the cache key for
     * BTC -> BCH, returns BTC.
     *
     * @param cacheKey the cache key being parsed
     * @return the 'from' currency, as encoded in the given cache key
     */
    private fun getCurrencyFromKey(cacheKey: String): String {
        return cacheKey.split(CACHE_KEY_DELIMITER.toRegex()).toTypedArray()[0]
    }

    /**
     * Updates the cache with a given set of currency rates.
     *
     * @param currencyEntities the list of currency pairs and their rates to put in the cache
     */
    private fun updateCache(currencyEntities: Collection<CurrencyEntity>) {
        for (currencyEntity in currencyEntities) {
            if (!currencyEntity.code.isBlank() && currencyEntity.rate > 0) {
                val cacheKey = getCacheKey(currencyEntity.iso, currencyEntity.code) ?: return
                mCache[cacheKey] = currencyEntity
            }
        }
    }

    /**
     * Update the price changes over the last 24hrs.
     */
    fun updatePriceChanges(priceChanges: Map<String, PriceChange>) {
        mPriceChanges.putAll(priceChanges)
    }

    /**
     * Get the price change over the last 24 hours for the given currency.
     *
     * @param currencyCode the currency we want to get the price change.
     * @return the price change.
     */
    fun getPriceChange(currencyCode: String): PriceChange? {
        return mPriceChanges[currencyCode.toUpperCase(Locale.ROOT)]
    }

    @get:Synchronized
    val allCurrencyCodesPossible: List<String>
        get() {
            return TokenUtil.getTokenItems()
                .map(TokenItem::symbol)
        }

    fun changes(): Flow<Unit> = changeEventChannel.asFlow()

    companion object {
        private val TAG = RatesRepository::class.java.name
        private const val CACHE_KEY_DELIMITER = ":"

        @Volatile
        private var mInstance: RatesRepository? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): RatesRepository {
            if (mInstance == null) {
                synchronized(RatesRepository::class.java) {
                    if (mInstance == null) {
                        mInstance = RatesRepository(context)
                    }
                }
            }
            return mInstance!!
        }
    }
}
