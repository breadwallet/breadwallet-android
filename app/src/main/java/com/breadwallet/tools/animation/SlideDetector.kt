/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/25/19.
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
package com.breadwallet.tools.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import com.bluelinelabs.conductor.Router

@Suppress("MagicNumber")
class SlideDetector(private val root: View) : View.OnTouchListener {

    constructor(router: Router, root: View) : this(root) {
        this.router = router
    }

    private var router: Router? = null

    private var origY: Float = 0f
    private var dY: Float = 0f

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                origY = root.y
                dY = root.y - event.rawY
            }
            MotionEvent.ACTION_MOVE -> if (event.rawY + dY > origY)
                root.animate()
                    .y(event.rawY + dY)
                    .setDuration(0)
                    .start()
            MotionEvent.ACTION_UP -> if (root.y > origY + root.height / 5) {
                root.animate()
                    .y((root.height * 2).toFloat())
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator(0.5f))
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            removeCurrentView()
                        }
                    })
                    .start()
            } else {
                root.animate()
                    .y(origY)
                    .setDuration(100)
                    .setInterpolator(OvershootInterpolator(0.5f))
                    .start()
            }
            else -> return false
        }
        return true
    }

    private fun removeCurrentView() {
        router?.popCurrentController()
    }
}
