package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.camera.CameraActivity;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.activities.intro.RecoverActivity;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.util.CryptoUriParser;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/9/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class DeepLinkingManager {
    private static final String TAG = DeepLinkingManager.class.getSimpleName();
    public static final String SCHEME_HTTPS = "https";

    private DeepLinkingManager() {
    }

    public static void handleUrlClick(final Activity activity, Intent intent) {
        //Go to intro screen if the wallet is not create yet
        if (WalletEthManager.getInstance(activity) == null) {
            Intent introIntent = new Intent(activity, IntroActivity.class);
            activity.startActivity(introIntent);
            return;
        }
        final Uri data = intent.getData();
        intent.setData(null);
        Log.e(TAG, "handleUrlClick: " + data);
        if (data != null && !data.toString().isEmpty()) {
            //handle external click with crypto scheme
            if (data.getScheme().equalsIgnoreCase(SCHEME_HTTPS)) {
                InputDataManager.processQrResult(activity, data.toString());
            } else {
                CryptoUriParser.processRequest(activity, data.toString(), WalletsMaster.getInstance(activity).getCurrentWallet(activity));
            }

        }
    }
}
