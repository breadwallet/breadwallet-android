package com.breadwallet.tools.util;

import android.content.Context;
import android.util.Log;

import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/17/18.
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
public class SyncTestLogger extends Thread {
    private static final String TAG = SyncTestLogger.class.getSimpleName();
    private Context mContext;

    public SyncTestLogger(Context app) {
        mContext = app;
    }

    @Override
    public void run() {
        super.run();

        while (true) {
            boolean needsLog = false;
            StringBuilder builder = new StringBuilder();
            List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(mContext).getAllWallets(mContext));
            for (BaseWalletManager w : list) {
                builder.append("   " + w.getIso());
                String connectionStatus = "";
                if (w.getConnectStatus() == 2)
                    connectionStatus = "Connected";
                else if (w.getConnectStatus() == 0)
                    connectionStatus = "Disconnected";
                else if (w.getConnectStatus() == 1)
                    connectionStatus = "Connecting";

                double progress = w.getSyncProgress(BRSharedPrefs.getStartHeight(mContext, w.getIso()));
                if (progress != 1) needsLog = true;
                builder.append(" - " + connectionStatus + " " + progress * 100 + "%     ");

            }
            if (needsLog)
                Log.e(TAG, "testLog: " + builder.toString());

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
