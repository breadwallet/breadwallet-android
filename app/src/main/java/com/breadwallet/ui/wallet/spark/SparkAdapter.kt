/**
 * BreadWallet
 *
 * Created by Alan Hill <alan.hill@breadwallet.com> on 6/7/19.
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
package com.breadwallet.ui.wallet.spark

import android.database.DataSetObservable
import android.database.DataSetObserver
import android.graphics.RectF
import com.breadwallet.model.PriceDataPoint

/**
 * A simple adapter class to display your points in the graph and has support for
 * registering/notifying {@link DataSetObserver}s when data is changed.
 *
 * Adapted from Robinhood's SparkView: https://github.com/robinhood/spark
 */
class SparkAdapter {

    private val dataSetObservable = DataSetObservable()

    var dataSet: List<PriceDataPoint> = emptyList()
    val count: Int
        get() {
            return dataSet.size
        }

    /**
     * Retrieve the Y axis value for the given index
     */
    fun getY(index: Int): Float = dataSet[index].closePrice.toFloat()

    /**
     * Get the boundaries of the entire dataset to be displayed to the user. It is the min and max
     * of the actual data points in the adapter.
     */
    fun getDataBounds(): RectF {
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE

        for (i in 0 until count) {
            val x = i.toFloat()
            minX = Math.min(minX, x)
            maxX = Math.max(maxX, x)

            val y = getY(i)
            minY = Math.min(minY, y)
            maxY = Math.max(maxY, y)
        }

        return RectF(minX, minY, maxX, maxY)
    }

    /**
     * Notify any registered observers that the data has changed.
     */
    fun notifyDataSetChanged() {
        dataSetObservable.notifyChanged()
    }

    /**
     * Notify any registered observers the data is no longer available or invalid
     */
    fun notifyDataSetInvalidated() {
        dataSetObservable.notifyInvalidated()
    }

    /**
     * Register a [DataSetObserver]
     */
    fun registerDataSetObserver(observer: DataSetObserver) {
        dataSetObservable.registerObserver(observer)
    }

    /**
     * Remove the given [DataSetObserver]
     */
    fun unregisterDataSetObserver(observer: DataSetObserver) {
        dataSetObservable.unregisterObserver(observer)
    }
}
