/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 8/21/19.
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
package com.platform

import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.browser.BrdNativeJs
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

/**
 * Build a signed request.
 */
@JvmOverloads
fun buildSignedRequest(
        url: String,
        body: String,
        method: String,
        target: String,
        contentType: String = BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8
): Request {
    var contentDigest = ""
    if (BRConstants.GET != method) {
        contentDigest = BrdNativeJs.sha256(body)
    }
    val requestDateString = Date().time.toString()

    val signature = BrdNativeJs.sign(
            method,
            contentDigest,
            contentType,
            requestDateString,
            target
    )

    val builder = Request.Builder()
            .url(url)
            .addHeader(BrdNativeJs.SIGNATURE_HEADER, signature)
            .addHeader(BrdNativeJs.DATE_HEADER, requestDateString)
            .header("content-type", contentType)
    when (method) {
        "POST" -> builder.post(body.toRequestBody(null))
        "PUT" -> builder.put(body.toRequestBody(null))
        "PATH" -> builder.patch(body.toRequestBody(null))
    }
    return builder.build()
}
