/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 9/17/19.
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
package com.platform.interfaces

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

/** Provides access to a Key-Value Store serving [JSONObject]. */
interface KVStoreProvider {
    /** Get value for [key]. */
    fun get(key: String): JSONObject?

    /** Put [value] for [key]. */
    fun put(key: String, value: JSONObject): Boolean

    /** Syncs the value for [key] and returns it, null if sync failed. */
    suspend fun sync(key: String): JSONObject?

    /** Syncs entire data store.*/
    suspend fun syncAll(syncOrder: List<String> = listOf()): Boolean

    /** Returns a [Flow] for a given [key]. */
    fun keyFlow(key: String): Flow<JSONObject>
}
