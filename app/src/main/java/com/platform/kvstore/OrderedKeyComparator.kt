/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 12/18/19.
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
package com.platform.kvstore

import com.platform.sqlite.KVItem

class OrderedKeyComparator(private val orderedKeys: List<String>) : Comparator<KVItem> {
    override fun compare(o1: KVItem?, o2: KVItem?): Int =
        when {
            o1 == null && o2 == null -> 0
            o1 == null -> 1
            o2 == null -> -1
            orderedKeys.contains(o1.key) && orderedKeys.contains((o2.key)) ->
                orderedKeys.indexOf(o1.key) - orderedKeys.indexOf(o2.key)
            orderedKeys.contains(o1.key) -> -1
            orderedKeys.contains(o2.key) -> 1
            else -> o1.key.compareTo(o2.key)
        }
}