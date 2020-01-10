/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 7/18/19.
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

import android.os.Handler
import android.view.MotionEvent
import android.view.View

/**
 * Exposes simple methods for detecting scrub events.
 *
 * Adapted from Robinhood's SparkView: https://github.com/robinhood/spark
 */
internal class ScrubGestureDetector(
        private val scrubListener: ScrubListener,
        private val handler: Handler,
        private val touchSlop: Float
) : View.OnTouchListener {

    private var enabled: Boolean = false
    private var downX: Float = 0.toFloat()
    private var downY: Float = 0.toFloat()

    private val longPressRunnable = Runnable { scrubListener.onScrubbed(downX, downY) }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!enabled) return false

        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // store the time to compute whether future events are 'long presses'
                downX = x
                downY = y

                handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT_MS)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // calculate the elapsed time since the down event
                val timeDelta = (event.eventTime - event.downTime).toFloat()

                // if the user has intentionally long-pressed
                if (timeDelta >= LONG_PRESS_TIMEOUT_MS) {
                    handler.removeCallbacks(longPressRunnable)
                    scrubListener.onScrubbed(x, y)
                } else {
                    // if we moved before longpress, remove the callback if we exceeded the tap slop
                    val deltaX = x - downX
                    val deltaY = y - downY
                    if (deltaX >= touchSlop || deltaY >= touchSlop) {
                        handler.removeCallbacks(longPressRunnable)
                        // We got a MOVE event that exceeded tap slop but before the long-press
                        // threshold, we don't care about this series of events anymore.
                        return false
                    }
                }

                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)
                scrubListener.onScrubEnded()
                return true
            }
            else -> return false
        }
    }

    internal interface ScrubListener {
        fun onScrubbed(x: Float, y: Float)
        fun onScrubEnded()
    }

    companion object {
        val LONG_PRESS_TIMEOUT_MS: Long = 250
    }
}
