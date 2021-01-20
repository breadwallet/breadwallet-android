/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 11/13/19.
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

import android.net.Uri
import android.util.Base64
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.crypto.Address
import com.breadwallet.crypto.Key
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.protocols.messageexchange.entities.PairingMetaData
import com.breadwallet.tools.util.BRConstants.WALLET_LINK_PATH
import com.breadwallet.tools.util.BRConstants.WALLET_PAIR_PATH
import com.breadwallet.util.CryptoUriParser
import com.platform.HTTPServer
import kotlinx.coroutines.flow.first
import java.net.URLEncoder

private const val SCAN_QR = "scanqr"
private const val ADDRESS_LIST = "addressList"
private const val ADDRESS = "address"
private const val BRD_HOST = "brd.com"
private const val BRD_PROTOCOL = "https"
private const val GIFT_PATH_PREFIX = "/x/gift/"
private const val PLATFORM_PATH_PREFIX = "/x/platform/"
private const val PLATFORM_DEBUG_PATH_PREFIX = "/x/debug"
private const val PLATFORM_URL_FORMAT = "/link?to=%s"
private const val PATH_ENCODING = "utf-8"
private const val QUERY_PARAM_WEB_BUNDLE = "web_bundle"
private const val QUERY_PARAM_BUNDLE_DEBUG_URL = "bundle_debug_url"

const val GIFT_BASE_URL = "$BRD_PROTOCOL://$BRD_HOST$GIFT_PATH_PREFIX"

/** Turn the url string into a [Link] or null if it is not supported. */
@Suppress("ComplexMethod")
suspend fun String.asLink(
    breadBox: BreadBox,
    uriParser: CryptoUriParser,
    scanned: Boolean = false
): Link? {
    if (isBlank()) return null
    val uri = Uri.parse(this)
    val (address, currencyCode) =
        breadBox.networks(true)
            .first()
            .mapNotNull { network ->
                Address.create(this, network)
                    .orNull()
                    ?.let { address ->
                        address to network.currency.code
                    }
            }
            .firstOrNull() ?: null to null

    return when {
        address != null && currencyCode != null ->
            Link.CryptoRequestUrl(
                currencyCode = currencyCode,
                address = address.toString()
            )
        uriParser.isCryptoUrl(this) ->
            uriParser.parseRequest(this)?.asCryptoRequestUrl()
        isGiftUrl(uri) -> uri.asGiftUrl(scanned = scanned)
        isPlatformUrl(uri) -> uri.asPlatformUrl()
        isPlatformDebugUrl(uri) -> uri.asPlatformDebugUrl()
        isWalletPairUrl(uri) -> Link.WalletPairUrl(PairingMetaData(this))
        isBreadUrl(uri) -> uri.asBreadUrl()
        else -> toByteArray().let { bytes ->
            when {
                Key.isProtectedPrivateKeyString(bytes) ->
                    Link.ImportWallet(this, true)
                Key.createFromPrivateKeyString(bytes).isPresent ->
                    Link.ImportWallet(this, false)
                else -> null
            }
        }
    }
}

fun CryptoRequest.asCryptoRequestUrl() =
    Link.CryptoRequestUrl(
        address = address,
        currencyCode = currencyCode,
        amount = amount ?: value,
        label = label,
        message = message,
        reqParam = reqVariable,
        rUrlParam = rUrl,
        destinationTag = destinationTag
    )

private fun Uri.asGiftUrl(scanned: Boolean): Link.ImportWallet {
    val key = Base64.decode(lastPathSegment ?: "", Base64.DEFAULT)
    return Link.ImportWallet(
        key.toString(Charsets.UTF_8),
        passwordProtected = false,
        gift = true,
        scanned = scanned
    )
}

