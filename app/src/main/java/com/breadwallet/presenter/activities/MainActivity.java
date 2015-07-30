package com.breadwallet.presenter.activities;

import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.fragments.MainFragmentDecoder;
import com.breadwallet.presenter.fragments.MainFragmentSettingsAll;
import com.breadwallet.presenter.fragments.allsettings.FragmentSettings;
import com.breadwallet.presenter.fragments.allsettings.settings.FragmentAbout;
import com.breadwallet.presenter.fragments.allsettings.settings.FragmentCurrency;
import com.breadwallet.presenter.fragments.allsettings.settings.FragmentRecoveryPhrase;
import com.breadwallet.presenter.fragments.allsettings.settings.FragmentWipeWallet;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.ParallaxViewPager;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.others.currency.CurrencyManager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity {
    public static final String TAG = "MainActivity";
    public static final String PREFS_NAME = "MyPrefsFile";
    public static MainActivity app;
    public static boolean decoderFragmentOn;
    public CustomPagerAdapter pagerAdapter;
    public ImageView pageIndicator;
    public Map<String, Integer> indicatorMap;
    public Button burgerButton;
    public Button locker;
    public MainFragmentSettingsAll mainFragmentSettingsAll;
    public ParallaxViewPager parallaxViewPager;
    public FragmentSettings fragmentSettings;
    public FragmentAbout fragmentAbout;
    public MainFragmentDecoder mainFragmentDecoder;
    public ClipboardManager myClipboard;
    public FragmentCurrency fragmentCurrency;
    public FragmentRecoveryPhrase fragmentRecoveryPhrase;
    public FragmentWipeWallet fragmentWipeWallet;
    public RelativeLayout burgerButtonLayout;
    private boolean onBackPressedAvailable = true;
    private boolean doubleBackToExitPressedOnce;

    /**
     * Public constructor used to assign the current instance to the app variable
     */
    public MainActivity() {
        app = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        burgerButtonLayout.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d(TAG, "CLicked on the burgerLayoutHelper!");
                SpringAnimator.showAnimation(burgerButton);
                FragmentAnimator.pressMenuButton(MainActivity.app, MainActivity.app.mainFragmentSettingsAll);
                return true;
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
        Log.d(TAG, "The burger button's id: " + burgerButton.getId());
        pageIndicator = (ImageView) findViewById(R.id.pagerindicator);
        pagerAdapter = new CustomPagerAdapter(getSupportFragmentManager());
        indicatorMap = new HashMap<>();
        fragmentSettings = new FragmentSettings();
        mainFragmentSettingsAll = new MainFragmentSettingsAll();
        mainFragmentDecoder = new MainFragmentDecoder();
        fragmentAbout = new FragmentAbout();
        fragmentCurrency = new FragmentCurrency();
        fragmentRecoveryPhrase = new FragmentRecoveryPhrase();
        fragmentWipeWallet = new FragmentWipeWallet();
        parallaxViewPager = ((ParallaxViewPager) findViewById(R.id.pager));
        parallaxViewPager
                .setOverlapPercentage(0.99f)
                .setAdapter(pagerAdapter);
        parallaxViewPager.setBackgroundResource(R.drawable.backgroundmain);
        indicatorMap.put("left", R.drawable.pageindicatorleft);
        indicatorMap.put("right", R.drawable.pageindicatorright);
        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (FragmentAnimator.multiplePressingAvailable) {
                FragmentAnimator.pauseTheAnimationAvailabilityFor(300);
                FragmentAnimator.pressMenuButton(app, mainFragmentSettingsAll);
                // return 'true' to prevent further propagation of the key event
                return true;
            }
        }

        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (onBackPressedAvailable) {
            onBackPressedAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onBackPressedAvailable = true;
                }
            }, 300);
            if (!FragmentAnimator.wipeWalletOpen) {
                if (FragmentAnimator.level == 0) {
                    if (doubleBackToExitPressedOnce) {
                        super.onBackPressed();
                        return;
                    }
                    if (decoderFragmentOn) {
                        hideDecoderFragment();
                    } else {
                        this.doubleBackToExitPressedOnce = true;
                        ((BreadWalletApp) getApplicationContext()).showCustomToast(this,
                                "Press again to exit!", 100, Toast.LENGTH_SHORT);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                doubleBackToExitPressedOnce = false;
                            }
                        }, 1000);
                    }
                } else if (FragmentAnimator.level == 1) {
                    FragmentAnimator.pressMenuButton(this, mainFragmentSettingsAll);
                    if (FragmentAnimator.multiplePressingAvailable)
                        hideDecoderFragment();
                } else {
                    FragmentAnimator.animateSlideToRight(this);
                }
            } else {
                FragmentAnimator.pressWipeWallet(this, fragmentSettings);
                activityButtonsEnable(true);
            }
        }
    }

    public void hideDecoderFragment() {
        CustomPagerAdapter.adapter.showFragments(true);
        decoderFragmentOn = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentAnimator.multiplePressingAvailable = true;
            }
        }, 300);
        getSupportFragmentManager().beginTransaction().
                setCustomAnimations(R.animator.from_top, R.animator.to_bottom).
                remove(mainFragmentDecoder).commit();

    }

    /**
     * Sets the little circle indicator to the selected page
     *
     * @patam x The page for the indicator to be shown
     */
    public void setPagerIndicator(int x) {

        String item = (x == 0) ? "left" : "right";
//        Log.d(TAG, "The item is: " + item);
        pageIndicator.setImageResource(indicatorMap.get(item));
    }

    public void activityButtonsEnable(boolean b) {
        if (b) {
            parallaxViewPager.setClickable(true);
            burgerButton.setVisibility(View.VISIBLE);
            locker.setVisibility(View.VISIBLE);
        } else {
            parallaxViewPager.setClickable(false);
            burgerButton.setVisibility(View.GONE);
            locker.setVisibility(View.GONE);
        }
    }
}
