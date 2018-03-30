package com.breadwallet.presenter.entities;


import android.content.Context;
import android.util.Log;

import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseTransaction;
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
    public String iso = "BTC"; //make it default
    public String address;
    public String scheme;
    public String r;
    public BigDecimal amount;
    public String label;
    public String message;
    public String req;

    public BaseTransaction tx;
    public String cn;
    public boolean isAmountRequested;

    public CryptoRequest(BaseTransaction tx, String certificationName, boolean isAmountRequested, String message, String address, BigDecimal amount) {
        this.isAmountRequested = isAmountRequested;
        this.tx = tx;
        this.cn = certificationName;
        this.address = address;
        this.amount = amount;
        this.message = message;
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
        BigDecimal minAmount = walletManager.getMinOutputAmount();
        BigDecimal amount = walletManager.getTransactionAmount(tx).abs();
        Log.e(TAG, "isSmallerThanMin: " + amount);
        return minAmount != null && amount.compareTo(minAmount) < 0;
    }

    public boolean isLargerThanBalance(Context app, BaseWalletManager walletManager) {
        return walletManager.getTransactionAmount(tx).abs().compareTo(walletManager.getCachedBalance(app)) > 0
                && walletManager.getTransactionAmount(tx).abs().compareTo(new BigDecimal(0)) > 0;
    }

    public boolean notEnoughForFee(Context app, BaseWalletManager walletManager) {
        BigDecimal maxOutput = walletManager.getMaxOutputAmount();
        if (maxOutput.compareTo(new BigDecimal(0)) <= 0) return false;
        BigDecimal feeForTx = walletManager.getFeeForTxAmount(maxOutput);
        return feeForTx.compareTo(new BigDecimal(0)) > 0;
    }

    public String getReceiver(BaseWalletManager walletManager) {
        String receiver;
        boolean certified = false;
        if (cn != null && cn.length() != 0) {
            certified = true;
        }
        receiver = walletManager.getTxAddress(tx).stringify();
        if (certified) {
            receiver = "certified: " + cn + "\n";
        }
        return receiver;
    }

}
