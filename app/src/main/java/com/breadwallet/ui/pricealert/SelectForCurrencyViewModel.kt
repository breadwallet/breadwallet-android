/**
 * BreadWallet
 * <p/>
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 6/6/2019.
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
import android.content.Context
import com.breadwallet.BreadApp
import com.breadwallet.presenter.entities.CurrencyEntity
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.ui.util.mutableLiveData
import com.breadwallet.wallet.WalletsMaster
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager

class SelectForCurrencyViewModel : ViewModel() {

    private val context: Context
        get() = BreadApp.getBreadContext().applicationContext

    private val selectedCurrency = mutableLiveData<CurrencyEntity>()
    private val availableCurrencies = mutableLiveData<List<CurrencyEntity>>()

    init {
        loadCurrencies()
    }

    fun selectCurrency(currency: CurrencyEntity) {
        selectedCurrency.postValue(currency)
    }

    fun getSelectedCurrency(): LiveData<CurrencyEntity> = selectedCurrency

    fun getCurrencies(): LiveData<List<CurrencyEntity>> = availableCurrencies

    private fun loadCurrencies() {
        val ratesRepository = RatesRepository.getInstance(context)
        val walletsMaster = WalletsMaster.getInstance()
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            ratesRepository
                    .getAllRatesForCurrency(WalletBitcoinManager.BITCOIN_CURRENCY_CODE)
                    .filter { !walletsMaster.isIsoCrypto(context, it.code) }
                    .apply(availableCurrencies::postValue)
        }
    }
}