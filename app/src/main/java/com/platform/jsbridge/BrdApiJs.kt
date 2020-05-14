/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 4/21/20.
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
package com.platform.jsbridge

import android.webkit.JavascriptInterface
import com.platform.APIClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class BrdApiJs(
    private val promise: NativePromiseFactory,
    private val apiClient: APIClient
) : JsApi {

    @JvmOverloads
    @JavascriptInterface
    fun request(
        method: String,
        path: String,
        body: String? = null,
        authenticate: Boolean = true
    ) = when (method.toUpperCase(Locale.ROOT)) {
        "GET" -> get(path, authenticate)
        "POST" -> post(path, body, authenticate)
        "PUT" -> put(path, body, authenticate)
        "DELETE" -> delete(path, authenticate)
        else -> promise.create {
            error("Unknown method '$method'.")
        }
    }

    @JvmOverloads
    @JavascriptInterface
    fun get(path: String, authenticate: Boolean = true) = promise.create {
        check(path.isNotBlank()) { "Path cannot be blank." }
        submitRequest(
            Request.Builder()
                .get()
                .url(apiClient.buildUrl(path))
                .build(),
            authenticate
        )
    }

    @JvmOverloads
    @JavascriptInterface
    fun post(path: String, body: String?, authenticate: Boolean = true) = promise.create {
        check(path.isNotBlank()) { "Path cannot be blank." }
        checkNotNull(body) { "Request body required for POST request." }
        submitRequest(
            Request.Builder()
                .post(body.toRequestBody("application/json".toMediaType()))
                .url(apiClient.buildUrl(path))
                .build(),
            authenticate
        )
    }

    @JvmOverloads
    @JavascriptInterface
    fun delete(path: String, authenticate: Boolean = true) = promise.create {
        check(path.isNotBlank()) { "Path cannot be blank." }
        submitRequest(
            Request.Builder()
                .delete()
                .url(apiClient.buildUrl(path))
                .build(),
            authenticate
        )
    }

    @JvmOverloads
    @JavascriptInterface
    fun put(path: String, body: String?, authenticate: Boolean = true) = promise.create {
        check(path.isNotBlank()) { "Path cannot be blank." }
        checkNotNull(body) { "Request body required for PUT request." }
        submitRequest(
            Request.Builder()
                .put(body.toRequestBody("application/json".toMediaType()))
                .url(apiClient.buildUrl(path))
                .build(),
            authenticate
        )
    }

    private fun submitRequest(request: Request, authenticate: Boolean) =
        apiClient.sendRequest(request, authenticate).asJsonObject()

    private fun APIClient.BRResponse.asJsonObject(): JSONObject =
        JSONObject().apply {
            put("isSuccessful", isSuccessful)
            put("status", code)
            put("headers", JSONObject(headers))
            put(
                "body", try {
                    when {
                        bodyText.isBlank() || contentType != "application/json" -> null
                        bodyText.startsWith("[") -> JSONArray(bodyText)
                        bodyText.startsWith("{") -> JSONObject(bodyText)
                        else -> error("Failed to parse response body.")
                    }
                } catch (e: JSONException) {
                    null
                }
            )
        }
}