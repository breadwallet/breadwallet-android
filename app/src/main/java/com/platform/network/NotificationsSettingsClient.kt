/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 7/11/19.
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
package com.platform.network

import android.content.Context
import android.util.Log
import com.breadwallet.BuildConfig
import com.breadwallet.tools.util.BRConstants
import com.platform.APIClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject

/**
 * Client responsible of registering and unregistering the Firebase token for push notifications
 * in our backend.
 */
interface NotificationsSettingsClient {

    /**
     * Send push notifications token. Return True if the token was successfully registered,
     * otherwise false.
     */
    fun registerToken(context: Context, token: String): Boolean

    /**
     * Remove push notifications token. Return True if the token was successfully unregistered,
     * otherwise false.
     */
    fun unregisterToken(context: Context, token: String): Boolean

}

object NotificationsSettingsClientImpl : NotificationsSettingsClient {

    private val TAG = NotificationsSettingsClientImpl::class.java.simpleName
    private const val ENDPOINT_PUSH_DEVICES = "/me/push-devices"
    private const val ENDPOINT_DELETE_PUSH_DEVICES = "/me/push-devices/apns/"
    private const val PUSH_SERVICE = "fcm"
    private const val KEY_TOKEN = "token"
    private const val KEY_SERVICE = "service"
    private const val KEY_DATA = "data"
    private const val KEY_DATA_ENVIRONMENT = "e"
    private const val KEY_DATA_BUNDLE_ID = "b"
    private const val ENVIRONMENT_DEVELOPMENT = "d"
    private const val ENVIRONMENT_PRODUCTION = "p"

    override fun registerToken(context: Context, token: String): Boolean {
        val url = APIClient.getBaseURL() + ENDPOINT_PUSH_DEVICES
        val deviceEnvironment = if (BuildConfig.DEBUG) {
            ENVIRONMENT_DEVELOPMENT
        } else {
            ENVIRONMENT_PRODUCTION
        }

        try {
            val payload = JSONObject()
            payload.put(KEY_TOKEN, token)
            payload.put(KEY_SERVICE, PUSH_SERVICE)

            val data = JSONObject()
            data.put(KEY_DATA_ENVIRONMENT, deviceEnvironment)
            data.put(KEY_DATA_BUNDLE_ID, context.packageName)
            payload.put(KEY_DATA, data)

            val json = BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8.toMediaTypeOrNull()

            val requestBody = payload.toString().toRequestBody(json)

            val request = Request.Builder()
                    .url(url)
                    .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8)
                    .header(BRConstants.HEADER_ACCEPT, BRConstants.CONTENT_TYPE_JSON).post(requestBody).build()

            val response = APIClient.getInstance(context).sendRequest(request, true)
            return response.isSuccessful

        } catch (e: JSONException) {
            Log.e(TAG, "Error constructing JSON payload while updating FCM registration token.", e)
        }
        return false
    }

    override fun unregisterToken(context: Context, token: String): Boolean {
        val url = APIClient.getBaseURL() + ENDPOINT_DELETE_PUSH_DEVICES + token
        val request = Request.Builder()
                .url(url)
                .delete()
                .build()
        val response = APIClient.getInstance(context).sendRequest(request, true)
        return response.isSuccessful
    }

}
