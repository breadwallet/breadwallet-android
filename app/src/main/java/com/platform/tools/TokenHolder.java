package com.platform.tools;

import android.content.Context;

import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.Utils;
import com.platform.APIClient;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/22/18.
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
public class TokenHolder {


    private static String mApiToken;
    private static String mOldApiToken;

    public synchronized static String retrieveToken(Context app) {
        //If token is not present
        if (Utils.isNullOrEmpty(mApiToken)) {
            //Check KeyStore
            byte[] tokenBytes = BRKeyStore.getToken(app);
            //Not in the KeyStore, update from server.
            if (Utils.isNullOrEmpty(tokenBytes)) {
                fetchNewToken(app);
            } else {
                mApiToken = new String(tokenBytes);
            }
        }
        return mApiToken;
    }

    public synchronized static void updateToken(Context app, String expiredToken) {
        if (mOldApiToken == null || !mOldApiToken.equals(expiredToken)) {
            mOldApiToken = mApiToken;
            fetchNewToken(app);
        }

    }

    private synchronized static void fetchNewToken(Context app) {
        mApiToken = APIClient.getInstance(app).getToken();
        if (!Utils.isNullOrEmpty(mApiToken)) {
            BRKeyStore.putToken(mApiToken.getBytes(), app);
        }
    }

}
