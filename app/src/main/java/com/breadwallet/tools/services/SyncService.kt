/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 3/27/19.
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
package com.breadwallet.tools.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import com.breadwallet.repository.WalletRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.wallet.WalletsMaster

/**
 * Service responsible of polling the progress from the current wallet being sync from the
 * WalletManager and post it to WalletRepository.
 */
class SyncService : JobService() {

    companion object {
        private val TAG = SyncService::class.java.toString()
        private const val JOB_ID = -0x1e8bca8e // Used to identify jobs that belong to this service. (Random number used for uniqueness.)
        private const val POLLING_MIN_INTERVAL_MILLIS = 500L
        private const val POLLING_MAX_INTERVAL_MILLIS = 1000L
        private const val EXTRA_WALLET_CURRENCY_CODE = "com.breadwallet.tools.services.EXTRA_WALLET_CURRENCY_CODE"
        const val PROGRESS_START : Double = 0.0
        const val PROGRESS_FINISH : Double = 1.0

        /**
         * Schedule the sync polling service with the specified parameters.
         *
         * @param context      The context in which we are operating.
         * @param currencyCode The currency code of the wallet that is syncing.
         */
        fun scheduleSyncService(context: Context, currencyCode: String) {
            val componentName = ComponentName(context, SyncService::class.java)
            val bundle = PersistableBundle()
            bundle.putString(EXTRA_WALLET_CURRENCY_CODE, currencyCode)
            val jobInfoBuilder = JobInfo.Builder(JOB_ID, componentName)
                    .setMinimumLatency(POLLING_MIN_INTERVAL_MILLIS)
                    .setOverrideDeadline(POLLING_MAX_INTERVAL_MILLIS)
                    .setExtras(bundle)
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            scheduler.schedule(jobInfoBuilder.build())
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        val currencyCode = params.extras.getString(EXTRA_WALLET_CURRENCY_CODE)
        WalletsMaster.getInstance().getWalletByIso(applicationContext, currencyCode)?.let { walletManager ->
            val progress = walletManager.getSyncProgress(
                    BRSharedPrefs.getStartHeight(applicationContext,
                    BRSharedPrefs.getCurrentWalletCurrencyCode(applicationContext)))
            Log.e(TAG, "startSyncPolling: Progress:$progress Wallet: $currencyCode")
            WalletRepository.getInstance(applicationContext).updateProgress(currencyCode, progress)
            if (progress > PROGRESS_START && progress < PROGRESS_FINISH) {
                scheduleSyncService(applicationContext, currencyCode)
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters?) = false

}

