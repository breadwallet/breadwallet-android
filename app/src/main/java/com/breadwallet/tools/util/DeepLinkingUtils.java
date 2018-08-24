package com.breadwallet.tools.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.tools.manager.InputDataManager;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.util.CryptoUriParser;

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
public class DeepLinkingUtils {
    private static final String TAG = DeepLinkingUtils.class.getSimpleName();

    private DeepLinkingUtils() {
    }

    public static void handleUrlClick(final Context context, Intent intent) {

        final Uri data = intent.getData();

        intent.setData(null);
        if (data != null && !data.toString().isEmpty()) {
            //Go to intro screen if the wallet is not create yet
            if (!WalletsMaster.getInstance(context).isBrdWalletCreated(context)) {
                Intent introIntent = new Intent(context, IntroActivity.class);
                context.startActivity(introIntent);
                return;
            }
            //handle external click with crypto scheme
            if (CryptoUriParser.isCryptoUrl(context, data.toString())) {
                CryptoUriParser.processRequest(context, data.toString(), WalletsMaster.getInstance(context).getCurrentWallet(context));
            } else {
                InputDataManager.processQrResult(context, data.toString());
            }
        }
    }
}
