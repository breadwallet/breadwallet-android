/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 12/3/19.
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
package com.breadwallet.ui.flowbind

import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

fun View.clicks(): Flow<Unit> =
    callbackFlow {
        setOnClickListener { offer(Unit) }
        awaitClose { setOnClickListener(null) }
    }.flowOn(Dispatchers.Main)

fun View.longClicks(): Flow<Unit> =
    callbackFlow {
        setOnLongClickListener { offer(Unit) }
        awaitClose { setOnLongClickListener(null) }
    }.flowOn(Dispatchers.Main)

fun View.touchEvents(consumed: Boolean = true): Flow<MotionEvent> =
    callbackFlow {
        setOnTouchListener { _, event ->
            offer(event)
            consumed
        }
        awaitClose { setOnTouchListener(null) }
    }.flowOn(Dispatchers.Main)

