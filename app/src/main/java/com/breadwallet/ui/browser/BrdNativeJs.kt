/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/6/2019.
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
package com.breadwallet.ui.browser

import android.webkit.JavascriptInterface
import com.breadwallet.tools.util.Utils
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Provides native methods for use in platform-content pages.
 */
object BrdNativeJs {

    const val JS_NAME = "brdNative"
    const val SIGNATURE_HEADER = "x-signature"
    const val DATE_HEADER = "x-date"

    private val sha256 by lazy { MessageDigest.getInstance("SHA-256") }
    private val mac by lazy {
        Mac.getInstance("Hmacsha256").apply {
            val uuid = UUID.randomUUID().toString()
            init(SecretKeySpec(uuid.toByteArray(), "Hmacsha256"))
        }
    }

    @JavascriptInterface
    fun sha256(input: String?): String = when {
        input == null || input.isBlank() -> ""
        else -> sha256.digest(input.toByteArray()).run(Utils::bytesToHex)
    }

    @JavascriptInterface
    fun sign(
        method: String,
        contentDigest: String,
        contentType: String,
        date: String,
        url: String
    ): String = mac.run {
        reset()
        val signingContent = method + contentDigest + contentType + date + url
        doFinal(signingContent.toByteArray()).run(Utils::bytesToHex)
    }
}
