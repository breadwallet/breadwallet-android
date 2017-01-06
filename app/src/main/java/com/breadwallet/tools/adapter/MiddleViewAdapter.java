package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.util.Log;
import android.view.WindowManager;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRStringFormatter;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/1/15.
 * Copyright (c) 2016 breadwallet LLC
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

public class MiddleViewAdapter {
    private static final String TAG = MiddleViewAdapter.class.getName();
    private static boolean syncing = false;

    public static void resetMiddleView(Activity app, String text) {
        if (syncing && (BRAnimator.level == 0 || BRAnimator.level == 1)) {
            try {
                ((BreadWalletApp) app.getApplication()).setTopMiddleView(BRConstants.BREAD_WALLET_TEXT, app.getString(R.string.syncing));
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
            return;
        }

        if (text != null) {
            try {
                ((BreadWalletApp) app.getApplication()).setTopMiddleView(BRConstants.BREAD_WALLET_TEXT, text);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        if (BRAnimator.level == 0 || BRAnimator.level == 1) {
            if (BreadWalletApp.unlocked) {
                try {
                    String tmp = BRStringFormatter.getCurrentBalanceText(app);
                    ((BreadWalletApp) app.getApplication()).setTopMiddleView(BRConstants.BREAD_WALLET_TEXT, tmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    ((BreadWalletApp) app.getApplication()).setTopMiddleView(BRConstants.BREAD_WALLET_IMAGE, "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if (BRAnimator.level == 2) {
            try {
                FragmentSettings myFragment = (FragmentSettings) app.getFragmentManager().findFragmentByTag(FragmentSettings.class.getName());
                if (myFragment != null && myFragment.isVisible()) {
                    ((BreadWalletApp) app.getApplication()).setTopMiddleView(BRConstants.BREAD_WALLET_TEXT, app.getString(R.string.middle_view_settings));
                } else {
                    ((BreadWalletApp) app.getApplication()).setTopMiddleView(BRConstants.BREAD_WALLET_TEXT, app.getString(R.string.middle_view_transaction_details));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                ((BreadWalletApp) app.getApplication()).setTopMiddleView(BRConstants.BREAD_WALLET_IMAGE, "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void setSyncing(Activity app, final boolean b) {
        if (app == null) app = MainActivity.app;
        final Activity finalApp = app;
        if (finalApp == null) return;
        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                syncing = b;
                resetMiddleView(finalApp, null);
            }
        });

    }

    public static boolean getSyncing() {
        return syncing;
    }
}