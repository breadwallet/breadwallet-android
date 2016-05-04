package com.breadwallet.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.wallet.BRPeerManager;

/**
 * BreadWallet
 *
 * Created by Mihail Gutan on 7/14/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = NetworkChangeReceiver.class.getName();
    private RelativeLayout networkErrorBar;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final MainActivity app = MainActivity.app;
        if(app == null) return;
        networkErrorBar = (RelativeLayout) app.findViewById(R.id.main_internet_status_bar);
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        boolean isConnected = connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
        BRPeerManager.getInstance(app).connect();
        if (!isConnected) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkErrorBar.setVisibility(View.VISIBLE);
                    BRPeerManager.stopSyncingProgressThread();
//                    ((BreadWalletApp) app.getApplication()).showCustomToast(app, app.getString(R.string.no_internet_connection),
//                            500, Toast.LENGTH_SHORT,0);
                }
            });

            Log.e(TAG, "Network Not Available ");

        } else {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkErrorBar.setVisibility(View.GONE);
                    BRPeerManager.startSyncingProgressThread();
//                    ((BreadWalletApp) app.getApplication()).showCustomToast(app, app.getString(R.string.no_internet_connection),
//                            500, Toast.LENGTH_SHORT,0);
                }
            });
            Log.e(TAG, "Network Available ");
        }
    }


}