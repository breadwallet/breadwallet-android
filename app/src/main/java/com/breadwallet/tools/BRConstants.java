package com.breadwallet.tools;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan on 2/16/16.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
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

    /**
     * BlockHeight prefs
     */
    public static final String BLOCK_HEIGHT = "blockHeight";

    /**
     * Permissions
     */
    public static final int CAMERA_REQUEST_ID = 34;

    /**
     * Request codes
     */
    public static final int RECOVERY_PHRASE_REQUEST_CODE = 111;
    public static final int PAY_REQUEST_CODE = 112;
    public static final int PUT_CANARY_REQUEST_CODE = 113;
    public static final int PUT_PHRASE_NEW_WALLET_REQUEST_CODE = 114;
    public static final int PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE = 115;


    public static final String CANARY_STRING = "canary";
    public static final String FIRST_ADDRESS = "firstAddress";

    private BRConstants() {
    }


}
