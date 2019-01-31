/*
 * BreadWallet
 *
 * Created by Ed Gamble <ed@breadwallet.com> on 1/31/18.
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

import java.util.Arrays;

public class BRCoreTransactionInput extends BRCoreJniReference {

    /**
     * Create a transaction input.
     *
     * @param hash
     * @param index
     * @param amount
     * @param script
     * @param signature
     * @param sequence The sequence number.  If -1, TXIN_SEQUENCE will be used.
     */
    public BRCoreTransactionInput (byte[] hash, long index, long amount,
                                   byte[] script,
                                   byte[] signature,
                                   long sequence) {
        this (createTransactionInput(hash, index, amount, script, signature, sequence));
    }

    public BRCoreTransactionInput(long jniReferenceAddress) {
        super(jniReferenceAddress);
    }

    protected static native long createTransactionInput (byte[] hash, long index, long amount,
                                                         byte[] script,
                                                         byte[] signature,
                                                         long sequence);

    public native String getAddress ();

    private native void setAddress (String address);

    public native byte[] getHash ();

    public native long getIndex ();

    public native long getAmount ();

    public native byte[] getScript ();

    public native byte[] getSignature ();

    public native long getSequence ();
}

