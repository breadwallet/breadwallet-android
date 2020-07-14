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

import android.content.Context
import android.webkit.JavascriptInterface
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.util.SupportUtils
import org.json.JSONException
import org.json.JSONObject

class SupportJs(
    private val promise: NativePromiseFactory,
    private val context: Context,
    private val breadBox: BreadBox,
    private val userManager: BrdUserManager
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
        debugJson.keys().forEach {
            debugMap[it] = debugJson.getString(it)
        }
        SupportUtils.submitEmailRequest(context, breadBox, userManager, debugMap)
    }
}