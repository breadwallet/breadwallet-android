package com.breadwallet.wallet.wallets.ela;

public class ElaTransaction {
    private String mTx;
    private long mAmount;

    public String getTx() {
        return mTx;
    }

    public void setTx(String tx) {
        this.mTx = tx;
    }

    public long getAmount() {
        return mAmount;
    }

    public void setAmount(long Amount) {
        this.mAmount = Amount;
    }
}
