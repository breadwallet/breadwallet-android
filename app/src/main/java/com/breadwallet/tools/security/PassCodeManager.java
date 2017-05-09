package com.breadwallet.tools.security;

import android.app.Activity;
import android.util.Log;

import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/20/15.
 * Copyright (c) 2016 breadwallet LLC
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
    public static final String TAG = PassCodeManager.class.getName();
    private static PassCodeManager instance;

    private PassCodeManager() {
    }

    public static PassCodeManager getInstance() {
        if (instance == null)
            instance = new PassCodeManager();
        return instance;
    }

    public boolean checkAuth(CharSequence passcode, Activity context) {
        String pass = KeyStoreManager.getPassCode(context);
        return pass != null && passcode.equals(pass);
    }

    public void setPassCode(String pass, Activity context) {
        KeyStoreManager.putPassCode(pass, context);
        KeyStoreManager.putLastPasscodeUsedTime(System.currentTimeMillis(), context);
        setSpendingLimitIfNotSet(context);
    }

    public int getLimit(Activity activity) {
        return SharedPreferencesManager.getLimit(activity);
    }

    public void setLimit(Activity activity, int limit) {
        SharedPreferencesManager.putLimit(activity, limit);
    }


    private void setSpendingLimitIfNotSet(final Activity activity) {
        if (activity == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                long limit = KeyStoreManager.getSpendLimit(activity);
                if (limit == 0) {
                    long totalSpent = BRWalletManager.getInstance(activity).getTotalSent();
                    long spendLimit = totalSpent + PassCodeManager.getInstance().getLimit(activity);
                    KeyStoreManager.putSpendLimit(spendLimit, activity);
                }
            }
        }).start();

    }
}
