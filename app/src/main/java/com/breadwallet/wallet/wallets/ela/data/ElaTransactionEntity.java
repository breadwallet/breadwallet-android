package com.breadwallet.wallet.wallets.ela.data;

import java.io.Serializable;
import java.math.BigDecimal;

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
    public long balanceAfterTx;
    public int txSize;
    public long amount;
    public boolean isValid;

    public ElaTransactionEntity(){}

    public ElaTransactionEntity(boolean isReceived, long timeStamp, int blockHeight, byte[] hash,
                                String txReversed, long fee, String toAddress, String fromAddress, long balanceAfterTx,
                                int txSize, long amount, boolean isValid){
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
        this.isValid = isValid;
    }
}
