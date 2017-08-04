package com.breadwallet.presenter.activities.util;

import android.app.Activity;
import android.content.Intent;
import android.view.Window;
import android.view.WindowManager;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.DisabledActivity;
import com.breadwallet.presenter.activities.InputWordsActivity;
import com.breadwallet.presenter.activities.SetPinActivity;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.activities.intro.RecoverActivity;
import com.breadwallet.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.tools.manager.ConnectionManager;
import com.breadwallet.tools.manager.CurrencyFetchManager;
import com.breadwallet.tools.security.AuthManager;
import com.platform.HTTPServer;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 4/27/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class ActivityUTILS {

    private static void setStatusBarColor(Activity app, int color) {
        if (app == null) return;
        Window window = app.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(app.getColor(color));

    }

    public static void init(Activity app) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HTTPServer.startServer();
            }
        }).start();
        //set status bar color
        ActivityUTILS.setStatusBarColor(app, android.R.color.transparent);
        ConnectionManager.getInstance();
        if (!(app instanceof IntroActivity || app instanceof RecoverActivity || app instanceof WriteDownActivity))
            CurrencyFetchManager.getInstance().startTimer(app);
        //show wallet locked if it is
        if (!isAppSafe(app))
            if (AuthManager.getInstance().isWalletDisabled(app))
                AuthManager.getInstance().setWalletDisabled(app);

    }

    //return true if the app does need to show the disabled wallet screen
    private static boolean isAppSafe(Activity app) {
        return app instanceof SetPinActivity || app instanceof InputWordsActivity;
    }

    public static void showWalletDisabled(Activity app, double waitTimeMinutes) {
        Intent intent = new Intent(app, DisabledActivity.class);
        intent.putExtra("waitTimeMinutes", waitTimeMinutes);
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);

    }
}
