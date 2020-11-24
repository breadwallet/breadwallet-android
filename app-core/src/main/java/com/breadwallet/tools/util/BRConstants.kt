/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 2/16/16.
 * Copyright (c) 2016 breadwallet LLC
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

import java.math.RoundingMode

const val btc = "btc"
const val eth = "eth"
const val bch = "bch"
const val hbar = "hbar"
const val xtz = "xtz"

object BRConstants {
    /**
     * Boolean values as Strings.
     */
    const val TRUE = "true"
    const val FALSE = "false"

    /**
     * Permissions
     */
    const val CAMERA_REQUEST_ID = 34
    const val GEO_REQUEST_ID = 35
    const val CAMERA_PERMISSIONS_RC = 36

    /**
     * Request codes for auth
     */
    const val SHOW_PHRASE_REQUEST_CODE = 111
    const val PAY_REQUEST_CODE = 112
    const val PUT_PHRASE_NEW_WALLET_REQUEST_CODE = 114
    const val PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE = 115
    const val PAYMENT_PROTOCOL_REQUEST_CODE = 116
    const val REQUEST_PHRASE_BITID = 117
    const val PROVE_PHRASE_REQUEST = 119
    const val UPLOAD_FILE_REQUEST = 120

    /**
     * Request codes for take picture
     */
    const val SCANNER_REQUEST = 201
    const val REQUEST_IMAGE_RC = 203

    /**
     * Currency units
     *
     *
     * TODO: No longer supported, remove with the
     * deletion of legacy.wallet.wallets.*
     */
    @Deprecated("")
    val CURRENT_UNIT_BITS = 0

    @Deprecated("")
    val CURRENT_UNIT_MBITS = 1

    @Deprecated("")
    val CURRENT_UNIT_BITCOINS = 2
    const val BITS_SYMBOL = "\u0180"
    const val ETH_SYMBOL = "\u039E"
    const val BITCOIN_SYBMOL_OLD = "\u0243"
    const val BITCOIN_SYMBOL = "\u20BF"
    @JvmField
    val ROUNDING_MODE = RoundingMode.HALF_EVEN
    const val WRITE_AHEAD_LOGGING = true

    /**
     * Support Center article ids.
     */
    const val FAQ_DISPLAY_CURRENCY = "display-currency"
    const val FAQ_RECOVER_WALLET = "recover-wallet"
    const val FAQ_RESCAN = "re-scan"
    const val FAQ_SECURITY_CENTER = "security-center"
    const val FAQ_PAPER_KEY = "paper-key"
    const val FAQ_ENABLE_FINGERPRINT = "enable-fingerprint-authentication"
    const val FAQ_TRANSACTION_DETAILS = "transaction-details"
    const val FAQ_RECEIVE = "receive-tx"
    const val FAQ_REQUEST_AMOUNT = "request-amount"
    const val FAQ_SEND = "send-tx"
    const val FAQ_WALLET_DISABLE = "wallet-disabled"
    const val FAQ_RESET_PIN_WITH_PAPER_KEY = "reset-pin-paper-key"
    const val FAQ_SET_PIN = "set-pin"
    const val FAQ_IMPORT_WALLET = "import-wallet"
    const val FAQ_WRITE_PAPER_KEY = "write-phrase"
    const val FAQ_START_VIEW = "start-view"
    const val FAQ_WIPE_WALLET = "wipe-wallet"
    const val FAQ_LOOP_BUG = "android-loop-bug"
    const val FAQ_BCH = "bitcoin-cash"
    const val FAQ_UNSUPPORTED_TOKEN = "unsupported-token"
    const val FAQ_FASTSYNC = "fastsync-explained"

    /**
     * API Constants
     */
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_ACCEPT = "Accept"

    // OkHttp standard; use for all outgoing HTTP requests.
    const val CONTENT_TYPE_JSON_CHARSET_UTF8 = "application/json; charset=UTF-8"

    // Server response content type; user to verify all incoming HTTP responses.
    const val CONTENT_TYPE_JSON = "application/json"
    const val CONTENT_TYPE_TEXT = "text/plain"
    const val AUTHORIZATION = "Authorization"

    /**
     * Extra constants
     */
    const val EXTRA_URL = "com.breadwallet.EXTRA_URL"
    const val DRAWABLE = "drawable"
    const val CURRENCY_PARAMETER_STRING_FORMAT = "%s?currency=%s"

    /**
     * Social media links and privacy policy URLS
     */
    const val URL_PRIVACY_POLICY = "https://brd.com/privacy"
    const val URL_TWITTER = "https://twitter.com/brdhq"
    const val URL_REDDIT = "https://www.reddit.com/r/BRDapp/"
    const val URL_BLOG = "https://brd.com/blog/"
    const val URL_BRD_HOST = "brd.com"
    const val WALLET_PAIR_PATH = "wallet-pair"
    const val WALLET_LINK_PATH = "link-wallet"
    const val STRING_RESOURCES_FILENAME = "string"
    const val BREAD = "bread"
    const val PROTOCOL = "https"
    const val GZIP = "gzip"
    const val CONTENT_ENCODING = "content-encoding"
    const val METHOD = "method"
    const val BODY = "body"
    const val HEADERS = "headers"
    const val CLOSE_ON = "closeOn"
    const val CLOSE = "/_close"
    const val ARTICLE_ID = "articleId"
    const val URL = "url"
    const val JSONRPC = "jsonrpc"
    const val VERSION_2 = "2.0"
    const val ETH_BALANCE = "eth_getBalance"
    const val LATEST = "latest"
    const val PARAMS = "params"
    const val ID = "id"
    const val RESULT = "result"
    const val ACCOUNT = "account"
    const val ETH_GAS_PRICE = "eth_gasPrice"
    const val ETH_ESTIMATE_GAS = "eth_estimateGas"
    const val ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction"
    const val ERROR = "error"
    const val CODE = "code"
    const val MESSAGE = "message"
    const val HASH = "hash"
    const val TO = "to"
    const val FROM = "from"
    const val CONTRACT_ADDRESS = "contractAddress"
    const val ADDRESS = "address"
    const val VALUE = "value"
    const val GAS = "gas"
    const val GAS_PRICE = "gasPrice"
    const val NONCE = "nonce"
    const val GAS_USED = "gasUsed"
    const val BLOCK_NUMBER = "blockNumber"
    const val ETH_BLOCK_NUMBER = "eth_blockNumber"
    const val ETH_TRANSACTION_COUNT = "eth_getTransactionCount"
    const val BLOCK_HASH = "blockHash"
    const val LOG_INDEX = "logIndex"
    const val INPUT = "input"
    const val CONFIRMATIONS = "confirmations"
    const val TRANSACTION_INDEX = "transactionIndex"
    const val TIMESTAMP = "timeStamp"
    const val IS_ERROR = "isError"
    const val TOPICS = "topics"
    const val DATA = "data"
    const val DATE = "Date"
    const val TRANSACTION_HASH = "transactionHash"
    const val CHECKOUT = "checkout"
    const val GET = "GET"
    const val POST = "POST"
    const val HEADER_WWW_AUTHENTICATE = "www-authenticate"
    const val NAME = "name"
    const val TOKEN = "token"
    const val STAGING = "staging"
    const val STAGE = "stage"
    const val CURRENCY_ERC20 = "erc20"
    const val RATES = "rates"
    const val CURRENCY = "currency"
    const val UTF_8 = "UTF-8"
    const val USD = "USD"
}
