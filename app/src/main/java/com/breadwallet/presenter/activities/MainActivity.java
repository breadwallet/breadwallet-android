package com.breadwallet.presenter.activities;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.fingerprint.FingerprintManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.fragments.FragmentAbout;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.presenter.fragments.FragmentDecoder;
import com.breadwallet.presenter.fragments.FragmentRecoveryPhrase;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.FragmentWipeWallet;
import com.breadwallet.presenter.fragments.PasswordDialogFragment;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.NetworkChangeReceiver;
import com.breadwallet.tools.SoftKeyboard;
import com.breadwallet.tools.adapter.AmountAdapter;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.adapter.ParallaxViewPager;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.auth.FingerprintAuthenticationDialogFragment;
import com.breadwallet.tools.auth.PasswordAuthenticationDialogFragment;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

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

public class MainActivity extends FragmentActivity implements Observer {
    public static final String TAG = "MainActivity";
    public static final String PREFS_NAME = "MyPrefsFile";
    public static MainActivity app;
    public static boolean decoderFragmentOn;
    public static boolean scanResultFragmentOn;
    public CustomPagerAdapter pagerAdapter;
    public static RelativeLayout pageIndicator;
    public ImageView pageIndicatorLeft;
    public ImageView pageIndicatorRight;
    public View middleView;
    public Map<String, Integer> burgerButtonMap;
    public Button burgerButton;
    public Button lockerButton;
    public FragmentSettingsAll fragmentSettingsAll;
    public static ParallaxViewPager parallaxViewPager;
    public FragmentSettings fragmentSettings;
    public FragmentAbout fragmentAbout;
    public FragmentDecoder mainFragmentDecoder;
    public ClipboardManager myClipboard;
    public FragmentCurrency fragmentCurrency;
    public FragmentRecoveryPhrase fragmentRecoveryPhrase;
    public FragmentWipeWallet fragmentWipeWallet;
    public FragmentScanResult fragmentScanResult;
    private boolean doubleBackToExitPressedOnce;
    public static final int BURGER = 0;
    public static final int CLOSE = 1;
    public static final int BACK = 2;
    public static final float PAGE_INDICATOR_SCALE_UP = 1.3f;
    public static boolean beenThroughSavedInstanceMethod = false;
    public ViewFlipper viewFlipper;
    public ViewFlipper lockerPayFlipper;
    public PasswordDialogFragment passwordDialogFragment;
    public RelativeLayout networkErrorBar;
    private NetworkChangeReceiver receiver = new NetworkChangeReceiver();
    public static boolean unlocked = false;
    public static Point screenParametersPoint = new Point();
    private int middleViewPressedCount = 0;
    public static final int DEBUG = 1;
    public static final int RELEASE = 2;
    public static int MODE = RELEASE;
    public TextView testnet;
    public SoftKeyboard softKeyboard;
    public RelativeLayout mainLayout;
    public FingerprintAuthenticationDialogFragment fingerprintAuthenticationDialogFragment;
    public PasswordAuthenticationDialogFragment passwordAuthenticationDialogFragment;
    public FingerprintManager fingerprintManager;


//    private native String messageFromNativeCode(String logThis);

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        beenThroughSavedInstanceMethod = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = this;
        Log.e(TAG, "MainActivity created!");
        initializeViews();
        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);
        setUpApi23();

        if (((BreadWalletApp) getApplication()).isEmulatorOrDebug()) {
            MODE = DEBUG;
            Log.e(TAG, "DEBUG MODE!!!!!!");
        }
        testnet.setVisibility(MODE == DEBUG ? View.VISIBLE : View.GONE);

        InputMethodManager im = (InputMethodManager) getSystemService(Service.INPUT_METHOD_SERVICE);
        softKeyboard = new SoftKeyboard(mainLayout, im);

        viewFlipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FragmentAnimator.level == 0 && unlocked) {
                    if (middleViewPressedCount % 2 == 0) {
                        ((BreadWalletApp) getApplication()).showCustomToast(app, getResources().
                                getString(R.string.middle_view_tip_first), (int) (screenParametersPoint.y * 0.7), Toast.LENGTH_LONG);
                        middleViewPressedCount++;
                    } else {
                        ((BreadWalletApp) getApplication()).showCustomToast(app, getResources().
                                getString(R.string.middle_view_tip_second), (int) (screenParametersPoint.y * 0.8), Toast.LENGTH_LONG);
                        middleViewPressedCount++;
                    }
                }
            }
        });

        burgerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Testing burger button_regular_blue! should work");
                SpringAnimator.showAnimation(burgerButton);
                if (FragmentAnimator.level > 1 || scanResultFragmentOn || decoderFragmentOn) {
                    Log.e(TAG, "CHECK:Should press back!");
                    app.onBackPressed();
                } else {
                    //check multi pressing availability here, because method onBackPressed does the checking as well.
                    if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                        FragmentAnimator.pressMenuButton(app, fragmentSettingsAll);
                        Log.e(TAG, "CHECK:Should press menu");
                    }
                }
            }
        });
        lockerButton.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(lockerButton);
