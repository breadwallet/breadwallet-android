/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 3/13/19.
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
package com.breadwallet.ui.recovery

import android.app.Activity
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.mvvm.Resource
import com.breadwallet.tools.security.PostAuth
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager

/**
 * View model encapsulating input words validation and  wallet recovery.
 */
class RecoveryKeyViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Recover wallet with the paper key and start kv store and wallets synchronization.
     */
    // TODO we shouldn't be passing the activity
    @JvmOverloads
    fun recoverWallet(activity: Activity, cleanPhrase: String? = null): LiveData<Resource<Void>> {
        val liveData = MutableLiveData<Resource<Void>>().apply {
            value = Resource.loading(null)
        }
        BRExecutor.getInstance().forBackgroundTasks().execute {
            cleanPhrase?.let { PostAuth.getInstance().setCachedPaperKey(it) }
            //Disallow BTC and BCH sending.
            BRSharedPrefs.putAllowSpend(getApplication(),
                    BaseBitcoinWalletManager.BITCASH_CURRENCY_CODE, false)
            BRSharedPrefs.putAllowSpend(getApplication(),
                    BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE, false)
            if (PostAuth.getInstance().onRecoverWalletAuth(activity, false)) {
                liveData.postValue(Resource.success(null))
            } else {
                liveData.postValue(Resource.error("Failed to recover the wallet", null))
            }
        }
        return liveData
    }

}