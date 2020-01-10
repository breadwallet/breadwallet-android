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
package com.breadwallet.ui.wallet.spark.animation

import android.animation.Animator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.graphics.PathMeasure
import androidx.annotation.IntRange
import com.breadwallet.ui.wallet.spark.SparkView

/**
 * Animates the sparkline by path-tracing from the first point to the last.
 *
 * Adapted from Robinhood's SparkView: https://github.com/robinhood/spark
 */
class LineSparkAnimator : Animator(), SparkAnimator {

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)

    override fun getAnimator(sparkView: SparkView): Animator? {
        val linePath = sparkView.sparkLinePath

        // get path length
        val pathMeasure = PathMeasure(linePath, false)
        val endLength = pathMeasure.length

        if (endLength <= 0) {
            return null
        }

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float

            val animatedPathLength = animatedValue * endLength

            linePath.reset()
            pathMeasure.getSegment(0f, animatedPathLength, linePath, true)

            // set the updated path for the animation
            sparkView.setAnimationPath(linePath)
        }

        return animator
    }

    override fun getStartDelay(): Long {
        return animator.startDelay
    }

    override fun setStartDelay(@IntRange(from = 0) startDelay: Long) {
        animator.startDelay = startDelay
    }

    override fun setDuration(@IntRange(from = 0) duration: Long): Animator {
        return animator.setDuration(duration)
    }

    override fun getDuration(): Long {
        return animator.duration
    }

    override fun setInterpolator(timeInterpolator: TimeInterpolator?) {
        animator.interpolator = timeInterpolator
    }

    override fun isRunning(): Boolean {
        return animator.isRunning
    }
}
