/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 5/31/2019.
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
package com.breadwallet.tools.util

/**
 * Returns a list containing only elements matching the given [predicate]
 * where [predicate] takes only the first ([L]) parameter of a pair.
 */
inline fun <L, R> Iterable<Pair<L, R>>.filterLeft(
        crossinline predicate: (L) -> Boolean
) = filter { predicate(it.first) }

/**
 * Returns a list containing only elements matching the given [predicate]
 * where [predicate] takes only the second ([R]) parameter of a pair.
 */
inline fun <L, R> Iterable<Pair<L, R>>.filterRight(
        crossinline predicate: (R) -> Boolean
) = filter { predicate(it.second) }

/**
 * Returns a list containing only the first ([L]) values of a pair.
 */
fun <L, R> Iterable<Pair<L, R>>.mapLeft() = map { it.first }

/**
 * Returns a list containing only the second ([R]) values of a pair.
 */
fun <L, R> Iterable<Pair<L, R>>.mapRight() = map { it.second }
