/**
 * BreadWallet
 *
 * Created by Jade Byfield <jade@breadwallet.com> on 9/13/2018.
 * Copyright (c) 2018 breadwallet LLC
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
package com.breadwallet.tools.util

import android.content.Context
import com.breadwallet.appcore.BuildConfig
import com.breadwallet.logger.logError
import com.breadwallet.model.TokenItem
import com.breadwallet.theme.R
import com.breadwallet.tools.manager.BRReportsManager
import com.platform.APIClient.BRResponse
import com.platform.APIClient.Companion.getBaseURL
import com.platform.APIClient.Companion.getInstance
import com.platform.util.getBooleanOrDefault
import com.platform.util.getJSONArrayOrNull
import com.platform.util.getJSONObjectOrNull
import com.platform.util.getStringOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.Locale
import kotlin.properties.Delegates

object TokenUtil {
    private const val ENDPOINT_CURRENCIES = "/currencies"
    private const val ENDPOINT_CURRENCIES_SALE_ADDRESS = "/currencies?saleAddress="
    private const val FIELD_CODE = "code"
    private const val FIELD_NAME = "name"
    private const val FIELD_SCALE = "scale"
    private const val FIELD_CONTRACT_ADDRESS = "contract_address"
    private const val FIELD_IS_SUPPORTED = "is_supported"
    private const val FIELD_SALE_ADDRESS = "sale_address"
    private const val FIELD_CONTRACT_INITIAL_VALUE = "contract_initial_value"
    private const val FIELD_COLORS = "colors"
    private const val FIELD_CURRENCY_ID = "currency_id"
    private const val FIELD_TYPE = "type"
    private const val FIELD_ALTERNATE_NAMES = "alternate_names"
    private const val FIELD_COINGECK_ID = "coingecko"
    private const val ICON_DIRECTORY_NAME_WHITE_NO_BACKGROUND = "white-no-bg"
    private const val ICON_DIRECTORY_NAME_WHITE_SQUARE_BACKGROUND = "white-square-bg"
    private const val ICON_FILE_NAME_FORMAT = "%s.png"
    private const val START_COLOR_INDEX = 0
    private const val END_COLOR_INDEX = 1
    private const val TOKENS_FILENAME = "tokens.json"
    private const val TOKENS_FILENAME_TESTNET = "tokens-testnet.json"
    private const val ETHEREUM = "ethereum"
    private const val ETHEREUM_TESTNET = "ropsten"
    private const val TESTNET = "testnet"
    private const val MAINNET = "mainnet"

    private var isMainnet: Boolean by Delegates.notNull()
    private lateinit var context: Context
    private var tokenItems: List<TokenItem> = ArrayList()
    private var tokenMap: Map<String, TokenItem> = HashMap()
    private val initLock = Mutex(locked = true)

    suspend fun waitUntilInitialized() = initLock.withLock { Unit }

    /**
     * When the app first starts, fetch our local copy of tokens.json from the resource folder
     *
     * @param context The Context of the caller
     */
    fun initialize(context: Context, forceLoad: Boolean, isMainnet: Boolean) {
        TokenUtil.isMainnet = isMainnet
        TokenUtil.context = context
        val fileName = if (isMainnet) TOKENS_FILENAME else TOKENS_FILENAME_TESTNET
        val tokensFile = File(context.filesDir, fileName)
        if (!tokensFile.exists() || forceLoad) {
            try {
                initLock.tryLock()
                val tokens = context.resources
                    .openRawResource(
                        if (isMainnet) {
                            com.breadwallet.appcore.R.raw.tokens
                        } else {
                            com.breadwallet.appcore.R.raw.tokens_testnet
                        }
                    ).reader().use { it.readText() }

                // Copy the APK tokens.json to a file on internal storage
                saveDataToFile(context, tokens, fileName)
                loadTokens(parseJsonToTokenList(tokens))
                initLock.unlock()
            } catch (e: IOException) {
                BRReportsManager.error("Failed to read res/raw/tokens.json", e)
            }
        } else {
            initLock.tryLock()
            fetchTokensFromServer()
            initLock.unlock()
        }
    }

    /**
     * This method can either fetch the list of supported tokens, or fetch a specific token by saleAddress
     * Request the list of tokens we support from the /currencies endpoint
     *
     * @param tokenUrl The URL of the endpoint to get the token metadata from.
     */
    private fun fetchTokensFromServer(tokenUrl: String): BRResponse {
        val request = Request.Builder()
            .get()
            .url(tokenUrl)
            .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8)
            .header(BRConstants.HEADER_ACCEPT, BRConstants.CONTENT_TYPE_JSON)
            .build()
        return getInstance(context).sendRequest(request, true)
    }

    @Synchronized
    fun getTokenItems(): List<TokenItem> {
        if (tokenItems.isEmpty()) {
            loadTokens(getTokensFromFile())
        }
        return tokenItems
    }

    /**
     * Return a TokenItem with the given currency code or null if non TokenItem has the currency code.
     *
     * @param currencyCode The currency code of the token we are looking.
     * @return The TokenItem with the given currency code or null.
     */
    fun tokenForCode(currencyCode: String): TokenItem? {
        return tokenMap[currencyCode.toLowerCase(Locale.ROOT)]
    }

    fun tokenForCoingeckoId(coingeckoId: String): TokenItem? {
        val matches = coingeckoIdMap.filterValues { coingeckoId.equals(it, true) }
        return matches.keys.firstOrNull()?.run(TokenUtil::tokenForCode)
            ?: tokenItems.firstOrNull {
                coingeckoId.equals(it.coingeckoId, true)
            }
    }

    fun tokenForCurrencyId(currencyId: String): TokenItem? {
        return tokenItems.find { it.currencyId.equals(currencyId, true) }
    }

    fun coingeckoIdForCode(code: String): String? {
        return coingeckoIdMap[code.toUpperCase(Locale.ROOT)]
            ?: tokenForCode(code)?.coingeckoId
    }

    private fun fetchTokensFromServer() {
        if (!isMainnet) return
        val response = fetchTokensFromServer(getBaseURL() + ENDPOINT_CURRENCIES)
        if (response.isSuccessful && response.bodyText.isNotEmpty()) {
            // Synchronize on the class object since getTokenItems is static and also synchronizes
            // on the class object rather than on an instance of the class.
            synchronized(TokenItem::class.java) {
                val responseBody = response.bodyText

                // Check if the response from the server is valid JSON before trying to save & parse.
                if (Utils.isValidJSON(responseBody)) {
                    saveDataToFile(context, responseBody, TOKENS_FILENAME)
                    loadTokens(parseJsonToTokenList(responseBody))
                }
            }
        } else {
            logError("failed to fetch tokens: ${response.code}")
        }
    }

    private fun parseJsonToTokenList(jsonString: String): ArrayList<TokenItem> {
        val tokenJsonArray = try {
            JSONArray(jsonString)
        } catch (e: JSONException) {
            BRReportsManager.error("Failed to parse Token list JSON.", e)
            JSONArray()
        }
        return List(tokenJsonArray.length()) { i ->
            try {
                tokenJsonArray.getJSONObject(i).asTokenItem()
            } catch (e: JSONException) {
                BRReportsManager.error("Failed to parse Token JSON.", e)
                null
            }
        }.filterNotNull().run(::ArrayList)
    }

    private fun saveDataToFile(context: Context, jsonResponse: String, fileName: String) {
        try {
            File(context.filesDir.absolutePath, fileName).writeText(jsonResponse)
        } catch (e: IOException) {
            BRReportsManager.error("Failed to write tokens.json file", e)
        }
    }

    private fun getTokensFromFile(): List<TokenItem> = try {
        val fileName = if (isMainnet) TOKENS_FILENAME else TOKENS_FILENAME_TESTNET
        val file = File(context.filesDir.path, fileName)
        parseJsonToTokenList(file.readText())
    } catch (e: IOException) {
        BRReportsManager.error("Failed to read tokens.json file", e)
        tokenItems
    }

    fun getTokenIconPath(currencyCode: String, withBackground: Boolean): String? {
        val bundleResource = ServerBundlesHelper
            .getExtractedPath(
                context,
                ServerBundlesHelper.getBundle(ServerBundlesHelper.Type.TOKEN),
                null
            )
        val iconFileName = ICON_FILE_NAME_FORMAT.format(currencyCode.toLowerCase(Locale.ROOT))
        val iconDirectoryName = if (withBackground) {
            ICON_DIRECTORY_NAME_WHITE_SQUARE_BACKGROUND
        } else {
            ICON_DIRECTORY_NAME_WHITE_NO_BACKGROUND
        }
        val iconDir = File(bundleResource, iconDirectoryName)
        val iconFile = File(iconDir, iconFileName)
        return if (iconFile.exists()) iconFile.absolutePath else null
    }

    fun getTokenStartColor(currencyCode: String): String? {
        val tokenItem = tokenMap[currencyCode.toLowerCase(Locale.ROOT)]
        return if (tokenItem != null && !tokenItem.startColor.isNullOrBlank()) {
            tokenItem.startColor
        } else {
            context.getString(R.color.wallet_delisted_token_background)
        }
    }

    fun getTokenEndColor(currencyCode: String): String? {
        val tokenItem = tokenMap[currencyCode.toLowerCase(Locale.ROOT)]
        return if (tokenItem != null && !tokenItem.endColor.isNullOrBlank()) {
            tokenItem.endColor
        } else {
            context.getString(R.color.wallet_delisted_token_background)
        }
    }

    fun isTokenSupported(symbol: String): Boolean {
        return tokenMap[symbol.toLowerCase(Locale.ROOT)]?.isSupported ?: true
    }

    private fun loadTokens(tokenItems: List<TokenItem>) {
        val native = tokenItems.filter(TokenItem::isNative).sortedBy { it.name }
        val tokens = tokenItems.filterNot(TokenItem::isNative).sortedBy { it.symbol }
        TokenUtil.tokenItems = native + tokens
        tokenMap = TokenUtil.tokenItems.associateBy { item ->
            item.symbol.toLowerCase(Locale.ROOT)
        }
    }

    private fun JSONObject.asTokenItem(): TokenItem? = try {
        val (startColor, endColor) = getJSONArrayOrNull(FIELD_COLORS)?.run {
            getStringOrNull(START_COLOR_INDEX) to getStringOrNull(END_COLOR_INDEX)
        } ?: null to null

        val name = getString(FIELD_NAME)
        TokenItem(
            address = getStringOrNull(FIELD_CONTRACT_ADDRESS),
            symbol = getString(FIELD_CODE),
            name = name,
            image = null,
            isSupported = getBooleanOrDefault(FIELD_IS_SUPPORTED, true),
            currencyId = getString(FIELD_CURRENCY_ID),
            type = getString(FIELD_TYPE),
            startColor = startColor,
            endColor = endColor,
            coingeckoId = getJSONObjectOrNull(FIELD_ALTERNATE_NAMES)
                ?.getStringOrNull(FIELD_COINGECK_ID)
        )
    } catch (e: JSONException) {
        BRReportsManager.error("Token JSON: $this")
        BRReportsManager.error("Failed to create TokenItem from JSON.", e)
        null
    }
}
