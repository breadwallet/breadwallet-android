/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 5/18/20.
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
package com.breadwallet.util

import com.breadwallet.BuildConfig
import com.breadwallet.logger.logError
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

private const val MAX_PAY_ID_RETRIES = 2
private const val PAY_ID_DELIMITER = "$"

private const val PAY_ID_VERSION = "1.0"
private const val PAY_ID_ACCEPT_TYPE = "application/payId+json"

private const val PAY_ID_HEADER_VERSION = "PayID-Version"
private const val PAY_ID_FIELD_ADDRESSES = "addresses"
private const val PAY_ID_FIELD_ENVIRONMENT = "environment"
private const val PAY_ID_FIELD_CURRENCY = "paymentNetwork"
private const val PAY_ID_FIELD_ADDRESS_DETAILS = "addressDetails"
private const val PAY_ID_FIELD_ADDRESS = "address"

fun String?.isPayId() =
    this?.let {
        val parts = split(PAY_ID_DELIMITER)
        parts.size == 2 && parts[1].isNotBlank()
    } ?: false

class PayIdService(private val httpClient: OkHttpClient) {

    suspend fun getAddress(payId: String, currencyCode: CurrencyCode): PayIdResult {
        if (!payId.isPayId()) return PayIdResult.InvalidPayId
        val parts = payId.split(PAY_ID_DELIMITER)
        val url = "https://${parts[1]}/${parts[0]}"

        return try {
            requestAddress(url, currencyCode)?.let {
                if (it.isBlank()) PayIdResult.NoAddress
                PayIdResult.Success(it)
            } ?: PayIdResult.NoAddress
        } catch (ex: Exception) {
            logError("payID: ${ex.message}")
            PayIdResult.ExternalError
        }
    }

    private suspend fun requestAddress(
        url: String,
        currencyCode: String,
        attempt: Int = 0
    ): String? {
        val request = Request.Builder()
            .addHeader("Accept", PAY_ID_ACCEPT_TYPE)
            .addHeader(PAY_ID_HEADER_VERSION, PAY_ID_VERSION)
            .url(url)
            .get()
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: IOException) {
            return if (attempt == MAX_PAY_ID_RETRIES) {
                logError("Failed to retrieve address from PayId endpoint: $url.", e)
                throw e
            } else {
                requestAddress(url, currencyCode, attempt + 1)
            }
        }

        if (response.isSuccessful) {
            val bodyString = checkNotNull(response.body).string()

            try {
                val addressesArray = JSONObject(bodyString).getJSONArray(PAY_ID_FIELD_ADDRESSES)
                for (i in 0 until addressesArray.length()) {
                    val addressObject = addressesArray.getJSONObject(i)
                    val environment = addressObject.getString(PAY_ID_FIELD_ENVIRONMENT)
                    val payIdCurrency = addressObject.getString(PAY_ID_FIELD_CURRENCY)
                    
                    if (isTargetEnvironment(environment) && isTargetCurrency(
                            payIdCurrency,
                            currencyCode
                        )
                    ) {
                        return addressObject
                            .getJSONObject(PAY_ID_FIELD_ADDRESS_DETAILS)
                            .getString(PAY_ID_FIELD_ADDRESS)
                    }
                }
            } catch (e: JSONException) {
                logError("Failed to parse PayId address from response body", e)
            }
        } else {
            logError("PayId request failed with status '${response.code}'")
        }

        return null
    }

    private fun isTargetEnvironment(environment: String) = when {
        environment.equals("TESTNET", true) -> BuildConfig.BITCOIN_TESTNET
        else -> !BuildConfig.BITCOIN_TESTNET
    }

    private fun isTargetCurrency(payIdCurrency: String, currencyCode: CurrencyCode) =
        payIdCurrency.equals(
            currencyCode,
            true
        )
}

sealed class PayIdResult {
    data class Success(val address: String) : PayIdResult()
    object InvalidPayId : PayIdResult()
    object ExternalError : PayIdResult()
    object NoAddress : PayIdResult()
}


