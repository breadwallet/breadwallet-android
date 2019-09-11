/**
 * BreadWallet
 * <p/>
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 5/30/2019.
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
package com.breadwallet.platform.pricealert

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.annotation.VisibleForTesting
import android.support.v4.app.NotificationCompat
import android.util.Log
import androidx.work.*
import com.breadwallet.R
import com.breadwallet.model.PriceAlert
import com.breadwallet.repository.PriceAlertRepository
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRApiManager
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.tools.util.filterLeft
import com.breadwallet.tools.util.mapLeft
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

/**
 * When the user has created [PriceAlert]s this worker
 * will provide them the latest information, update
 * their state, and dispatch notifications to the user.
 */
class PriceAlertWorker(
        context: Context,
        workParams: WorkerParameters
) : Worker(context, workParams) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "price_alert_notification"
        private const val INPUT_UPDATE_RATES = "update_rates"

        private val TAG: String = PriceAlertWorker::class.java.simpleName

        @VisibleForTesting
        val uniqueWorkName: String = PriceAlertWorker::class.java.simpleName

        /**
         * Schedule [PriceAlertWorker] to run periodically.
         *
         * @param interval The minutes between each run, must be >= 15.
         * @param updateRates Whether or not to update rates before checking alerts, default true.
         */
        fun scheduleWork(interval: Int = 15, updateRates: Boolean = true) {
            WorkManager.getInstance().enqueueUniquePeriodicWork(
                    uniqueWorkName,
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<PriceAlertWorker>(
                            interval.toLong(), TimeUnit.MINUTES,
                            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS, TimeUnit.MILLISECONDS)
                            .setInputData(Data.Builder()
                                    .putBoolean(INPUT_UPDATE_RATES, updateRates)
                                    .build())
                            .setConstraints(Constraints.Builder()
                                    // TODO: Maybe UNMETERED, maybe user preference, maybe not at all?
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build())
                            .build())
        }

        fun cancelWork() {
            WorkManager.getInstance().cancelUniqueWork(uniqueWorkName)
        }

        @TargetApi(Build.VERSION_CODES.O)
        fun setupNotificationChannel(context: Context) {
            val title = context.getString(R.string.PriceAlertNotification_channelTitle)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, title,
                    NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = context.getString(R.string.PriceAlertNotification_channelDescription)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val notificationManager: NotificationManager by lazy {
        applicationContext.getSystemService(NotificationManager::class.java)
    }

    override fun doWork(): Result {
        val alerts = PriceAlertRepository.getAlerts()
        if (alerts.isEmpty()) {
            Log.d(TAG, "Orphaned PriceAlertWorker, cancelling periodic work.")
            cancelWork()
            return Result.success()
        }

        if (inputData.getBoolean(INPUT_UPDATE_RATES, true)) {
            // Manually update RatesRepository data
            BRApiManager.getInstance().updateRatesSync(applicationContext)
        }

        // List all alerts and their latest price
        val alertsWithCurrentPrice = alerts.mapToCurrentPrice()

        // List all triggered alerts and their latest price
        val time = Date().time
        val triggered = alertsWithCurrentPrice
                .filterLeft(PriceAlert::hasNotBeenTriggered)
                .onEach { Log.d(TAG, it.toString()) }
                .filter { (alert, currentPrice) ->
                    alert.isTriggerMet(currentPrice, time)
                }

        Log.d(TAG, "Triggered Alerts: ${triggered.size}")

        if (triggered.isNotEmpty()) {
            setupNotificationChannel(applicationContext)
        }

        PriceAlertRepository.batch {
            // Update pinnedPrice for alerts that hit the inverse of their threshold or expire
            alertsWithCurrentPrice
                    .filterLeft(PriceAlert::isPercentageChangeType)
                    .filter { (alert, currentPrice) ->
                        alert.isTriggerUnmet(currentPrice, time)
                    }
                    .map { (alert, currentPrice) ->
                        updatePinnedPrice(alert, currentPrice)
                        alert.copy(pinnedPrice = currentPrice)
                    }
                    .forEach { alert ->
                        updateStartTime(alert, time)
                    }

            // Update pinnedPrice and startTime to the current rate and time
            triggered.filterLeft(PriceAlert::isPercentageChangeType)
                    .map { (alert, currentPrice) ->
                        updatePinnedPrice(alert, currentPrice)
                        alert.copy(pinnedPrice = currentPrice)
                    }
                    .forEach { alert ->
                        updateStartTime(alert, time)
                    }

            // List alerts that are stale until returned below their threshold
            val staleAlerts = triggered.mapLeft()
                    .filter(PriceAlert::isPriceTargetType)

            // List stale alerts that are now fresh
            val refreshedAlerts = alertsWithCurrentPrice
                    .filterLeft(PriceAlert::hasBeenTriggered)
                    .filter { (alert, currentPrice) ->
                        alert.isTriggerUnmet(currentPrice)
                    }
                    .mapLeft()

            (staleAlerts + refreshedAlerts).forEach(::toggleHasBeenTriggered)
        }

        // Dispatch notifications for each triggered alert
        triggered.filterLeft(PriceAlert::isPercentageChangeType)
                .forEach { (alert, currentPrice) ->
                    val pinnedPrice = getFormattedAmount(alert.toCurrencyCode, alert.pinnedPrice)
                    val exchangeRate = getFormattedAmount(alert.toCurrencyCode, currentPrice)
                    val stringId = when {
                        alert.pinnedPrice < currentPrice ->
                            R.string.PriceAlertNotification_percentChangeUp
                        else -> R.string.PriceAlertNotification_percentChangeDown
                    }

                    val diffPercent = when {
                        alert.pinnedPrice < currentPrice ->
                            (currentPrice - alert.pinnedPrice) / alert.pinnedPrice * 100f
                        else ->
                            (alert.pinnedPrice - currentPrice) / currentPrice * 100f
                    }.toBigDecimal()
                            .setScale(2, RoundingMode.HALF_EVEN)
                            .toString()
                    val message = applicationContext.getString(stringId, alert.forCurrencyCode,
                            diffPercent, pinnedPrice, exchangeRate)
                    dispatchNotification(alert.hashCode(), message)
                }

        triggered.filterLeft(PriceAlert::isPriceTargetType)
                .forEach { (alert, currentPrice) ->
                    val rateDifference = getFormattedAmount(alert.toCurrencyCode,
                            (alert.pinnedPrice - currentPrice).absoluteValue)
                    val exchangeRate = getFormattedAmount(alert.toCurrencyCode, currentPrice)
                    val stringId = when {
                        alert.pinnedPrice < currentPrice ->
                            R.string.PriceAlertNotification_priceTargetUp
                        else -> R.string.PriceAlertNotification_priceTargetDown
                    }

                    val message = applicationContext.getString(stringId,
                            alert.forCurrencyCode, rateDifference, exchangeRate)
                    dispatchNotification(alert.hashCode(), message)
                }

        return Result.success()
    }

    private fun dispatchNotification(notificationId: Int, message: String) {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(applicationContext.getString(R.string.PriceAlertNotification_title))
                .setContentText(message)
                .setSmallIcon(R.drawable.notification_icon)
                .build()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Transforms a list of [PriceAlert]s into a list of [PriceAlert]s
     * with their current exchange rate for [PriceAlert.forCurrencyCode]
     * to [PriceAlert.toCurrencyCode].
     */
    private fun List<PriceAlert>.mapToCurrentPrice(): List<Pair<PriceAlert, Float>> =
            map { alert ->
                alert to RatesRepository.getInstance(applicationContext)
                        .getFiatForCrypto(BigDecimal.ONE, alert.forCurrencyCode, alert.toCurrencyCode)
                        .setScale(2, RoundingMode.HALF_EVEN)
                        .toFloat()
            }

    private fun getFormattedAmount(iso: String, amount: Float): String =
            CurrencyUtils.getFormattedAmount(applicationContext, iso, amount.toBigDecimal())
}