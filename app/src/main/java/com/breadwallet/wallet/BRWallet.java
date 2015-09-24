package com.breadwallet.wallet;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/23/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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
public class BRWallet {
    public static final String TAG = BRWallet.class.getName();
    public ByteBuffer walletBuff;

    private BRWallet() {

    }

    public static BRWallet createFromBytes(byte[] bytes) throws IOException {
        final BRWallet wallet = new BRWallet();
        final ByteBuffer buf = ByteBuffer.wrap(bytes);

        wallet.walletBuff = buf;

        // If needed: buf.order(ByteOrder.LITTLE_ENDIAN);

        // Example to convert unsigned short to a positive int

        return wallet;
    }

    public native long walletBalance();

    public native List<BRUTXO> walletUTXOs();

    public native List<BRTransaction> walletTransactions();

    public native long walletTotalSent();

    public native long walletTotalReceived();

    public native void walletSetFeePerKb();


    private class BRUTXO {
        BigInteger hash;
        int n;
    }
}
