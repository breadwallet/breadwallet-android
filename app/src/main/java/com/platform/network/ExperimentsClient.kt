/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 8/14/19.
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
package com.platform.network

import android.content.Context
import android.util.Log
import com.breadwallet.model.Experiment
import com.breadwallet.tools.manager.BRReportsManager
import com.platform.APIClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Client responsible of interacting with the me/experiments endpoint from where we fetch feature flags.
 */
interface ExperimentsClient {

    /**
     * Fetch the list of the available experiments.
     */
    fun getExperiments(context: Context): List<Experiment>

}

object ExperimentsClientImpl : ExperimentsClient {
    private val TAG = ExperimentsClientImpl::class.java.simpleName

    private const val EXPERIMENTS_ENDPOINT = "/me/experiments"
    private const val JSON_ID = "id"
    private const val JSON_NAME = "name"
    private const val JSON_ACTIVE = "active"
    private const val JSON_META = "meta"

    override fun getExperiments(context: Context): List<Experiment> {
        val url = APIClient.getBaseURL() + EXPERIMENTS_ENDPOINT
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        val response = APIClient.getInstance(context).sendRequest(request, true)

        if (!response.isSuccessful) {
            Log.e(TAG, "Failed to fetch experiments: ${response.code}")
            return emptyList()
        }

        if (response.bodyText.isNullOrBlank()) {
            return emptyList()
        }

        return parseExperiments(response.bodyText)
    }

    private fun parseExperiments(responseBody: String): List<Experiment>  =
            try {
                val jsonArray = JSONArray(responseBody)
                List(jsonArray.length()) { parseExperiment(jsonArray.getJSONObject(it)) }
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to parse experiments response", e)
                BRReportsManager.reportBug(e)
                emptyList()
            }

    private fun parseExperiment(messageJson: JSONObject) =
            Experiment(
                    messageJson.getInt(JSON_ID),
                    messageJson.getString(JSON_NAME),
                    messageJson.getBoolean(JSON_ACTIVE),
                    messageJson.optString(JSON_META))
}
