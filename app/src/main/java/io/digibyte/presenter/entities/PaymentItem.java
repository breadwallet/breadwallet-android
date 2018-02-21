package io.digibyte.presenter.entities;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 11/19/15.
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

public class PaymentItem {
    public static final String TAG = PaymentItem.class.getName();

    public byte[] serializedTx;
    public String[] addresses;
    public long amount;
    public String cn;
    public boolean isAmountRequested;
    public String comment;

    public PaymentItem(String[] addresses, byte[] tx, long theAmount, String theCn, boolean isAmountRequested) {
        this.isAmountRequested = isAmountRequested;
        this.serializedTx = tx;
        this.addresses = addresses;
        this.amount = theAmount;
        this.cn = theCn;
    }

    public PaymentItem(String[] addresses, byte[] tx,long theAmount, String theCn, boolean isAmountRequested, String comment) {
        this.isAmountRequested = isAmountRequested;
        this.serializedTx = tx;
        this.addresses = addresses;
        this.amount = theAmount;
        this.cn = theCn;
        this.comment = comment;
    }

}
