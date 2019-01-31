/*
 * BreadWallet
 *
 * Created by Ed Gamble <ed@breadwallet.com> on 1/22/18.
 * Copyright (c) 2018 breadwallet LLC
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
package com.breadwallet.core;

public class BRCorePaymentProtocolRequest extends BRCoreJniReference {
    //
    //
    //
    public BRCorePaymentProtocolRequest(byte[] data) {
        super(createPaymentProtocolRequest(data));
    }

    public native String getNetwork();

    public native BRCoreTransactionOutput[] getOutputs ();

    public native long getTime ();

    public native long getExpires();

    public native String getMemo();

    public native String getPaymentURL ();

    public native byte[] getMerchantData ();

    public native long getVersion ();

    public native String getPKIType ();

    public native byte[] getPKIData ();

    public native byte[] getSignature ();

    public native byte[] getDigest ();

    public native byte[][] getCerts ();

    private static native long createPaymentProtocolRequest(byte[] data);

    public native byte[] serialize ();

    public native void disposeNative ();

    protected static native void initializeNative ();

    static { initializeNative(); }
}
