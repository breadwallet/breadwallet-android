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

fun String?.isPayId() =
    this?.let {
        val parts = split(PAY_ID_DELIMITER)
        parts.size == 2 && parts[1].isNotBlank()
    } ?: false

class PayIdService(private val httpClient: OkHttpClient) {

    suspend fun getAddress(payId: String, currencyCode: CurrencyCode): PayIdResult {
        val currencyId = currencyCode.toPayIdCurrency() ?: return PayIdResult.CurrencyNotSupported
        if (!payId.isPayId()) return PayIdResult.InvalidPayId
        val parts = payId.split(PAY_ID_DELIMITER)
        val url = "https://${parts[1]}/${parts[0]}"

        return try {
            requestAddress(url, currencyId)?.let {
                if (it.isBlank()) PayIdResult.NoAddress
                PayIdResult.Success(it)
            } ?: PayIdResult.NoAddress
        } catch (ex: Exception) {
            logError("payID: ${ex.message}")
            PayIdResult.ExternalError
        }
    }

    private suspend fun requestAddress(url: String, currencyId: String, attempt: Int = 0): String? {
        val request = Request.Builder()
            .addHeader("Accept", "application/$currencyId+json")
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
                requestAddress(url, currencyId, attempt + 1)
            }
        }

        val body = if (response.isSuccessful) {
            val bodyString = checkNotNull(response.body).string()

            try {
                JSONObject(bodyString)
            } catch (e: JSONException) {
                logError("Failed to parse PayId address from response body", e)
                null
            }
        } else {
            logError("PayId request failed with status '${response.code}'")
            null
        }

        return body?.getJSONObject("addressDetails")?.getString("address")
    }
}

private fun CurrencyCode.toPayIdCurrency() = when {
    isBitcoin() -> if (BuildConfig.BITCOIN_TESTNET) "btc-testnet" else "btc-mainnet"
    isEthereum() -> if (BuildConfig.BITCOIN_TESTNET) "eth-testnet" else "eth-mainnet"
    isRipple() -> if (BuildConfig.BITCOIN_TESTNET) "xrpl-testnet" else "xrpl-mainnet"
    else -> null
}

sealed class PayIdResult {
    data class Success(val address: String) : PayIdResult()
    object InvalidPayId : PayIdResult()
    object ExternalError : PayIdResult()
    object NoAddress : PayIdResult()
    object CurrencyNotSupported : PayIdResult()
}


