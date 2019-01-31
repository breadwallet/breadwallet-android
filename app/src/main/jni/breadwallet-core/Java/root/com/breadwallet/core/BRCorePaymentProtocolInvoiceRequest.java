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

public class BRCorePaymentProtocolInvoiceRequest extends BRCoreJniReference {
    public BRCorePaymentProtocolInvoiceRequest(byte[] data) {
        super(createPaymentProtocolInvoiceRequest(data));
    }

    public BRCorePaymentProtocolInvoiceRequest (BRCoreKey senderPublicKey, long amount,
                                                String pkiType, byte[] pkiData,
                                                String memo, String notifyURL,
                                                byte[] signature) {
        super (createPaymentProtocolInvoiceRequestFull(senderPublicKey, amount,
                pkiType, pkiData,
                memo, notifyURL,
                signature));
    }

    public BRCoreKey getSenderPublicKey () {
        return new BRCoreKey (getSenderPublicKeyReference());
    }

    protected native long getSenderPublicKeyReference ();

    public native long getAmount ();

    public native String getPKIType ();

    public native byte[] getPKIData ();

    public native String getMemo ();

    public native String getNotifyURL ();

    public native byte[] getSignature ();

    private static native long createPaymentProtocolInvoiceRequest(byte[] data);

    private static native long createPaymentProtocolInvoiceRequestFull(BRCoreKey senderPublicKey, long amount,
                                                                        String pkiType, byte[] pkiData,
                                                                        String memo, String notifyURL,
                                                                        byte[] signature);

    public native byte[] serialize ();

    public native void disposeNative ();
}
