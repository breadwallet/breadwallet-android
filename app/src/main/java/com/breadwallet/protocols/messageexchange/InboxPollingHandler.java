/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 4/8/19.
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
package com.breadwallet.protocols.messageexchange;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

/**
 * Class responsible of periodically requesting {@link MessageExchangeService} to fetch new messages.
 */
public class InboxPollingHandler {
    private final static String TAG = InboxPollingHandler.class.getName();

    private static final int BACKGROUND_POLL_PERIOD_MILLIS = 30 * 1000;
    private static final int FOREGROUND_POLL_PERIOD_MILLIS = 2 * 1000;

    private static InboxPollingHandler mInstance;

    private Handler mInboxPollingHandler;

    private InboxPollingHandler() {
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mInboxPollingHandler = new Handler(handlerThread.getLooper());
    }

    synchronized public static InboxPollingHandler getInstance() {
        if (mInstance == null) {
            mInstance = new InboxPollingHandler();
        }
        return mInstance;
    }

    /**
     * Start periodically inbox polling every {@value FOREGROUND_POLL_PERIOD_MILLIS} milliseconds.
     *
     * @param context Application context.
     */
    public void startPolling(Context context) {
        Runnable pollingRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Enqueuing next inbox polling");
                final Intent intent = MessageExchangeService.createIntent(context,
                        MessageExchangeService.ACTION_RETRIEVE_MESSAGES);
                MessageExchangeService.enqueueWork(context, intent);
                // Enqueue next inbox polling.
                mInboxPollingHandler.postDelayed(this, FOREGROUND_POLL_PERIOD_MILLIS);
            }
        };
        mInboxPollingHandler.removeCallbacksAndMessages(null); // remove whatever could be enqueued
        mInboxPollingHandler.post(pollingRunnable);
    }

    /**
     * Enqueue task to stop polling messages after {@value BACKGROUND_POLL_PERIOD_MILLIS} milliseconds.
     */
    public void enqueueCleanUp() {
        Runnable cleanUpRunnable = () -> {
            Log.d(TAG, "Stopping inbox polling");
            mInboxPollingHandler.removeCallbacksAndMessages(null);
        };
        mInboxPollingHandler.postDelayed(cleanUpRunnable, BACKGROUND_POLL_PERIOD_MILLIS);
    }
}
