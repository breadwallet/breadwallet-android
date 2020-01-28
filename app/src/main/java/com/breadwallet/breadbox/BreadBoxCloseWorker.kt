/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/10/19.
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
package com.breadwallet.breadbox

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.breadwallet.logger.logDebug
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.util.concurrent.TimeUnit

/** Immediately closes BreadBox.  */
class BreadBoxCloseWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams),
    KodeinAware {

    override val kodein by kodein(applicationContext)

    override fun doWork() =
        with(direct.instance<BreadBox>()) {
            if (isOpen) {
                logDebug("Closing BreadBox")
                close()
            } else {
                logDebug("BreadBox already closed")
            }
            Result.success()
        }

    companion object {
        private const val TAG_CLOSE_BREADBOX = "background-close-breadbox"
        private const val CLOSE_DELAY_SECONDS = 30

        /** Enqueue job with a delay of [CLOSE_DELAY_SECONDS]. */
        fun enqueueWork() {
            logDebug("Enqueueing BreadBoxCloseWorker")
            WorkManager.getInstance().enqueue(
                OneTimeWorkRequestBuilder<BreadBoxCloseWorker>()
                    .setInitialDelay(CLOSE_DELAY_SECONDS.toLong(), TimeUnit.SECONDS)
                    .addTag(TAG_CLOSE_BREADBOX)
                    .build()
            )
        }

        /** Cancel enqueued [BreadBoxCloseWorker]. */
        fun cancelEnqueuedWork() {
            logDebug("Cancelling BreadBoxCloseWorker")
            WorkManager.getInstance().cancelAllWorkByTag(TAG_CLOSE_BREADBOX)
        }
    }
}
