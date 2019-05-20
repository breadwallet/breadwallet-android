package com.breadwallet.tools.util;

import java.math.RoundingMode;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 2/16/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public final class BRConstants {
    /**
     * Boolean values as Strings.
     */
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    /**
     * Native library name
     */
    public static final String NATIVE_LIB_NAME = "core";

    /**
     * Permissions
     */
    public static final int CAMERA_REQUEST_ID = 34;
    public static final int GEO_REQUEST_ID = 35;
    public static final int CAMERA_REQUEST_GLIDERA_ID = 36;

    /**
     * Request codes for auth
     */
    public static final int SHOW_PHRASE_REQUEST_CODE = 111;
    public static final int PAY_REQUEST_CODE = 112;
    public static final int PUT_PHRASE_NEW_WALLET_REQUEST_CODE = 114;
    public static final int PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE = 115;
    public static final int PAYMENT_PROTOCOL_REQUEST_CODE = 116;
    public static final int REQUEST_PHRASE_BITID = 117;
    public static final int PROVE_PHRASE_REQUEST = 119;
    public static final int UPLOAD_FILE_REQUEST = 120;

    /**
     * Request codes for take picture
     */
    public static final int SCANNER_REQUEST = 201;
    public static final int REQUEST_IMAGE_CAPTURE = 203;

    /**
     * Currency units
     */
    public static final int CURRENT_UNIT_BITS = 0;
    public static final int CURRENT_UNIT_MBITS = 1;
    public static final int CURRENT_UNIT_BITCOINS = 2;

    public static final String BITS_SYMBOL = "\u0180";
    public static final String ETH_SYMBOL = "\u039E";
    public static final String BITCOIN_SYBMOL_OLD = "\u0243";
    public static final String BITCOIN_SYMBOL = "\u20BF";

    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    public static final boolean WRITE_AHEAD_LOGGING = true;

    /**
     * Support Center article ids.
     */
    public static final String FAQ_DISPLAY_CURRENCY = "display-currency";
    public static final String FAQ_RECOVER_WALLET = "recover-wallet";
    public static final String FAQ_RESCAN = "re-scan";
    public static final String FAQ_SECURITY_CENTER = "security-center";
    public static final String FAQ_PAPER_KEY = "paper-key";
    public static final String FAQ_ENABLE_FINGERPRINT = "enable-fingerprint-authentication";
    public static final String FAQ_FINGERPRINT_SPENDING_LIMIT = "fingerprint-spending-limit";
    public static final String FAQ_TRANSACTION_DETAILS = "transaction-details";
    public static final String FAQ_RECEIVE = "receive-tx";
    public static final String FAQ_REQUEST_AMOUNT = "request-amount";
    public static final String FAQ_SEND = "send-tx";
    public static final String FAQ_WALLET_DISABLE = "wallet-disabled";
    public static final String FAQ_RESET_PIN_WITH_PAPER_KEY = "reset-pin-paper-key";
    public static final String FAQ_SET_PIN = "set-pin";
    public static final String FAQ_IMPORT_WALLET = "import-wallet";
    public static final String FAQ_WRITE_PAPER_KEY = "write-phrase";
    public static final String FAQ_START_VIEW = "start-view";
    public static final String FAQ_WIPE_WALLET = "wipe-wallet";
    public static final String FAQ_LOOP_BUG = "android-loop-bug";
    public static final String FAQ_BCH = "bitcoin-cash";
    public static final String FAQ_UNSUPPORTED_TOKEN = "unsupported-token";

    /**
     * API Constants
     */

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    // OkHttp standard; use for all outgoing HTTP requests.
    public static final String CONTENT_TYPE_JSON_CHARSET_UTF8 = "application/json; charset=utf-8";
    // Server response content type; user to verify all incoming HTTP responses.
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
    public static final String AUTHORIZATION = "Authorization";


    /**
     * Extra constants
     */

    public static final String EXTRA_URL = "com.breadwallet.EXTRA_URL";
    public static final String DRAWABLE = "drawable";
    public static final String CURRENCY_PARAMETER_STRING_FORMAT = "%s?currency=%s";

    /**
     * Social media links and privacy policy URLS
     */

    public static final String URL_PRIVACY_POLICY = "https://brd.com/privacy";
    public static final String URL_TWITTER = "https://twitter.com/brdhq";
    public static final String URL_REDDIT = "https://www.reddit.com/r/BRDapp/";
    public static final String URL_BLOG = "https://brd.com/blog/";
    public static final String URL_BRD_HOST = "brd.com";
    public static final String WALLET_PAIR_PATH = "wallet-pair";
    public static final String WALLET_LINK_PATH = "link-wallet";


    /**
     * Font constants
     */
    public static final String TYPEFACE_PATH_CIRCULARPRO_BOLD = "fonts/CircularPro-Bold.otf";
    public static final String TYPEFACE_PATH_CIRCULARPRO_BOOK = "fonts/CircularPro-Book.otf";


    public static final String STRING_RESOURCES_FILENAME = "string";
    public static final String BREAD = "bread";

    public static final String PROTOCOL = "https";
    public static final String GZIP = "gzip";
    public static final String CONTENT_ENCODING = "content-encoding";
    public static final String METHOD = "method";
    public static final String BODY = "body";
    public static final String HEADERS = "headers";
    public static final String CLOSE_ON = "closeOn";
    public static final String CLOSE = "/_close";
    public static final String ARTICLE_ID = "articleId";
    public static final String URL = "url";
    public static final String JSONRPC = "jsonrpc";
    public static final String VERSION_2 = "2.0";
    public static final String ETH_BALANCE = "eth_getBalance";
    public static final String LATEST = "latest";
    public static final String PARAMS = "params";
    public static final String ID = "id";
    public static final String RESULT = "result";
    public static final String ACCOUNT = "account";
    public static final String ETH_GAS_PRICE = "eth_gasPrice";
    public static final String ETH_ESTIMATE_GAS = "eth_estimateGas";
    public static final String ETH_SEND_RAW_TRANSACTION = "eth_sendRawTransaction";
    public static final String ERROR = "error";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String HASH = "hash";
    public static final String TO = "to";
    public static final String FROM = "from";
    public static final String CONTRACT_ADDRESS = "contractAddress";
    public static final String ADDRESS = "address";
    public static final String VALUE = "value";
    public static final String GAS = "gas";
    public static final String GAS_PRICE = "gasPrice";
    public static final String NONCE = "nonce";
    public static final String GAS_USED = "gasUsed";
    public static final String BLOCK_NUMBER = "blockNumber";
    public static final String ETH_BLOCK_NUMBER = "eth_blockNumber";
    public static final String ETH_TRANSACTION_COUNT = "eth_getTransactionCount";
    public static final String BLOCK_HASH = "blockHash";
    public static final String LOG_INDEX = "logIndex";
    public static final String INPUT = "input";
    public static final String CONFIRMATIONS = "confirmations";
    public static final String TRANSACTION_INDEX = "transactionIndex";
    public static final String TIMESTAMP = "timeStamp";
    public static final String IS_ERROR = "isError";
    public static final String TOPICS = "topics";
    public static final String DATA = "data";
    public static final String DATE = "date";
    public static final String TRANSACTION_HASH = "transactionHash";
    public static final String CHECKOUT = "checkout";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String HEADER_WWW_AUTHENTICATE = "www-authenticate";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "selected";
    public static final String SELECTED = "selected";
    public static final String ENABLED = "enabled";
    public static final String PRIVATE = "private";
    public static final String TOKEN = "token";
    public static final String FEE_PER_KB = "fee_per_kb";
    public static final String STAGING = "staging";
    public static final String STAGE = "stage";
    public static final String CURRENCY_ERC20 = "erc20";
    public static final String RATES = "rates";
    public static final String CURRENCY = "currency";
    public static final String UTF_8 = "UTF-8";

    /**
     * Commonly used regular expressions.
     */
    public static final String SPACE_REGEX = "\\s";

    private BRConstants() {
    }

}
