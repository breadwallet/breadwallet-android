package com.breadwallet.tools.util;

import com.breadwallet.BuildConfig;

import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

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

public class BRConstants {

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
    public static final int SEND_BCH_REQUEST = 118;
    public static final int PROVE_PHRASE_REQUEST = 119;

    /**
     * Request codes for take picture
     */
    public static final int SCANNER_REQUEST = 201;
    public static final int SCANNER_BCH_REQUEST = 202;
    public static final int REQUEST_IMAGE_CAPTURE = 203;


    public static final String CANARY_STRING = "canary";
    public static final String FIRST_ADDRESS = "firstAddress";
    public static final String SECURE_TIME_PREFS = "secureTime";
    public static final String PHRASE_WARNING_TIME = "phraseWarningTime";
    public static final String EXCHANGE_RATES = "exchangeRates";
    public static final String FEE_KB_PREFS = "feeKb";
    public static final String ECONOMY_FEE_KB_PREFS = "EconomyFeeKb";
    public static final String LITTLE_CIRCLE = "\u2022";
    public static String SUPPORT_EMAIL = "support@loafwallet.com";

    public static final int ONE_BITCOIN = 100000000;
    public static final int HUNDRED_BITS = 10000000;


    /**
     * BRWalletManager
     */
    public static final long TX_FEE_PER_KB = 5000;
    public static final long DEFAULT_FEE_PER_KB = (TX_FEE_PER_KB * 1000 + 190) / 191;
    public static final long MAX_FEE_PER_KB = (100100 * 1000 + 190) / 191;


    /**
     * BRSharedPrefs
     */
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String RECEIVE_ADDRESS = "receive_address";
    public static final String WALLET_NAME = "wallet_name";
    public static final String START_HEIGHT = "startHeight";
    public static final String LAST_BLOCK_HEIGHT = "lastBlockHeight";
    public static final String TIPS_SHOWN = "tipsShown";
    public static final String CURRENT_UNIT = "currencyUnit";
    public static final String CURRENT_CURRENCY = "currentCurrency";
    public static final String POSITION = "position";
    public static final String RATE = "rate";
    public static final String PHRASE_WRITTEN = "phraseWritten";
    public static final String LIMIT_PREFS = "fingerprintLimit";
    public static final String ALLOW_SPEND = "allowSpend";
    public static final String USER_ID = "userId";
    public static final String GEO_PERMISSIONS_REQUESTED = "geoPermissionsRequested";

    /**
     * Currency units
     */
    public static final int CURRENT_UNIT_PHOTONS = 0;
    public static final int CURRENT_UNIT_LITES = 1;
    public static final int CURRENT_UNIT_LITECOINS = 2;

    public static final String litecoinLowercase = "\u0142";
    public static final String litecoinUppercase = "\u0141";

    public static final long PASS_CODE_TIME_LIMIT = TimeUnit.MILLISECONDS.convert(6, TimeUnit.DAYS);

    public static boolean PLATFORM_ON = true;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;


    /**
     * Support Center article ids.
     */
    public static final String displayCurrency = "display-currency";
    public static final String recoverWallet = "recover-wallet";
    public static final String reScan = "re-scan";
    public static final String securityCenter = "security-center";
    public static final String paperKey = "paper-key";
    public static final String enableFingerprint = "enable-fingerprint-authentication";
    public static final String fingerprintSpendingLimit = "fingerprint-spending-limit";
    public static final String transactionDetails = "transaction-details";
//    public static final String manageWallet = "manage-wallet";
    public static final String receive = "receive-litecoin";
//    public static final String requestAmount = "request-amount";
    public static final String send = "send-litecoin";
    public static final String walletDisabled = "wallet-disabled";
    public static final String resetPinWithPaperKey = "reset-pin-paper-key";
    public static final String setPin = "set-pin";
    public static final String importWallet = "import-wallet";
    public static final String writePhrase = "write-phrase";
//    public static final String confirmPhrase = "confirm-phrase";
    public static final String startView = "start-view";
    public static final String wipeWallet = "wipe-wallet";

    // or https://twitter.com/loafwallet
    public static final String TWITTER_LINK = "https://twitter.com/ltcfoundation";
    public static final String REDDIT_LINK = "https://www.reddit.com/r/litecoin";
    public static final String BLOG_LINK = "http://loafwallet.org";

    public static final String BLOCK_EXPLORER_BASE_URL = BuildConfig.BITCOIN_TESTNET ? "http://explorer.litecointools.com/tx/" :"https://insight.litecore.io/tx/";

    private BRConstants() {
    }

}
