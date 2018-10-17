/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 10/13/18.
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

package com.breadwallet.protocols.messageexchange;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;

/**
 * Recurrent worker used to enqueue inbox polling.
 */
public class InboxPollingWorker extends Worker {
    private static final String TAG = InboxPollingWorker.class.getName();

    private static final String TAG_INBOX_POLLING = "foreground-polling";
    private static final int FOREGROUND_POLL_PERIOD_SECONDS = 2;

    /**
     * Start inbox polling by enqueueing {@link InboxPollingWorker}.
     */
    public static void initialize() {
        Log.d(TAG, "Start InboxPollingWorker");
        // Remove previously enqueued work.
        InboxPollingWorker.cancelEnqueuedWork();
        enqueueWork(0);
    }

    /**
     * Cancel all the enqueued {@link InboxPollingWorker}.
     */
    static void cancelEnqueuedWork() {
        Log.d(TAG, "Cancelling InboxPollingWorker");
        WorkManager.getInstance().cancelAllWorkByTag(TAG_INBOX_POLLING);
    }

    /**
     * Enqueue {@link InboxPollingWorker}.
     *
     * @param delay The delay in seconds.
     */
    private static void enqueueWork(int delay) {
        WorkManager.getInstance().enqueue(new OneTimeWorkRequest.Builder(InboxPollingWorker.class)
                .setInitialDelay(delay, TimeUnit.SECONDS)
                .addTag(TAG_INBOX_POLLING)
                .build());
    }

    /**
     * Request {@link MessageExchangeService} to poll inbox messages and enqueue {@link InboxPollingWorker} again to be
     * executed after {@value #FOREGROUND_POLL_PERIOD_SECONDS}.
     *
     * @return {@link @androidx.work.Worker.Result.SUCCESS}
     */
    @NonNull
    @Override
    public Result doWork() {
        final Intent intent = MessageExchangeService.createIntent(
                getApplicationContext(),
                MessageExchangeService.ACTION_RETRIEVE_MESSAGES);
        MessageExchangeService.enqueueWork(getApplicationContext(), intent);
        // Enqueue next inbox polling.
        enqueueWork(FOREGROUND_POLL_PERIOD_SECONDS);
        return Result.SUCCESS;
    }
}
