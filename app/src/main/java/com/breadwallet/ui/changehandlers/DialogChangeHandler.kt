package com.breadwallet.ui.changehandlers

import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler

/** Adds a transparent scrim to the previous view and fades in the new view vertically. */
class DialogChangeHandler : FadeChangeHandler(ANIMATION_DURATION, false) {

    companion object {
        private const val ANIMATION_DURATION = 250L
    }

    override fun getAnimator(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        toAddedToContainer: Boolean
    ) = animatorWithScrim(
        super.getAnimator(container, from, to, isPush, toAddedToContainer),
        from,
        to,
        isPush
    )
}
