/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/10/19.
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

import android.net.Uri
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.addressFor
import com.breadwallet.breadbox.isErc20
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.breadbox.urlScheme
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.logger.logError
import com.breadwallet.tools.util.EventUtils
import java.math.BigDecimal
import java.util.HashMap

private const val AMOUNT = "amount"
private const val VALUE = "value"
private const val LABEL = "label"
private const val MESSAGE = "message"
/** "req" parameter, whose value is a required variable which are prefixed with a req-. */
private const val REQ = "req"
/** "r" parameter, whose value is a URL from which a PaymentRequest message should be fetched */
private const val R_URL = "r"
private const val TOKEN_ADDRESS = "tokenaddress"

class CryptoUriParser(
    private val breadBox: BreadBox
) {

    fun createUrl(currencyCode: String, request: CryptoRequest): Uri {
        require(currencyCode.isNotBlank())

        val uriBuilder = Uri.Builder()

        val system = checkNotNull(breadBox.getSystemUnsafe())
        val wallet = checkNotNull(system.wallets.find {
            it.currency.code.equals(currencyCode, true)
        })

        uriBuilder.scheme(wallet.urlScheme)

        if (wallet.currency.isErc20()) {
            uriBuilder.appendQueryParameter(TOKEN_ADDRESS, wallet.currency.issuer.orNull())
        }

        if (!request.hasAddress()) {
            request.address = wallet.target.toSanitizedString()
        } else {
            val address = checkNotNull(wallet.addressFor(request.address))
            request.address = address.toSanitizedString()
        }

        uriBuilder.appendPath(request.address)

        if (request.amount != null && request.amount > BigDecimal.ZERO) {
            val amountParamName = when {
                currencyCode.isEthereum() -> VALUE
                else -> AMOUNT
            }
            val amountValue = request.amount.toPlainString()
            uriBuilder.appendQueryParameter(amountParamName, amountValue)
        }

        if (!request.label.isNullOrEmpty()) {
            uriBuilder.appendQueryParameter(LABEL, request.label)
        }

        if (!request.message.isNullOrEmpty()) {
            uriBuilder.appendQueryParameter(MESSAGE, request.message)
        }

        if (!request.rUrl.isNullOrBlank()) {
            uriBuilder.appendQueryParameter(R_URL, request.rUrl)
        }

        return Uri.parse(uriBuilder.build().toString().replace("/", ""))
    }

    fun isCryptoUrl(url: String): Boolean {
        val request = parseRequest(url)
        val wallets = checkNotNull(breadBox.getSystemUnsafe()).wallets
        return if (request != null && request.scheme.isNotBlank()) {
            val wallet = wallets.firstOrNull {
                it.urlScheme == request.scheme
            }

            if (wallet != null) {
                request.isPaymentProtocol || request.hasAddress()
            } else false
        } else false
    }

    @Suppress("LongMethod", "ComplexMethod", "ReturnCount")
    fun parseRequest(requestString: String): CryptoRequest? {
        if (requestString.isBlank()) return null

        val wallets = checkNotNull(breadBox.getSystemUnsafe()).wallets

        val builder = CryptoRequest.Builder()

        val uri = Uri.parse(requestString).run {
            // Formats `ethereum:0x0...` as `ethereum://0x0`
            // to ensure the uri fields are parsed consistently.
            Uri.parse("$scheme://${schemeSpecificPart.trimStart('/')}")
        }

        builder.scheme = uri.scheme
        builder.address = uri.host ?: ""

        val tokenAddress = uri.getQueryParameter(TOKEN_ADDRESS) ?: ""
        if (tokenAddress.isNotBlank()) {
            val tokenWallet = wallets.firstOrNull { it.currency.uids.contains(tokenAddress) }
            if (tokenWallet == null) {
                return null
            } else {
                builder.currencyCode = tokenWallet.currency.code
            }
        } else {
            builder.currencyCode = wallets
                .mapNotNull { wallet ->
                    if (wallet.urlScheme == uri.scheme) {
                        wallet.currency.code
                    } else null
                }
                .firstOrNull()
        }

        if (builder.currencyCode.isNullOrBlank()) {
            return null
        }

        val query = uri.query
        if (query.isNullOrBlank()) {
            return builder.build()
        }
        pushUrlEvent(uri)

        with(uri) {
            getQueryParameter(REQ)?.run(builder::setReqUrl)
            getQueryParameter(R_URL)?.run(builder::setRUrl)
            getQueryParameter(LABEL)?.run(builder::setLabel)
            getQueryParameter(MESSAGE)?.run(builder::setMessage)
            try {
                getQueryParameter(AMOUNT)
                    ?.let(::BigDecimal)
                    ?.run(builder::setAmount)
            } catch (e: NumberFormatException) {
                logError("Failed to parse amount string.", e)
            }
            // ETH payment request amounts are called `value`
            getQueryParameter(VALUE)
                ?.run(::BigDecimal)
                ?.run(builder::setValue)
        }

        return builder.build()
    }

    private fun pushUrlEvent(u: Uri?) {
        val attr = HashMap<String, String>()
        attr["scheme"] = if (u == null) "null" else u.scheme
        attr["host"] = if (u == null) "null" else u.host
        attr["path"] = if (u == null) "null" else u.path
        EventUtils.pushEvent(EventUtils.EVENT_SEND_HANDLE_URL, attr)
    }
}
