package com.breadwallet.presenter.entities;


import android.content.Context;
import android.util.Log;

import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.entities.GenericTransactionMetaData;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;

import java.io.Serializable;
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

public class CryptoRequest implements Serializable {
    public static final String TAG = CryptoRequest.class.getName();
    private String mCurrencyCode = WalletBitcoinManager.BITCOIN_CURRENCY_CODE; //make it default
    private String mAddress;
    private String mScheme;
    private String mRUrl;// "r" parameter, whose value is a URL from which a PaymentRequest message should be fetched
    private BigDecimal mAmount;
    private String mLabel;
    private String mMessage;
    private String mReqVariable;// "req" parameter, whose value is a required variable which are prefixed with a req-.
    private BigDecimal mValue; // ETH payment request amounts are called `value`
    private GenericTransactionMetaData mGenericTransactionMetaData;
    private boolean mIsAmountRequested;

    public CryptoRequest(Builder builder) {
        mCurrencyCode = builder.getCurrencyCode(); //make it default
        mAddress = builder.getAddress();
        mScheme = builder.getScheme();
        mRUrl = builder.getRUrl();
        mAmount = builder.getAmount();
        mLabel = builder.getLabel();
        mMessage = builder.getMessage();
        mReqVariable = builder.getReqVariable();
        mValue = builder.getValue(); // ETH payment request amounts are called `value`
        mGenericTransactionMetaData = builder.getGenericTransactionMetaData();
    }

    public boolean isPaymentProtocol() {
        return !Utils.isNullOrEmpty(mRUrl);
    }

    public boolean hasAddress() {
        return !Utils.isNullOrEmpty(mAddress);
    }

    public boolean isSmallerThanMin(Context app, BaseWalletManager walletManager) {
        BigDecimal minAmount = walletManager.getMinOutputAmount(app);
        BigDecimal absAmt = mAmount.abs();
        Log.e(TAG, "isSmallerThanMin: " + absAmt);
        return minAmount != null && absAmt.compareTo(minAmount) < 0 && mGenericTransactionMetaData == null;
    }

    public boolean isLargerThanBalance(BaseWalletManager walletManager) {
        return mAmount.abs().compareTo(walletManager.getBalance()) > 0
                && mAmount.abs().compareTo(BigDecimal.ZERO) > 0;
    }

    //not enough money for a tx + fee
    public boolean notEnoughForFee(Context app, BaseWalletManager walletManager) {
        BigDecimal balance = walletManager.getBalance();

        boolean isErc20 = WalletsMaster.getInstance().isCurrencyCodeErc20(app, walletManager.getCurrencyCode());

        if (isErc20) {
            BigDecimal feeForTx = walletManager.getEstimatedFee(mAmount, null);
            return mAmount.compareTo(balance) > 0 || feeForTx.compareTo(WalletEthManager.getInstance(app).getBalance()) > 0;
        } else {
            BigDecimal minAmount = walletManager.getMinOutputAmount(app);
            BigDecimal feeForTx = walletManager.getEstimatedFee(mAmount, null);
            BigDecimal minAmountFee = walletManager.getEstimatedFee(minAmount, null);
            return mAmount.add(feeForTx).compareTo(balance) > 0 && minAmount.add(minAmountFee).compareTo(balance) > 0;
        }
    }

    //the fee needs adjustments (amount + fee > balance but possible to adjust the amount to create a tx)
    public boolean feeOverBalance(Context app, BaseWalletManager walletManager) {
        BigDecimal balance = walletManager.getBalance();

        boolean isErc20 = WalletsMaster.getInstance().isCurrencyCodeErc20(app, walletManager.getCurrencyCode());

        if (isErc20) {
            return false; //never need adjustment for ERC20s (fee is in ETH)
        } else {
            BigDecimal minAmount = walletManager.getMinOutputAmount(app);
            BigDecimal feeForTx = walletManager.getEstimatedFee(mAmount, null);
            BigDecimal minAmountFee = walletManager.getEstimatedFee(minAmount, null);
            return mAmount.add(feeForTx).compareTo(balance) > 0 && minAmount.add(minAmountFee).compareTo(balance) < 0;
        }
    }

