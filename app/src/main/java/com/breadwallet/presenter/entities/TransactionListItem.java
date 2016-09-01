package com.breadwallet.presenter.entities;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 1/13/16.
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

public class TransactionListItem {
    public static final String TAG = TransactionListItem.class.getName();
    private long timeStamp;
    private int blockHeight;
    private String hash;
    private long sent;
    private long received;
    private long fee;
    private String to[];
    private String from[];
    private long balanceAfterTx;
    private long outAmounts[];

    private TransactionListItem() {
    }

    public TransactionListItem(long timeStamp, int blockHeight, String hash, long sent,
                               long received, long fee, String to[], String from[],
                               long balanceAfterTx, long[] outAmounts) {
        this.timeStamp = timeStamp;
        this.blockHeight = blockHeight;
        this.hash = hash;
        this.sent = sent;
        this.received = received;
        this.fee = fee;
        this.to = to;
        this.from = from;
        this.balanceAfterTx = balanceAfterTx;
        this.outAmounts = outAmounts;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public long getFee() {
        return fee;
    }

    public String[] getFrom() {
        return from;
    }

    public String getHexId() {
        return hash;
    }

    public long getReceived() {
        return received;
    }

    public long getSent() {
        return sent;
    }

    public static String getTAG() {
        return TAG;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String[] getTo() {
        return to;
    }

    public long getBalanceAfterTx() {
        return balanceAfterTx;
    }

    public long[] getOutAmounts() {
        return outAmounts;
    }

}
