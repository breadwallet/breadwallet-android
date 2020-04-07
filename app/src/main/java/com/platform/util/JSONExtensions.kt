/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/10/19.
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

import org.json.JSONObject

/**
 * Returns the value mapped by name of null if it doesn't exist.
 */
fun JSONObject.getStringOrNull(name: String): String? =
    if (this.has(name)) this.getString(name) else null

/** Returns the value mapped by name or [default] if it doesn't exist. */
fun JSONObject.getIntOrDefault(name: String, default: Int = 0) =
    if (has(name)) getInt(name) else default

/** Returns the value mapped by name or [default] if it doesn't exist. */
fun JSONObject.getLongOrDefault(name: String, default: Long = 0) =
    if (has(name)) getLong(name) else default

/** Returns the value mapped by name or [default] if it doesn't exist. */
fun JSONObject.getDoubleOrDefault(name: String, default: Double = 0.0) =
    if (has(name)) getDouble(name) else default

/** Returns the value mapped by name or [default] if it doesn't exist or is malformed. */
fun JSONObject.getDoubleOrDefaultSafe(name: String, default: Double = 0.0) =
    try {
        getDoubleOrDefault(name, default)
    } catch (e: Exception) {
        default
    }