package com.breadwallet.tools.adapter;

import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.others.CurrencyManager;

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
    public static final String TAG = "MiddleViewAdapter";

    public static void resetMiddleView(String text) {
        MainActivity app = MainActivity.app;
        if (FragmentAnimator.level == 0 || FragmentAnimator.level == 1) {
            if (MainActivity.unlocked) {
                String tmp = CurrencyManager.getCurrentBalanceText();
                ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, tmp);
            } else {
                ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_IMAGE, "");
            }
        } else if (FragmentAnimator.level == 2) {
            String tmp = "settings";
            ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, tmp);
        } else if (text != null) {
            ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT,
                    text);
        } else {
            ((BreadWalletApp) app.getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_IMAGE, "");
        }
    }
}