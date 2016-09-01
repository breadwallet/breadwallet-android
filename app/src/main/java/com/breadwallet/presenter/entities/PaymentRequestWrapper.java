package com.breadwallet.presenter.entities;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 11/12/15.
 * Copyright (c) 2016 breadwallet LLC
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

public class PaymentRequestWrapper {
    public final String TAG = PaymentRequestWrapper.class.getName();

    //error types
    public static final int INSUFFICIENT_FUNDS_ERROR = 1;
    public static final int SIGNING_FAILED_ERROR = 2;
    public static final int INVALID_REQUEST_ERROR = 3;
    public static final int REQUEST_TOO_LONG_ERROR = 4;
    public static final int AMOUNTS_ERROR = 5;

    //errors
    public int error = 0;

    //response
    public byte[] payment;
    public byte[] serializedTx;

    //Protocol
    public boolean isPaymentRequest;
    public byte[] signature;
    public byte[] pkiData;
    public String pkiType;

    //Protocol Details
    public String network;
    public long time;
    public long expires;
    public String memo;
    public String paymentURL;
    public byte[] merchantData;

    //Outputs
    public String[] addresses;
    public long amount;
    public long fee;

    private PaymentRequestWrapper() {
    }

    public void byteSignature(byte[] fromJNI) {
        this.signature = fromJNI;
    }

    public void pkiData(byte[] pkiData) {
        this.pkiData = pkiData;
    }

    public void merchantData(byte[] merchantData) {
        this.merchantData = merchantData;
    }

    public void payment(byte[] payment) {
        this.payment = payment;
    }

    public void serializedTx(byte[] serializedTx) {
        this.serializedTx = serializedTx;
    }

}
