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
    public static final int CANARY_REQUEST_CODE = 113;
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
    public static final int SCANNER_DID_REQUEST = 204;

    public static final String CANARY_STRING = "canary";


    public static final int PROFILE_REQUEST_NICKNAME = 301;
    public static final int PROFILE_REQUEST_EMAIL = 302;
    public static final int PROFILE_REQUEST_MOBILE = 303;
    public static final int PROFILE_REQUEST_ID = 304;

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
    public static final String FAQ_RECEIVE = "receive-bitcoin";
    public static final String FAQ_REQUEST_AMOUNT = "request-amount";
    public static final String FAQ_SEND = "send-bitcoin";
    public static final String FAQ_WALLET_DISABLE = "wallet-disabled";
    public static final String FAQ_RESET_PIN_WITH_PAPER_KEY = "reset-pin-paper-key";
    public static final String FAQ_SET_PIN = "set-pin";
    public static final String FAQ_IMPORT_WALLET = "import-wallet";
    public static final String FAQ_WRITE_PAPER_KEY = "write-phrase";
    public static final String FAQ_START_VIEW = "start-view";
    public static final String FAQ_WIPE_WALLET = "wipe-wallet";
    public static final String FAQ_LOOP_BUG = "android-loop-bug";
    public static final String FAQ_BCH = "bitcoin-cash";

    /**
     * API Constants
     */

    public static final String HTTPS_PROTOCOL = "https://";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_VALUE_CONTENT_TYPE = "application/json; charset=utf-8";
    public static final String HEADER_VALUE_ACCEPT = "application/json";


    /**
     * Extra constants
     */

    public static final String EXTRA_URL = "com.breadwallet.EXTRA_URL";
    public static final String DRAWABLE = "drawable";
    public static final String CURRENCY_PARAMETER_STRING_FORMAT = "%s?currency=%s";

    /**
     * Social media links and privacy policy URLS
     *
     */

    public static final String URL_PRIVACY_POLICY = "https://github.com/elastos/Elastos.Developer.Doc/blob/master/Ignore/Doc/Privacy_Policy.md";
    public static final String URL_TWITTER = "https://twitter.com/breadapp";
    public static final String URL_REDDIT = "https://reddit.com/r/breadwallet/";
    public static final String URL_BLOG  = "http://t.me/elastoswalletelephant";


    public static final String DPOS_VOTE_ID = "317DD1D2188C459EB24EAEBC81932F6ADB305FF66F073AB1DC767869EF2B1A04273A8A8754652DA3B89E7692BEA99FA3A1E0B2CD767377300AA4A7B0E8C7D00E";
    public static final String REA_PACKAGE_ID = "cc053c61afc22dda9a309e96943c1734";
    public static final String DEVELOPER_WEBSITE = "62f3178e1ad6f98bf383ee5bc98ac8e7f9affaaf705ab337bde46082d3dde7a9";
    public static final String HASH_ID = "14bd1d772afc8e633ce95b988646bd9ed5df65cb33ec98a43b41e6cb6de5276ca9cce91d069dc688619f1a250842b37d380d4fb984c9f912a2f86edcf5dddba9";

    private BRConstants() {
    }

}
