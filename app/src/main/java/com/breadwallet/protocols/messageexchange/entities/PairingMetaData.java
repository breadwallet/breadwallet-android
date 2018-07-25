package com.breadwallet.protocols.messageexchange.entities;

import android.net.Uri;

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
public class PairingMetaData {
    public static final String QUERY_PARAM_PUBLIC_KEY = "publicKey";
    public static final String QUERY_PARAM_ID = "id";
    public static final String QUERY_PARAM_SERVICE = "service";

    private String mPublicKeyHex;
    private String mId;
    private String mService;

    public PairingMetaData(String publicKeyHex, String id, String service) {
        this.mPublicKeyHex = publicKeyHex;
        this.mId = id;
        this.mService = service;
    }

    public String getPublicKeyHex() {
        return mPublicKeyHex;
    }

    public void setPublicKeyHex(String publicKeyHex) {
        this.mPublicKeyHex = mPublicKeyHex;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getService() {
        return mService;
    }

    public void setService(String service) {
        this.mService = service;
    }

    public static PairingMetaData parseUriString(String uriString) {
        if (Utils.isNullOrEmpty(uriString)) {
            return null;
        }
        Uri uri = Uri.parse(uriString);
        String publicKeyHex = uri.getQueryParameter(QUERY_PARAM_PUBLIC_KEY);
        String idString = uri.getQueryParameter(QUERY_PARAM_ID);
        String service = uri.getQueryParameter(QUERY_PARAM_SERVICE);
        return new PairingMetaData(publicKeyHex, idString, service);
    }
}