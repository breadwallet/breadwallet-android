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

import kotlin.math.absoluteValue

/**
 * This immutable model holds the data necessary to store
 * and validate triggering of user defined price alerts.
 *
 * To create a new [PriceAlert], use one of the appropriate
 * factory methods in [PriceAlert.Companion].  The constructor
 * should only be used when deserializing a PriceAlert.
 */
data class PriceAlert(
        /**
         * The [Type] of trigger to fire this.
         */
        val type: Type,
        /**
         * The [Direction] that will be used when validating a trigger is met.
         */
        val direction: Direction,
        /**
         * The currency code for the asset we are tracking the value of.
         */
        val forCurrencyCode: String,
        /**
         * The user defined value the trigger will match against.
         * Depending on the [type], this could represent an exact
         * value in [toCurrencyCode] or a percentage used when comparing
         * [pinnedPrice] to the current asset price.
         */
        val value: Float,
        /**
         * The user defined currency the trigger will match against.
         */
        val toCurrencyCode: String,
        /**
         * An optional record of the time in milliseconds at which
         * this alert was created.
         */
        val startTime: Long = 0,
        /**
         * An optional record of the exchange rate at the time of
         * alert creation.  Used for updating [Type.PERCENTAGE_CHANGE]
         * alerts with the current price after being triggered so
         * the alert repeats indefinitely.
         */
        val pinnedPrice: Float = 0f,
        /**
         * If an alert has been triggered, this will be true and
         * future alerts should not be dispatched.
         *
         * When [hasBeenTriggered] is true, [isTriggerUnmet] should
         * be called to reset this flag when needed.
         */
        val hasBeenTriggered: Boolean = false
) {

    enum class Type {
        /**
         * Triggered if the current price of [forCurrencyCode] is greater
         * or less than [value] depending on [direction].
         */
        PRICE_TARGET,
        /**
         * Triggered if the latest price of [forCurrencyCode] is greater
         * or less than the user stored [value] as a percentage of [pinnedPrice].
         */
        PERCENTAGE_CHANGE,
        /**
         * Triggered if the latest price of [forCurrencyCode] is greater
         * or less than the user stored [value] as a percentage of [pinnedPrice]
         * within one day.
         */
        PERCENTAGE_CHANGE_DAY,
        /**
         * Triggered if the latest price of [forCurrencyCode] is greater
         * or less than the user stored [value] as a percentage of [pinnedPrice]
         * within one day and one week.
         */
        PERCENTAGE_CHANGE_DAY_WEEK;

        fun isPriceTarget() = this == PRICE_TARGET
        fun isPercentageChange() = when (this) {
            PERCENTAGE_CHANGE,
            PERCENTAGE_CHANGE_DAY,
            PERCENTAGE_CHANGE_DAY_WEEK -> true
            else -> false
        }

        fun isPercentageChangeWithWindow() = when (this) {
            PERCENTAGE_CHANGE_DAY,
            PERCENTAGE_CHANGE_DAY_WEEK -> true
            else -> false
        }
    }

    enum class Direction {
        /**
         * Triggers will be satisfied when the current asset price is
         * above or below the defined threshold.
         */
        BOTH,
        /**
         * Triggers will only be satisfied when the current asset price
         * is above the defined threshold.
         */
        INCREASE,
        /**
         * Triggers to be satisfied when the current asset price is
         * below the defined threshold.
         */
        DECREASE
    }

    init {
        require(value > 0) { "value must be a positive number" }
        require(pinnedPrice >= 0) { "pinnedPrice must be a positive number" }
        require(toCurrencyCode.isNotBlank()) { "toCurrencyCode must not be blank" }
        require(forCurrencyCode.isNotBlank()) { "forCurrencyCode must not be blank" }

        if (type.isPercentageChange()) {
            require(pinnedPrice > 0f) {
                "pinnedPrice must not be 0 when type is Type.PERCENTAGE_CHANGE_*"
            }
            if (type.isPercentageChangeWithWindow()) {
                require(startTime > 0) {
                    "startTime must be greater than 0 for Type.PERCENTAGE_CHANGE_*"
                }
            }
        }
    }

    /**
     * Returns whether or not this alerts trigger is satisfied based on
     * the [currentPrice].
     *
     * @param currentPrice The current price of [forCurrencyCode] in [toCurrencyCode].
     */
    fun isTriggerMet(currentPrice: Float, currentTime: Long = 0): Boolean {
        return when (type) {
            Type.PRICE_TARGET -> isPriceTargetTriggered(currentPrice)
            Type.PERCENTAGE_CHANGE -> isPercentageChangeTriggered(currentPrice)
            Type.PERCENTAGE_CHANGE_DAY -> isPercentageChangeInDayTriggered(currentPrice, currentTime, false)
            Type.PERCENTAGE_CHANGE_DAY_WEEK -> isPercentageChangeInDayTriggered(currentPrice, currentTime, true)
        }
    }

    /**
     * Returns whether or not this alert is still past its trigger threshold.
     *
     * For [Type.PRICE_TARGET] alerts where [hasBeenTriggered] is true, this
     * function is used to reset [hasBeenTriggered] flag so the alert will
     * be triggered again.
     *
     * For [Type.PERCENTAGE_CHANGE] alerts, this function is used to update
     * the [pinnedPrice] with the current exchange rate so the alert's threshold
     * will be relative to the actual exchange rate and not stale data.
     *
     * For [Type.PERCENTAGE_CHANGE_DAY] and [Type.PERCENTAGE_CHANGE_DAY_WEEK]
     * alerts, this function is used to update the [startTime] and [pinnedPrice].
     *
     * @param currentPrice The current price of [forCurrencyCode] in [toCurrencyCode].
     */
    fun isTriggerUnmet(currentPrice: Float, currentTime: Long = 0): Boolean {
        return when (type) {
            Type.PRICE_TARGET -> !isPriceTargetTriggered(currentPrice)
            Type.PERCENTAGE_CHANGE -> !isPercentageChangeTriggered(currentPrice)
            Type.PERCENTAGE_CHANGE_DAY -> !isInWindow(currentTime, DAY_IN_MS)
            Type.PERCENTAGE_CHANGE_DAY_WEEK -> !isInWindow(currentTime, WEEK_IN_MS)
        }
    }

    /**
     * Returns whether or not this price target alert is triggered
     * based on the [direction] and [value] compared to [currentPrice].
     */
    private fun isPriceTargetTriggered(currentPrice: Float): Boolean {
        check(type.isPriceTarget())
        return when (direction) {
            Direction.INCREASE -> currentPrice > value
            Direction.DECREASE -> currentPrice < value
            Direction.BOTH -> error("Direction.BOTH unsupported for Type.PRICE_TARGET")
        }
    }

    /**
     * Returns whether or not this percent change alert is triggered
     * using the difference (as a %) of [pinnedPrice] and [currentPrice].
     */
    private fun isPercentageChangeTriggered(currentPrice: Float): Boolean {
        check(type.isPercentageChange())
        val percentChanged = percentageChanged(currentPrice, pinnedPrice)
        return when (direction) {
            Direction.INCREASE -> {
                if (percentChanged <= 0) false
                else percentChanged >= value
            }
            Direction.DECREASE -> {
                if (percentChanged >= 0) false
                else percentChanged.absoluteValue >= value
            }
            Direction.BOTH -> {
                percentChanged.absoluteValue >= value
            }
        }
    }

    /**
     * Returns whether or not this percent change alert is triggered
     * using the difference (as a %) of [pinnedPrice] and [currentPrice]
     * within a day of [startTime].  Optionally checks within a week of
     * [startTime] if [andWeek] is true.
     */
    private fun isPercentageChangeInDayTriggered(currentPrice: Float, currentTime: Long, andWeek: Boolean): Boolean {
        check(type.isPercentageChangeWithWindow())
        require(currentTime > startTime) { "currentTime must be greater than startTime." }

        val window = if (andWeek) WEEK_IN_MS else DAY_IN_MS
        return if (!isInWindow(currentTime, window)) false
        else isPercentageChangeTriggered(currentPrice)
    }

    /**
     * Returns true if the time between [currentTime] and [startTime]
     * is less than [window].
     */
    private fun isInWindow(currentTime: Long, window: Long): Boolean {
        check(type.isPercentageChangeWithWindow())
        return currentTime - startTime <= window
    }

    /**
     * Returns the whole number percentage difference between
     * [currentPrice] and [originalPrice].
     */
    private fun percentageChanged(currentPrice: Float, originalPrice: Float): Float {
        val difference = currentPrice - originalPrice
        return difference / originalPrice * 100
    }

    /**
     * Inverse of [hasBeenTriggered] for functional composition.
     */
    fun hasNotBeenTriggered() = !hasBeenTriggered

    /**
     * True if this alert is of type [Type.PRICE_TARGET].
     */
    fun isPriceTargetType() = type.isPriceTarget()

    /**
     * True if this alert is of type [Type.PERCENTAGE_CHANGE],
     * [Type.PERCENTAGE_CHANGE_DAY], or [Type.PERCENTAGE_CHANGE_DAY_WEEK].
     */
    fun isPercentageChangeType() = type.isPercentageChange()

    companion object {
        /**
         * 24 hours in milliseconds for time operations.
         *
         * hours in day * minutes * seconds * ms
         */
        private const val DAY_IN_MS: Long = 24 * 60 * 60 * 1000
        /**
         * 7 days in milliseconds for time operations.
         */
        private const val WEEK_IN_MS: Long = 7 * DAY_IN_MS

        /**
         * Create an alert of type [Type.PRICE_TARGET] with [Direction.INCREASE].
         */
        fun priceTargetIncrease(forCurrencyCode: String, value: Float, currencyCode: String) =
                PriceAlert(Type.PRICE_TARGET, Direction.INCREASE, forCurrencyCode, value, currencyCode)

        /**
         * Create an alert of type [Type.PRICE_TARGET] with [Direction.DECREASE].
         */
        fun priceTargetDecrease(forCurrencyCode: String, value: Float, currencyCode: String) =
                PriceAlert(Type.PRICE_TARGET, Direction.DECREASE, forCurrencyCode, value, currencyCode)

        /**
         * Create an alert of type [Type.PERCENTAGE_CHANGE] with [Direction.INCREASE].
         */
        fun percentageIncreased(forCurrencyCode: String, value: Float, currencyCode: String, pinnedPrice: Float) =
                percentageChangeTrigger(Direction.INCREASE, forCurrencyCode, value, currencyCode, pinnedPrice)

        /**
         * Create an alert of type [Type.PERCENTAGE_CHANGE] with [Direction.DECREASE].
         */
        fun percentageDecreased(forCurrencyCode: String, value: Float, currencyCode: String, pinnedPrice: Float) =
                percentageChangeTrigger(Direction.DECREASE, forCurrencyCode, value, currencyCode, pinnedPrice)

        /**
         * Create an alert of type [Type.PERCENTAGE_CHANGE] with [Direction.BOTH].
         */
        fun percentageChanged(forCurrencyCode: String, value: Float, currencyCode: String, pinnedPrice: Float) =
                percentageChangeTrigger(Direction.BOTH, forCurrencyCode, value, currencyCode, pinnedPrice)

        private fun percentageChangeTrigger(direction: Direction, forCurrencyCode: String, value: Float, currencyCode: String, pinnedPrice: Float) =
                PriceAlert(Type.PERCENTAGE_CHANGE, direction, forCurrencyCode, value, currencyCode, pinnedPrice = pinnedPrice)

        /**
         * Create an alert of type [Type.PERCENTAGE_CHANGE_DAY] with [Direction.INCREASE].
         */
        fun percentageIncreasedInDay(forCurrencyCode: String, value: Float, currencyCode: String, startTime: Long, pinnedPrice: Float) =
                PriceAlert(Type.PERCENTAGE_CHANGE_DAY, Direction.INCREASE, forCurrencyCode, value, currencyCode, startTime, pinnedPrice)

        /**
         * Create an alert of type [Type.PERCENTAGE_CHANGE_DAY] with [Direction.DECREASE].
         */
        fun percentageDecreasedInDay(forCurrencyCode: String, value: Float, currencyCode: String, startTime: Long, pinnedPrice: Float) =
                PriceAlert(Type.PERCENTAGE_CHANGE_DAY, Direction.DECREASE, forCurrencyCode, value, currencyCode, startTime, pinnedPrice)

        /**
         * Create an alert of type [Type.PERCENTAGE_CHANGE_DAY] with [Direction.BOTH].
         */
        fun percentageChangedInDay(forCurrencyCode: String, value: Float, currencyCode: String, startTime: Long, pinnedPrice: Float) =
                PriceAlert(Type.PERCENTAGE_CHANGE_DAY, Direction.BOTH, forCurrencyCode, value, currencyCode, startTime, pinnedPrice)

        /**
         * Create an alert of type [Type.PERCENTAGE_CHANGE_DAY_WEEK] with [Direction.INCREASE].
         */
        fun percentageIncreasedInDayAndWeek(forCurrencyCode: String, value: Float, currencyCode: String, startTime: Long, pinnedPrice: Float) =
                PriceAlert(Type.PERCENTAGE_CHANGE_DAY_WEEK, Direction.INCREASE, forCurrencyCode, value, currencyCode, startTime, pinnedPrice)

        /**
         * Create an alert of type [Type.PERCENTAGE_CHANGE_DAY_WEEK] with [Direction.DECREASE].
         */
        fun percentageDecreasedInDayAndWeek(forCurrencyCode: String, value: Float, currencyCode: String, startTime: Long, pinnedPrice: Float) =
                PriceAlert(Type.PERCENTAGE_CHANGE_DAY_WEEK, Direction.DECREASE, forCurrencyCode, value, currencyCode, startTime, pinnedPrice)

        /**
         * Create an alert of type [Type.PERCENTAGE_CHANGE_DAY_WEEK] with [Direction.BOTH].
         */
        fun percentageChangedInDayAndWeek(forCurrencyCode: String, value: Float, currencyCode: String, startTime: Long, pinnedPrice: Float) =
                PriceAlert(Type.PERCENTAGE_CHANGE_DAY_WEEK, Direction.BOTH, forCurrencyCode, value, currencyCode, startTime, pinnedPrice)
    }
}