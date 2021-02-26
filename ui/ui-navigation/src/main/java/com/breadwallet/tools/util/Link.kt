/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 11/24/202.
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
package com.breadwallet.tools.util

import com.breadwallet.protocols.messageexchange.entities.PairingMetaData
import com.breadwallet.util.CurrencyCode
import dev.zacsweers.redacted.annotations.Redacted
import java.io.Serializable
import java.math.BigDecimal

sealed class Link {

    /**
     * A request to transfer a crypto via URL.
     * Loosely observes the following specs:
     *
     * https://github.com/bitcoin/bips/blob/master/bip-0021.mediawiki
     * https://github.com/bitcoin/bips/blob/master/bip-0072.mediawiki
     * https://github.com/ethereum/EIPs/blob/master/EIPS/eip-681.md
     */
    data class CryptoRequestUrl(
        val currencyCode: CurrencyCode,
        @Redacted val address: String? = null,
        val amount: BigDecimal? = null,
        @Redacted val label: String? = null,
        @Redacted val message: String? = null,
        @Redacted val reqParam: String? = null,
        @Redacted val rUrlParam: String? = null,
        @Redacted val destinationTag: String? = null,
    ) : Link(), Serializable

    data class WalletPairUrl(
        val pairingMetaData: PairingMetaData
    ) : Link()

    data class PlatformUrl(
        val url: String
    ) : Link()

    data class PlatformDebugUrl(
        val webBundle: String?,
        val webBundleUrl: String?
    ) : Link()

    data class ImportWallet(
        @Redacted val privateKey: String,
        val passwordProtected: Boolean,
        val gift: Boolean = false,
        val scanned: Boolean = false
    ) : Link()

    sealed class BreadUrl : Link() {
        object ScanQR : BreadUrl()
        object Address : BreadUrl()
        object AddressList : BreadUrl()
    }
}
