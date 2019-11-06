package com.breadwallet.ui.navigation

import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.util.isBitcoin
import com.breadwallet.util.isBitcoinCash
import com.breadwallet.util.isEthereum
import com.platform.HTTPServer

/** Returns the full support URL for the articleId and currencyCode. */
fun NavigationEffect.GoToFaq.asSupportUrl() = buildString {
    append(HTTPServer.getPlatformUrl(HTTPServer.URL_SUPPORT))
    if (articleId.isNotBlank()) {
        append(UiUtils.ARTICLE_QUERY_STRING)
        append(articleId)

        val currencyCode = currencyCode ?: ""
        if (currencyCode.isNotBlank()) {
            // TODO: Better is Erc20 check
            val codeOrErc20 = if (
                currencyCode.isBitcoin()
                || currencyCode.isEthereum()
                || currencyCode.isBitcoinCash()
            ) {
                currencyCode.toLowerCase()
            } else {
                BRConstants.CURRENCY_ERC20
            }

            append("${UiUtils.CURRENCY_QUERY_STRING}$codeOrErc20")
        }
    }
}
