package com.breadwallet.ui.changehandlers

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.breadwallet.R

/** The default scrim color resource id. */
private const val DEFAULT_SCRIM_COLOR_RES = R.color.black_trans

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
