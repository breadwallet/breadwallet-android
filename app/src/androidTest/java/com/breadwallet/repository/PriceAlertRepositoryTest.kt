/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 7/2/19.
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
package com.breadwallet.repository

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.breadwallet.model.PriceAlert.Companion.percentageChanged
import com.breadwallet.model.PriceAlert.Companion.percentageDecreasedInDay
import com.breadwallet.model.PriceAlert.Companion.priceTargetIncrease
import com.breadwallet.tools.manager.BRSharedPrefs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PriceAlertRepositoryTest {

    @Before
    fun setup() {
        BRSharedPrefs.initialize(InstrumentationRegistry.getInstrumentation().context)
    }

    @After
    fun cleanup() {
        BRSharedPrefs.putPriceAlerts(emptySet())
        PriceAlertRepository.removeAll()
    }

    @Test
    fun testGetsAlertsFromSharedPrefsWhenCacheEmpty() {
        val alertsOnDisk = setOf(
            percentageChanged("ETH", 10f, "USD", 300f),
            priceTargetIncrease("ETH", 500f, "USD")
        )

        BRSharedPrefs.putPriceAlerts(alertsOnDisk)

        val loadedAlerts = PriceAlertRepository.getAlerts()

        assertEquals(alertsOnDisk.toList(), loadedAlerts)
    }

    @Test
    fun testGetsAlertsFromCacheWhenNotEmpty() {
        val alerts = setOf(
            percentageChanged("ETH", 10f, "USD", 300f),
            priceTargetIncrease("ETH", 500f, "USD")
        )

        BRSharedPrefs.putPriceAlerts(alerts)
        PriceAlertRepository.getAlerts()
        // Remove alerts from disk because we cannot spy on BRSharedPrefs
        // This ensures our subject does not pull from BRSharedPrefs
        BRSharedPrefs.putPriceAlerts(emptySet())

        val loadedAlerts = PriceAlertRepository.getAlerts()

        assertEquals(alerts.toList(), loadedAlerts)
    }

    @Test
    fun testSetAlertsWritesToCache() {
        val alerts = listOf(
            percentageChanged("ETH", 10f, "USD", 300f),
            priceTargetIncrease("ETH", 500f, "USD")
        )

        PriceAlertRepository.setAlerts(alerts)

        val alertsInCache = PriceAlertRepository.getAlerts()

        assertEquals(alerts, alertsInCache)
    }

    @Test
    fun testSetAlertsWritesToDisk() {
        val alerts = setOf(
            percentageChanged("ETH", 10f, "USD", 300f),
            priceTargetIncrease("ETH", 500f, "USD")
        )

        PriceAlertRepository.setAlerts(alerts.toList())

        val alertsOnDisk = BRSharedPrefs.getPriceAlerts()

        assertEquals(alerts, alertsOnDisk)
    }

    @Test
    fun testPutAlertWritesToCache() {
        val alerts = listOf(
            percentageChanged("ETH", 10f, "USD", 300f),
            priceTargetIncrease("ETH", 500f, "USD")
        )
        PriceAlertRepository.setAlerts(alerts)

        val newAlert = percentageChanged("BCH", 10f, "USD", 200f)

        PriceAlertRepository.putAlert(newAlert)

        val alertsInCache = PriceAlertRepository.getAlerts()

        assertEquals(alerts + newAlert, alertsInCache)
    }

    @Test
    fun testPutAlertWritesToDisk() {
        val newAlert = percentageChanged("BCH", 10f, "USD", 200f)
        val alerts = listOf(
            percentageChanged("ETH", 10f, "USD", 300f),
            priceTargetIncrease("ETH", 500f, "USD")
        )

        PriceAlertRepository.setAlerts(alerts)
        PriceAlertRepository.putAlert(newAlert)

        val newAlerts = BRSharedPrefs.getPriceAlerts()

        assertEquals(alerts + newAlert, newAlerts.toList())
    }

    @Test
    fun testRemoveAlertWritesToCache() {
        val subject = percentageChanged("ETH", 10f, "USD", 300f)
        val alerts = listOf(subject, priceTargetIncrease("ETH", 500f, "USD"))

        PriceAlertRepository.setAlerts(alerts)
        PriceAlertRepository.removeAlert(subject)

        val newAlerts = PriceAlertRepository.getAlerts()

        assertFalse { newAlerts.contains(subject) }
    }

    @Test
    fun testRemoveAlertWritesToDisk() {
        val subject = percentageChanged("ETH", 10f, "USD", 300f)
        val alerts = listOf(subject, priceTargetIncrease("ETH", 500f, "USD"))

        PriceAlertRepository.setAlerts(alerts)
        PriceAlertRepository.removeAlert(subject)

        val newAlerts = BRSharedPrefs.getPriceAlerts()

        assertFalse { newAlerts.contains(subject) }
    }

    @Test
    fun testToggleHasBeenTriggeredWritesToCache() {
        val alerts = listOf(
            priceTargetIncrease("BTC", 50f, "USD"),
            priceTargetIncrease("ETH", 500f, "USD")
                .copy(hasBeenTriggered = true)
        )
        PriceAlertRepository.setAlerts(alerts)

        alerts.forEach { PriceAlertRepository.toggleHasBeenTriggered(it) }

        val newAlerts = PriceAlertRepository.getAlerts()
        val expected = alerts.map { it.copy(hasBeenTriggered = !it.hasBeenTriggered) }

        assertEquals(expected.first().hasBeenTriggered, newAlerts.first().hasBeenTriggered)
        assertEquals(expected.last().hasBeenTriggered, newAlerts.last().hasBeenTriggered)
    }

    @Test
    fun testToggleHasBeenTriggeredWritesToDisk() {
        val alerts = listOf(
            priceTargetIncrease("BTC", 50f, "USD"),
            priceTargetIncrease("ETH", 500f, "USD")
                .copy(hasBeenTriggered = true)
        )
        PriceAlertRepository.setAlerts(alerts)

        val expected = alerts
            .onEach { PriceAlertRepository.toggleHasBeenTriggered(it) }
            .map { it.copy(hasBeenTriggered = !it.hasBeenTriggered) }

        val newAlerts = BRSharedPrefs.getPriceAlerts().reversed()

        assertEquals(expected.first().hasBeenTriggered, newAlerts.first().hasBeenTriggered)
        assertEquals(expected.last().hasBeenTriggered, newAlerts.last().hasBeenTriggered)
    }

    @Test
    fun testUpdatePinnedPriceWritesToCache() {
        val alerts = listOf(
            percentageChanged("BCH", 10f, "USD", 300f),
            percentageChanged("ETH", 10f, "USD", 200f)
        )
        PriceAlertRepository.setAlerts(alerts)

        val expected = 100f
        alerts.forEach { PriceAlertRepository.updatePinnedPrice(it, expected) }

        val newAlerts = PriceAlertRepository.getAlerts()

        assertEquals(expected, newAlerts.first().pinnedPrice)
        assertEquals(expected, newAlerts.last().pinnedPrice)
    }

    @Test
    fun testUpdatePinnedPriceWritesToDisk() {
        val alerts = listOf(
            percentageChanged("BCH", 10f, "USD", 300f),
            percentageChanged("ETH", 10f, "USD", 200f)
        )
        PriceAlertRepository.setAlerts(alerts)

        val expected = 100f
        alerts.onEach { PriceAlertRepository.updatePinnedPrice(it, expected) }

        val newAlerts = BRSharedPrefs.getPriceAlerts()

        assertEquals(expected, newAlerts.first().pinnedPrice)
        assertEquals(expected, newAlerts.last().pinnedPrice)
    }

    @Test
    fun testUpdateStartTimeWritesToCache() {
        val alerts = listOf(
            percentageDecreasedInDay("BCH", 10f, "USD", 1, 300f),
            percentageDecreasedInDay("ETH", 10f, "USD", 1, 200f)
        )
        PriceAlertRepository.setAlerts(alerts)

        val expected: Long = 2
        alerts.forEach { PriceAlertRepository.updateStartTime(it, expected) }

        val newAlerts = PriceAlertRepository.getAlerts()

        assertEquals(expected, newAlerts.first().startTime)
        assertEquals(expected, newAlerts.last().startTime)
    }

    @Test
    fun testUpdateStartTimeWritesToDisk() {
        val alerts = listOf(
            percentageDecreasedInDay("BCH", 10f, "USD", 1, 300f),
            percentageDecreasedInDay("ETH", 10f, "USD", 1, 200f)
        )
        PriceAlertRepository.setAlerts(alerts)

        val expected: Long = 2
        alerts.forEach { PriceAlertRepository.updateStartTime(it, expected) }

        val newAlerts = BRSharedPrefs.getPriceAlerts()

        assertEquals(expected, newAlerts.first().startTime)
        assertEquals(expected, newAlerts.last().startTime)
    }

    @Test
    fun testBatchDoesNotWriteToDiskUntilComplete() {
        val subject = percentageDecreasedInDay("BCH", 10f, "USD", 1, 300f)

        PriceAlertRepository.batch {
            putAlert(subject)
            assertTrue { BRSharedPrefs.getPriceAlerts().isEmpty() }
        }

        assertTrue { BRSharedPrefs.getPriceAlerts().isNotEmpty() }
    }
}
