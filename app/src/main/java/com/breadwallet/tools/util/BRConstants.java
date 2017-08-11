package com.breadwallet.tools.util;

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
     * RequestQRActivity
     */
    public static final String INTENT_EXTRA_REQUEST_AMOUNT = "request_amount";
    public static final String INTENT_EXTRA_REQUEST_ADDRESS = "request_address";

    /**
     * Auth modes
     */
    public static final int AUTH_FOR_PHRASE = 11;
    public static final int AUTH_FOR_PAY = 12;
    public static final int AUTH_FOR_GENERAL = 13;
    public static final int AUTH_FOR_LIMIT = 14;
    public static final int AUTH_FOR_PAYMENT_PROTOCOL = 15;
    public static final int AUTH_FOR_BIT_ID = 16;
    public static final int AUTH_FOR_BCH = 17;

    /**
     * BlockHeight prefs
     */
    public static final String BLOCK_HEIGHT = "blockHeight";

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

    /**
     * Request codes for take picture
     */
    public static final int REQUEST_IMAGE_CAPTURE = 111;

    public static final String CANARY_STRING = "canary";
    public static final String FIRST_ADDRESS = "firstAddress";
    public static final String SECURE_TIME_PREFS = "secureTime";
    public static final String PHRASE_WARNING_TIME = "phraseWarningTime";
    public static final String EXCHANGE_RATES = "exchangeRates";
    public static final String FEE_KB_PREFS = "feeKb";
    public static final String LITTLE_CIRCLE = "\u2022";
    public static String SUPPORT_EMAIL = "support@breadwallet.com";

    public static final int ONE_BITCOIN = 100000000;
    public static final int HUNDRED_BITS = 10000000;


    /**
     * MainActivity
     */
    public static final int BURGER = 0;
    public static final int CLOSE = 1;
    public static final int BACK = 2;
    public static final int DEBUG = 1;
    public static final int RELEASE = 2;
    public static final float PAGE_INDICATOR_SCALE_UP = 1.3f;


    /**
     * BRWalletManager
     */
    public static final long TX_FEE_PER_KB = 5000;
    public static final long DEFAULT_FEE_PER_KB = (TX_FEE_PER_KB * 1000 + 190) / 191;
    public static final long MAX_FEE_PER_KB = (100100 * 1000 + 190) / 191;

    /**
     * BreadWalletApp
     */
    public static final int BREAD_WALLET_IMAGE = 0;
    public static final int BREAD_WALLET_TEXT = 1;
    public static final int LOCKER_BUTTON = 2;
    public static final int PAY_BUTTON = 3;
    public static final int REQUEST_BUTTON = 4;

    /**
     * FragmentDecoder
     */
    public static final String CAMERA_GUIDE_RED = "red";
    public static final String CAMERA_GUIDE = "reg";
    public static final String TEXT_EMPTY = "";

    /**
     * FragmentScanResult
     */
    public static final int BITCOIN_LEFT = 1;
    public static final int BITCOIN_RIGHT = 2;
    public static final String DOUBLE_ARROW = "\u21CB";

    /**
     * FragmentSpendLimit
     */
    public static final int limit1 = 10000000;
    public static final int limit2 = 100000000;
    public static final int limit3 = 1000000000;

    /**
     * PasswordDialogFragment
     */
    public static final int AUTH_MODE_CHECK_PASS = 0;
    public static final int AUTH_MODE_NEW_PASS = 1;
    public static final int AUTH_MODE_CONFIRM_PASS = 2;

    /**
     * AmountAdapter
     */
    public static final int MAX_DIGITS_AFTER_SEPARATOR_BITS = 2;
    public static final int MAX_DIGITS_AFTER_SEPARATOR_MBITS = 5;
    public static final int MAX_DIGITS_AFTER_SEPARATOR_BITCOINS = 8;
    public static final int DIGITS_LIMIT = 12;

    /**
     * SharedPreferencesManager
     */
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String RECEIVE_ADDRESS = "receive_address";
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
    public static final int CURRENT_UNIT_BITS = 0;
    public static final int CURRENT_UNIT_MBITS = 1;
    public static final int CURRENT_UNIT_BITCOINS = 2;

    public static final String bitcoinLowercase = "\u0180";
    public static final String bitcoinUppercase = "\u0243";

    public static final long PASS_CODE_TIME_LIMIT = TimeUnit.MILLISECONDS.convert(6, TimeUnit.DAYS);

    public static final boolean PLATFORM_ON = false;

    private BRConstants() {
    }

}
