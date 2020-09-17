/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 9/15/20.
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

private const val FIO_DELIMITER = "@"
private const val FIO_TESTNET_ENDPOINT = "https://testnet.fioprotocol.io/v1/chain/get_pub_address"
private const val FIO_MAINNET_ENDPOINT = "https://api.fio.services/v1/chain/get_pub_address"
private const val MAX_FIO_RETRIES = 2

private const val FIO_FIELD_PUBLIC_ADDRESS = "public_address"
private const val FIO_QUERY_KEY_DESTINATION_TAG = "dt"

fun String?.isFio() =
    this?.let {
        val parts = split(FIO_DELIMITER)
        parts.size == 2 && parts[1].isNotBlank()
    } ?: false

class FioService(private val httpClient: OkHttpClient) : AddressResolverService {

    override suspend fun resolveAddress(fioTarget: String, currencyCode: CurrencyCode, nativeCurrencyCode: CurrencyCode): AddressResult {
        if (!fioTarget.isFio()) return AddressResult.Invalid

        return try {
            requestAddress(fioTarget, currencyCode, nativeCurrencyCode)?.let { (address, destinationTag) ->
                if (address.isBlank()) AddressResult.NoAddress
                AddressResult.Success(address, destinationTag)
            } ?: AddressResult.NoAddress
        } catch (ex: Exception) {
            logError("FIO: ${ex.message}", ex)
            AddressResult.ExternalError
        }
    }

    private suspend fun requestAddress(
        fioTarget: String,
        currencyCode: String,
        nativeCurrencyCode: CurrencyCode,
        attempt: Int = 0
    ): Pair<String, String?>? {
        val request = Request.Builder()
            .url(if (BuildConfig.BITCOIN_TESTNET) FIO_TESTNET_ENDPOINT else FIO_MAINNET_ENDPOINT)
            .method(
                "POST",
                JSONObject()
                    .put("fio_address", fioTarget)
                    .put("chain_code", nativeCurrencyCode)
                    .put("token_code", currencyCode)
                    .toString()
                    .toRequestBody("application/json".toMediaType())
            )
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: IOException) {
            return if (attempt == MAX_FIO_RETRIES) {
                logError("Failed to retrieve address from FIO endpoint", e)
                throw e
            } else {
                requestAddress(fioTarget, currencyCode, nativeCurrencyCode, attempt + 1)
            }
        }

        if (response.isSuccessful) {
            val bodyString = checkNotNull(response.body).string()

            try {
                val addressString = JSONObject(bodyString).getString(FIO_FIELD_PUBLIC_ADDRESS)
                val (address, queryString) = parseAddressString(addressString)
                val destinationTag = queryString[FIO_QUERY_KEY_DESTINATION_TAG]

                return Pair(address, destinationTag)

            } catch (e: JSONException) {
                logError("Failed to parse FIO address from response body", e)
            }
        } else {
            logError("FIO request failed with status '${response.code}'")
        }

        return null
    }

    private fun parseAddressString(addressStr: String) : Pair<String, Map<String, String>> {
        val parts = addressStr.split("?")
        val address = parts[0]
        val queryStrMap = mutableMapOf<String, String>()

        try {
            if (parts.size > 1) {
                val kvs = parts[1].split("&")
                kvs.forEach {
                    val (key, value) = it.split("=")
                    queryStrMap[key] = value
                }
            }
        } catch (e: Exception) {
            logError("Malformed address string: $addressStr", e)
        }
        return Pair(address, queryStrMap)
    }
}