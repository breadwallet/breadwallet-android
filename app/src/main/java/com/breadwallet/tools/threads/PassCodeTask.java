package com.breadwallet.tools.threads;

import android.app.Activity;
import android.app.FragmentManager;
import android.util.Log;

import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.fragments.PasswordDialogFragment;
import com.breadwallet.tools.security.KeyStoreManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 3/24/16.
 * Copyright (c) 2016 Mihail Gutan <mihail@breadwallet.com>
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
public class PassCodeTask extends Thread {
    public static final String TAG = PassCodeTask.class.getName();
    static PasswordDialogFragment passwordDialogFragment;
    String pass = "";
    private Activity activity;

    public PassCodeTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run() {
        super.run();
        if (activity == null) return;
        final FragmentManager fm = activity.getFragmentManager();
        while (pass != null && pass.isEmpty()) {
            Log.e(TAG, "in the while: " + getName());
            if (passwordDialogFragment == null) {
                Log.e(TAG, "starting new password dialog!!!");
                passwordDialogFragment = new PasswordDialogFragment();
                passwordDialogFragment.setFirstTimeTrue();
                passwordDialogFragment.show(fm, PasswordDialogFragment.class.getName());
            }
            if (passwordDialogFragment != null && !passwordDialogFragment.isVisible()) {
                passwordDialogFragment.dismiss();
                passwordDialogFragment = new PasswordDialogFragment();
                passwordDialogFragment.setFirstTimeTrue();
                passwordDialogFragment.show(fm, PasswordDialogFragment.class.getName());
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pass = KeyStoreManager.getPassCode(activity);
            Log.e(TAG, ">>>>>>>>>>>>*&*&*&*&*&*&*>>>>>>>> pass: " + pass);

        }
        if (passwordDialogFragment != null) {
            try {
                passwordDialogFragment.dismiss();
            } catch (IllegalStateException ex){
                ex.printStackTrace();
            }
        }
        if (activity != null)
            ((BreadWalletApp) activity.getApplication()).hideKeyboard(activity);

    }

}
