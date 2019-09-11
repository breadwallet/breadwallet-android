package com.breadwallet.platform.pricealert

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.breadwallet.model.PriceAlert
import com.breadwallet.presenter.entities.CurrencyEntity
import com.breadwallet.repository.*
import com.breadwallet.tools.manager.BRSharedPrefs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PriceAlertWorkerTest {

    private val workManager: WorkManager
        get() = WorkManager.getInstance()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getTargetContext()
        val config = Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        BRSharedPrefs.provideContext(context)
    }

    @After
    fun cleanup() {
        PriceAlertRepository.removeAll()
    }

    @Test
    @Throws(Exception::class)
    fun testSerialization() {
        val priceTargetIndex = PriceAlert.Type.values().indexOf(PriceAlert.Type.PRICE_TARGET)
        val increaseIndex = PriceAlert.Direction.values().indexOf(PriceAlert.Direction.INCREASE)
        val priceAlert = PriceAlert.priceTargetIncrease("ETH", 10f, "USD")
        val expected = "[$priceTargetIndex,$increaseIndex,\"ETH\",10,\"USD\",0,0,false]"

        assertEquals(expected, priceAlert.asJsonArrayString())
    }

    @Throws(Exception::class)
    @Test
    fun testDeserialization() {
        val priceTargetIndex = PriceAlert.Type.values().indexOf(PriceAlert.Type.PRICE_TARGET)
        val increaseIndex = PriceAlert.Direction.values().indexOf(PriceAlert.Direction.INCREASE)
        val serializedString = "[$priceTargetIndex,$increaseIndex,\"ETH\",10,\"USD\",0,0,false]"
        val expected = PriceAlert.priceTargetIncrease("ETH", 10f, "USD")

        assertEquals(expected, PriceAlert.fromJsonArrayString(serializedString))
    }

    @Test
    @Throws(Exception::class)
    fun testScheduleWorkEnqueuesWorkRequest() {
        PriceAlertWorker.scheduleWork(updateRates = false)

        val workInfos = workManager.getWorkInfosForUniqueWork(PriceAlertWorker.uniqueWorkName).get()

        assertEquals(1, workInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, workInfos.single().state)
    }

    @Test
    @Throws(Exception::class)
    fun testCancelWorkRemovesWorkRequest() {
        PriceAlertWorker.scheduleWork(updateRates = false)
        PriceAlertWorker.cancelWork()

        val workInfo = workManager.getCryptoPriceAlertWorkerInfo()

        assertEquals(WorkInfo.State.CANCELLED, workInfo.state)
    }

    @Test
    @Throws(Exception::class)
    fun testWorkIsCancelledWhenNoAlertsAvailable() {
        PriceAlertWorker.scheduleWork(updateRates = false)
        advanceTestDriver()

        val completedWorkInfo = workManager.getCryptoPriceAlertWorkerInfo()

        assertEquals(WorkInfo.State.CANCELLED, completedWorkInfo.state)
    }

    @Test
    @Throws(Exception::class)
    fun testTogglesHasBeenTriggeredWhenTriggered() {
        putAlerts(PriceAlert.priceTargetIncrease("BTC", 100f, "USD"))
        putRates(CurrencyEntity("USD", "Dollar", 101f, "BTC"))

        PriceAlertWorker.scheduleWork(updateRates = false)
        advanceTestDriver()

        val processedAlert = PriceAlertRepository.getAlerts().single()

        assertTrue { processedAlert.hasBeenTriggered }
    }


    @Test
    @Throws(Exception::class)
    fun testDoesNotToggleHasBeenTriggeredWhenNotTriggered() {
        putAlerts(PriceAlert.priceTargetIncrease("BTC", 100f, "USD"))
        putRates(CurrencyEntity("USD", "Dollar", 100f, "BTC"))

        PriceAlertWorker.scheduleWork(updateRates = false)
        advanceTestDriver()

        val processedAlert = PriceAlertRepository.getAlerts().single()

        assertFalse { processedAlert.hasBeenTriggered }
    }

    @Test
    @Throws(Exception::class)
    fun testTogglesHasBeenTriggeredWhenTriggerUnmet() {
        putAlerts(PriceAlert.priceTargetIncrease("BTC", 100f, "USD"))
        putRates(CurrencyEntity("USD", "Dollar", 99f, "BTC"))

        PriceAlertWorker.scheduleWork(updateRates = false)
        advanceTestDriver()

        val processedAlert = PriceAlertRepository.getAlerts().single()

        assertFalse { processedAlert.hasBeenTriggered }
    }

    @Test
    @Throws(Exception::class)
    fun testUpdatesPinnedPriceWhenTriggered() {
        putAlerts(PriceAlert.percentageChanged("BTC", 10f, "USD", 100f))
        putRates(CurrencyEntity("USD", "Dollar", 90f, "BTC"))

        PriceAlertWorker.scheduleWork(updateRates = false)
        advanceTestDriver()

        val alert = PriceAlertRepository.getAlerts().single()

        assertEquals(90f, alert.pinnedPrice)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdatesStartTimeWhenOutOfWindow() {
        val now = Date().time - (24 * 60 * 60 * 1000) - 1 // Over one day ago
        putAlerts(PriceAlert.percentageDecreasedInDay("BTC", 10f, "USD", now, 100f))
        putRates(CurrencyEntity("USD", "Dollar", 100f, "BTC"))

        PriceAlertWorker.scheduleWork(updateRates = false)
        advanceTestDriver()

        val alert = PriceAlertRepository.getAlerts().single()

        assertNotEquals(now, alert.startTime)
    }

    @Test
    @Throws(Exception::class)
    fun testDoesNotUpdateStartTimeWhenInWindow() {
        val now = Date().time - (24 * 60 * 60 * 1000) // One day ago
        putAlerts(PriceAlert.percentageDecreasedInDayAndWeek("BTC", 10f, "USD", now, 100f))
        putRates(CurrencyEntity("USD", "Dollar", 100f, "BTC"))

        PriceAlertWorker.scheduleWork(updateRates = false)
        advanceTestDriver()

        val alert = PriceAlertRepository.getAlerts().single()

        assertEquals(now, alert.startTime)
    }

    private fun WorkManager.getCryptoPriceAlertWorkerInfo() =
            getWorkInfosForUniqueWork(PriceAlertWorker.uniqueWorkName).get().single()

    private fun putAlerts(vararg priceAlerts: PriceAlert) {
        PriceAlertRepository.setAlerts(priceAlerts.asList())
    }

    private fun putRates(vararg currencyEntity: CurrencyEntity) {
        val ratesRepo = RatesRepository.getInstance(InstrumentationRegistry.getTargetContext())
        ratesRepo.putCurrencyRates(currencyEntity.asList())
    }

    private fun advanceTestDriver() {
        val id = workManager.getCryptoPriceAlertWorkerInfo().id
        WorkManagerTestInitHelper.getTestDriver().apply {
            setPeriodDelayMet(id)
            setAllConstraintsMet(id)
        }
    }
}
