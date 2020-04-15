/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 1/15/20.
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

interface JsApi

class NativeApisJs(
    private val apis: List<JsApi>
) {
    companion object {
        private const val JS_NAME = "NativeApisJs"

        fun with(vararg apis: JsApi) =
            NativeApisJs(apis.toList())
    }

    @JavascriptInterface
    fun getApiNamesJson(): String =
        apis.joinToString(prefix = "[", postfix = "]") {
            "\"${it::class.java.simpleName}_Native\""
        }

    fun attachToWebView(webView: WebView) {
        webView.addJavascriptInterface(this, JS_NAME)
        apis.forEach { api ->
            val name = "${api::class.java.simpleName}_Native"
            webView.addJavascriptInterface(api, name)
        }
    }
}
