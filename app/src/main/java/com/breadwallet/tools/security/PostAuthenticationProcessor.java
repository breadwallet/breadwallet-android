package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;

import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.IntroShowPhraseActivity;
import com.breadwallet.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan on 4/14/16.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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
public class PostAuthenticationProcessor {
    public static final String TAG = PostAuthenticationProcessor.class.getName();

    private static PostAuthenticationProcessor instance;

    private PostAuthenticationProcessor() {
    }

    public static PostAuthenticationProcessor getInstance() {
        if (instance == null) {
            instance = new PostAuthenticationProcessor();
        }
        return instance;
    }

    public void onCreateWalletAuth(Activity app) {
        boolean success = BRWalletManager.getInstance(app).generateRandomSeed();
        if (success) {
            ((IntroActivity) app).showWarningFragment();
        } else {
            throw new NullPointerException("failed to generate seed");
        }

    }

    public  void onRecoverWalletAuth(Activity app) {

    }

    public  void onShowPhraseAuth(Activity app) {

    }

    public  void onPublishTxAuth(Activity app) {

    }

    public  void onCanaryCheckAuth(Activity app) {

    }


}
