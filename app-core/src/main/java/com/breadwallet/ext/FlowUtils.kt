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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T> {
    var lastEmissionMs = 0L
    return transform { value ->
        val currentMs = System.currentTimeMillis()
        if (currentMs - lastEmissionMs >= windowDuration) {
            lastEmissionMs = currentMs
            emit(value)
        }
    }
}

fun <T> Flow<T>.throttleLatest(
    windowDuration: Long
): Flow<T> = channelFlow {
    val mutex = Mutex()
    val hasLatest = AtomicBoolean(false)
    val latest = AtomicReference<T>(null)
    collectLatest { value ->
        if (mutex.tryLock()) {
            offer(value)
            launch(Dispatchers.Default) {
                delay(windowDuration)
                while (hasLatest.getAndSet(false) && isActive) {
                    offer(latest.getAndSet(null))
                    delay(windowDuration)
                }
                mutex.unlock()
            }
        } else {
            latest.set(value)
            hasLatest.set(true)
        }
    }
}
