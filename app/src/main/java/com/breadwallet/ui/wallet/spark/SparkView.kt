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

import android.animation.Animator
import android.content.Context
import android.database.DataSetObserver
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import com.breadwallet.R
import com.breadwallet.ui.wallet.spark.animation.SparkAnimator
import java.util.Collections

/**
 * A view that shows a Spark line to the user without any axes.
 *
 * Adapted from Robinhood's SparkView: https://github.com/robinhood/spark
 *
 * Styleable elements:
 *
 * <ul>
 *     <li>spark_lineColor - the color you want the line to be</li>
 *     <li>spark_fillColor - the background color for the graph</li>
 *     <li>spark_lineWidth - how thick you want the spark line to be</li>
 *     <li>spark_cornerRadius - this helps to smooth out the graph</li>
 *     <li>spark_fillType - what direction do you want the fill to go?</li>
 * </ul>
 */
class SparkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), ScrubGestureDetector.ScrubListener {

    annotation class FillType {
        companion object {
            /**
             * Fill type constant for having no fill on the graph
             */
            const val NONE = 0

            /**
             * Fill type constant for always filling the area above the sparkline.
             */
            const val UP = 1

            /**
             * Fill type constant for always filling the area below the sparkline
             */
            const val DOWN = 2

            /**
             * Fill type constant for filling toward zero. This will fill downward if your sparkline is
             * positive, or upward if your sparkline is negative. If your sparkline intersects zero,
             * each segment will still color toward zero.
             */
            const val TOWARD_ZERO = 3
        }
    }


    @ColorInt private var lineColor: Int = -1
    @ColorInt private var fillColor: Int = -1
    private var lineWidth: Float = 0f
    private var cornerRadius: Float = 0f
    @FillType private var fillType = FillType.NONE
    private var scrubLineWidth: Float = 0.toFloat()
    @ColorInt private var scrubLineColor: Int = -1
    private var scrubEnabled: Boolean

    // the onDraw data
    private val renderPath = Path()
    private val sparkPath = Path()
    private val scrubLinePath = Path()

    // misc fields
    private lateinit var scaleHelper: ScaleHelper
    private val sparkLinePaint = Paint(ANTI_ALIAS_FLAG)
    private val sparkFillPaint = Paint(ANTI_ALIAS_FLAG)
    private val scrubLinePaint = Paint(ANTI_ALIAS_FLAG)
    private var scrubGestureDetector: ScrubGestureDetector
    var scrubListener: OnScrubListener? = null

    // adapter
    private var adapter: SparkAdapter? = null

    private val contentRect = RectF()

    private val xPoints: MutableList<Float> = mutableListOf()
    private val yPoints: MutableList<Float> = mutableListOf()
    val sparkLinePath : Path get() = Path(sparkPath)
    var sparkAnimator: SparkAnimator? = null
    val animator: Animator? get() = sparkAnimator?.getAnimator(this)
    private var pathAnimator: Animator? = null

