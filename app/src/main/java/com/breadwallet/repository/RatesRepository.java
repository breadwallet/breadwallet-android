/**
 * BreadWallet
 * <p/>
 * Created by Ahsan Butt on <ahsan.butt@breadwallet.com> 4/15/19.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.repository;

import android.content.Context;
import android.util.Log;

import com.breadwallet.model.PriceChange;
import com.breadwallet.model.PriceDataPoint;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.TokenUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.ui.wallet.Interval;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.platform.entities.TokenListMetaData;
import com.platform.network.service.CurrencyHistoricalDataClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for Currency Rates data. Contains methods for reading and writing rates data,
 * and abstracts away the underlying data source.
 */
public class RatesRepository {
    private static final String TAG = RatesRepository.class.getName();
    private static final String CACHE_KEY_DELIMITER = ":";
    private static RatesRepository mInstance;

    private ConcurrentHashMap<String, CurrencyEntity> mCache;
    private ConcurrentHashMap<String, PriceChange> mPriceChanges;

    private Context mContext;

    synchronized public static RatesRepository getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new RatesRepository(context);
        }

        return mInstance;
    }

    private RatesRepository(Context context) {
        mContext = context;
        mCache = new ConcurrentHashMap<>();
        mPriceChanges = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves the currency rate between two currencies.
     *
     * @param fromCurrency the 'from' currency
     * @return a currency entity encapsulating the two currencies and the rate between them, returns
     * null if either the currencies are empty or the rate wasn't found
     * @aram toCurrency the 'to' currency
     */
    public CurrencyEntity getCurrencyByCode(String fromCurrency, String toCurrency) {
        if (Utils.isNullOrEmpty(fromCurrency) || Utils.isNullOrEmpty(toCurrency)) {
            return null;
        }

        fromCurrency = TokenUtil.getExchangeRateCode(fromCurrency);
        String cacheKey = getCacheKey(fromCurrency, toCurrency);
        CurrencyEntity currencyEntity = mCache.get(cacheKey);

        if (currencyEntity == null) {
            currencyEntity = RatesDataSource.getInstance(mContext).getCurrencyByCode(mContext, fromCurrency, toCurrency);
            if (currencyEntity != null) {
                mCache.put(cacheKey, currencyEntity);
            }
        }

        return currencyEntity;
    }

    /**
     * Persists the given set of currency entities, which map rates between currencies.
     *
     * @param currencyEntities the list of currency pairs and their rates to persist
     */
    public void putCurrencyRates(Collection<CurrencyEntity> currencyEntities) {
        if (currencyEntities == null || currencyEntities.size() == 0) {
            return;
        }

        boolean isRatesUpdateSuccessful = RatesDataSource.getInstance(mContext).putCurrencies(mContext, currencyEntities);

        if (isRatesUpdateSuccessful) {
            updateCache(currencyEntities);
        }
    }

    /**
     * Retrieves all rates for a given currency (e.g., BTC -> USD etc.).
     *
     * @param currencyCode the currency code for which rates are being retrieved
     * @return a list of currency entities, which encapsulate two currencies and the rate between
     * them, returns an empty list if no rates are found for the specified currency
     */
    public List<CurrencyEntity> getAllRatesForCurrency(String currencyCode) {
        return RatesDataSource.getInstance(mContext).getAllCurrencies(mContext, currencyCode);
    }

    /**
     * Get the fiat value for a given amount of crypto. Because we only store the fiat exchange rate for BTC we have to
     * calculate the crypto to fiat rate from crypto to BTC and BTC to fiat rates.
     *
     * @param cryptoAmount Amount of crypto we want to calculate.
     * @param cryptoCode   Code of the crypto we want to calculate.
     * @param fiatCode     Code of the fiat we want to get the exchange for.
     * @return The fiat value of the given crypto.
     */
    public BigDecimal getFiatForCrypto(BigDecimal cryptoAmount, String cryptoCode, String fiatCode) {
        //fiat rate for btc
        CurrencyEntity btcRate = getCurrencyByCode(WalletBitcoinManager.BITCOIN_CURRENCY_CODE, fiatCode);
        //Btc rate for the given crypto
        CurrencyEntity cryptoBtcRate = getCurrencyByCode(cryptoCode, WalletBitcoinManager.BITCOIN_CURRENCY_CODE);
        if (btcRate == null) {
            Log.e(TAG, "getFiatForBch: No " + fiatCode + " rates for BTC");
            return null;
        }
        if (cryptoBtcRate == null) {
            Log.e(TAG, "getFiatForBch: No BTC rates for " + cryptoCode);
            return null;
        }

        return cryptoAmount.multiply(new BigDecimal(cryptoBtcRate.rate)).multiply(new BigDecimal(btcRate.rate));
    }

    /**
     * Generates the cache key that is or would be used to store the rate between the two given
     * currencies.
     *
     * @param fromCurrency the 'from' currency
     * @param toCurrency   the 'to' currency
     * @return the cache key
     */
    private String getCacheKey(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            return null;
        }

        return fromCurrency.toLowerCase() + CACHE_KEY_DELIMITER + toCurrency.toLowerCase();
    }

    /**
     * Parses a cache key to return the 'from' currency. For example, given the cache key for
     * BTC -> BCH, returns BTC.
     *
     * @param cacheKey the cache key being parsed
     * @return the 'from' currency, as encoded in the given cache key
     */
    private String getCurrencyFromKey(String cacheKey) {
        return cacheKey.split(CACHE_KEY_DELIMITER)[0];
    }

    /**
     * Updates the cache with a given set of currency rates.
     *
     * @param currencyEntities the list of currency pairs and their rates to put in the cache
     */
    private void updateCache(Collection<CurrencyEntity> currencyEntities) {
        for (CurrencyEntity currencyEntity : currencyEntities) {
            if (!Utils.isNullOrEmpty(currencyEntity.code) && currencyEntity.rate > 0) {
                String cacheKey = getCacheKey(currencyEntity.iso, currencyEntity.code);
                mCache.put(cacheKey, currencyEntity);
            }
        }
    }

    /**
     * Update the price changes over the last 24hrs.
     */
    public void updatePriceChanges(Map<String, PriceChange> priceChanges) {
        mPriceChanges.putAll(priceChanges);
    }

    /**
     * Get the price change over the last 24 hours for the given currency.
     *
     * @param currencyCode the currency we want to get the price change.
     * @return the price change.
     */
    public PriceChange getPriceChange(String currencyCode) {
        return mPriceChanges.get(TokenUtil.getExchangeRateCode(currencyCode));
    }

    public List<PriceDataPoint> getHistoricalData(String fromCurrencyCode, String toCurrencyCode, Interval interval) {
        fromCurrencyCode = TokenUtil.getExchangeRateCode(fromCurrencyCode);
        return CurrencyHistoricalDataClient.INSTANCE.getHistoricalData(mContext, fromCurrencyCode, toCurrencyCode, interval);
    }


    public synchronized List<String> getAllCurrencyCodesPossible(Context context) {
        LinkedHashSet<String> currencyCodes = new LinkedHashSet<>();
        for (TokenListMetaData.TokenInfo tokenInfo : BRConstants.DEFAULT_WALLETS) {
            currencyCodes.add(tokenInfo.symbol);
        }
        for (TokenItem tokenItem : TokenUtil.getTokenItems(context)) {
            currencyCodes.add(tokenItem.getExchangeRateCurrencyCode());
        }
        return new ArrayList<>(currencyCodes);
    }
}
