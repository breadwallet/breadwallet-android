package com.breadwallet.presenter.activities;

import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.fragments.FragmentAbout;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.presenter.fragments.FragmentRecoveryPhrase;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.FragmentWipeWallet;
import com.breadwallet.presenter.fragments.MainFragmentDecoder;
import com.breadwallet.presenter.fragments.MainFragmentSettingsAll;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.ParallaxViewPager;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.others.CurrencyManager;

import java.util.HashMap;
import java.util.Map;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 8/4/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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

public class MainActivity extends FragmentActivity {
    public static final String TAG = "MainActivity";
    public static final String PREFS_NAME = "MyPrefsFile";
    public static MainActivity app;
    public static boolean decoderFragmentOn;
    public static boolean scanResultFragmentOn;
    public CustomPagerAdapter pagerAdapter;
    public static RelativeLayout pageIndicator;
    public ImageView pageIndicatorLeft;
    public ImageView pageIndicatorRight;
    public Map<String, Integer> burgerButtonMap;
    public Button burgerButton;
    public Button locker;
    public MainFragmentSettingsAll mainFragmentSettingsAll;
    public static ParallaxViewPager parallaxViewPager;
    public FragmentSettings fragmentSettings;
    public FragmentAbout fragmentAbout;
    public MainFragmentDecoder mainFragmentDecoder;
    public ClipboardManager myClipboard;
    public FragmentCurrency fragmentCurrency;
    public FragmentRecoveryPhrase fragmentRecoveryPhrase;
    public FragmentWipeWallet fragmentWipeWallet;
    public RelativeLayout burgerButtonLayout;
    public FragmentScanResult fragmentScanResult;
    private boolean onBackPressedAvailable = true;
    private boolean doubleBackToExitPressedOnce;
    public static final int BURGER = 0;
    public static final int CLOSE = 1;
    public static final int BACK = 2;
    public static final float PAGE_INDICATOR_SCALE_UP = 1.3f;
    public static boolean beenThroughSavedInstanceMethod = false;

    /**
     * Public constructor used to assign the current instance to the app variable
     */

