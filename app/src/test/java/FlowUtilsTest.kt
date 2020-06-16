/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 4/1/20.
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
package com.breadwallet

import com.breadwallet.ext.throttleLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class FlowUtilsTest {

    // TODO: Use runBlockingTest: https://github.com/Kotlin/kotlinx.coroutines/issues/1204
    @Test
    fun throttleLatestTest() = runBlocking {
        assertEquals(
            listOf(1, 3),
            testFlow(count = 3, millis = 100)
                .throttleLatest(300)
                .toList()
        )

        assertEquals(
            listOf(1, 2, 3),
            testFlow(count = 3, millis = 100)
                .throttleLatest(100)
                .toList()
        )

        assertEquals(
            listOf(1, 10),
            testFlow(count = 10, millis = 100)
                .throttleLatest(1_000)
                .toList()
        )

        assertEquals(
            listOf(1, 3, 4, 5),
            flow {
                emit(1)
                delay(500)
                emit(2)
                delay(200)
                emit(3)
                delay(800)
                emit(4)
                delay(600)
                emit(5)
            }.throttleLatest(1_000).toList()
        )
    }

    private fun testFlow(count: Int, millis: Long) = flow {
        repeat(count) {
            emit(it + 1)
            delay(millis)
        }
    }
}