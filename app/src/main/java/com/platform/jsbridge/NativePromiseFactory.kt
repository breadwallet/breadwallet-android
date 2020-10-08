/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/8/19.
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
package com.platform.jsbridge

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.breadwallet.util.errorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.random.nextInt

class NativePromiseFactory(webView: WebView) {

    private var webView: WebView? = webView
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + errorHandler())
    private val binderMap = ConcurrentHashMap<String, NativePromiseJs<*>>()

    private val evaluateJs: (String) -> Unit = { js: String ->
        scope.launch(Dispatchers.Main) {
            webView.evaluateJavascript(js, null)
        }
    }

    fun create(executor: suspend () -> JSONObject?) =
        NativePromiseJs(scope, executor, evaluateJs).also { promise ->
            binderMap[promise.jsName] = promise
        }

    fun createForArray(executor: suspend () -> JSONArray?) =
        NativePromiseJs(scope, executor, evaluateJs).also { promise ->
            binderMap[promise.jsName] = promise
        }

    fun createForString(executor: suspend () -> String) =
        NativePromiseJs(scope, executor, evaluateJs).also { promise ->
            binderMap[promise.jsName] = promise
        }

    fun createForUnit(executor: suspend () -> Unit) =
        NativePromiseJs(scope, executor, evaluateJs).also { promise ->
            binderMap[promise.jsName] = promise
        }

    fun dispose() {
        scope.cancel()
        binderMap.clear()
        webView = null
    }

    /**
     * Using two components, [NativePromiseJs] tracks native work via
     * [executor] and resolves a Javascript promise. This class depends on
     * JavaScript added by platform-content to provide the promise hooks.
     * See assets/native-api-explorer.html for the snippet.
     */
    class NativePromiseJs<V>(
        private val scope: CoroutineScope,
        private val executor: suspend () -> V?,
        private val evaluateJs: (js: String) -> Unit,
        private val valueWriter: ((V?) -> String) = DEFAULT_VALUE_WRITER
    ) {

        companion object {
            private const val CALLBACK_NAME_LENGTH = 6
            private val UPPER_CHAR_RANGE = 65..90
            private val LOWER_CHAR_RANGE = 97..122

            private val DEFAULT_VALUE_WRITER = { value: Any? ->
                if (value == null) "null" else when (value) {
                    is String -> "\"$value\""
                    is JSONObject -> value.toString()
                    is JSONArray -> value.toString()
                    else -> "null"
                }
            }
        }

        private var fulfilled = false

        /** A lazily generated name used for lookup and execution of the js callback. */
        @get:JavascriptInterface
        val jsName by lazy {
            val suffix = CharArray(CALLBACK_NAME_LENGTH) { index ->
                val charRange = when {
                    index % 2 == 0 -> UPPER_CHAR_RANGE
                    else -> LOWER_CHAR_RANGE
                }
                Random.Default.nextInt(charRange).toChar()
            }
            "cb_$suffix"
        }

        private fun resolve(result: V? = null) = evaluateJs(
            """
                (function() {
                    let callback = window.brdCallbacks["$jsName"]
                    callback.resolve.call(callback.scope, ${valueWriter(result)})
                    delete window.brdCallbacks["$jsName"]
                })()
            """.trimIndent()
        )

        private fun reject(e: Exception) = evaluateJs(
            """
                (function() {
                    let callback = window.brdCallbacks["$jsName"]
                    callback.reject.call(callback.scope, new Error('${e.message?.replace("'", "\\'")}'))
                    delete window.brdCallbacks["$jsName"]
                })()
            """.trimIndent()
        )

        @JavascriptInterface
        fun execute(): Unit = synchronized(this) {
            check(!fulfilled) { "Cannot execute fulfilled NativePromise." }
            scope.launch {
                try {
                    resolve(executor())
                } catch (e: Exception) {
                    reject(e)
                } finally {
                    fulfilled = true
                }
            }
        }
    }
}
