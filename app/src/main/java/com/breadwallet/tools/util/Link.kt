package com.breadwallet.tools.util

import android.net.Uri
import com.breadwallet.app.BreadApp
import com.breadwallet.crypto.Key
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.legacy.wallet.entities.GenericTransactionMetaData
import com.breadwallet.protocols.messageexchange.entities.PairingMetaData
import com.breadwallet.tools.util.BRConstants.WALLET_LINK_PATH
import com.breadwallet.tools.util.BRConstants.WALLET_PAIR_PATH
import com.breadwallet.util.CryptoUriParser
import com.breadwallet.util.CurrencyCode
import com.platform.HTTPServer
import com.platform.tools.BRBitId
import org.kodein.di.erased.instance
import java.io.Serializable
import java.math.BigDecimal
import java.net.URLEncoder

private const val SCAN_QR = "scanqr"
private const val ADDRESS_LIST = "addressList"
private const val ADDRESS = "address"
private const val BRD_HOST = "brd.com"
private const val BRD_PROTOCOL = "https"
private const val PLATFORM_PATH_PREFIX = "/x/platform/"
private const val PLATFORM_DEBUG_PATH_PREFIX = "/x/debug"
private const val PLATFORM_URL_FORMAT = "/link?to=%s"
private const val PATH_ENCODING = "utf-8"
private const val QUERY_PARAM_WEB_BUNDLE = "web_bundle"
private const val QUERY_PARAM_BUNDLE_DEBUG_URL = "bundle_debug_url"

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
        val currencyCode: CurrencyCode = "BTC", // TODO: Const
        val address: String? = null,
        val amount: BigDecimal? = null,
        val label: String? = null,
        val message: String? = null,
        val reqParam: String? = null,
        val rUrlParam: String? = null,
        val transactionMetaData: GenericTransactionMetaData? = null
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
        val privateKey: String,
        val passwordProtected: Boolean
    ) : Link() {
        override fun toString() = "ImportWallet()"
    }

    sealed class BreadUrl : Link() {
        object ScanQR : BreadUrl()
        object Address : BreadUrl()
        object AddressList : BreadUrl()
    }
}

/** Turn the url string into a [Link] or null if it is not supported. */
@Suppress("ComplexMethod")
fun String.asLink(): Link? {
    val uri = Uri.parse(this)
    return when {
        isBlank() -> null
        uriParser.isCryptoUrl(this) ->
            uriParser.parseRequest(this)?.asCryptoRequestUrl()
        isPlatformUrl(uri) -> uri.asPlatformUrl()
        isPlatformDebugUrl(uri) -> uri.asPlatformDebugUrl()
        BRBitId.isBitId(this) -> {
            // The BitID process is handled internally here
            // and in BRActivity#onActivityResult.  Link
            // consumers do not need to handle this case.
            BRBitId.completeBitID(false)
            null
        }
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

private val uriParser by lazy {
    BreadApp.getKodeinInstance().instance<CryptoUriParser>()
}

private fun CryptoRequest.asCryptoRequestUrl() =
    Link.CryptoRequestUrl(
        address = address,
        currencyCode = currencyCode,
        amount = amount ?: value,
        label = label,
        message = message,
        reqParam = reqVariable,
        rUrlParam = rUrl,
        transactionMetaData = genericTransactionMetaData
    )

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
