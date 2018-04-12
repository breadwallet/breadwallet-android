package com.breadwallet.tools.util;

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
     * Native library name
     */
    public static final String NATIVE_LIB_NAME =  "core";

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
    public static final int UPLOAD_FILE_REQUEST = 120;

    /**
     * Request codes for take picture
     */
    public static final int SCANNER_REQUEST = 201;
    public static final int SCANNER_BCH_REQUEST = 202;
    public static final int REQUEST_IMAGE_CAPTURE = 203;

    public static final String CANARY_STRING = "canary";
//    public static final String FIRST_ADDRESS = "firstAddress";
//    public static final String SECURE_TIME_PREFS = "secureTime";
//    public static final String FEE_KB_PREFS = "feeKb";
//    public static final String ECONOMY_FEE_KB_PREFS = "EconomyFeeKb";
    public static final String LITTLE_CIRCLE = "\u2022";
    public static String SUPPORT_EMAIL = "support@breadwallet.com";


    public static final int ONE_ETH = 100000000;
    public static final int HUNDRED_BITS = 10000000;

    /**
     * Currency units
     */
    public static final int CURRENT_UNIT_BITS = 0;
    public static final int CURRENT_UNIT_MBITS = 1;
    public static final int CURRENT_UNIT_BITCOINS = 2;

    public static final String symbolBits = "\u0180";
    public static final String symbolEther = "\u039E";
    public static final String symbolBitcoinSecondary = "\u0243";
    public static final String symbolBitcoinPrimary = "\u20BF";

    public static final long PASS_CODE_TIME_LIMIT = TimeUnit.MILLISECONDS.convert(6, TimeUnit.DAYS);

    public static boolean PLATFORM_ON = true;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.CEILING;
    public static final boolean WAL = true;

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
    public static final String receive = "receive-bitcoin";
    public static final String requestAmount = "request-amount";
    public static final String send = "send-bitcoin";
    public static final String walletDisabled = "wallet-disabled";
    public static final String resetPinWithPaperKey = "reset-pin-paper-key";
    public static final String setPin = "set-pin";
    public static final String importWallet = "import-wallet";
    public static final String writePhrase = "write-phrase";
//    public static final String confirmPhrase = "confirm-phrase";
    public static final String startView = "start-view";
    public static final String wipeWallet = "wipe-wallet";
    public static final String loopBug = "android-loop-bug";
    public static final String bchFaq = "bitcoin-cash";

    private BRConstants() {
    }

}