    public GenericTransactionMetaData getGenericTransactionMetaData() {
        return mGenericTransactionMetaData;
    }

    public void setGenericTransactionMetaData(GenericTransactionMetaData genericTransactionMetaData) {
        mGenericTransactionMetaData = genericTransactionMetaData;
    }

    public String getCurrencyCode() {
        return mCurrencyCode;
    }

    public String getAddress() {
        return mAddress;
    }

    /**
     * Return the address of the crypto request removing the scheme if required.
     * @param includeScheme If true the scheme will be removed from the address when present.
     * @return The address of the request.
     */
    public String getAddress(boolean includeScheme) {
        if (mAddress.contains(":") && !includeScheme) {
            return mAddress.split(":")[1];
        } else {
            return mAddress;
        }
    }

    public String getScheme() {
        return mScheme;
    }

    public String getRUrl() {
        return mRUrl;
    }

    public BigDecimal getAmount() {
        return mAmount;
    }

    public String getLabel() {
        return mLabel;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getReqVariable() {
        return mReqVariable;
    }

    public BigDecimal getValue() {
        return mValue;
    }

    public boolean isAmountRequested() {
        return mIsAmountRequested;
    }

    public void setCurrencyCode(String currencyCode) {
        mCurrencyCode = currencyCode;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public void setScheme(String scheme) {
        mScheme = scheme;
    }

    public void setRUrl(String rUrl) {
        mRUrl = rUrl;
    }

    public void setAmount(BigDecimal amount) {
        mAmount = amount;
    }

    public void setLabel(String label) {
        mLabel = label;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public void setReqVariable(String reqVariable) {
        mReqVariable = reqVariable;
    }

    public void setValue(BigDecimal value) {
        mValue = value;
    }

    public void setIsAmountRequested(boolean isAmountRequested) {
        mIsAmountRequested = isAmountRequested;
    }

    public static class Builder {
        private String mCurrencyCode;
        private String mAddress;
        private String mScheme;
        private String mRUrl;
        private BigDecimal mAmount;
        private String mLabel;
        private String mMessage;
        private String mReqVariable;
        private BigDecimal mValue; // ETH payment request amounts are called `value`
        private GenericTransactionMetaData mGenericTransactionMetaData;

        public String getCurrencyCode() {
            return mCurrencyCode;
        }

        public Builder setCurrencyCode(String currencyCode) {
            mCurrencyCode = currencyCode;
            return this;
        }

        public String getAddress() {
            return mAddress;
        }

        public Builder setAddress(String address) {
            mAddress = address;
            return this;
        }

        public String getScheme() {
            return mScheme;
        }

        public Builder setScheme(String scheme) {
            mScheme = scheme;
            return this;
        }

        public String getRUrl() {
            return mRUrl;
        }

        public Builder setRUrl(String rUrl) {
            mRUrl = rUrl;
            return this;
        }

        public BigDecimal getAmount() {
            return mAmount;
        }

        public Builder setAmount(BigDecimal amount) {
            mAmount = amount;
            return this;
        }

        public String getLabel() {
            return mLabel;
        }

        public Builder setLabel(String label) {
            mLabel = label;
            return this;
        }

        public String getMessage() {
            return mMessage;
        }

        public Builder setMessage(String message) {
            mMessage = message;
            return this;
        }

        public String getReqVariable() {
            return mReqVariable;
        }

        public Builder setReqUrl(String reqUrl) {
            mReqVariable = reqUrl;
            return this;
        }

        public BigDecimal getValue() {
            return mValue;
        }

        public Builder setValue(BigDecimal value) {
            mValue = value;
            return this;
        }

        public GenericTransactionMetaData getGenericTransactionMetaData() {
            return mGenericTransactionMetaData;
        }

        public Builder setGenericTransactionMetaData(GenericTransactionMetaData genericTransactionMetaData) {
            mGenericTransactionMetaData = genericTransactionMetaData;
            return this;
        }

        public CryptoRequest build() {
            return new CryptoRequest(this);
        }
    }
}
