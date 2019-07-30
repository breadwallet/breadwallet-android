/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 7/11/19.
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
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject

/**
 * Client responsible of registering and unregistering the Firebase token for push notifications
 * in our backend.
 */
interface NotificationsSettingsClient {

    /**
     * Send push notifications token.
     */
    fun registerToken(context: Context, token: String)

    /**
     * Remove push notifications token.
     */
    fun unregisterToken(context: Context, token: String)

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

    override fun registerToken(context: Context, token: String) {
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

            val json = MediaType.parse(BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8)

            val requestBody = RequestBody.create(json, payload.toString())

            val request = Request.Builder()
                    .url(url)
                    .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8)
                    .header(BRConstants.HEADER_ACCEPT, BRConstants.CONTENT_TYPE_JSON).post(requestBody).build()

            val response = APIClient.getInstance(context).sendRequest(request, true)
            if (response.isSuccessful) {
                Log.d(TAG, "Token registered")
            } else {
                Log.e(TAG, "Failed to register new token ${response.isSuccessful}")
            }

        } catch (e: JSONException) {
            Log.e(TAG, "Error constructing JSON payload while updating FCM registration token.", e)
        }
    }

    override fun unregisterToken(context: Context, token: String) {
        val url = APIClient.getBaseURL() + ENDPOINT_DELETE_PUSH_DEVICES + token
        val request = Request.Builder()
                .url(url)
                .delete()
                .build()
        val response = APIClient.getInstance(context).sendRequest(request, true)
        if (response.isSuccessful) {
            Log.d(TAG, "Token unregistered")
        } else {
            Log.e(TAG, "Failed to unregister token ${response.isSuccessful}")
        }
    }

}