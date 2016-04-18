package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 8/20/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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
public class PassCodeManager {
    private static PassCodeManager instance;
    public static final String TAG = PassCodeManager.class.getName();

    private PassCodeManager() {
    }

    public static PassCodeManager getInstance() {
        if (instance == null)
            instance = new PassCodeManager();
        return instance;
    }

    public boolean checkAuth(CharSequence passcode, Activity context) {

        return passcode.equals(String.valueOf(KeyStoreManager.getPassCode(context)));
    }

    public void setPassCode(String pass, Activity context) {
        KeyStoreManager.putPassCode(Integer.valueOf(pass), context);
    }
}
