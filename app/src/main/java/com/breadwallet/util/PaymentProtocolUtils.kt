/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 1/15/20.
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
package com.breadwallet.util

import com.breadwallet.crypto.PaymentProtocolPayment
import com.breadwallet.crypto.PaymentProtocolRequest
import com.breadwallet.crypto.PaymentProtocolRequestType
import com.breadwallet.crypto.Wallet
import com.breadwallet.tools.util.BRConstants
import com.platform.APIClient

private const val HEADER_PAYMENT_REQUEST_BIP70 =
    "application/bitcoin-paymentrequest"
private const val HEADER_PAYMENT_REQUEST_BITPAY_V1 = "application/payment-request"
private const val HEADER_PAYMENT_BIP70 = "application/bitcoin-payment"
private const val HEADER_PAYMENT_BITPAY_V1 = "application/payment"
private const val HEADER_PAYMENT_ACK_BIP70 = "application/bitcoin-paymentack"
private const val HEADER_PAYMENT_ACK_BITPAY_V1 = "application/payment-ack"
const val HEADER_BITPAY_PARTNER_KEY = "BP_PARTNER"
const val HEADER_BITPAY_PARTNER = "brd"

/** Return the header required to fetch a [PaymentProtocolRequest] */
fun CurrencyCode.getPaymentRequestHeader(): String =
    when {
        isBitcoin() -> HEADER_PAYMENT_REQUEST_BIP70
        else -> HEADER_PAYMENT_REQUEST_BITPAY_V1
    }

/** Return the content type header required to post a [PaymentProtocolPayment] */
fun PaymentProtocolRequest.getContentTypeHeader(): String =
    when (type) {
        PaymentProtocolRequestType.BIP70 -> HEADER_PAYMENT_BIP70
        PaymentProtocolRequestType.BITPAY -> HEADER_PAYMENT_BITPAY_V1
        else -> ""
    }

/** Return the content type header required to post a [PaymentProtocolPayment] */
fun PaymentProtocolRequest.getAcceptHeader(): String =
    when (type) {
        PaymentProtocolRequestType.BIP70 -> HEADER_PAYMENT_ACK_BIP70
        PaymentProtocolRequestType.BITPAY -> HEADER_PAYMENT_ACK_BITPAY_V1
        else -> ""
    }

/** Build a [PaymentProtocolRequest] from an [APIClient.BRResponse] */
fun buildPaymentProtocolRequest(
    wallet: Wallet,
    response: APIClient.BRResponse
): PaymentProtocolRequest? {
    val header = response.headers[BRConstants.HEADER_CONTENT_TYPE.toLowerCase()].orEmpty()
    return when {
        header.startsWith(HEADER_PAYMENT_BIP70, true) -> {
            PaymentProtocolRequest.createForBip70(
                wallet,
                response.body
            ).orNull()
        }
        header.startsWith(HEADER_PAYMENT_BITPAY_V1, true) -> {
            PaymentProtocolRequest.createForBitPay(
                wallet,
                response.bodyText
            ).orNull()
        }
        else -> null
    }
}