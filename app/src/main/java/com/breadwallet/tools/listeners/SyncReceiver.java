package com.breadwallet.tools.listeners;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.WalletsMaster;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/19/17.
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
public class SyncReceiver extends IntentService {
    public final String TAG = SyncReceiver.class.getSimpleName();
    public final static String SYNC_RECEIVER = "SYNC_RECEIVER";
    public static Context app;

    static {
        System.loadLibrary(BRConstants.NATIVE_LIB_NAME);
    }

    public SyncReceiver() {
        super("SyncReceiver");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        if (workIntent != null)
            if (SYNC_RECEIVER.equals(workIntent.getAction())) {
                app = getApplication();
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        WalletsMaster.getInstance(app).initLastWallet(app);
                    }
                });
            }
    }
}
