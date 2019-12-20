/**
 * BreadWallet
 * <p/>
 * Created by Alan Hill <alan.hill@breadwallet.com> on 6/5/19.
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

package com.platform.network.service

import android.content.Context
import android.util.Log
import com.breadwallet.model.PriceChange
import com.breadwallet.tools.util.Utils
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.repository.RatesRepository
import com.breadwallet.ui.wallet.Interval
import com.platform.APIClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * This provides the methods to retrieve the historical data desired.
 */
object CurrencyHistoricalDataClient {

    private val TAG = CurrencyHistoricalDataClient::class.java.simpleName

    /* JSON values from min-api.cryptocompare.com */
    private const val RESPONSE = "Response"
    private const val ERROR = "Error"
    private const val MESSAGE = "Message"
    private const val DATA = "Data"
    private const val TIME = "time"
    private const val CLOSE = "close"
    private const val RAW = "RAW"
    private const val PRICE_CHANGE_24_HOURS = "CHANGE24HOUR"
    private const val PRICE_PERCENTAGE_CHANGE_24_HOURS = "CHANGEPCT24HOUR"

    private const val BASE_URL = "https://min-api.cryptocompare.com/data"
    private const val HISTORICAL_DATA_URL = "$BASE_URL/%s?fsym=%s&tsym=%s&limit=%d"
    private const val PRICE_VARIATION_URL = "$BASE_URL/pricemultifull?fsyms=%s&tsyms=%s"

    private enum class History(val value: String) {
        MINUTE("histominute"),
        HOUR("histohour"),
        TODAY("histoday")
    }

    /**
     * How far back in time are we going to get the historical data.
     *
     * @param value the number of hours, minutes, days, etc
     * @param keepValue the value we want to keep, so for PAST_DAY the value is 8, that means we
     * want to keep every 8th value
     */
    private enum class Limit(val value: Int, val keepValue: Int) {
        // The number of minutes in a 24 period, keep every 8th value
        PAST_DAY(1440, 8),
        // the number of hours in the week, keep every value
        PAST_WEEK(168, 0),
        // the number days in a month, we use 30 for simplicity, keep all values
        PAST_MONTH(30, 0),
        // the number of days in 3 months, 90 for simplicity, keep all values
        PAST_3_MONTHS(90, 0),
        // the number of days in a year, keep every 2nd value
        PAST_YEAR(365, 2),
        // the number of days in 3 years, keep every 5th value
        PAST_3_YEARS(1095, 5),
    }

    fun getHistoricalData(context: Context, fromCurrency: String, toCurrency: String, interval: Interval): List<PriceDataPoint> {
        return when (interval) {
            Interval.ONE_DAY -> {
                getPastDay(context, fromCurrency, toCurrency)
            }
            Interval.ONE_WEEK -> {
                getPastWeek(context, fromCurrency, toCurrency)
            }
            Interval.ONE_MONTH -> {
                getPastMonth(context, fromCurrency, toCurrency)
            }
            Interval.THREE_MONTHS -> {
                getPastThreeMonths(context, fromCurrency, toCurrency)
            }
            Interval.ONE_YEAR -> {
                getPastYear(context, fromCurrency, toCurrency)
            }
            Interval.THREE_YEARS -> {
                getPastThreeYears(context, fromCurrency, toCurrency)
            }
        }
    }

    /**
     * Retrieve the currency's history for the past 24 hours.
     *
     * @param fromCurrency the currency you are looking up, eg BTC
     * @param toCurrency the currency you are converting to, eg USD
     */
    private fun getPastDay(context: Context, fromCurrency: String, toCurrency: String) = fetchHistoricalData(
            context,
            fromCurrency,
            toCurrency,
            CurrencyHistoricalDataClient.History.MINUTE,
            CurrencyHistoricalDataClient.Limit.PAST_DAY
    )

    /**
     * Retrieve the currency's history for the past 7 days.
     *
     * @param fromCurrency the currency you are looking up, eg BTC
     * @param toCurrency the currency you are converting to, eg USD
     */
    private fun getPastWeek(context: Context, fromCurrency: String, toCurrency: String) = fetchHistoricalData(
            context,
            fromCurrency,
            toCurrency,
            CurrencyHistoricalDataClient.History.HOUR,
            CurrencyHistoricalDataClient.Limit.PAST_WEEK
    )

    /**
     * Retrieve the currency's history for the past month.
     *
     * @param fromCurrency the currency you are looking up, eg BTC
     * @param toCurrency the currency you are converting to, eg USD
     */
    private fun getPastMonth(context: Context, fromCurrency: String, toCurrency: String) = fetchHistoricalData(
            context,
            fromCurrency,
            toCurrency,
            CurrencyHistoricalDataClient.History.TODAY,
            CurrencyHistoricalDataClient.Limit.PAST_MONTH
    )

    /**
     * Retrieve the currency's history for the past 3 months.
     *
     * @param fromCurrency the currency you are looking up, eg BTC
     * @param toCurrency the currency you are converting to, eg USD
     */
    private fun getPastThreeMonths(context: Context, fromCurrency: String, toCurrency: String) = fetchHistoricalData(
            context,
            fromCurrency,
            toCurrency,
            CurrencyHistoricalDataClient.History.TODAY,
            CurrencyHistoricalDataClient.Limit.PAST_3_MONTHS
    )

