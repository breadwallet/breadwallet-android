/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 2/27/20.
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
package com.breadwallet.util

import com.breadwallet.crypto.utility.CompletionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import java.lang.Exception

suspend fun <R, E : Exception> asyncApiCall(
    body: CompletionHandler<R, E>.() -> Unit
): R = AsyncCompletionHandler<R, E>().apply(body).await()

@UseExperimental(ExperimentalCoroutinesApi::class)
class AsyncCompletionHandler<R, E : Exception> : CompletionHandler<R, E> {

    private val resultChannel = Channel<R>(RENDEZVOUS)

    override fun handleData(data: R) {
        check(!resultChannel.isClosedForSend)
        resultChannel.offer(data)
        resultChannel.close()
    }

    override fun handleError(error: E) {
        check(!resultChannel.isClosedForSend)
        resultChannel.close(error)
    }

    suspend fun await(): R {
        check(!resultChannel.isClosedForReceive)
        return resultChannel.receive()
    }
}