//                passwordDialogFragment.show(fm, TAG);
                ((BreadWalletApp) getApplication()).checkAndPromptForAuthentication(app);

            }
        });
        scaleView(pageIndicatorLeft, 1f, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP);

    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;
        MiddleViewAdapter.resetMiddleView(null);
        networkErrorBar.setVisibility(CurrencyManager.isNetworkAvailable() ? View.GONE : View.VISIBLE);
        startStopReceiver(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "Activity onPause");
        startStopReceiver(false);

    }

    @Override
    protected void onStop() {
        super.onStop();
        CurrencyManager.stopTimerTask();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
        FragmentAnimator.level = 0;
        CurrencyManager.stopTimerTask();
        Log.e(TAG, "Activity Destroyed!");
        softKeyboard.unRegisterSoftKeyboardCallback();

    }

    /**
     * Initializes all the views and components
     */

    private void initializeViews() {
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        testnet = (TextView) findViewById(R.id.testnet);
        networkErrorBar = (RelativeLayout) findViewById(R.id.main_internet_status_bar);
        burgerButton = (Button) findViewById(R.id.main_button_burger);
        lockerPayFlipper = (ViewFlipper) findViewById(R.id.locker_pay_flipper);
        viewFlipper = (ViewFlipper) MainActivity.app.findViewById(R.id.middle_view_flipper);
        lockerButton = (Button) findViewById(R.id.main_button_locker);
        pageIndicator = (RelativeLayout) findViewById(R.id.main_pager_indicator);
        pageIndicatorLeft = (ImageView) findViewById(R.id.circle_indicator_left);
        middleView = findViewById(R.id.main_label_breadwallet);
        pageIndicatorRight = (ImageView) findViewById(R.id.circle_indicator_right);
        pagerAdapter = new CustomPagerAdapter(getFragmentManager());
        burgerButtonMap = new HashMap<>();
        fragmentSettings = new FragmentSettings();
        fragmentSettingsAll = new FragmentSettingsAll();
        mainFragmentDecoder = new FragmentDecoder();
        fragmentAbout = new FragmentAbout();
        fragmentCurrency = new FragmentCurrency();
        fragmentRecoveryPhrase = new FragmentRecoveryPhrase();
        fragmentWipeWallet = new FragmentWipeWallet();
        fragmentScanResult = new FragmentScanResult();
        fingerprintAuthenticationDialogFragment = new FingerprintAuthenticationDialogFragment();
        passwordAuthenticationDialogFragment = new PasswordAuthenticationDialogFragment();
        passwordDialogFragment = new PasswordDialogFragment();
        parallaxViewPager = ((ParallaxViewPager) findViewById(R.id.main_viewpager));
        parallaxViewPager.setOverlapPercentage(0.99f).setAdapter(pagerAdapter);
        parallaxViewPager.setBackgroundResource(R.drawable.backgroundmain);
        burgerButtonMap.put("burger", R.drawable.burger);
        burgerButtonMap.put("close", R.drawable.x);
        burgerButtonMap.put("back", R.drawable.navigationback);
        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (FragmentAnimator.level > 1 || scanResultFragmentOn || decoderFragmentOn) {
                this.onBackPressed();
            } else if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                FragmentAnimator.pressMenuButton(app, fragmentSettingsAll);
            }
        }
        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
            Log.e(TAG, "onBackPressed!");
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
                            getResources().getString(R.string.mainactivity_press_back_again), 140,
                            Toast.LENGTH_SHORT);
                    makeDoubleBackToExitPressedOnce(1000);

                    break;
                case 1:
                    FragmentAnimator.pressMenuButton(this, fragmentSettingsAll);
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

    public void activityButtonsEnable(final boolean b) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!unlocked) {
                    lockerButton.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
                    lockerButton.setClickable(b);
                } else {
                    lockerButton.setVisibility(View.INVISIBLE);
                    lockerButton.setClickable(false);
                }
                parallaxViewPager.setClickable(b);
                viewFlipper.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
                burgerButton.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
                burgerButton.setClickable(b);
            }
        });

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

    private void makeDoubleBackToExitPressedOnce(int ms) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, ms);
    }

    private void startStopReceiver(boolean b) {
        if (b) {
            this.registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        } else {
            this.unregisterReceiver(receiver);
        }
    }

    public void setUnlocked(boolean b) {
        unlocked = b;
        lockerButton.setVisibility(b ? View.GONE : View.VISIBLE);
        lockerButton.setClickable(!b);
    }

    public void pay(View view) {
        SpringAnimator.showAnimation(view);
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.coinflip);
        mp.start();
        ((BreadWalletApp) getApplication()).checkAndPromptForAuthentication(this);
        if (AmountAdapter.isPayLegal()) {
            //TODO implement pay method
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("insufficient funds")
                    .setCancelable(false)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        //TODO balance observer stuff here
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            setUnlocked(true);
            String tmp = CurrencyManager.getCurrentBalanceText();
            ((BreadWalletApp) getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, tmp);
            softKeyboard.closeSoftKeyboard();
        } else {
            setUnlocked(false);
            ((BreadWalletApp) getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_IMAGE, null);
        }
    }


    public boolean isSoftKeyboardShown() {
        int[] location = new int[2];
        viewFlipper.getLocationOnScreen(location);
        boolean isShown = location[1] < 0;
        Log.e(TAG, "The keyboard is shown: " + isShown + " y location: " + location[1]);
        return isShown;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void showAuthDialog() {
        android.app.FragmentManager fm = getFragmentManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            if (fingerprintManager.hasEnrolledFingerprints()) {
                Log.e(TAG, "Starting the fingerprint Dialog! API 23+");
                fingerprintAuthenticationDialogFragment.setStage(
                        FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
//                fingerprintAuthenticationDialogFragment.setStage(
//                        FingerprintAuthenticationDialogFragment.Stage.PASSWORD);

                fingerprintAuthenticationDialogFragment.show(fm, FingerprintAuthenticationDialogFragment.class.getName());
                return;
            }
        }
        Log.e(TAG, "Starting the password Dialog! API <23");
        passwordAuthenticationDialogFragment.show(fm, PasswordAuthenticationDialogFragment.class.getName());

    }

    private void setUpApi23() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        }
    }

    public FragmentDecoder getFragmentDecoder() {
        mainFragmentDecoder = new FragmentDecoder();
        return mainFragmentDecoder;
    }

    public FragmentScanResult getFragmentScanResult() {
        fragmentScanResult = new FragmentScanResult();
        return fragmentScanResult;
    }

    public void confirmPay(PaymentRequestEntity request) {
        SharedPreferences settings;
        boolean certified = false;
        if (request.cn != null && request.cn.length() != 0) {
            certified = true;
        }
        StringBuilder allAddresses = new StringBuilder();
        for (String s : request.addresses) {
            allAddresses.append(s + ", ");
        }
        String certification = certified ? "certified: " + request.cn + "\n" : "";

        //DecimalFormat decimalFormat = new DecimalFormat("0.00");
        settings = MainActivity.app.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
        float rate = settings.getFloat(FragmentCurrency.RATE, 1.0f);
        String amount = String.valueOf(CurrencyManager.getExchangeFromSatoshi(rate, new Double(request.amount)));
        ((BreadWalletApp) getApplication()).showCustomDialog("payment info", certification + allAddresses.toString() +
                "\n\n" + "amount " + CurrencyManager.getFormattedCurrencyString("BTC", String.valueOf(request.amount / 100))
                + " (" + CurrencyManager.getFormattedCurrencyString(iso, amount) + ")", "send");
    }

}