    /**
     * Retrieve the currency's history for the past year.
     *
     * @param fromCurrency the currency you are looking up, eg BTC
     * @param toCurrency the currency you are converting to, eg USD
     */
    private fun getPastYear(context: Context, fromCurrency: String, toCurrency: String) = fetchHistoricalData(
            context,
            fromCurrency,
            toCurrency,
            CurrencyHistoricalDataClient.History.TODAY,
            CurrencyHistoricalDataClient.Limit.PAST_YEAR
    )

    /**
     * Retrieve the currency's history for the past 3 years.
     *
     * @param fromCurrency the currency you are looking up, eg BTC
     * @param toCurrency the currency you are converting to, eg USD
     */
    private fun getPastThreeYears(context: Context, fromCurrency: String, toCurrency: String) = fetchHistoricalData(
            context,
            fromCurrency,
            toCurrency,
            CurrencyHistoricalDataClient.History.TODAY,
            CurrencyHistoricalDataClient.Limit.PAST_3_YEARS
    )

    /**
     * Retrieve the currencies price variation over the las 24 hours.
     */
    @JvmStatic
    fun fetch24HrsChange(context: Context, currencies: List<String>, toCurrency: String): Map<String, PriceChange> {
        val requestUrl = String.format(PRICE_VARIATION_URL, currencies.joinToString(","), toCurrency)
        val request = Request.Builder()
                .url(requestUrl)
                .get()
                .build()
        val response = APIClient.getInstance(context).sendRequest(request, false)
        return if (response.isSuccessful) {
            Log.d(TAG, response.bodyText)
            parsePriceChangeResponse(response.bodyText, currencies, toCurrency)
        } else {
            Log.e(TAG, "Failed to fetch price changre ${response.code}")
            emptyMap()
        }
    }

    private fun fetchHistoricalData(
            context: Context,
            fromCurrency: String,
            toCurrency: String,
            history: History,
            limit: Limit
    ): List<PriceDataPoint> {
        val requestUrl = String.format(HISTORICAL_DATA_URL, history.value, fromCurrency, toCurrency, limit.value)

        val request = Request.Builder()
                .url(requestUrl)
                .get()
                .build()
        val response = APIClient.getInstance(context).sendRequest(request, false)
        val dataList = mutableListOf<PriceDataPoint>()

        if (!response.isSuccessful || Utils.isNullOrEmpty(response.body)) {
            return emptyList()
        }

        try {
            val body = JSONObject(response.bodyText)
            val errorMessage = getErrorMessage(body)

            if (!Utils.isNullOrEmpty(errorMessage)) {
                Log.e(TAG, "There was an error requesting historical data for: $requestUrl, message: $errorMessage")
                return emptyList()
            }

            val dataArray = body.getJSONArray(DATA)
            for (i in 0 until dataArray.length()) {
                val dataObject = dataArray.getJSONObject(i)

                val date = if (dataObject.has(TIME)) {
                    val cal = Calendar.getInstance(Locale.getDefault())
                    cal.time.apply {
                        // since the api returns seconds, we need to convert to milliseconds for
                        // when we are setting the time
                        time = dataObject.getLong(TIME) * 1000
                    }
                } else {
                    Date()
                }

                val close = if (dataObject.has(CLOSE)) {
                    dataObject.getDouble(CLOSE)
                } else {
                    0.0
                }

                val dataPoint = PriceDataPoint(date, close)

                if (limit.keepValue == 0 || i % limit.keepValue == 0) {
                    dataList.add(dataPoint)
                }
            }
        } catch (exception: JSONException) {
            Log.e(TAG, "There was a problem parsing the JSON for $requestUrl", exception)
        }

        return dataList.toList()
    }

    private fun getErrorMessage(messageObject: JSONObject): String {
        try {
            if (messageObject.has(RESPONSE) && messageObject.getString(RESPONSE) == ERROR) {
                return messageObject.getString(MESSAGE)
            }
        } catch (exception: JSONException) {
            Log.e(TAG, "There was a problem checking to see if the response has an error", exception)
        }

        return ""
    }

    private fun parsePriceChangeResponse(
            responseBody: String,
            currencies: List<String>,
            toCurrency: String
    ): Map<String, PriceChange> =
            try {
                val priceChangeMap = mutableMapOf<String, PriceChange>()
                val jsonRawSection = JSONObject(responseBody).getJSONObject(RAW)
                currencies.forEach { currencyCode ->
                    if (jsonRawSection.has(currencyCode)) {
                        val currencyData = jsonRawSection.getJSONObject(currencyCode).getJSONObject(toCurrency)
                        val priceChange = PriceChange(
                                change24Hrs = currencyData.getDouble(PRICE_CHANGE_24_HOURS),
                                changePercentage24Hrs = currencyData.getDouble(PRICE_PERCENTAGE_CHANGE_24_HOURS))
                        priceChangeMap[currencyCode] = priceChange
                    }
                }
                priceChangeMap
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to parse the price variation", e)
                emptyMap()
            }

}
