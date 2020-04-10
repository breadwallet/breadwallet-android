/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 6/11/19.
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
import com.breadwallet.model.InAppMessage
import com.breadwallet.tools.manager.BRReportsManager
import com.platform.APIClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import com.platform.util.getStringOrNull
import org.json.JSONObject

/**
 * Client responsible of interacting with the messages endpoint.
 */
object InAppMessagesClient {
    private val TAG = InAppMessagesClient::class.java.simpleName

    private const val NOTIFICATIONS_ENDPOINT = "/me/messages"
    private const val JSON_ID = "id"
    private const val JSON_MESSAGE_ID = "message_id"
    private const val JSON_TITLE = "title"
    private const val JSON_BODY = "body"
    private const val JSON_IMAGE_URL = "image_url"
    private const val JSON_CTA = "cta"
    private const val JSON_CTA_URL = "cta_url"
    private const val JSON_TYPE = "type"

    /**
     * Fetch the latest messages, a filter by Message.Type can be applied.
     */
    fun fetchMessages(context: Context, type: InAppMessage.Type? = null): List<InAppMessage> {
        val url = APIClient.getBaseURL() + NOTIFICATIONS_ENDPOINT
        val request = Request.Builder()
                .url(url)
                .get()
                .build()
        val response = APIClient.getInstance(context).sendRequest(request, true)

        if (!response.isSuccessful) {
            Log.e(TAG, "Failed to fetch notifications: " + response.bodyText)
            return emptyList()
        }

        if (response.bodyText.isNullOrEmpty()) {
            return emptyList()
        }

        return parseMessages(response.bodyText).filter { it.type == (type ?: return@filter true) }
    }

    private fun parseMessages(responseBody: String): List<InAppMessage> =
            try {
                val jsonArray = JSONArray(responseBody)
                List(jsonArray.length()) { parseMessage(jsonArray.getJSONObject(it)) }
            } catch (e: JSONException) {
                Log.e(TAG, "Failed to parse the notification", e)
                BRReportsManager.reportBug(e)
                emptyList()
            }

    private fun parseMessage(messageJson: JSONObject) =
            InAppMessage(
                    messageJson.getString(JSON_ID),
                    InAppMessage.Type.fromString(messageJson.getString(JSON_TYPE)),
                    messageJson.getString(JSON_MESSAGE_ID),
                    messageJson.getString(JSON_TITLE),
                    messageJson.getString(JSON_BODY),
                    messageJson.getStringOrNull(JSON_CTA),
                    messageJson.getStringOrNull(JSON_CTA_URL),
                    messageJson.getStringOrNull(JSON_IMAGE_URL))

}
