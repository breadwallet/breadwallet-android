package com.breadwallet.legacy.presenter.entities;

import com.breadwallet.tools.util.Utils;

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
    private String mCurrencyCode; //make it default
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
    private String destinationTag; // xrp destination tag for exchange account routing.

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
        destinationTag = builder.destinationTag;
    }

    public boolean isPaymentProtocol() {
        return !Utils.isNullOrEmpty(mRUrl);
    }

    public boolean hasAddress() {
        return !Utils.isNullOrEmpty(mAddress);
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

    public void setDestinationTag(String destinationTag) {
        this.destinationTag = destinationTag;
    }

    public String getDestinationTag() {
        return destinationTag;
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
        private String destinationTag;

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

        public String getDestinationTag() {
            return destinationTag;
        }

        public Builder setDestinationTag(String destinationTag) {
            this.destinationTag = destinationTag;
            return this;
        }

        public CryptoRequest build() {
            return new CryptoRequest(this);
        }
    }
}
