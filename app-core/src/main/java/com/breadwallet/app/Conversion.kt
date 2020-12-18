/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 7/22/2020.
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
package com.breadwallet.app

import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferState
import java.util.Date

private const val PREFIX_DELIMITER = "-"
private const val BUY_CLASS_PREFIX = "buy"
private const val TRADE_CLASS_PREFIX = "trade"

sealed class Conversion {
    abstract val currencyCode: String
    abstract fun isTriggered(transfer: Transfer): Boolean
    abstract fun serialize(): String

    companion object {
        fun deserialize(value: String): Conversion {
            return when (value.substringBefore(PREFIX_DELIMITER)) {
                BUY_CLASS_PREFIX -> Buy.deserialize(value)
                TRADE_CLASS_PREFIX -> Trade.deserialize(value)
                else -> throw IllegalStateException("Unknown Type")
            }
        }
    }
}

data class Buy(override val currencyCode: String, val amount: Double, val timestamp: Long) :
    Conversion() {
    override fun isTriggered(transfer: Transfer) =
        transfer.amount.toBigDecimal().toDouble() == amount &&
            transfer.state.type == TransferState.Type.INCLUDED &&
            transfer.confirmation.get().confirmationTime.after(Date(timestamp)) &&
            transfer.wallet.currency.code.equals(currencyCode, true)

    override fun serialize() = "$BUY_CLASS_PREFIX$PREFIX_DELIMITER$currencyCode;$amount;$timestamp"

    companion object {
        fun deserialize(value: String): Buy {
            val (currencyCode, amount, timestamp) =
                value.substringAfter(PREFIX_DELIMITER).split(";")
            return Buy(currencyCode, amount.toDouble(), timestamp.toLong())
        }
    }
}

data class Trade(override val currencyCode: String, val hashString: String) : Conversion() {
    override fun isTriggered(transfer: Transfer) =
        transfer.hashString().equals(hashString, true) &&
            transfer.state.type == TransferState.Type.INCLUDED

    override fun serialize() = "$TRADE_CLASS_PREFIX$PREFIX_DELIMITER$currencyCode;$hashString"

    companion object {
        fun deserialize(value: String): Trade {
            val (currencyCode, hashString) = value.substringAfter(PREFIX_DELIMITER).split(";")
            return Trade(currencyCode, hashString)
        }
    }
}