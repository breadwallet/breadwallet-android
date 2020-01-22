/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/18/19.
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
package com.breadwallet.ui.wallet

import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import com.google.android.material.appbar.AppBarLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import androidx.core.os.bundleOf

import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.EventUtils
import kotlinx.android.synthetic.main.controller_wallet.*
import kotlinx.android.synthetic.main.rewards_announcement_view.*

import java.io.File

/**
 * TODO: All of this should be merged into [WalletController].
 */
class BrdWalletController : WalletController("BRD") {
    private var mAppBarLayoutRoot: AppBarLayout? = null
    private val mCollapseRewardsDelayHandler = Handler()
    private var mCollapseRewardsRunnable: Runnable? = null

    // The view was expanded and ready to lock when collapsed again.
    private var mCanScroll = false

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        mAppBarLayoutRoot = activity!!.layoutInflater.inflate(
            R.layout.rewards_announcement_view,
            null
        ) as AppBarLayout
        transaction_list_coordinator_layout.addView(mAppBarLayoutRoot, 0)
        lockRewardsViewToCollapsed(mAppBarLayoutRoot!!, tx_list)
        mAppBarLayoutRoot!!.setOnClickListener {
            //Collapse without animation before showing the rewards webview.
            EventUtils.pushEvent(EventUtils.EVENT_REWARDS_BANNER)
            mAppBarLayoutRoot!!.setExpanded(false, true)
            UiUtils.openRewardsWebView(activity)
        }

        if (!BRSharedPrefs.getRewardsAnimationShown()) {
            mAppBarLayoutRoot!!.addOnOffsetChangedListener(object :
                AppBarLayout.OnOffsetChangedListener {
                private var mScrollRange =
                    UNINITIALIZED_POSITION
                private var mTotalDistance = 0

                override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                    if (mScrollRange == UNINITIALIZED_POSITION) {
                        mScrollRange = appBarLayout.totalScrollRange
                    }
                    val distance = mScrollRange + verticalOffset
                    if (verticalOffset == 0) {
                        mTotalDistance = distance
                        mCanScroll = true
                    } else if (distance != 0) {
                        // The alpha to use for the expanded layout (e.g. 0.2f).
                        val expandedAlpha = distance.toFloat() / mTotalDistance.toFloat() / 2
                        // The alpha to use for the collapsed layout (the opposite of expanded alpha).
                        val collapsedAlpha = 1f - expandedAlpha
                        expanded_rewards_layout.alpha = expandedAlpha / 2
                        collapsed_rewards_toolbar.alpha = collapsedAlpha
                    }
                    if (distance == 0 && mCanScroll) {
                        // Fully collapsed.
                        collapsed_rewards_toolbar.alpha = 1f
                        expanded_rewards_layout.alpha = 0f
                    }
                }
            })

            // Prepares and plays the confetti brd video.
            confetti_video_view.setVideoURI(CONFETTI_VIDEO_URI)
            confetti_video_view.start()
            confetti_video_view.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.setOnInfoListener { _, what, _ ->
                    when (what) {
                        MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                            BRSharedPrefs.putRewardsAnimationShown(wasShown = true)
                            mAppBarLayoutRoot!!.setExpanded(true, true)
                            true
                        }
                        else -> false
                    }
                }
            }
            mCollapseRewardsRunnable = Runnable {
                mAppBarLayoutRoot!!.setExpanded(false, true)
            }
            mCollapseRewardsDelayHandler.postDelayed(
                mCollapseRewardsRunnable,
                COLLAPSE_REWARDS_DELAY_MILLISECONDS.toLong()
            )
        }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        if (mCollapseRewardsRunnable != null) {
            mCollapseRewardsDelayHandler.removeCallbacks(mCollapseRewardsRunnable)
        }
        mAppBarLayoutRoot!!.setExpanded(false, false)
    }

    /**
     * Locks the AppBarLayout to collapsed and does not allow expanding.
     *
     * @param appBarLayout                The AppBarLayout to lock
     * @param transactionListRecyclerView The list view that affects the AppBarLayout while scrolling.
     */
    private fun lockRewardsViewToCollapsed(
        appBarLayout: AppBarLayout,
        transactionListRecyclerView: RecyclerView
    ) {
        val params = appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
        var behavior = params.behavior as AppBarLayout.Behavior?
        if (behavior == null) {
            behavior = AppBarLayout.Behavior()
            params.behavior = behavior
            appBarLayout.layoutParams = params
        }
        behavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return false
            }
        })
        transactionListRecyclerView.isNestedScrollingEnabled = false
    }

    companion object {
        private val CONFETTI_VIDEO_URI = Uri.parse(
            "android.resource://"
                + BuildConfig.APPLICATION_ID + File.separator + R.raw.confetti
        )
        private const val UNINITIALIZED_POSITION = -1
        private const val COLLAPSE_REWARDS_DELAY_MILLISECONDS = 6000
    }
}
