/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 11/6/19.
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
package com.breadwallet.ui.navigation

import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.util.isErc20
import com.platform.HTTPServer
import java.util.Locale

/** Returns the full support URL for the articleId and currencyCode. */
fun NavigationTarget.SupportPage.asSupportUrl() = buildString {
    append(HTTPServer.getPlatformUrl(HTTPServer.URL_SUPPORT))
    if (articleId.isNotBlank()) {
        append(UiUtils.ARTICLE_QUERY_STRING)
        append(articleId)

        val currencyCode = currencyCode ?: ""
        if (currencyCode.isNotBlank()) {
            val codeOrErc20 = if (currencyCode.isErc20()) {
                BRConstants.CURRENCY_ERC20
            } else {
                currencyCode.toLowerCase(Locale.ROOT)
            }

            append("${UiUtils.CURRENCY_QUERY_STRING}$codeOrErc20")
        }
    }
}
