/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 1/14/20.
 * Copyright (c) 2020 breadwallet LLC
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

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

@Suppress("NOTHING_TO_INLINE")
inline fun Any.newTrace(name: String): Trace =
    FirebasePerformance.getInstance()
        .newTrace(name)
        .also { trace ->
            trace.putAttribute("class_name", this::class.java.simpleName)
        }

inline fun Trace.trace(crossinline block: (trace: Trace) -> Unit) {
    try {
        start()
        block(this)
    } finally {
        stop()
    }
}

inline fun <T> Trace.traceResult(crossinline block: (trace: Trace) -> T): T {
    try {
        start()
        return block(this)
    } finally {
        stop()
    }
}
