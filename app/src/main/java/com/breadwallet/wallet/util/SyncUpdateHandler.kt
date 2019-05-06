/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 4/29/19.
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
package com.breadwallet.wallet.util

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.breadwallet.repository.WalletRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.wallet.WalletsMaster

/**
 * Class responsible for fetching the synchronization progress of a wallet from core.
 */
object SyncUpdateHandler {
    private val TAG = SyncUpdateHandler::class.java.name

    private val mSyncUpdaterHandler: Handler
    private const val POLLING_MAX_INTERVAL_MILLIS = 2000L
    const val PROGRESS_START: Double = 0.0
    const val PROGRESS_FINISH: Double = 1.0


    init {
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mSyncUpdaterHandler = Handler(handlerThread.looper)
    }

    /**
     * Start fetching synchronization updates for the given wallet.
     */
    fun startWalletSync(context: Context, currencyCode: String) {
        mSyncUpdaterHandler.removeCallbacksAndMessages(null) // remove whatever could be enqueued
        val syncRunnable = object : Runnable {
            override fun run() {
                WalletsMaster.getInstance().getWalletByIso(context, currencyCode)?.let { walletManager ->
                    val progress = walletManager.getSyncProgress(
                            BRSharedPrefs.getStartHeight(context,
                                    BRSharedPrefs.getCurrentWalletCurrencyCode(context)))
                    Log.e(TAG, "startSyncPolling: Progress:$progress Wallet: $currencyCode")
                    WalletRepository.getInstance(context).updateProgress(currencyCode, progress)
                    if (progress > PROGRESS_START && progress < PROGRESS_FINISH) {
                        mSyncUpdaterHandler.postDelayed(this, POLLING_MAX_INTERVAL_MILLIS)
                    }
                }
            }
        }
        mSyncUpdaterHandler.post(syncRunnable)
    }

    /**
     * Cancel any pending sync wallet update.
     */
    fun cancelWalletSync() {
        mSyncUpdaterHandler.removeCallbacksAndMessages(null)
    }

}