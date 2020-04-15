/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/13/19.
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
package com.breadwallet.ext

import com.breadwallet.logger.logError
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Dispatch each item emitted by this flow to [consumer], launching in [scope]. */
fun <T> Flow<T>.bindConsumerIn(consumer: Consumer<T>, scope: CoroutineScope) =
    onEach { consumer.accept(it) }
        .catch { e ->
            if (e is IllegalStateException) {
                logError("Attempted to dispatch item in dead consumer.", e)
            } else {
                throw e
            }
        }
        .launchIn(scope)

fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T> {
    var lastEmissionMs = 0L
    return transform { value ->
        val currentMs = System.currentTimeMillis()
        if (currentMs - lastEmissionMs > windowDuration) {
            lastEmissionMs = currentMs
            emit(value)
        }
    }
}

fun <T> Flow<T>.throttleLatest(windowDuration: Long): Flow<T> {
    val mutex = Mutex()
    var latest: T? = null
    return transformLatest { value ->
        if (mutex.isLocked) {
            latest = value
        } else {
            emit(value)
        }
    }.transform { value ->
        mutex.withLock {
            emit(value)
            delay(windowDuration)

            latest?.let { emit(it) }
            latest = null
        }
    }
}