/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/11/19.
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
package com.breadwallet.ui.changehandlers

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.breadwallet.theme.R

/** The default scrim color resource id. */
private val DEFAULT_SCRIM_COLOR_RES = R.color.black_trans

/** The default scrim fade animation duration. */
private const val DEFAULT_SCRIM_FADE_DURATION = 250L

/**
 * Returns an [Animator] that will play [primaryAnimator] and
 * animate the background [View]'s foreground color from nothing
 * to [scrimColorRes] ([DEFAULT_SCRIM_COLOR_RES] by default).
 * The reverse is applied for a pop operation.
 *
 * If a target background view does not exist, [primaryAnimator]
 * is returned and no scrim will be modified.
 */
@Suppress("LongParameterList")
fun animatorWithScrim(
    primaryAnimator: Animator,
    from: View?,
    to: View?,
    isPush: Boolean,
    scrimColorRes: Int = DEFAULT_SCRIM_COLOR_RES,
    duration: Long = DEFAULT_SCRIM_FADE_DURATION
): Animator = AnimatorSet().apply {
    this.duration = duration
    val context = (from?.context ?: to?.context)!!
    val transparentColor = context.getColor(scrimColorRes)
    val scrimTarget = (if (isPush) from else to) ?: return primaryAnimator
    val animator = when {
        isPush -> ValueAnimator.ofArgb(0, transparentColor)
        else -> ValueAnimator.ofArgb(transparentColor, 0)
    }
    animator.addUpdateListener { anim ->
        val colorValue = anim.animatedValue as Int
        when (val drawable = scrimTarget.foreground) {
            is ColorDrawable -> drawable.color = colorValue
            else -> scrimTarget.foreground = ColorDrawable(colorValue)
        }
    }
    playTogether(animator, primaryAnimator)
}
