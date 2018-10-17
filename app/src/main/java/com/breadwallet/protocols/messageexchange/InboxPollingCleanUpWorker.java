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

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;


/**
 * Worker in charge of performing a clean up enqueued work after the app goes to background.
 */
public class InboxPollingCleanUpWorker extends Worker {
    private static final String TAG = InboxPollingCleanUpWorker.class.getName();

    private static final String TAG_BACKGROUND_POLLING = "background-polling";
    private static final int BACKGROUND_POLL_PERIOD_SECONDS = 30;

    /**
     * Enqueue job to cancel all {@link InboxPollingWorker} after {@value #BACKGROUND_POLL_PERIOD_SECONDS} time.
     */
    static void enqueueWork() {
        Log.d(TAG, "Enqueueing InboxPollingCleanUpWorker");
        WorkManager.getInstance().enqueue(new OneTimeWorkRequest.Builder(InboxPollingCleanUpWorker.class)
                .setInitialDelay(BACKGROUND_POLL_PERIOD_SECONDS, TimeUnit.SECONDS)
                .addTag(TAG_BACKGROUND_POLLING).build());
    }

    /**
     * Cancel enqueued {@link InboxPollingCleanUpWorker}.
     */
    static void cancelEnqueuedWork() {
        Log.d(TAG, "Cancelling InboxPollingCleanUpWorker");
        WorkManager.getInstance().cancelAllWorkByTag(TAG_BACKGROUND_POLLING);
    }

    /**
     * Cancel enqueued work with {@value #TAG_BACKGROUND_POLLING} tag.
     *
     * @return {@link @androidx.work.Worker.Result.SUCCESS}
     */
    @NonNull
    @Override
    public Result doWork() {
        InboxPollingWorker.cancelEnqueuedWork();
        return Result.SUCCESS;
    }
}
