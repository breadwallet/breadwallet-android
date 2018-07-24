package com.breadwallet.protocols.messageexchange.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class RequestMetaData implements Parcelable {
    private String mId;
    private String mCurrencyCode;
    private String mNetwork;
    private String mAddress;
    private String mAmount;
    private String mMemo;

    public static final Creator<RequestMetaData> CREATOR = new Creator<RequestMetaData>() {
        @Override
        public RequestMetaData[] newArray(int size) {
            return new RequestMetaData[size];
        }

        @Override
        public RequestMetaData createFromParcel(Parcel source) {
            return new RequestMetaData(source);
        }
    };

    public RequestMetaData(String id, String currencyCode, String network, String address, String amount, String memo) {
        mId = id;
        mCurrencyCode = currencyCode;
        mNetwork = network;
        mAddress = address;
        mAmount = amount;
        mMemo = memo;
    }

    public RequestMetaData(Parcel source) {
        mId = source.readString();
        mCurrencyCode = source.readString();
        mNetwork = source.readString();
        mAddress = source.readString();
        mAmount = source.readString();
        mMemo = source.readString();
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getCurrencyCode() {
        return mCurrencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        mCurrencyCode = currencyCode;
    }

    public String getNetwork() {
        return mNetwork;
    }

    public void setNetwork(String network) {
        mNetwork = network;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public String getAmount() {
        return mAmount;
    }

    public void setAmount(String amount) {
        mAmount = amount;
    }

    public String getMemo() {
        return mMemo;
    }

    public void setMemo(String memo) {
        mMemo = memo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeString(mId);
        destination.writeString(mCurrencyCode);
        destination.writeString(mNetwork);
        destination.writeString(mAddress);
        destination.writeString(mAmount);
        destination.writeString(mMemo);
    }
}
