/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 11/14/18.
 * Copyright (c) 2018 breadwallet LLC
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
package com.breadwallet.presenter.activities.intro;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.breadwallet.R;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.fragments.FragmentOnBoarding;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.platform.HTTPServer;

import java.util.ArrayList;
import java.util.List;

/**
 * New on boarding activity
 */

public class OnBoardingActivity extends BRActivity {
    private static final String TAG = OnBoardingActivity.class.getSimpleName();
    private List<View> mIndicators = new ArrayList<>();
    private ImageButton mBackButton;
    private Button mSkipButton;

    private static NextScreen mNextScreen;

    public enum NextScreen {
        BUY_SCREEN,
        HOME_SCREEN
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);
        final ViewPager viewPager = findViewById(R.id.view_pager);
        mIndicators.add(findViewById(R.id.indicator1));
        mIndicators.add(findViewById(R.id.indicator2));
        mIndicators.add(findViewById(R.id.indicator3));
        mBackButton = findViewById(R.id.button_back);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mSkipButton = findViewById(R.id.button_skip);
        mSkipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(OnBoardingPagerAdapter.COUNT - 1);
            }
        });

        OnBoardingPagerAdapter onBoardingPagerAdapter = new OnBoardingPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(onBoardingPagerAdapter);
        // Attach the page change listener inside the activity
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            // This method will be invoked when a new page becomes selected.
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    showHideToolBarButtons(true);
                } else {
                    showHideToolBarButtons(false);
                }
                setActiveIndicator(position);
            }

            // This method will be invoked when the current page is scrolled
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            // Called when the scroll state changes:
            // SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    public static void showBuyScreen(Activity activity) {
        String url = String.format(BRConstants.CURRENCY_PARAMETER_STRING_FORMAT, HTTPServer.URL_BUY,
                WalletBitcoinManager.getInstance(activity).getCurrencyCode());
        UiUtils.startWebActivity(activity, url);
    }

    private void showHideToolBarButtons(boolean show) {
        mSkipButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        mBackButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == InputPinActivity.SET_PIN_REQUEST_CODE) {
            if (data != null) {
                boolean isPinAccepted = data.getBooleanExtra(InputPinActivity.EXTRA_PIN_ACCEPTED, false);
                if (isPinAccepted) {
                    switch (mNextScreen) {
                        case BUY_SCREEN:
                            showBuyScreen(this);
                            break;
                        case HOME_SCREEN:
                            UiUtils.startBreadActivity(this, false);
                            break;
                    }
                }
            }
        }
    }

    private void setActiveIndicator(int position) {
        for (int i = 0; i < mIndicators.size(); i++) {
            View view = mIndicators.get(i);
            view.setBackground(getDrawable(i == position ? R.drawable.page_indicator_active : R.drawable.page_indicator_inactive));
        }
    }

    public static void setNextScreen(NextScreen nextScreen) {
        mNextScreen = nextScreen;
    }

    public static class OnBoardingPagerAdapter extends FragmentPagerAdapter {
        private static final int COUNT = 3;

        OnBoardingPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return COUNT;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            return FragmentOnBoarding.newInstance(position);
        }

    }

}