    private val dataSetObserver = object : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()
            populatePath()
            sparkAnimator?.let { doPathAnimation() }
        }

        override fun onInvalidated() {
            super.onInvalidated()
            clearData()
        }
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SparkView)

        lineColor = typedArray.getColor(R.styleable.SparkView_spark_lineColor, 0)
        fillColor = typedArray.getColor(R.styleable.SparkView_spark_fillColor, 0)
        lineWidth = typedArray.getDimension(R.styleable.SparkView_spark_lineWidth, 0f)
        cornerRadius = typedArray.getDimension(R.styleable.SparkView_spark_cornerRadius, 0f)
        scrubLineColor = typedArray.getColor(R.styleable.SparkView_spark_scrubLineColor, 0)
        scrubLineWidth = typedArray.getDimension(R.styleable.SparkView_spark_scrubLineWidth, 0f)
        scrubEnabled = typedArray.getBoolean(R.styleable.SparkView_spark_scrubEnabled, false)

        val fillType = typedArray.getInt(R.styleable.SparkView_spark_fillType, FillType.NONE)
        setFillType(fillType)

        typedArray.recycle()

        sparkLinePaint.apply {
            style = Paint.Style.STROKE
            color = lineColor
            strokeWidth = lineWidth
            strokeCap = Paint.Cap.ROUND

            if (cornerRadius != 0f) {
                pathEffect = CornerPathEffect(cornerRadius)
            }
        }

        sparkFillPaint.apply {
            set(sparkLinePaint)
            color = fillColor
            style = Paint.Style.FILL
            strokeWidth = 0f
        }

        scrubLinePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = scrubLineWidth
            color = scrubLineColor
            strokeCap = Paint.Cap.ROUND
        }

        val handler = Handler()
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        scrubGestureDetector = ScrubGestureDetector(this, handler, touchSlop)
        scrubGestureDetector.setEnabled(scrubEnabled)
        setOnTouchListener(scrubGestureDetector)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        updateContentRect()
        populatePath()
    }

    /**
     * Set the [FillType] for the graph
     */
    fun setFillType(@FillType fillType: Int) {
        if (this.fillType != fillType) {
            this.fillType = fillType
            populatePath()
        }
    }

    /**
     * Sets the backing [SparkAdapter] to generate the points to be graphed
     */
    fun setAdapter(adapter: SparkAdapter?) {
        if (this.adapter != null) {
            this.adapter!!.unregisterDataSetObserver(dataSetObserver)
        }
        this.adapter = adapter
        if (this.adapter != null) {
            this.adapter!!.registerDataSetObserver(dataSetObserver)
        }
        populatePath()
    }

    /**
     * Set the path to animate in onDraw, used for getAnimator purposes
     */
    fun setAnimationPath(animationPath: Path) {
        this.renderPath.reset()
        this.renderPath.addPath(animationPath)
        this.renderPath.rLineTo(0f, 0f)

        invalidate()
    }

    private fun populatePath() {
        if (adapter == null || width == 0 || height == 0) {
            return
        }

        val adapterCount = adapter!!.count

        // to draw anything, we need 2 or more points
        if (adapterCount < 2) {
            clearData()
            return
        }

        scaleHelper = ScaleHelper(adapter!!, contentRect, lineWidth, true)

        xPoints.clear()
        yPoints.clear()

        // restart the path
        sparkPath.reset()

        for (i in 0 until adapterCount) {
            val x = scaleHelper.getX(i.toFloat())
            val y = scaleHelper.getY(adapter!!.getY(i))

            xPoints.add(x)
            yPoints.add(y)

            if (i == 0) {
                sparkPath.moveTo(x, y)
            } else {
                sparkPath.lineTo(x, y)
            }
        }

        val fillEdge = getFillEdge()
        if (fillEdge != null) {
            val lastX = scaleHelper.getX(adapterCount - 1f)
            sparkPath.lineTo(lastX, fillEdge)
            sparkPath.lineTo(paddingStart.toFloat(), fillEdge)
            sparkPath.close()
        }

        renderPath.reset()
        renderPath.addPath(sparkPath)

        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.apply {
            if (fillType != FillType.NONE) {
                drawPath(renderPath, sparkFillPaint)
            }

            drawPath(renderPath, sparkLinePaint)
            drawPath(scrubLinePath, scrubLinePaint)
        }
    }

    override fun onScrubbed(x: Float, y: Float) {
        if (adapter != null && adapter!!.count == 0) return
        scrubListener?.let {
            parent.requestDisallowInterceptTouchEvent(true)
            val index = getNearestIndex(xPoints, x)
            it.onScrubbed(adapter!!.dataSet[index])
        }

        setScrubLine(x)
    }

    override  fun onScrubEnded() {
        scrubLinePath.reset()
        if (scrubListener != null) scrubListener!!.onScrubbed(null)
        invalidate()
    }


    private fun clearData() {
        renderPath.reset()
        sparkPath.reset()
        invalidate()
    }

    private fun updateContentRect() {
        contentRect.set(
            paddingStart.toFloat(),
            paddingTop.toFloat(),
            (width - paddingEnd).toFloat(),
            (height - paddingBottom).toFloat()
        )
    }

    private fun getFillEdge(): Float? {
        return when (fillType) {
            FillType.NONE -> null
            FillType.UP -> paddingTop.toFloat()
            FillType.DOWN -> height.toFloat() - paddingBottom
            FillType.TOWARD_ZERO -> {
                val zero = scaleHelper.getY(0f)
                val bottom = height.toFloat() - paddingBottom
                Math.min(zero, bottom)
            }
            else -> throw IllegalStateException("Unknown fill-type: $fillType")
        }
    }

    private fun doPathAnimation() {
        pathAnimator?.cancel()
        pathAnimator = animator
        pathAnimator?.start()
    }

    private fun setScrubLine(x: Float) {
        val xPosition = resolveBoundedScrubLine(x)
        scrubLinePath.reset()
        scrubLinePath.moveTo(xPosition, paddingTop.toFloat())
        scrubLinePath.lineTo(xPosition, (height - paddingBottom).toFloat())
        invalidate()
    }

    /**
     * Bounds the x coordinate of a scrub within the bounding rect minus padding and line width.
     */
    private fun resolveBoundedScrubLine(x: Float): Float {
        val scrubLineOffset = scrubLineWidth / 2

        val leftBound = paddingStart + scrubLineOffset
        if (x < leftBound) {
            return leftBound
        }

        val rightBound = width.toFloat() - paddingEnd.toFloat() - scrubLineOffset
        return if (x > rightBound) {
            rightBound
        } else x

    }

    /**
     * Listener for a user scrubbing (dragging their finger along) the graph.
     */
    interface OnScrubListener {
        /**
         * Indicates the user is currently scrubbing over the given value. A null value indicates
         * that the user has stopped scrubbing.
         */
        fun onScrubbed(value: Any?)
    }

    companion object {
        /**
         * Class for helping handle scaling logic
         */
        class ScaleHelper(adapter: SparkAdapter, contentRect: RectF, lineWidth: Float, fill: Boolean) {
            var width: Float = 0f
            var height: Float = 0f
            var size: Int = 0

            var xScale: Float = 0f
            var yScale: Float = 0f

            var xTranslation: Float = 0f
            var yTranslation: Float = 0f

            init {
                val leftPadding = contentRect.left
                val topPadding = contentRect.top
                val lineWidthOffset = if (fill) {
                    0f
                } else {
                    lineWidth
                }
                width = contentRect.width() - lineWidthOffset
                height = contentRect.height() - lineWidthOffset

                size = adapter.count

                val bounds = adapter.getDataBounds()

                val dx = if (bounds.width() == 0f) {
                    -1f
                } else {
                    0f
                }
                val dy = if (bounds.height() == 0f) {
                    -1f
                } else {
                    0f
                }
                bounds.inset(dx, dy)

                val minX = bounds.left
                val maxX = bounds.right
                val minY = bounds.top
                val maxY = bounds.bottom

                // xScale compresses or expands the x values to be inside the view
                xScale = width / (maxX - minX)

                // xTranslation will the x points within the appropriate bounds
                xTranslation = leftPadding - (minX * xScale) + (lineWidthOffset / 2)

                // yScale will compress or expand the y values
                yScale = height / (maxY - minY)

                // yTranslation will move y into the view
                yTranslation = minY * yScale + topPadding + (lineWidthOffset / 2)
            }

            /**
             * Given the "raw" X value, scale it to fit within our view
             */
            fun getX(rawX: Float): Float {
                return rawX * xScale + xTranslation
            }

            /**
             * Given the "raw" Y value, scale it to fit within our view.
             */
            fun getY(rawY: Float): Float {
                return height - (rawY * yScale) + yTranslation
            }
        }

        /**
         * returns the nearest index (into [.adapter]'s data) for the given x coordinate.
         */
        internal fun getNearestIndex(points: List<Float>, x: Float): Int {
            var index = Collections.binarySearch(points, x)

            // if binary search returns positive, we had an exact match, return that index
            if (index >= 0) return index

            // otherwise, calculate the binary search's specified insertion index
            index = -1 - index

            // if we're inserting at 0, then our guaranteed nearest index is 0
            if (index == 0) return index

            // if we're inserting at the very end, then our guaranteed nearest index is the final one
            if (index == points.size) return --index

            // otherwise we need to check which of our two neighbors we're closer to
            val deltaUp = points[index] - x
            val deltaDown = x - points[index - 1]
            if (deltaUp > deltaDown) {
                // if the below neighbor is closer, decrement our index
                index--
            }

            return index
        }
    }
}
