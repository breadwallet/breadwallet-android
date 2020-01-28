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
package com.breadwallet.repository

import com.breadwallet.model.PriceAlert
import com.breadwallet.tools.manager.BRSharedPrefs
import org.json.JSONArray

/**
 * A storage provider for user defined [PriceAlert]s.
 *
 * The default implementation is provided by [PriceAlertRepository.Companion].
 */
interface PriceAlertRepository {
    companion object : PriceAlertRepository by PriceAlertRepositoryImpl()

    /** Get the user's list of [PriceAlert] s.*/
    fun getAlerts(): List<PriceAlert>

    /** Clear existing alerts and replace them with [newAlerts]. */
    fun setAlerts(newAlerts: List<PriceAlert>)

    /** Toggle the [PriceAlert.hasBeenTriggered] flag. */
    fun toggleHasBeenTriggered(priceAlert: PriceAlert)

    /** Update [PriceAlert.pinnedPrice] to [pinnedPrice]. */
    fun updatePinnedPrice(priceAlert: PriceAlert, pinnedPrice: Float)

    /** Update [PriceAlert.startTime] to [currentTime]. */
    fun updateStartTime(priceAlert: PriceAlert, currentTime: Long)

    /** Add a new [PriceAlert]. */
    fun putAlert(priceAlert: PriceAlert)

    /** Remove the [priceAlert]. */
    fun removeAlert(priceAlert: PriceAlert)

    /** Remove all [PriceAlert]s. */
    fun removeAll()

    /**
     * Provides a [PriceAlertRepository] where operations
     * will not write to disk until [process] completes.
     */
    fun batch(process: PriceAlertRepository.() -> Unit)
}

/**
 * A default implementation of [PriceAlertRepository] that
 * uses a [Set] for memory caching and [BRSharedPrefs] for
 * disk storage.
 */
private open class PriceAlertRepositoryImpl(
        initialList: List<PriceAlert> = emptyList()
) : PriceAlertRepository {

    private val alerts = initialList.toMutableSet()

    override fun getAlerts(): List<PriceAlert> =
            synchronized(alerts) {
                if (alerts.isEmpty()) {
                    alerts.addAll(BRSharedPrefs.getPriceAlerts())
                }
                alerts.toList()
            }

    override fun setAlerts(newAlerts: List<PriceAlert>) =
            synchronized(alerts) {
                alerts.clear()
                alerts.addAll(newAlerts)
                writeToDisk()
            }

    override fun toggleHasBeenTriggered(priceAlert: PriceAlert) =
            synchronized(alerts) {
                check(alerts.contains(priceAlert)) { "Could not find alert $priceAlert" }
                alerts.replace(priceAlert) {
                    copy(hasBeenTriggered = !hasBeenTriggered)
                }
                writeToDisk()
            }

    override fun updatePinnedPrice(priceAlert: PriceAlert, pinnedPrice: Float) =
            synchronized(alerts) {
                check(alerts.contains(priceAlert)) { "Could not find alert $priceAlert" }
                alerts.replace(priceAlert) {
                    copy(pinnedPrice = pinnedPrice)
                }
                writeToDisk()
            }

    override fun updateStartTime(priceAlert: PriceAlert, currentTime: Long) =
            synchronized(alerts) {
                check(alerts.contains(priceAlert)) { "Could not find alert $priceAlert" }
                alerts.replace(priceAlert) {
                    copy(startTime = currentTime)
                }
                writeToDisk()
            }

    override fun putAlert(priceAlert: PriceAlert) =
            synchronized(alerts) {
                check(!alerts.contains(priceAlert)) { "Cannot add duplicate alerts." }
                alerts.add(priceAlert)
                writeToDisk()
            }

    override fun removeAlert(priceAlert: PriceAlert) =
            synchronized(alerts) {
                check(alerts.contains(priceAlert)) { "Cannot remove non-existent alert." }
                alerts.remove(priceAlert)
                writeToDisk()
            }

    override fun removeAll() = synchronized(alerts) {
        alerts.clear()
        writeToDisk()
    }

    override fun batch(process: PriceAlertRepository.() -> Unit) =
            synchronized(alerts) {
                val repo = object : PriceAlertRepositoryImpl(getAlerts()) {
                    // Disable writing to database.
                    override fun writeToDisk() = Unit
                }
                process(repo)
                setAlerts(repo.getAlerts())
            }

    /**
     * Saves [alerts] with [BRSharedPrefs].
     *
     * Open so disk storage can be skipped in [batch].
     */
    open fun writeToDisk() = synchronized(alerts) {
        BRSharedPrefs.putPriceAlerts(alerts)
    }

    private fun MutableSet<PriceAlert>.replace(
            target: PriceAlert,
            mutate: PriceAlert.() -> PriceAlert
    ) {
        val targetIndex = indexOf(target)
        val newAlerts = mapIndexed { index, alert ->
            if (targetIndex == index) mutate(target) else alert
        }.toSet()
        clear()
        addAll(newAlerts)
    }
}

/**
 * Writes this [PriceAlert] model as a [JSONArray] string
 * for the purpose of serialization.
 *
 * [JSONArray] is used instead of a [JSONObject] to reduce
 * size on disk.
 */
// When modifying the schema, only add fields and omit values of deprecated fields.
fun PriceAlert.asJsonArrayString(): String =
        JSONArray().apply {
            put(0, PriceAlert.Type.values().indexOf(type))
            put(1, PriceAlert.Direction.values().indexOf(direction))
            put(2, forCurrencyCode)
            put(3, value)
            put(4, toCurrencyCode)
            put(5, startTime)
            put(6, pinnedPrice)
            put(7, hasBeenTriggered)
        }.toString()

/**
 * Returns a [PriceAlert] from a JSON array string created
 * using [asJsonArrayString].
 */
fun PriceAlert.Companion.fromJsonArrayString(jsonArray: String): PriceAlert =
        JSONArray(jsonArray).run {
            PriceAlert(
                    PriceAlert.Type.values()[getInt(0)],
                    PriceAlert.Direction.values()[getInt(1)],
                    getString(2),
                    (get(3) as Number).toFloat(),
                    getString(4),
                    getLong(5),
                    (get(6) as Number).toFloat(),
                    getBoolean(7)
            )
        }
