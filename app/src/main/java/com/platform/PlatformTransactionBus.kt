/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 1/15/20.
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
package com.platform

import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.ui.send.TransferField
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import java.math.BigDecimal

object PlatformTransactionBus {
    private val messageChannel = BroadcastChannel<TransactionMessage>(BUFFERED)

    fun sendMessage(message: TransactionMessage) {
        messageChannel.offer(message)
    }

    fun requests() =
        messageChannel.asFlow().filterIsInstance<ConfirmTransactionMessage>()

    fun results() =
        messageChannel.asFlow().filterIsInstance<TransactionResultMessage>()
}

sealed class TransactionMessage

data class ConfirmTransactionMessage(
    val currencyCode: String,
    val fiatCode: String,
    val feeCode: String,
    val targetAddress: String,
    val transferSpeed: TransferSpeed,
    val amount: BigDecimal,
    val fiatAmount: BigDecimal,
    val fiatTotalCost: BigDecimal,
    val fiatNetworkFee: BigDecimal,
    val transferFields: List<TransferField>
) : TransactionMessage()

sealed class TransactionResultMessage : TransactionMessage() {
    data class TransactionConfirmed(
        val transaction: ConfirmTransactionMessage
    ) : TransactionResultMessage()

    data class TransactionCancelled(
        val transaction: ConfirmTransactionMessage
    ) : TransactionResultMessage()
}
