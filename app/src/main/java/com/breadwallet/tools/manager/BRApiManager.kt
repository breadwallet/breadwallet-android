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
import android.os.NetworkOnMainThreadException
import android.util.Log
import androidx.annotation.WorkerThread
import com.breadwallet.legacy.presenter.entities.CurrencyEntity
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs.getPreferredFiatIso
import com.breadwallet.tools.manager.BRSharedPrefs.putSecureTime
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.btc
import com.breadwallet.tools.util.eth
import com.platform.APIClient
import com.platform.APIClient.Companion.getBaseURL
import com.platform.network.service.CurrencyHistoricalDataClient.fetch24HrsChange
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.LinkedHashSet
import java.util.Locale

private const val TAG = "BRApiManager"
private const val BIT_PAY_URL = "https://bitpay.com/rates"
private const val DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z"
private const val DATE_HEADER = "date"
private const val CURRENCY_QUERY_STRING = "/rates?currency="
private const val CURRENCIES_PATH = "/currencies"
private const val NAME = "name"
private const val CODE = "code"
private const val RATE = "rate"
private const val TOKEN_RATES_URL_PREFIX =
    "https://min-api.cryptocompare.com/data/pricemulti?fsyms="
private const val TOKEN_RATES_URL_SUFFIX = "&tsyms="
private const val FSYMS_CHAR_LIMIT = 300
private const val CONTRACT_INITIAL_VALUE = "contract_initial_value"

class BRApiManager private constructor() {

