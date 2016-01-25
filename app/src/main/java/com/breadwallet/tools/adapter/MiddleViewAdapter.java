package com.breadwallet.tools.adapter;

import android.util.Log;

import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.animation.FragmentAnimator;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/1/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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
public class MiddleViewAdapter {
    private static final String TAG = MiddleViewAdapter.class.getName();

    public static void resetMiddleView(String text) {
        Log.e(TAG, "in the resetMiddleView: " + text);
        MainActivity app = MainActivity.app;
        if (app == null) return;
        if (FragmentAnimator.level == 0 || FragmentAnimator.level == 1) {
            if (BreadWalletApp.unlocked) {
                String tmp = CurrencyManager.getInstance(MainActivity.app).getCurrentBalanceText();
                ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, tmp);
            } else {
                ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_IMAGE, "");
            }
        } else if (FragmentAnimator.level == 2) {
            FragmentSettings myFragment = (FragmentSettings)app.getFragmentManager().findFragmentByTag(FragmentSettings.class.getName());
            if (myFragment != null && myFragment.isVisible()) {
                ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, "settings");
            } else {
                ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, "transaction details");
            }
        } else if (text != null) {
            ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, text);
        } else {
            ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_IMAGE, "");
        }
    }
}