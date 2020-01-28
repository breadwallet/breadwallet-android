/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/7/2019.
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
package com.platform.util

import org.apache.commons.io.IOUtils
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

/** Caches the result of [getInputStream] and returns the cached data on subsequent calls. */
class CachedInputHttpServletRequest(
        request: HttpServletRequest
) : HttpServletRequestWrapper(request) {

    private var cached = false
    private lateinit var cachedData: ByteArray

    override fun getInputStream(): ServletInputStream {
        if (!cached) {
            cachedData = IOUtils.toByteArray(super.getInputStream())
            cached = true
        }

        return object : ServletInputStream() {
            var finished = false
            val inputStream = cachedData.inputStream()

            override fun read(): Int =
                    inputStream.read().also {
                        finished = it == -1
                    }

            override fun isFinished() = finished
            override fun isReady() = true
            override fun setReadListener(readListener: ReadListener?) = Unit
        }
    }
}
