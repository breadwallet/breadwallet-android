/**
 * BreadWallet
 * <p/>
 * Created by Mihail on <Mihail@brd.com> 2/13/19.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.ui.wallet.WalletActivity;

import java.io.File;

/**
 * Sub class of Wallet Activity for additional views needed.
 */
public class BrdWalletActivity extends WalletActivity {
    private static final Uri CONFETTI_VIDEO_URI = Uri.parse("android.resource://"
            + BuildConfig.APPLICATION_ID + File.separator + R.raw.confetti);
    private static final int UNINITIALIZED_POSITION = -1;
    private AppBarLayout mAppBarLayoutRoot;

    // The view was expanded and ready to lock when collapsed again.
    private boolean mCanScroll = false;

    /**
     * Start the wallet activity for the given currency.
     *
     * @param callerActivity Activity from where WalletActivity is started.
     * @param currencyCode   The currency code of the wallet to be shown.
     */
    public static void start(Activity callerActivity, String currencyCode) {
        Intent intent = new Intent(callerActivity, BrdWalletActivity.class);
        intent.putExtra(EXTRA_CURRENCY_CODE, currencyCode);
        callerActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppBarLayoutRoot = (AppBarLayout) getLayoutInflater().inflate(R.layout.rewards_announcement_view, null);
        final CoordinatorLayout coordinatorLayout = findViewById(R.id.transaction_list_coordinator_layout);
        coordinatorLayout.addView(mAppBarLayoutRoot, 0);
        final AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        final ViewGroup expandedRewardsView = findViewById(R.id.expanded_rewards_layout);
        final ViewGroup collapsedRewardsView = findViewById(R.id.collapsed_rewards_toolbar);
        final RecyclerView transactionListRecyclerView = findViewById(R.id.tx_list);
        mAppBarLayoutRoot.setOnClickListener(view -> {
            //Collapse without animation before showing the rewards webview.
            EventUtils.pushEvent(EventUtils.EVENT_REWARDS_BANNER);
            appBarLayout.setExpanded(false, false);
            UiUtils.openRewardsWebView(BrdWalletActivity.this);
        });

        if (!BRSharedPrefs.getRewardsAnimationShown(BrdWalletActivity.this)) {
            appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                private int mScrollRange = UNINITIALIZED_POSITION;
                private int mTotalDistance = 0;

                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                    if (mScrollRange == UNINITIALIZED_POSITION) {
                        mScrollRange = appBarLayout.getTotalScrollRange();
                    }
                    int distance = mScrollRange + verticalOffset;
                    if (verticalOffset == 0) {
                        mTotalDistance = distance;
                        mCanScroll = true;
                    } else if (distance != 0) {
                        // The alpha to use for the expanded layout (e.g. 0.2f).
                        float expandedAlpha = ((float) distance / (float) mTotalDistance) / 2;
                        // The alpha to use for the collapsed layout (the opposite of expanded alpha).
                        float collapsedAlpha = 1f - expandedAlpha;
                        expandedRewardsView.setAlpha(expandedAlpha / 2);
                        collapsedRewardsView.setAlpha(collapsedAlpha);
                    }
                    if (distance == 0 && mCanScroll) {
                        // Fully collapsed.
                        collapsedRewardsView.setAlpha(1f);
                        expandedRewardsView.setAlpha(0f);
                        lockRewardsViewToCollapsed(appBarLayout, transactionListRecyclerView);
                    }
                }
            });
            final VideoView videoView = findViewById(R.id.confetti_video_view);

            // Prepares and plays the confetti brd video.
            videoView.setVideoURI(CONFETTI_VIDEO_URI);
            videoView.start();
            videoView.setOnPreparedListener(mediaPlayer ->
                    mediaPlayer.setOnInfoListener((sourceMediaPlayer, what, extra) -> {
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            BRSharedPrefs.putRewardsAnimationShown(BrdWalletActivity.this, true);
                            appBarLayout.setExpanded(true, true);
                            return true;
                        }
                        return false;
                    }));
        } else {
            lockRewardsViewToCollapsed(appBarLayout, transactionListRecyclerView);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAppBarLayoutRoot.setExpanded(false, false);
    }

    /**
     * Locks the AppBarLayout to collapsed and does not allow expanding.
     *
     * @param appBarLayout                The AppBarLayout to lock
     * @param transactionListRecyclerView The list view that affects the AppBarLayout while scrolling.
     */
    private void lockRewardsViewToCollapsed(AppBarLayout appBarLayout,
                                            RecyclerView transactionListRecyclerView) {
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
        if (behavior == null) {
            behavior = new AppBarLayout.Behavior();
            params.setBehavior(behavior);
            appBarLayout.setLayoutParams(params);
        }
        behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
            @Override
            public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                return false;
            }
        });
        transactionListRecyclerView.setNestedScrollingEnabled(false);
    }
}
