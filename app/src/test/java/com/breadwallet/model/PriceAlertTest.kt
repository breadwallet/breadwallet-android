/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 5/30/2019.
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
package com.breadwallet.model

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
import com.breadwallet.model.PriceAlertTest.Companion.DAY_IN_MS
import com.breadwallet.model.PriceAlertTest.Companion.WEEK_IN_MS
import org.junit.Test
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PriceAlertTest {

    companion object {
        const val DAY_IN_MS: Long = 24 * 60 * 60 * 1000
        const val WEEK_IN_MS: Long = 7 * DAY_IN_MS
    }

    class PriceTargetIncrease {
        @Test
        fun testTriggeredWhenTargetPriceAboveTarget() =
                assertTriggeredTrue(101f) { priceTargetIncrease("ETH", 100f, "USD") }

        @Test
        fun testTriggerIsUnmetWhenPriceGoesBelowTarget() =
                assertTriggerUnmetTrue(99f) { priceTargetIncrease("ETH", 100f, "USD") }

        @Test
        fun testNotTriggeredWhenCurrentPriceUnchanged() =
                assertTriggeredFalse(100f) { priceTargetIncrease("ETH", 100f, "USD") }
    }

    class PriceTargetDecrease {
        @Test
        fun testTriggeredWhenTargetPriceBelowTarget() =
                assertTriggeredTrue(99f) { priceTargetDecrease("ETH", 100f, "USD") }

        @Test
        fun testTriggerUnmetWhenTargetPriceAboveTarget() =
                assertTriggerUnmetTrue(101f) { priceTargetDecrease("ETH", 100f, "USD") }

        @Test
        fun testNotTriggeredWhenCurrentPriceUnchanged() =
                assertTriggeredFalse(100f) { priceTargetDecrease("ETH", 100f, "USD") }
    }

    class PercentageChangeIncrease {
        @Test
        fun testTriggeredWhenIncreasedByTargetPercent() =
                assertTriggeredTrue(110f) { percentageIncreased("BCH", 10f, "USD", 100f) }

        @Test
        fun testNotTriggeredWhenDecreasedByTargetPercent() =
                assertTriggeredFalse(90f) { percentageIncreased("BCH", 10f, "USD", 100f) }

        @Test
        fun testTriggeredUnmetWhenDecreasedByTargetPercent() =
                assertTriggerUnmetTrue(90f) { percentageIncreased("BCH", 10f, "USD", 100f) }

        @Test
        fun testNotTriggeredWhenCurrentPriceUnchanged() =
                assertTriggeredFalse(100f) { percentageChanged("BCH", 10F, "USD", 100f) }
    }

    class PercentageChangeDecrease {
        @Test
        fun testTriggeredWhenDecreasedByTargetPercent() =
                assertTriggeredTrue(90f) { percentageDecreased("BCH", 10f, "USD", 100f) }

        @Test
        fun testNotTriggeredWhenIncreasedByTargetPercent() =
                assertTriggeredFalse(110f) { percentageDecreased("BCH", 10f, "USD", 100f) }

        @Test
        fun testTriggeredUnmetWhenIncreasedByTargetPercent() =
                assertTriggerUnmetTrue(110f) { percentageDecreased("BCH", 10f, "USD", 100f) }

        @Test
        fun testNotTriggeredWhenCurrentPriceUnchanged() =
                assertTriggeredFalse(100f) { percentageChanged("BCH", 10F, "USD", 100f) }
    }

    class PercentageChangeBoth {
        @Test
        fun testTriggeredWhenIncreasedByTargetPercent() =
                assertTriggeredTrue(110f) { percentageChanged("BCH", 10f, "USD", 100f) }

        @Test
        fun testTriggeredWhenDecreasedByTargetPercent() =
                assertTriggeredTrue(90f) { percentageChanged("BCH", 10f, "USD", 100f) }

        @Test
        fun testNotTriggeredWhenCurrentPriceUnchanged() =
                assertTriggeredFalse(100f) { percentageChanged("BCH", 10F, "USD", 100f) }
    }

    class PercentageChangeIncreaseInDay {
        @Test
        fun testTriggeredWhenIncreasedByTargetPercentInDay() =
                assertTriggeredTrue(110f, inDay()) { startTime ->
                    percentageIncreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testNotTriggeredWhenIncreasedByTargetPercentInOverADay() =
                assertTriggeredFalse(110f, overDay()) { startTime ->
                    percentageIncreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testNotTriggeredWhenDecreasedByTargetPercentInDay() =
                assertTriggeredFalse(90f, inDay()) { startTime ->
                    percentageIncreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerUnmetWhenStartTimeIsOverADay() =
                assertTriggerUnmetTrue(110f, overDay()) { startTime ->
                    percentageIncreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerNotUnmetWhenStartTimeIsUnderADay() =
                assertTriggerUnmetFalse(110f, inDay()) { startTime ->
                    percentageIncreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }
    }

    class PercentageChangeIncreaseInDayAndWeek {
        @Test
        fun testTriggeredWhenIncreasedByTargetPercentInDay() =
                assertTriggeredTrue(110f, inDay()) { startTime ->
                    percentageIncreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggeredWhenIncreasedByTargetPercentInWeek() =
                assertTriggeredTrue(110f, inWeek()) { startTime ->
                    percentageIncreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testNotTriggeredWhenIncreasedByTargetPercentInOverAWeek() =
                assertTriggeredFalse(110f, overWeek()) { startTime ->
                    percentageIncreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerNotUnmetWhenStartTimeIsOverADay() =
                assertTriggerUnmetFalse(110f, overDay()) { startTime ->
                    percentageIncreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerUnmetWhenStartTimeIsOverAWeek() =
                assertTriggerUnmetTrue(110f, overWeek()) { startTime ->
                    percentageIncreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }
    }

    class PercentageChangeDecreaseInDay {
        @Test
        fun testTriggeredWhenDecreasedByTargetPercentInDay() =
                assertTriggeredTrue(90f, inDay()) { startTime ->
                    percentageDecreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testNotTriggeredWhenDecreasedByTargetPercentInOverADay() =
                assertTriggeredFalse(90f, overDay()) { startTime ->
                    percentageDecreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testNotTriggeredWhenIncreasedByTargetPercentInDay() =
                assertTriggeredFalse(110f, inDay()) { startTime ->
                    percentageDecreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerUnmetWhenStartTimeIsOverADay() =
                assertTriggerUnmetTrue(110f, overDay()) { startTime ->
                    percentageDecreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerNotUnmetWhenStartTimeInDay() =
                assertTriggerUnmetFalse(110f, inDay()) { startTime ->
                    percentageDecreasedInDay("BCH", 10f, "USD", startTime, 100f)
                }
    }

    class PercentageChangeDecreaseInDayAndWeek {
        @Test
        fun testTriggeredWhenDecreasedByTargetPercentInDay() =
                assertTriggeredTrue(90f, inDay()) { startTime ->
                    percentageDecreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggeredWhenDecreasedByTargetPercentInWeek() =
                assertTriggeredTrue(90f, inWeek()) { startTime ->
                    percentageDecreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testNotTriggeredWhenDecreasedByTargetPercentInOverAWeek() =
                assertTriggeredFalse(90f, overWeek()) { startTime ->
                    percentageDecreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerNotUnmetWhenStartTimeIsOverADay() =
                assertTriggerUnmetFalse(90f, overDay()) { startTime ->
                    percentageDecreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerUnmetWhenStartTimeIsOverAWeek() =
                assertTriggerUnmetTrue(90f, overWeek()) { startTime ->
                    percentageDecreasedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }
    }

    class PercentageChangeBothInDay {
        @Test
        fun testTriggeredWhenIncreasedByTargetPercentInDay() =
                assertTriggeredTrue(110f, inDay()) { startTime ->
                    percentageChangedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggeredWhenDecreasedByTargetPercentInDay() =
                assertTriggeredTrue(90f, inDay()) { startTime ->
                    percentageChangedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testNotTriggeredWhenIncreasedByTargetPercentInOverADay() =
                assertTriggeredFalse(110f, overDay()) { startTime ->
                    percentageChangedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testNotTriggeredWhenDecreasedByTargetPercentInOverADay() =
                assertTriggeredFalse(90f, overDay()) { startTime ->
                    percentageChangedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerNotUnmetWhenStartTimeIsInDay() =
                assertTriggerUnmetFalse(90f, inDay()) { startTime ->
                    percentageChangedInDay("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerUnmetWhenStartTimeIsOverADay() =
                assertTriggerUnmetTrue(90f, overDay()) { startTime ->
                    percentageChangedInDay("BCH", 10f, "USD", startTime, 100f)
                }
    }

    class PercentageChangeBothInDayAndWeek {
        @Test
        fun testTriggeredWhenIncreasedByTargetPercentInDay() =
                assertTriggeredTrue(110f, inDay()) { startTime ->
                    percentageChangedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggeredWhenDecreasedByTargetPercentInDay() =
                assertTriggeredTrue(90f, inDay()) { startTime ->
                    percentageChangedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggeredWhenIncreasedByTargetPercentInWeek() =
                assertTriggeredTrue(110f, inWeek()) { startTime ->
                    percentageChangedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggeredWhenDecreasedByTargetPercentInWeek() =
                assertTriggeredTrue(90f, inWeek()) { startTime ->
                    percentageChangedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerNotUnmetWhenStartTimeIsInDay() =
                assertTriggerUnmetFalse(90f, inDay()) { startTime ->
                    percentageChangedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerNotUnmetWhenStartTimeIsOverADay() =
                assertTriggerUnmetFalse(90f, overDay()) { startTime ->
                    percentageChangedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }

        @Test
        fun testTriggerUnmetWhenStartTimIsOverAWeek() =
                assertTriggerUnmetTrue(90f, overWeek()) { startTime ->
                    percentageChangedInDayAndWeek("BCH", 10f, "USD", startTime, 100f)
                }
    }
}

/**
 * Encapsulates a [PriceAlert] factory and provides it a [PriceAlert.startTime].
 */
typealias AlertFactory = (@ParameterName("startTime") Long) -> PriceAlert

/**
 * Assert that calling [PriceAlert.isTriggerMet] on the [PriceAlert]
 * returned from [alertFactory] is true.
 *
 * @param offsetMs Offset to subtract from the current instant in ms for [alertFactory].
 */
private fun assertTriggeredTrue(currentPrice: Float, offsetMs: Long = 0, alertFactory: AlertFactory) =
        assertTrue("Expected isTriggerMet to be true.") {
            val now = Date().time
            alertFactory(now - offsetMs).isTriggerMet(currentPrice, now)
        }

/**
 * Assert that calling [PriceAlert.isTriggerMet] on the [PriceAlert]
 * returned from [alertFactory] is false.
 *
 * @param offsetMs Offset to subtract from the current instant in ms for [alertFactory].
 */
private fun assertTriggeredFalse(currentPrice: Float, offsetMs: Long = 0, alertFactory: AlertFactory) =
        assertFalse("Expected isTriggerMet to be false") {
            val now = Date().time
            alertFactory(now - offsetMs).isTriggerMet(currentPrice, now)
        }

/**
 * Assert that calling [PriceAlert.isTriggerUnmet] on the [PriceAlert]
 * returned from [alertFactory] is true.
 *
 * @param offsetMs Offset to subtract from the current instant in ms for [alertFactory].
 */
private fun assertTriggerUnmetTrue(currentPrice: Float, offsetMs: Long = 0, alertFactory: AlertFactory) =
        assertTrue("Expected isTriggerUnmet to be true") {
            val now = Date().time
            alertFactory(now - offsetMs).isTriggerUnmet(currentPrice, now)
        }

/**
 * Assert that calling [PriceAlert.isTriggerUnmet] on the [PriceAlert]
 * returned from [alertFactory] is false.
 *
 * @param offsetMs Offset to subtract from the current instant in ms for [alertFactory].
 */
private fun assertTriggerUnmetFalse(currentPrice: Float, offsetMs: Long = 0, alertFactory: AlertFactory) =
        assertFalse("Expected isTriggerUnmet to be false") {
            val now = Date().time
            alertFactory(now - offsetMs).isTriggerUnmet(currentPrice, now)
        }

/**
 * Just over one week ago.
 */
private fun overWeek() = WEEK_IN_MS + 1

/**
 * Just under one week ago.
 */
private fun inWeek() = WEEK_IN_MS - 1

/**
 * Just over one day ago.
 */
private fun overDay() = DAY_IN_MS + 1

/**
 * Just under one ago.
 */
private fun inDay() = DAY_IN_MS - 1
