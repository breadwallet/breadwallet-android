package com.breadwallet.presenter.entities;


import com.platform.entities.TxMetaData;

import java.math.BigDecimal;

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

public class TxUiHolder {
    public static final String TAG = TxUiHolder.class.getName();
    private long timeStamp;
    private int blockHeight;
    private byte[] txHash;
    private BigDecimal sent;
    private BigDecimal received;
    private BigDecimal fee;//satoshis or gas paid
    private BigDecimal feeRate;//gas price
    private BigDecimal feeLimit;//gas limit
    private String to;
    private String from;
    public String txReversed;
    private BigDecimal balanceAfterTx;
    private BigDecimal amount;
    private boolean isValid;
    private int txSize;
    public TxMetaData metaData;
    public Object transaction;
    private TxUiHolder() {
    }

    public TxUiHolder(Object transaction, long timeStamp, int blockHeight, byte[] hash, String txReversed, BigDecimal sent,
                      BigDecimal received, BigDecimal fee, BigDecimal feeRate, BigDecimal feeLimit, String to, String from,
                      BigDecimal balanceAfterTx, int txSize, BigDecimal amount, boolean isValid) {
        this.transaction = transaction;
        this.timeStamp = timeStamp;
        this.blockHeight = blockHeight;
        this.txReversed = txReversed;
        this.txHash = hash;
        this.sent = sent;
        this.received = received;
        this.fee = fee;
        this.feeRate = feeRate;
        this.feeLimit = feeLimit;
        this.to = to;
        this.from = from;
        this.balanceAfterTx = balanceAfterTx;
        this.amount = amount;
        this.isValid = isValid;
        this.txSize = txSize;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public int getTxSize() {
        return txSize;
    }

    public String getFrom() {
        return from;
    }

    public byte[] getTxHash() {
        return txHash;
    }

    public String getTxHashHexReversed() {
        return txReversed;
    }

    public BigDecimal getReceived() {
        return received;
    }

    public BigDecimal getSent() {
        return sent;
    }

    public static String getTAG() {
        return TAG;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getTo() {
        return to;
    }

    public BigDecimal getBalanceAfterTx() {
        return balanceAfterTx;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isValid() {
        return isValid;
    }

    public BigDecimal getFeeRate() {
        return feeRate;
    }

    public BigDecimal getFeeLimit() {
        return feeLimit;
    }
}
