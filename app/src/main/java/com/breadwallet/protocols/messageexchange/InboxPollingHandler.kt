/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 4/8/19.
 * Copyright (c) 2019 breadwallet LLC
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
package com.breadwallet.protocols.messageexchange

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * Class responsible of periodically requesting [MessageExchangeService] to fetch new messages.
 */
class InboxPollingHandler private constructor() {
    private val mInboxPollingHandler: Handler

    /**
     * Start periodically inbox polling every {@value FOREGROUND_POLL_PERIOD_MILLIS} milliseconds.
     *
     * @param context Application context.
     */
    fun startPolling(context: Context?) {
        val pollingRunnable: Runnable = object : Runnable {
            override fun run() {
                Log.d(TAG, "Enqueuing next inbox polling")
                val intent = MessageExchangeService.createIntent(
                    context,
                    MessageExchangeService.ACTION_RETRIEVE_MESSAGES
                )
                MessageExchangeService.enqueueWork(context, intent)
                // Enqueue next inbox polling.
                mInboxPollingHandler.postDelayed(this, FOREGROUND_POLL_PERIOD_MILLIS.toLong())
            }
        }
        mInboxPollingHandler.removeCallbacksAndMessages(null) // remove whatever could be enqueued
        mInboxPollingHandler.post(pollingRunnable)
    }

    /**
     * Enqueue task to stop polling messages after {@value BACKGROUND_POLL_PERIOD_MILLIS} milliseconds.
     */
    fun enqueueCleanUp() {
        val cleanUpRunnable = Runnable {
            Log.d(TAG, "Stopping inbox polling")
            mInboxPollingHandler.removeCallbacksAndMessages(null)
        }
        mInboxPollingHandler.postDelayed(cleanUpRunnable, BACKGROUND_POLL_PERIOD_MILLIS.toLong())
    }

    companion object {
        private val TAG = InboxPollingHandler::class.java.name
        private const val BACKGROUND_POLL_PERIOD_MILLIS = 30 * 1000
        private const val FOREGROUND_POLL_PERIOD_MILLIS = 2 * 1000
        private var mInstance: InboxPollingHandler? = null

        @get:Synchronized
        val instance: InboxPollingHandler
            get() {
                if (mInstance == null) {
                    mInstance = InboxPollingHandler()
                }
                return checkNotNull(mInstance)
            }
    }

    init {
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mInboxPollingHandler = Handler(handlerThread.looper)
    }
}