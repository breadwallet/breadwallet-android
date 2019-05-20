/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/19/19.
 * Copyright (c) 2019 breadwallet LLC
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
package com.breadwallet.wallet.util;

import android.support.annotation.NonNull;
import android.util.Log;

import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;

/**
 * Worker in charge of disconnecting the wallets after a delay after the app goes to background.
 */
public class WalletConnectionCleanUpWorker extends Worker {
    private static final String TAG = WalletConnectionCleanUpWorker.class.getName();

    private static final String TAG_BACKGROUND_WALLETS_DISCONNECT = "background-wallets-disconnect";
    private static final int DISCONNECT_WALLETS_DELAY_SECONDS = 30;

    /**
     * Enqueue job to disconnect all wallets after {@value #DISCONNECT_WALLETS_DELAY_SECONDS} time.
     */
    public static void enqueueWork() {
        Log.d(TAG, "Enqueueing WalletConnectionCleanUpWorker");
        WorkManager.getInstance().enqueue(new OneTimeWorkRequest.Builder(WalletConnectionCleanUpWorker.class)
                .setInitialDelay(DISCONNECT_WALLETS_DELAY_SECONDS, TimeUnit.SECONDS)
                .addTag(TAG_BACKGROUND_WALLETS_DISCONNECT).build());
    }

    /**
     * Cancel enqueued {@link WalletConnectionCleanUpWorker}.
     */
    public static void cancelEnqueuedWork() {
        Log.d(TAG, "Cancelling WalletConnectionCleanUpWorker");
        WorkManager.getInstance().cancelAllWorkByTag(TAG_BACKGROUND_WALLETS_DISCONNECT);
    }

    /**
     * Disconnect all wallets.
     *
     * @return {@link @androidx.work.Worker.Result.SUCCESS}
     */
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork for disconnecting all the wallets.");
        List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance().getAllWallets(getApplicationContext()));
        for (final BaseWalletManager walletManager : list) {
            walletManager.disconnect(getApplicationContext());
        }
        return Result.SUCCESS;
    }
}
