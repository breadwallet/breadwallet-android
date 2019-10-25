package com.breadwallet.util

import android.net.Uri
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.breadbox.urlScheme
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.legacy.wallet.util.CryptoUriParser
import java.math.BigDecimal

// TODO: Currently an as needed reimplementation of CryptoUriParser
//  in kotlin to support the new generic core.
class CryptoUriParser2(
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

        if (!request.hasAddress()) {
            request.address = wallet.source.toSanitizedString()
        }
        uriBuilder.appendPath(request.address)

        if (request.amount != null && request.amount > BigDecimal.ZERO) {
            val amountParamName = when {
                currencyCode.isEthereum() -> CryptoUriParser.VALUE
                else -> CryptoUriParser.AMOUNT
            }
            uriBuilder.appendQueryParameter(amountParamName, request.amount.toPlainString())
        }

        if (!request.label.isNullOrEmpty()) {
            uriBuilder.appendQueryParameter(CryptoUriParser.LABEL, request.label)
        }

        if (!request.message.isNullOrEmpty()) {
            uriBuilder.appendQueryParameter(CryptoUriParser.MESSAGE, request.message)
        }

        if (!request.rUrl.isNullOrBlank()) {
            uriBuilder.appendQueryParameter(CryptoUriParser.R_URL, request.rUrl)
        }

        return Uri.parse(uriBuilder.build().toString().replace("/", ""))
    }
}
