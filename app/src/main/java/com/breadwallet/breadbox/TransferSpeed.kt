/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 8/17/20.
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
package com.breadwallet.breadbox

import com.breadwallet.util.isBitcoin
import com.breadwallet.util.isErc20
import com.breadwallet.util.isEthereum
import java.util.concurrent.TimeUnit

private const val PREFIX_DELIMITER = "-"

sealed class TransferSpeed {
    abstract val targetTime: Long
    abstract val currencyCode: String
    override fun toString(): String {
        return "${this::class.simpleName}$PREFIX_DELIMITER$currencyCode"
    }

    companion object {
        fun valueOf(value: String): TransferSpeed {
            val currencyCode = value.substringAfter(PREFIX_DELIMITER)
            return when (value.substringBefore(PREFIX_DELIMITER)) {
                Economy::class.simpleName -> Economy(currencyCode)
                Regular::class.simpleName -> Regular(currencyCode)
                Priority::class.simpleName -> Priority(currencyCode)
                else -> error("Unknown Type")
            }
        }
    }

    class Economy(override val currencyCode: String) :  TransferSpeed() {
        override val targetTime = when {
            currencyCode.run { isEthereum() || isErc20() } -> TimeUnit.MINUTES.toMillis(5L)
            else -> TimeUnit.HOURS.toMillis(7L)
        }
    }

    class Regular(override val currencyCode: String): TransferSpeed() {
        override val targetTime = when {
            currencyCode.isBitcoin() -> TimeUnit.MINUTES.toMillis(30L)
            else -> TimeUnit.MINUTES.toMillis(3L)
        }
    }

    class Priority(override val currencyCode: String) :  TransferSpeed() {
        override val targetTime = when {
            currencyCode.run { isEthereum() || isErc20() } -> TimeUnit.MINUTES.toMillis(1L)
            else -> 0L
        }
    }
}