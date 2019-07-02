/**
 * BreadWallet
 * <p/>
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 6/2/2019.
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
package com.breadwallet.ui.pricealert

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.breadwallet.model.PriceAlert
import com.breadwallet.repository.PriceAlertRepository
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.ui.util.mutableLiveData

class PriceAlertListViewModel : ViewModel() {

    private val TAG: String = PriceAlertListViewModel::class.java.simpleName

    private val priceAlerts = mutableLiveData<List<PriceAlert>>()

    fun getPriceAlerts(): LiveData<List<PriceAlert>> {
        fetchAlerts()
        return priceAlerts
    }

    fun removePriceAlert(priceAlert: PriceAlert) {
        try {
            PriceAlertRepository.removeAlert(priceAlert)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to remove price alert.", e)
        }
        fetchAlerts()
    }

    private fun fetchAlerts() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            priceAlerts.postValue(PriceAlertRepository.getAlerts())
        }
    }
}