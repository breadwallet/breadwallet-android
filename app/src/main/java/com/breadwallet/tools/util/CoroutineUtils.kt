/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/03/19.
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

import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.IOException

/** The initial attempt count for network operations. */
private const val NET_INITIAL_ATTEMPT = 1
/** Delay in ms to wait before retrying a network call. */
private const val NET_RETRY_DELAY_MS: Long = 2 * 1000

/**
 * Calls the [block] suspend function and returns its value.
 *
 * If [block] throws a temporary error, it will be retried
 * up to [retryAttempts] times.
 *
 * If [block] does not return in less than [timeoutMs]
 * it will be cancelled and a retry attempt will occur if
 * not passed the retry limit.
 *
 * Retry attempts are delayed by [NET_RETRY_DELAY_MS].
 */
suspend fun <R> netRetry(
    retryAttempts: Int,
    timeoutMs: Long,
    block: suspend CoroutineScope.() -> R
): R {
    repeat(NET_INITIAL_ATTEMPT + retryAttempts) { retryIndex ->
        logDebug("Retry attempt $retryIndex")
        try {
            return withTimeout(timeoutMs, block)
        } catch (e: IOException) {
            logError("Network response error.", e)
            if (retryIndex == retryAttempts) {
                throw e
            }
        } catch (e: TimeoutCancellationException) {
            logError("Network timed out.", e)
            if (retryIndex == retryAttempts) {
                // Limit reached, bubble error
                throw e
            }
        } catch (e: CancellationException) {
            logError("Network request cancelled.", e)
            return@repeat
        } catch (e: Exception) {
            logError("Unhandled exception", e)
            if (retryIndex == retryAttempts) {
                throw e
            }
        }
        delay(NET_RETRY_DELAY_MS * (retryIndex + 1))
    }
    error("Retry limit hit, exception should bubbled.")
}