package com.breadwallet.presenter.entities;

public class TokenItem {

    public String address;
    public String symbol;
    public String name;
    public String image; // may need to change to int if logos become resources
    public boolean mIsAdded;
    private String mStartColor;
    private String mEndColor;
    private String mContractInitialValue; // This is the initial value of a token during its ICO
    private boolean mIsSupported;


    public TokenItem(String address, String symbol, String name, String image, boolean isSupported){
        this.address = address;
        this.symbol = symbol;
        this.name = name;
        this.image = image;
        this.mIsSupported = isSupported;
    }

    public void setStartColor(String startColor) {
        mStartColor = startColor;
    }

    public String getStartColor() {
        return mStartColor;
    }

    public void setEndColor(String endColor) {
        mEndColor = endColor;
    }

    public String getEndColor() {
        return mEndColor;
    }

    public void setContractInitialValue(String contractInitialvalue) {
        mContractInitialValue = contractInitialvalue;
    }

    public String getContractInitialValue() {
        return mContractInitialValue;
    }

    public boolean isSupported() {
        return mIsSupported;
    }
}
