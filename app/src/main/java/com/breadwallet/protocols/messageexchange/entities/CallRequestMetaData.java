package com.breadwallet.protocols.messageexchange.entities;

import android.os.Parcel;

/**
 * BreadWallet
 * <p/>
 * Created by Shivangi Gandhi on <shivangi@brd.com> 7/25/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class CallRequestMetaData extends RequestMetaData {
    private String mAbi;

    public static final Creator<CallRequestMetaData> CREATOR = new Creator<CallRequestMetaData>() {
        @Override
        public CallRequestMetaData[] newArray(int size) {
            return new CallRequestMetaData[size];
        }

        @Override
        public CallRequestMetaData createFromParcel(Parcel source) {
            return new CallRequestMetaData(source);
        }
    };

    public CallRequestMetaData(String id, String currencyCode, String network, String address, String amount, String memo, String abi) {
        super(id, currencyCode, network, address, amount, memo);
        mAbi = abi;
    }

    public CallRequestMetaData(Parcel source) {
        super(source);
        mAbi = source.readString();
    }

    public String getAbi() {
        return mAbi;
    }

    public void setAbi(String abi) {
        mAbi = abi;
    }

    @Override
    protected void writeToParcel(Parcel destination) {
        destination.writeString(mAbi);
    }
}