private fun Uri.asPlatformUrl(): Link.PlatformUrl {
    val rawPath = "/${checkNotNull(path).replace(PLATFORM_PATH_PREFIX, "")}"
    val encodedPath = URLEncoder.encode(rawPath, PATH_ENCODING)
    val platformUrl = PLATFORM_URL_FORMAT.format(encodedPath)
    return Link.PlatformUrl("${HTTPServer.getPlatformBaseUrl()}$platformUrl")
}

private fun Uri.asPlatformDebugUrl() =
    Link.PlatformDebugUrl(
        webBundle = getQueryParameter(QUERY_PARAM_WEB_BUNDLE),
        webBundleUrl = getQueryParameter(QUERY_PARAM_BUNDLE_DEBUG_URL)
    )

private fun Uri.asBreadUrl(): Link? {
    val path = (path ?: "").trimStart('/')
    return when {
        host.equals(SCAN_QR, true) || path.equals(SCAN_QR, true) ->
            Link.BreadUrl.ScanQR
        // TODO: Pre-4.0 this would copy the current wallet,
        //  evaluate the purpose of this url and implement if needed.
        host.equals(ADDRESS, true) || path.equals(ADDRESS, true) ->
            Link.BreadUrl.Address
        // Address list was not implemented before 4.0
        host.equals(ADDRESS_LIST, true) || path.equals(ADDRESS, true) ->
            Link.BreadUrl.AddressList
        else -> null
    }
}

/** Returns true if [uri] will produce a [Link.BreadUrl]. */
private fun isBreadUrl(uri: Uri): Boolean {
    // Check bread scheme links
    val isBrdScheme = uri.scheme.equals(BRConstants.BREAD, ignoreCase = true)
    val isValidHost = uri.host.run {
        equals(SCAN_QR, true) ||
            equals(ADDRESS, true) ||
            equals(ADDRESS_LIST, true)
    }
    if (isBrdScheme && isValidHost) return true

    // Check brd.com links
    val isBrdDomain = uri.host.equals(BRD_HOST, true)
    val isValidPath = (uri.path ?: "")
        .trimStart('/')
        .run {
            equals(SCAN_QR, true) ||
                equals(ADDRESS, true) ||
                equals(ADDRESS_LIST, true)
        }

    return isBrdDomain && isValidPath
}

private fun isGiftUrl(uri: Uri): Boolean {
    val isProtocolValid = uri.toString().startsWith(BRD_PROTOCOL)
    val isHostValid = BRD_HOST.equals(uri.host, ignoreCase = true)
    val isPathValid = (uri.path ?: "").run {
        isNotBlank() && startsWith(GIFT_PATH_PREFIX)
    }
    return isProtocolValid && isHostValid && isPathValid
}

/** Returns true if [uri] will produce a [Link.PlatformUrl] */
private fun isPlatformUrl(uri: Uri): Boolean {
    val isProtocolValid = uri.toString().startsWith(BRD_PROTOCOL)
    val isHostValid = BRD_HOST.equals(uri.host, ignoreCase = true)
    val isPathValid = (uri.path ?: "").run {
        isNotBlank() && startsWith(PLATFORM_PATH_PREFIX)
    }
    return isProtocolValid && isHostValid && isPathValid
}

/** Returns true if [uri] will produce a [Link.PlatformDebugUrl] */
private fun isPlatformDebugUrl(uri: Uri): Boolean {
    val isHostValid = uri.host.equals(BRD_HOST, ignoreCase = true)
    val isPathValid = (uri.path ?: "").run {
        isNotBlank() && startsWith(PLATFORM_DEBUG_PATH_PREFIX)
    }
    return isHostValid && isPathValid
}

/** Returns true if [uri] will produce a [Link.WalletPairUrl] */
private fun isWalletPairUrl(uri: Uri): Boolean {
    val isBrdHost = uri.host.equals(BRD_HOST, ignoreCase = true)
    val isPairOrLinkPath = (uri.path ?: "").run {
        contains(WALLET_PAIR_PATH) || contains(WALLET_LINK_PATH)
    }
    return isBrdHost && isPairOrLinkPath
}
