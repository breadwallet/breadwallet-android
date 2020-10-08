/**
 * BreadWallet
 *
 * Created by Ahsan Butt on <ahsan.butt@breadwallet.com> 7/14/2020.
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
import com.breadwallet.tools.util.CUSTOM_DATA_KEY_TITLE
import com.breadwallet.tools.util.DebugInfo
import com.breadwallet.tools.util.SupportManager
import org.json.JSONException
import org.json.JSONObject

private const val SUPPORT_SUBJECT = "BRD Order Inquiry"
private const val SUPPORT_TEXT = "[Please add any further information about your order that you wish here, otherwise all you need to do is send this email. BRD Support will assist you as soon as possible.]"

class SupportJs(
    private val promise: NativePromiseFactory,
    private  val supportManager: SupportManager
) : JsApi {

    @JavascriptInterface
    fun submitRequest(
        debugData: String
    ) = promise.createForUnit {
        val debugJson = try {
            JSONObject(debugData)
        } catch (e: JSONException) {
            JSONObject()
        }
        val debugMap = mutableMapOf<String, String>()
        debugMap[CUSTOM_DATA_KEY_TITLE] = "Order Details"
        debugJson.keys().forEach {
            debugMap[it] = debugJson.getString(it)
        }
        supportManager.submitEmailRequest(
            subject = SUPPORT_SUBJECT,
            body =  SUPPORT_TEXT,
            diagnostics = listOf(DebugInfo.CUSTOM),
            customData = debugMap,
            attachLogs = false
        )
    }
}