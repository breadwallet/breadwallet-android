package com.breadwallet.presenter.entities;


import android.content.Context;
import android.util.Log;

import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 11/20/15.
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

public class CryptoRequest {
    public static final String TAG = CryptoRequest.class.getName();
    public String iso;
    public String address;
    public String r;
    public BigDecimal amount;
    public String label;
    public String message;
    public String req;

    public BRCoreTransaction tx;
    public String cn;
    public boolean isAmountRequested;
    public String comment;

    public CryptoRequest(BRCoreTransaction tx, String certificationName, boolean isAmountRequested, String comment, String address, BigDecimal amount) {
        this.isAmountRequested = isAmountRequested;
        this.tx = tx;
        this.cn = certificationName;
        this.comment = comment;
        this.address = address;
        this.amount = amount;
    }

    public CryptoRequest() {

    }

    public boolean isPaymentProtocol() {
        return !Utils.isNullOrEmpty(r);
    }

    public boolean hasAddress() {
        return !Utils.isNullOrEmpty(address);
    }


    public boolean isSmallerThanMin(Context app, BaseWalletManager walletManager) {
        long minAmount = walletManager.getWallet().getMinOutputAmount();
        long amount = Math.abs(walletManager.getWallet().getTransactionAmount(tx));
        Log.e(TAG, "isSmallerThanMin: " + amount);
        return amount < minAmount;
    }

    public boolean isLargerThanBalance(Context app, BaseWalletManager walletManager) {
        return Math.abs(walletManager.getWallet().getTransactionAmount(tx)) > walletManager.getCachedBalance(app)
                && Math.abs(walletManager.getWallet().getTransactionAmount(tx)) > 0;
    }

    public boolean notEnoughForFee(Context app, BaseWalletManager walletManager) {
        long feeForTx = walletManager.getWallet().getTransactionFee(tx);
        if (feeForTx == 0) {
            long maxOutput = walletManager.getWallet().getMaxOutputAmount();
            feeForTx = walletManager.getWallet().getFeeForTransactionAmount(maxOutput);
            return feeForTx > 0;
        }
        return false;
    }

    public String getReceiver(BaseWalletManager walletManager) {
        String receiver;
        boolean certified = false;
        if (cn != null && cn.length() != 0) {
            certified = true;
        }
        receiver = walletManager.getWallet().getTransactionAddress(tx).stringify();
        if (certified) {
            receiver = "certified: " + cn + "\n";
        }
        return receiver;
    }

}
