package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.wallet.BRWalletManager;

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
    private static final int ONE_BITCOIN = 100000000;
    public static final String TAG = PassCodeManager.class.getName();
    public static final String LIMIT_PREFS = "fingerprintLimit";

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
        setSpendingLimitIfNotSet(context);
    }

    public int getLimit(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(LIMIT_PREFS, ONE_BITCOIN);
    }

    public void setLimit(Activity activity, int limit) {
        SharedPreferences prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(LIMIT_PREFS, limit);
        editor.apply();
    }


    private void setSpendingLimitIfNotSet(Activity activity) {
        if (activity == null) return;
        long limit = KeyStoreManager.getSpendLimit(activity);
        if (limit == 0) {
            long totalSpent = BRWalletManager.getInstance(activity).getTotalSent();
            long spendLimit = totalSpent + PassCodeManager.getInstance().getLimit(activity);
            KeyStoreManager.putSpendLimit(spendLimit, activity);
        }
    }
}
