/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/10/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.platform.tools

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.util.Log

import com.breadwallet.tools.security.BRKeyStore
import com.platform.APIClient

object TokenHolder {
    private val TAG = TokenHolder::class.java.simpleName
    private var mApiToken: String? = null
    private var mOldApiToken: String? = null

    @Synchronized
    fun retrieveToken(app: Context): String? {
        //If token is not present
        if (mApiToken.isNullOrBlank()) {
            //Check KeyStore
            val tokenBytes = BRKeyStore.getToken(app) ?: byteArrayOf()
            //Not in the KeyStore, update from server.
            if (tokenBytes.isNotEmpty()) {
                fetchNewToken(app)
            } else {
                mApiToken = String(tokenBytes)
            }
        }
        return mApiToken
    }

    @Synchronized
    fun updateToken(app: Context, expiredToken: String?): String? {
        if (mOldApiToken == null || mOldApiToken != expiredToken) {
            Log.e(TAG, "updateToken: updating the token")
            mOldApiToken = mApiToken
            fetchNewToken(app)
        }
        return mApiToken
    }

    @Synchronized
    fun fetchNewToken(app: Context) {
        mApiToken = APIClient.getInstance(app).token
        if (!mApiToken.isNullOrEmpty()) {
            BRKeyStore.putToken(mApiToken!!.toByteArray(), app)
        }
    }

    @VisibleForTesting
    @Synchronized
    fun reset() {
        mApiToken = null
        mOldApiToken = null
    }
}