    public MainActivity() {
        app = this;
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        beenThroughSavedInstanceMethod = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e(TAG, "Activity created!");
        if (savedInstanceState != null) {
            return;
        }
        initializeViews();
        burgerButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(burgerButton);
                if (FragmentAnimator.level > 1 || scanResultFragmentOn) {
                    Log.e(TAG, "CHECK:Should press back!");
                    app.onBackPressed();
                } else {
                    FragmentAnimator.pressMenuButton(app, mainFragmentSettingsAll);
                    Log.e(TAG, "CHECK:Should press menu");
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        CurrencyManager.startTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        CurrencyManager.stoptimertask();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
        CurrencyManager.stoptimertask();
        Log.e(TAG, "Activity Destroyed!");
    }

    /**
     * Initializes all the views and components
     */

    private void initializeViews() {
        burgerButtonLayout = (RelativeLayout) findViewById(R.id.burgerbuttonlayout);
        burgerButton = (Button) findViewById(R.id.mainbuttonburger);
        locker = (Button) findViewById(R.id.mainbuttonlocker);
        pageIndicator = (RelativeLayout) findViewById(R.id.pager_indicator);
        pageIndicatorLeft = (ImageView) findViewById(R.id.circle_indicator_left);

        pageIndicatorRight = (ImageView) findViewById(R.id.circle_indicator_right);
        pagerAdapter = new CustomPagerAdapter(getSupportFragmentManager());
        burgerButtonMap = new HashMap<>();
        fragmentSettings = new FragmentSettings();
        mainFragmentSettingsAll = new MainFragmentSettingsAll();
        mainFragmentDecoder = new MainFragmentDecoder();
        fragmentAbout = new FragmentAbout();
        fragmentCurrency = new FragmentCurrency();
        fragmentRecoveryPhrase = new FragmentRecoveryPhrase();
        fragmentWipeWallet = new FragmentWipeWallet();
        fragmentScanResult = new FragmentScanResult();
        parallaxViewPager = ((ParallaxViewPager) findViewById(R.id.pager));
        parallaxViewPager
                .setOverlapPercentage(0.99f)
                .setAdapter(pagerAdapter);
        parallaxViewPager.setBackgroundResource(R.drawable.backgroundmain);
        burgerButtonMap.put("burger", R.drawable.burger);
        burgerButtonMap.put("close", R.drawable.x);
        burgerButtonMap.put("back", R.drawable.navigationback);
        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        scaleView(pageIndicatorLeft, 1f, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (FragmentAnimator.level > 1) {
                this.onBackPressed();
            } else {
                if(scanResultFragmentOn){
                    app.onBackPressed();
                }
                FragmentAnimator.pressMenuButton(app, mainFragmentSettingsAll);
            }
            return true;
        }
        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        Log.e(TAG, "onBackPressed!");
        if (onBackPressedAvailable) {
            onBackPressedAvailable = false;
            makeOnBackPressedAvailable(300);

            //old implimentation, keep in case the new ones fail
//            if (!FragmentAnimator.wipeWalletOpen) {
//                if (FragmentAnimator.level == 0) {
//                    if (doubleBackToExitPressedOnce) {
//                        super.onBackPressed();
//                        return;
//                    }
//                    if (decoderFragmentOn) {
//                        FragmentAnimator.hideDecoderFragment();
//                    } else {
//                        this.doubleBackToExitPressedOnce = true;
//                        ((BreadWalletApp) getApplicationContext()).showCustomToast(this,
//                                getResources().getString(R.string.mainactivity_press_back_again), 100, Toast.LENGTH_SHORT);
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                doubleBackToExitPressedOnce = false;
//                            }
//                        }, 1000);
//                    }
//                } else if (FragmentAnimator.level == 1) {
//                    FragmentAnimator.pressMenuButton(this, mainFragmentSettingsAll);
//                    if (FragmentAnimator.multiplePressingAvailable)
//                        FragmentAnimator.hideDecoderFragment();
//                } else {
//                    FragmentAnimator.animateSlideToRight(this);
//                }
//            } else {
//                FragmentAnimator.pressWipeWallet(this, fragmentSettings);
//                activityButtonsEnable(true);
//            }

            if (FragmentAnimator.wipeWalletOpen) {
                FragmentAnimator.pressWipeWallet(this, fragmentSettings);
                activityButtonsEnable(true);
                return;
            }
            //switch the level of fragments creation.
            switch (FragmentAnimator.level) {
                case 0:
                    if (doubleBackToExitPressedOnce) {
                        super.onBackPressed();
                        break;
                    }
                    if (decoderFragmentOn) {
                        FragmentAnimator.hideDecoderFragment();
                        break;
                    }
                    if (scanResultFragmentOn) {
                        FragmentAnimator.hideScanResultFragment();
                        break;
                    }
                    this.doubleBackToExitPressedOnce = true;
                    ((BreadWalletApp) getApplicationContext()).showCustomToast(this,
                            getResources().getString(R.string.mainactivity_press_back_again), 140, Toast.LENGTH_SHORT);
                    makeDoubleBackToExitPressedOnce(1000);

                    break;
                case 1:
                    FragmentAnimator.pressMenuButton(this, mainFragmentSettingsAll);
                    if (FragmentAnimator.multiplePressingAvailable)
                        FragmentAnimator.hideDecoderFragment();
                    break;
                default:
                    FragmentAnimator.animateSlideToRight(this);
                    break;
            }
        }
    }

    /**
     * Sets the little circle indicator to the selected page
     *
     * @patam x The page for the indicator to be shown
     */
    public void setPagerIndicator(int x) {
        if (x == 0) {
            Log.d(TAG, "Left Indicator changed");
            pageIndicatorLeft.setImageResource(R.drawable.circle_indicator_active);
            pageIndicatorRight.setImageResource(R.drawable.circle_indicator);
            scaleView(pageIndicatorLeft, 1f, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP);
            scaleView(pageIndicatorRight, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP, 1f);
        } else if (x == 1) {
            Log.d(TAG, "Right Indicator changed");
            pageIndicatorLeft.setImageResource(R.drawable.circle_indicator);
            pageIndicatorRight.setImageResource(R.drawable.circle_indicator_active);
            scaleView(pageIndicatorRight, 1f, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP);
            scaleView(pageIndicatorLeft, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP, 1f);
        } else {
            Log.e(TAG, "Something went wrong setting the circle pageIndicator");
        }
    }

    public void setBurgerButtonImage(int x) {
        String item = null;
        switch (x) {
            case 0:
                item = "burger";
                break;
            case 1:
                item = "close";
                break;
            case 2:
                item = "back";
                break;
        }
        if (item != null && item.length() > 0)
            burgerButton.setBackgroundResource(burgerButtonMap.get(item));
    }

    public void activityButtonsEnable(boolean b) {
        if (b) {
            parallaxViewPager.setClickable(b);
            burgerButton.setVisibility(View.VISIBLE);
            burgerButton.setClickable(b);
            locker.setVisibility(View.VISIBLE);
            locker.setClickable(b);
            burgerButtonLayout.setVisibility(View.VISIBLE);
            burgerButtonLayout.setClickable(b);
        } else {
            parallaxViewPager.setClickable(b);
            burgerButton.setVisibility(View.GONE);
            burgerButton.setClickable(b);
            locker.setVisibility(View.GONE);
            locker.setClickable(b);
            burgerButtonLayout.setVisibility(View.GONE);
            burgerButtonLayout.setClickable(b);
        }
    }

    public void scaleView(View v, float startScaleX, float endScaleX, float startScaleY, float endScaleY) {
        Animation anim = new ScaleAnimation(
                startScaleX, endScaleX, // Start and end values for the X axis scaling
                startScaleY, endScaleY, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        v.startAnimation(anim);
    }

    private void makeOnBackPressedAvailable(int ms) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                onBackPressedAvailable = true;
            }
        }, ms);
    }

    private void makeDoubleBackToExitPressedOnce(int ms) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, ms);
    }
}
