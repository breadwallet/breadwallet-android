package com.breadwallet.wallet.wallets.ela.data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
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
public class ElaTransactionEntity implements Serializable {

//    public String transaction;
    public boolean isReceived;
    public long timeStamp;
    public int blockHeight;
    public byte[] hash;
    public String txReversed;
    public long fee;
    public String toAddress;
    public String fromAddress;
    public String memo;
    public long balanceAfterTx;
    public int txSize;
    public long amount;
    public String meno;
    public boolean isValid;
    public boolean isVote;

    public ElaTransactionEntity(){}

    public ElaTransactionEntity(boolean isReceived, long timeStamp, int blockHeight, byte[] hash,
                                String txReversed, long fee, String toAddress, String fromAddress, long balanceAfterTx,
                                int txSize, long amount, String meno,boolean isValid, boolean isVote){
        this.isReceived = isReceived;
        this.timeStamp = timeStamp;
        this.blockHeight = blockHeight;
        this.hash = hash;
        this.txReversed = txReversed;
        this.fee = fee;
        this.toAddress = toAddress;
        this.fromAddress = fromAddress;
        this.balanceAfterTx = balanceAfterTx;
        this.txSize = txSize;
        this.amount = amount;
        this.memo = meno;
        this.isValid = isValid;
        this.isVote = isVote;
    }
}
