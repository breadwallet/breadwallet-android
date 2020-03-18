/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on on 6/22/17.
 * Copyright (c) 2017 breadwallet LLC
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
package com.platform.entities

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class TokenListMetaData(
    enabledCurrenciesList: List<TokenInfo> = listOf(),
    hiddenCurrenciesList: List<TokenInfo> = listOf()
) {
    var enabledCurrencies = mutableListOf<TokenInfo>()
    var hiddenCurrencies = mutableListOf<TokenInfo>()

    companion object {
        private const val ENABLED_CURRENCIES = "enabledCurrencies"
        private const val HIDDEN_CURRENCIES = "hiddenCurrencies"
    }

    init {
        enabledCurrencies.addAll(enabledCurrenciesList)
        hiddenCurrencies.addAll(hiddenCurrenciesList)
    }

    constructor(json: JSONObject) : this() {
        enabledCurrencies = jsonToMetaData(json.getJSONArray(ENABLED_CURRENCIES))
        hiddenCurrencies = jsonToMetaData(json.getJSONArray(HIDDEN_CURRENCIES))
    }

    @Throws(JSONException::class)
    private fun jsonToMetaData(json: JSONArray): MutableList<TokenInfo> =
        MutableList(json.length()) {
            val tokenStr = json.getString(it)
            when {
                tokenStr.contains(":") -> {
                    val parts =
                        tokenStr.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    TokenInfo(parts[0], true, parts[1])
                }
                else -> TokenInfo(tokenStr, false, null)
            }
        }

    data class TokenInfo(var symbol: String, var erc20: Boolean, var contractAddress: String?) {
        override fun toString() = when {
            erc20 -> "$symbol:$contractAddress"
            else -> symbol
        }
    }

    /**
     * TokenListMetaData:
     *
     *
     * Key: “token-list-metadata”
     *
     *
     * {
     * “classVersion”: 2, //used for versioning the schema
     * "enabledCurrencies": ["btc":"eth": "erc20:0xsd98fjetc"] enabled currencies
     * "hiddenCurrencies": "bch"] hidden currencies
     * }
     */
}
