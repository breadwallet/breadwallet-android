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
import android.content.Context
import android.util.Log
import com.breadwallet.BreadApp
import com.breadwallet.R
import com.breadwallet.model.PriceAlert.Companion.percentageChanged
import com.breadwallet.model.PriceAlert.Companion.percentageChangedInDay
import com.breadwallet.model.PriceAlert.Companion.percentageChangedInDayAndWeek
import com.breadwallet.model.PriceAlert.Companion.percentageDecreased
import com.breadwallet.model.PriceAlert.Companion.percentageDecreasedInDay
import com.breadwallet.model.PriceAlert.Companion.percentageDecreasedInDayAndWeek
import com.breadwallet.model.PriceAlert.Companion.percentageIncreased
import com.breadwallet.model.PriceAlert.Companion.percentageIncreasedInDay
import com.breadwallet.model.PriceAlert.Companion.percentageIncreasedInDayAndWeek
import com.breadwallet.model.PriceAlert.Companion.priceTargetDecrease
import com.breadwallet.model.PriceAlert.Companion.priceTargetIncrease
import com.breadwallet.repository.PriceAlertRepository
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.platform.pricealert.PriceAlertWorker
import com.breadwallet.ui.util.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

class NewPriceAlertViewModel(
        defaultCurrency: String? = null
) : ViewModel() {

    companion object {
        private val TAG = NewPriceAlertViewModel::class.java.simpleName

        private const val ALERT_PRICE_CHANGE = 0
        private const val ALERT_PERCENT_CHANGE = 1
        private const val ALERT_PERCENT_INCREASE = 2
        private const val ALERT_PERCENT_DECREASE = 3

        private const val WINDOW_ANYTIME = 0
        private const val WINDOW_DAY = 1
        private const val WINDOW_DAY_WEEK = 2
    }

    // TODO: Inject classes that require a Context
    private val context: Context
        get() = BreadApp.getBreadContext().applicationContext

    private val alertSaved = mutableLiveData(false)
    private val forCurrency = mutableLiveData(defaultCurrency)
    private val toCurrency = mutableLiveData(BRSharedPrefs.getPreferredFiatIso())
    private val alertWhen = mutableLiveData(ALERT_PERCENT_CHANGE)
    private val window = mutableLiveData(WINDOW_ANYTIME)
    private val alertValue = mutableLiveData<Float>()
    private val exchangeRate = forCurrency.combineLatest(toCurrency, ::Pair)
            .switchMap { (forCurrency, toCurrency) ->
                mutableLiveData<Float>().also {
                    it.postValue(RatesRepository.getInstance(context)
                            .getFiatForCrypto(BigDecimal.ONE, forCurrency, toCurrency)
                            .setScale(2, RoundingMode.HALF_EVEN)
                            .toFloat())
                }
            }

    fun getSelectedCurrency(): LiveData<String> = forCurrency

    fun setSelectedCurrency(selection: String) =
            forCurrency.postValue(selection)

    fun getToCurrency(): LiveData<String> = toCurrency

    fun setToCurrency(selection: String) =
            toCurrency.postValue(selection)

    fun getExchangeRate(): LiveData<Float> = exchangeRate

    fun getSelectedAlertName(): LiveData<String> =
            alertWhen.map {
                context.getString(when (it) {
                    ALERT_PRICE_CHANGE -> R.string.NewPriceAlert_priceChangedByValue
                    ALERT_PERCENT_CHANGE -> R.string.NewPriceAlert_priceChangedByPercent
                    ALERT_PERCENT_INCREASE -> R.string.NewPriceAlert_priceIncreasedByPercent
                    ALERT_PERCENT_DECREASE -> R.string.NewPriceAlert_priceDecreasedByPercent
                    else -> error("Unsupported alert type: $it")
                })
            }

    fun goToNextAlert() {
        val current = alertWhen.value!!
        alertWhen.postValue(
                if (current == ALERT_PERCENT_DECREASE) {
                    ALERT_PRICE_CHANGE
                } else current + 1
        )
    }

    fun getWindowSelectVisible(): LiveData<Boolean> =
            alertWhen.map { it in ALERT_PERCENT_CHANGE..ALERT_PERCENT_DECREASE }
                    .distinctUntilChanged()

    fun getSelectedWindowName(): LiveData<String> =
            window.map {
                context.getString(when (it) {
                    WINDOW_ANYTIME -> R.string.NewPriceAlert_windowAnytime
                    WINDOW_DAY -> R.string.NewPriceAlert_windowInADay
                    WINDOW_DAY_WEEK -> R.string.NewPriceAlert_windowInADayAndWeek
                    else -> error("Unsupported alert type: $it")
                })
            }

    fun goToNextWindow() {
        val current = window.value!!
        window.postValue(
                if (current == WINDOW_DAY_WEEK) {
                    WINDOW_ANYTIME
                } else current + 1
        )
    }

    fun setAlertValue(value: Float) {
        alertValue.postValue(value)
    }

    fun getAlertSaved(): LiveData<Boolean> = alertSaved

    fun saveAlert() {
        val selectedCrypto = forCurrency.value ?: return
        val value = alertValue.value ?: return
        val toCurrency = toCurrency.value ?: return
        val exchangeRate = exchangeRate.value ?: return
        val alertType = alertWhen.value!!
        val window = window.value!!
        val alert = when (alertType) {
            ALERT_PRICE_CHANGE -> when {
                exchangeRate < value -> priceTargetIncrease(selectedCrypto, value, toCurrency)
                else -> priceTargetDecrease(selectedCrypto, value, toCurrency)
            }
            ALERT_PERCENT_CHANGE -> when (window) {
                WINDOW_ANYTIME -> percentageChanged(selectedCrypto, value, toCurrency, exchangeRate)
                WINDOW_DAY -> percentageChangedInDay(selectedCrypto, value, toCurrency, Date().time, exchangeRate)
                WINDOW_DAY_WEEK -> percentageChangedInDayAndWeek(selectedCrypto, value, toCurrency, Date().time, exchangeRate)
                else -> error("Unknown window value: $window")
            }
            ALERT_PERCENT_INCREASE -> when (window) {
                WINDOW_ANYTIME -> percentageIncreased(selectedCrypto, value, toCurrency, exchangeRate)
                WINDOW_DAY -> percentageIncreasedInDay(selectedCrypto, value, toCurrency, Date().time, exchangeRate)
                WINDOW_DAY_WEEK -> percentageIncreasedInDayAndWeek(selectedCrypto, value, toCurrency, Date().time, exchangeRate)
                else -> error("Unknown window value: $window")
            }
            ALERT_PERCENT_DECREASE -> when (window) {
                WINDOW_ANYTIME -> percentageDecreased(selectedCrypto, value, toCurrency, exchangeRate)
                WINDOW_DAY -> percentageDecreasedInDay(selectedCrypto, value, toCurrency, Date().time, exchangeRate)
                WINDOW_DAY_WEEK -> percentageDecreasedInDayAndWeek(selectedCrypto, value, toCurrency, Date().time, exchangeRate)
                else -> error("Unknown window value: $window")
            }
            else -> error("Unknown alert value: $alertType")
        }

        try {
            PriceAlertRepository.putAlert(alert)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Attempted to add duplicate alert, ignoring.", e)
        }
        PriceAlertWorker.scheduleWork(BRSharedPrefs.getPriceAlertsInterval())
        alertSaved.postValue(true)
    }
}