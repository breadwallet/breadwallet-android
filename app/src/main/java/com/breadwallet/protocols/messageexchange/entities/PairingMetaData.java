package com.breadwallet.protocols.messageexchange.entities;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.breadwallet.tools.util.Utils;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/13/18.
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
public class PairingMetaData implements Parcelable {

    private static final String TAG = PairingMetaData.class.getSimpleName();
    private static final String QUERY_PARAM_ID = "id";
    private static final String QUERY_PARAM_PUBLIC_KEY = "publicKey";
    private static final String QUERY_PARAM_SERVICE = "service";
    private static final String QUERY_PARAM_RETURN_URL = "return-to";

    private String mId;
    private String mPublicKeyHex; // Hex encoded pub key
    private String mService;
    private String mReturnUrl;

    public static final Creator<PairingMetaData> CREATOR = new Creator<PairingMetaData>() {
        @Override
        public PairingMetaData[] newArray(int size) {
            return new PairingMetaData[size];
        }

        @Override
        public PairingMetaData createFromParcel(Parcel source) {
            return new PairingMetaData(source);
        }
    };

    public PairingMetaData(String id, String publicKeyHex, String service, String returnUrl) {
        mId = id;
        mPublicKeyHex = publicKeyHex;
        mService = service;
        mReturnUrl = returnUrl;
    }

    public PairingMetaData(Parcel source) {
        mId = source.readString();
        mPublicKeyHex = source.readString();
        mService = source.readString();
        mReturnUrl = source.readString();
    }

    public PairingMetaData(String uriString) {
        Log.e(TAG, "PairingMetaData: " + uriString);
        if (!Utils.isNullOrEmpty(uriString)) {
            Uri uri = Uri.parse(uriString);
            mId = uri.getQueryParameter(QUERY_PARAM_ID);
            mPublicKeyHex = uri.getQueryParameter(QUERY_PARAM_PUBLIC_KEY);
            mService = uri.getQueryParameter(QUERY_PARAM_SERVICE);
            mReturnUrl = uri.getQueryParameter(QUERY_PARAM_RETURN_URL);
        }
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getPublicKeyHex() {
        return mPublicKeyHex;
    }

    public void setPublicKeyHex(String publicKeyHex) {
        mPublicKeyHex = publicKeyHex;
    }

    public String getService() {
        return mService;
    }

    public void setService(String service) {
        mService = service;
    }

    public String getReturnUrl() {
        return mReturnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.mReturnUrl = returnUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeString(mId);
        destination.writeString(mPublicKeyHex);
        destination.writeString(mService);
        destination.writeString(mReturnUrl);
    }
}
