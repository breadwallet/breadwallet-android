package com.breadwallet.presenter.entities;

import android.util.Log;

import com.breadwallet.core.ethereum.BREthereumTransaction;
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
    private BigDecimal fee;//satoshis or gas paid
    private String to;
    private String from;
    public String txReversed;
    public String memo;
    private BigDecimal balanceAfterTx;
    private BigDecimal amount;
    private boolean isValid;
    private int txSize;
    public TxMetaData metaData;
    private Object transaction;
    private boolean isReceived;
    private boolean isVote;

    //todo refactor this useless class
    public TxUiHolder(Object transaction, boolean isReceived, long timeStamp, int blockHeight, byte[] hash, String txReversed,
                      BigDecimal fee,  String to, String from,
                      BigDecimal balanceAfterTx, int txSize, BigDecimal amount, boolean isValid, boolean isVote){
        this(transaction, isReceived, timeStamp, blockHeight, hash, txReversed, fee, to, from,
                balanceAfterTx, txSize, amount, isValid);
        this.isVote = isVote;
    }

    public TxUiHolder(Object transaction, boolean isReceived, long timeStamp, int blockHeight, byte[] hash, String txReversed,
                      BigDecimal fee,  String to, String from,
                      BigDecimal balanceAfterTx, int txSize, BigDecimal amount, boolean isValid) {
        this.transaction = transaction;
        this.timeStamp = timeStamp;
        this.blockHeight = blockHeight;
        this.txReversed = txReversed;
        this.txHash = hash;
        this.isReceived = isReceived;
        this.fee = fee;
        this.to = to;
        this.from = from;
        this.balanceAfterTx = balanceAfterTx;
        this.amount = amount;
        this.isValid = isValid;
        this.txSize = txSize;
    }

    public boolean isVote(){
        return isVote;
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

    public String getHashReversed() {
        return txReversed;
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

    public String getTxReversed() {
        return txReversed;
    }

    public TxMetaData getMetaData() {
        return metaData;
    }

    public Object getTransaction() {
        return transaction;
    }

    public boolean isReceived() {
        return isReceived;
    }



}