    @WorkerThread
    fun updateFiatRates(context: Context) {
        if (UiUtils.isMainThread()) {
            throw NetworkOnMainThreadException()
        }
        val set: MutableSet<CurrencyEntity> = LinkedHashSet()
        try {
            val arr = fetchFiatRates(context)
            if (arr != null) {
                val length = arr.length()
                for (i in 0 until length) {
                    try {
                        val tmpObj = arr[i] as JSONObject
                        val code = tmpObj.getString(CODE)
                        CurrencyEntity(
                            name = tmpObj.getString(NAME),
                            code = code,
                            rate = tmpObj.getString(RATE).toFloat(),
                            iso = btc.toUpperCase(Locale.ROOT)
                        ).run(set::add)
                    } catch (e: JSONException) {
                        Log.e(TAG, "updateFiatRates: ", e)
                    }
                }
            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies, response string: $arr")
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateFiatRates: ", e)
        }
        val ratesRepository = RatesRepository.getInstance(context)

        if (set.size > 0) {
            ratesRepository.putCurrencyRates(set)
        }
    }

    /**
     * Synchronously updates RateRepository data.
     */
    @WorkerThread
    fun updateRatesSync(context: Context) {
        Log.d(TAG, "Fetching rates")
        //Update Crypto Rates
        val codeList = RatesRepository.getInstance(context).allCurrencyCodesPossible
        updateCryptoRates(context, codeList)

        //Update BTC/Fiat rates
        updateFiatRates(context)
    }

    @WorkerThread
    fun updateCryptoData(
        context: Context,
        currencyCodes: List<String>
    ) {
        //Update Crypto Rates
        updateCryptoRates(context, currencyCodes)
        //Update new tokens rate (e.g. CCC)
        fetchNewTokensData(context)
    }

    @WorkerThread
    private fun updateCryptoRates(
        context: Context,
        currencyCodeList: List<String>
    ) {
        val currencyCodeListChunks: MutableList<String> = ArrayList()
        var chunkStringBuilder = StringBuilder()
        for (currencyCode in currencyCodeList) {
            //check if there's enough space before appending the param.
            //code length + 1 (comma)
            if (chunkStringBuilder.length + currencyCode.length + 1 > FSYMS_CHAR_LIMIT) {
                //One chunk is full, add it and create new builder.
                currencyCodeListChunks.add(chunkStringBuilder.toString())
                chunkStringBuilder = StringBuilder()
            }
            chunkStringBuilder.append(currencyCode)
            chunkStringBuilder.append(',')
        }
        currencyCodeListChunks.add(chunkStringBuilder.toString())
        for (currencyCodeChunk in currencyCodeListChunks) {
            fetchCryptoRates(context, currencyCodeChunk, btc)
        }
    }

    /**
     * Gets the rates from cryptocompare for the provided codeList
     *
     * @param context       The Context
     * @param codeListChunk The comma separated code list.
     */
    private fun fetchCryptoRates(
        context: Context,
        codeListChunk: String,
        targetCurrencyCode: String
    ) {
        val codeListChunkUppercase = codeListChunk.toUpperCase(Locale.ROOT)
        val targetCodeUppercase = targetCurrencyCode.toUpperCase(Locale.ROOT)
        val url = buildString {
            append(TOKEN_RATES_URL_PREFIX)
            append(codeListChunkUppercase)
            append(TOKEN_RATES_URL_SUFFIX)
            append(targetCodeUppercase)
        }
        val result = urlGET(context, url)
        try {
            if (result.isNullOrBlank()) {
                Log.e(TAG, "fetchCryptoRates: Failed to fetch")
                return
            }
            val ratesJsonObject = JSONObject(result)
            if (ratesJsonObject.length() == 0) {
                Log.e(TAG, "fetchCryptoRates: empty json")
                return
            }
            val keys = ratesJsonObject.keys()
            val ratesList: MutableSet<CurrencyEntity> = LinkedHashSet()
            while (keys.hasNext()) {
                val currencyCode = keys.next()
                val jsonObject = ratesJsonObject.getJSONObject(currencyCode)
                val rate = jsonObject.getString(targetCodeUppercase).toFloat()
                CurrencyEntity(
                    code = targetCodeUppercase,
                    name = "",
                    rate = rate,
                    iso = currencyCode
                ).run(ratesList::add)
            }
            RatesRepository.getInstance(context).putCurrencyRates(ratesList)
        } catch (e: JSONException) {
            BRReportsManager.reportBug(e)
            Log.e(TAG, "fetchCryptoRates: ", e)
        }
    }

    fun fetchPriceChanges(
        context: Context,
        tokenList: List<String>
    ) {
        val toCurrency = getPreferredFiatIso(context)
        val priceChanges = fetch24HrsChange(context, tokenList, toCurrency)
        RatesRepository.getInstance(context).updatePriceChanges(priceChanges)
    }

    companion object {
        @Volatile
        private var mInstance: BRApiManager? = null

        @Synchronized
        fun getInstance(): BRApiManager {
            if (mInstance == null) {
                synchronized(BRApiManager::class.java) {
                    if (mInstance == null) {
                        mInstance = BRApiManager()
                    }
                }
            }
            return mInstance!!
        }

        @WorkerThread
        private fun fetchFiatRates(app: Context): JSONArray? {
            //Fetch the BTC-Fiat rates
            val url = getBaseURL() + CURRENCY_QUERY_STRING + btc.toUpperCase(Locale.ROOT)
            val jsonString = urlGET(app, url)
            var jsonArray: JSONArray? = null
            if (jsonString == null) {
                Log.e(TAG, "fetchFiatRates: failed, response is null")
                return null
            }
            try {
                val obj = JSONObject(jsonString)
                jsonArray = obj.getJSONArray(BRConstants.BODY)
            } catch (ex: JSONException) {
                Log.e(TAG, "fetchFiatRates: ", ex)
            }
            return jsonArray ?: backupFetchRates(app)
        }

        /**
         * Fetches data from /currencies api meant for new icos and tokens with no public rates yet.
         *
         * @param context Context
         */
        @WorkerThread
        private fun fetchNewTokensData(context: Context) {
            val url = getBaseURL() + CURRENCIES_PATH
            val tokenDataJsonString = urlGET(context, url)
            if (tokenDataJsonString.isNullOrBlank()) {
                Log.e(TAG, "fetchFiatRates: failed, response is null")
                return
            }
            try {
                val currencyEntities = mutableListOf<CurrencyEntity>()
                val tokenDataArray = JSONArray(tokenDataJsonString)
                for (i in 0 until tokenDataArray.length()) {
                    val tokenDataJsonObject = tokenDataArray.getJSONObject(i)
                    if (tokenDataJsonObject.has(CONTRACT_INITIAL_VALUE)) {
                        val priceInEth =
                            tokenDataJsonObject.getString(CONTRACT_INITIAL_VALUE)
                                .replace(eth.toUpperCase(Locale.ROOT), "")
                                .trim()
                        val name = tokenDataJsonObject.getString(BRConstants.NAME)
                        val code = tokenDataJsonObject.getString(BRConstants.CODE)
                        val ethCurrencyEntity = CurrencyEntity(
                            code = eth.toUpperCase(Locale.ROOT),
                            name = name,
                            rate = priceInEth.toFloat(),
                            iso = code
                        )
                        convertEthRateToBtc(context, ethCurrencyEntity)?.run(currencyEntities::add)
                    }
                }
                RatesRepository.getInstance(context).putCurrencyRates(currencyEntities)
            } catch (ex: JSONException) {
                Log.e(TAG, "fetchNewTokensData: ", ex)
            } catch (ex: NumberFormatException) {
                Log.e(TAG, "fetchNewTokensData: ", ex)
            }
        }

        private fun convertEthRateToBtc(
            context: Context,
            currencyEntity: CurrencyEntity?
        ): CurrencyEntity? {
            if (currencyEntity == null) {
                return null
            }
            val ethBtcExchangeRate = RatesRepository.getInstance(context)
                .getCurrencyByCode(eth, btc)
            if (ethBtcExchangeRate == null) {
                Log.e(TAG, "computeCccRates: ethBtcExchangeRate is null")
                return null
            }
            val newRate = (currencyEntity.rate * ethBtcExchangeRate.rate).toBigDecimal().toFloat()
            return CurrencyEntity(
                btc.toUpperCase(Locale.ROOT),
                currencyEntity.name,
                newRate,
                currencyEntity.iso
            )
        }

        /**
         * uses https://bitpay.com/rates to fetch the rates as a backup in case our api is down.
         *
         * @param context
         * @return JSONArray with rates data.
         */
        @WorkerThread
        private fun backupFetchRates(context: Context): JSONArray? {
            val ratesJsonString = urlGET(context, BIT_PAY_URL)
            var ratesJsonArray: JSONArray? = null
            if (ratesJsonString != null) {
                try {
                    val ratesJsonObject = JSONObject(ratesJsonString)
                    ratesJsonArray = ratesJsonObject.getJSONArray(BRConstants.DATA)
                } catch (e: JSONException) {
                    Log.e(TAG, "backupFetchRates: ", e)
                }
                return ratesJsonArray
            }
            return null
        }

        @WorkerThread
        fun urlGET(app: Context, myURL: String): String? {
            val builder = Request.Builder()
                .url(myURL)
                .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8)
                .header(BRConstants.HEADER_ACCEPT, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8)
                .get()
            val request = builder.build()
            var bodyText: String? = null
            val resp = APIClient.getInstance(app).sendRequest(request, false)
            try {
                bodyText = resp.bodyText
                val strDate = resp.headers[DATE_HEADER]
                if (strDate == null) {
                    Log.e(TAG, "urlGET: strDate is null!")
                    return bodyText
                }
                val formatter = SimpleDateFormat(DATE_FORMAT, Locale.US)
                val date = formatter.parse(strDate)
                if (date != null) {
                    val timeStamp = date.time
                    putSecureTime(app, timeStamp)
                }
            } catch (e: ParseException) {
                Log.e(TAG, "urlGET: ", e)
            }
            return bodyText
        }
    }
}
