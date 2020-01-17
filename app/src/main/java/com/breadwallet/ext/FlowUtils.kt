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

import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Dispatch each item emitted by this flow to [consumer], launching in [scope]. */
fun <T> Flow<T>.bindConsumerIn(consumer: Consumer<T>, scope: CoroutineScope) =
    onEach { consumer.accept(it) }.launchIn(scope)

fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T> = flow {
    var lastEmissionMs = 0L
    collect { value ->
        val currentMs = System.currentTimeMillis()
        if (currentMs - lastEmissionMs > windowDuration) {
            lastEmissionMs = currentMs
            emit(value)
        }
    }
}

fun <T> Flow<T>.throttleLatest(windowDuration: Long): Flow<T> = channelFlow {
    val isActive = AtomicBoolean(false)
    val latest = AtomicReference<T>()
    val emissions = AtomicInteger(0)
    collect { value ->
        if (!isActive.get()) {
            isActive.set(true)

            offer(value)
            delay(windowDuration)

            if (emissions.get() > 1) {
                offer(latest.get())
            }

            isActive.set(false)
            emissions.set(0)
        }
        latest.set(value)
        emissions.incrementAndGet()
    }
}
