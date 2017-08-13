package com.breadwallet.presenter.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.customviews.BubbleTextView;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BlockEntity;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.FragmentWithdrawBch;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.sqlite.SQLiteManager;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.util.NetworkChangeReceiver;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.security.RequestHandler;
import com.breadwallet.tools.security.RootHelper;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.adapter.ParallaxViewPager;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;
import com.breadwallet.wallet.BreadLibs;
import com.google.firebase.crash.FirebaseCrash;
import com.platform.APIClient;
import com.platform.middlewares.plugins.CameraPlugin;
import com.platform.middlewares.plugins.GeoLocationPlugin;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import static com.breadwallet.tools.util.BRConstants.PLATFORM_ON;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
 * Copyright (c) 2016 breadwallet LLC
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
    private static final String TAG = MainActivity.class.getName();

    public static MainActivity app;
    public RelativeLayout pageIndicator;
    private ImageView pageIndicatorLeft;
    private ImageView pageIndicatorRight;
    private Map<String, Integer> burgerButtonMap;
    private Button burgerButton;
    public Button lockerButton;
    public TextView pay;
    private ProgressBar syncProgressBar;
    private TextView syncProgressText;
    public ParallaxViewPager parallaxViewPager;
    public ViewFlipper viewFlipper;
    public ViewFlipper lockerPayFlipper;
    private RelativeLayout networkErrorBar;
    private final NetworkChangeReceiver receiver = new NetworkChangeReceiver();
    public static final Point screenParametersPoint = new Point();
    private int middleViewState = 0;
    private BroadcastReceiver mPowerKeyReceiver = null;
    private int middleBubbleBlocksCount = 0;
    private static int MODE = BRConstants.RELEASE;
    public BubbleTextView middleBubble1;
    public BubbleTextView middleBubble2;
    public BubbleTextView middleBubbleBlocks;
    public BubbleTextView qrBubble1;
    public BubbleTextView qrBubble2;
    public BubbleTextView sendBubble1;
    public BubbleTextView sendBubble2;
    private ToastUpdater toastUpdater;

    public static boolean appInBackground = false;

    //loading the native library
    static {
        System.loadLibrary("core");

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        if (savedInstanceState != null)
//            savedInstanceState.clear();
        super.onCreate(null);
//        BreadLibs.initNativeLib(this, "libCore.so");

        setContentView(R.layout.activity_main);
        app = this;
        initializeViews();

        Utils.printPhoneSpecs();

        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        checkDeviceRooted();

        if (Utils.isEmulatorOrDebug(this)) {
            MODE = BRConstants.DEBUG;
            Log.i(TAG, "DEBUG MODE!");
        }

        setListeners();
        BRAnimator.scaleView(pageIndicatorLeft, 1f, BRConstants.PAGE_INDICATOR_SCALE_UP, 1f,
                BRConstants.PAGE_INDICATOR_SCALE_UP);
        setStatusBarColor();

        setUrlHandler(getIntent());
        if (PLATFORM_ON)
            APIClient.getInstance(this).updatePlatform();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    private void setStatusBarColor() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(R.color.status_bar));
    }

    private void setUrlHandler(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;
        String scheme = data.getScheme();
        if (scheme != null && (scheme.startsWith("bitcoin") || scheme.startsWith("bitid"))) {
            String str = intent.getDataString();
            RequestHandler.processRequest(this, str);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setUrlHandler(intent);

    }

    private void setListeners() {
        pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    hideAllBubbles();
                    String amountHolder = FragmentScanResult.instance.getBitcoinValue().value;
                    String addressHolder = FragmentScanResult.address;
                    String multiplyBy = "100";
                    int unit = SharedPreferencesManager.getCurrencyUnit(app);
                    if (unit == BRConstants.CURRENT_UNIT_MBITS) multiplyBy = "100000";
                    if (unit == BRConstants.CURRENT_UNIT_BITCOINS) multiplyBy = "100000000";
                    BRWalletManager.getInstance(app).pay(addressHolder, new BigDecimal(amountHolder).multiply(new BigDecimal(multiplyBy)), null, false);
                }
            }
        });
        pay.setFilterTouchesWhenObscured(true);
        viewFlipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.scanResultFragmentOn)
                    return;
                if (MiddleViewAdapter.getSyncing() && BRAnimator.level == 0) {
                    hideAllBubbles();
                    if (middleBubbleBlocksCount == 0) {
                        middleBubbleBlocksCount = 1;
                        middleBubbleBlocks.setVisibility(View.VISIBLE);
                        SpringAnimator.showBubbleAnimation(middleBubbleBlocks);
                        if (toastUpdater != null) {
                            toastUpdater.interrupt();
                        }
                        toastUpdater = null;
                        toastUpdater = new ToastUpdater();
                        toastUpdater.start();
                    } else {
                        middleBubbleBlocksCount = 0;
                        middleBubbleBlocks.setVisibility(View.GONE);
                    }
                    return;
                }
                if (BRAnimator.level == 0 && BreadWalletApp.unlocked) {
                    hideAllBubbles();
                    if (middleViewState == 0) {
                        middleBubble2.setVisibility(View.GONE);
                        middleBubble1.setVisibility(View.VISIBLE);
                        SpringAnimator.showBubbleAnimation(middleBubble1);
                        middleViewState++;
                    } else if (middleViewState == 1) {
                        middleBubble2.setVisibility(View.VISIBLE);
                        SpringAnimator.showBubbleAnimation(middleBubble2);
                        middleBubble1.setVisibility(View.GONE);
                        middleViewState++;
                    } else {
                        hideAllBubbles();
                        middleViewState = 0;
                    }

                }
            }
        });

        burgerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: ");
                hideAllBubbles();
                SpringAnimator.showAnimation(burgerButton);
                if (BRAnimator.level > 1 || BRAnimator.scanResultFragmentOn || BRAnimator.decoderFragmentOn ) {
                    onBackPressed();
                } else {
                    //check multi pressing availability here, because method onBackPressed does the checking as well.
                    if (BRAnimator.checkTheMultipressingAvailability()) {
                        BRAnimator.pressMenuButton(app);
                    }
                }
            }
        });
        lockerButton.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                hideAllBubbles();
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    SpringAnimator.showAnimation(lockerButton);
                    if (!KeyStoreManager.getPassCode(app).isEmpty())
                        ((BreadWalletApp) getApplication()).promptForAuthentication(app,
                                BRConstants.AUTH_FOR_GENERAL, null, null, null, null, false);
                }

            }
        });
        networkErrorBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BRPeerManager.getInstance(app).refreshConnection();
                    }
                }, 400);

            }
        });
    }

    private void checkDeviceRooted() {
        final boolean hasBitcoin = CurrencyManager.getInstance(this).getBALANCE() > 0;
        boolean isDebuggable = 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);
        if (RootHelper.isDeviceRooted() && !isDebuggable) {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (app == null) {
                        Log.e(TAG, "WARNING: checkDeviceRooted: app - null");
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(app);
                    builder.setTitle(R.string.device_security_compromised)
                            .setMessage(String.format(getString(R.string.rooted_message),
                                    hasBitcoin ? getString(R.string.rooted_message_holder1) : ""))
                            .setCancelable(false)
                            .setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    if (app != null && !app.isDestroyed())
                        alert.show();
                }
            }, 10000);

        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        app = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((BreadWalletApp) getApplicationContext()).setUnlocked(false);
        appInBackground = false;
        middleViewState = 0;
        middleBubbleBlocksCount = 0;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                BRWalletManager.getInstance(app).setUpTheWallet(MainActivity.this);
            }
        }).start();

        app = this;
        final BRWalletManager m = BRWalletManager.getInstance(this);
        CurrencyManager currencyManager = CurrencyManager.getInstance(this);
        currencyManager.startTimer();
        currencyManager.deleteObservers();
        currencyManager.addObserver(this);
        final boolean isNetworkAvailable = ((BreadWalletApp) getApplication()).hasInternetAccess();
        networkErrorBar.setVisibility(isNetworkAvailable ? View.GONE : View.VISIBLE);

        lockerButton.setVisibility(BreadWalletApp.unlocked ? View.INVISIBLE : View.VISIBLE);
        startStopReceiver(true);
        BRPeerManager.getInstance(app).refreshConnection();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                BRWalletManager.getInstance(app).askForPasscode();
            }
        }, 1000);
        if (!m.isPasscodeEnabled(this)) {
            //Device passcode/password should be enabled for the app to work
            ((BreadWalletApp) getApplication()).showDeviceNotSecuredWarning(this);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (SharedPreferencesManager.getPhraseWroteDown(app)) return;
                long balance = CurrencyManager.getInstance(app).getBALANCE();
                int limit = SharedPreferencesManager.getLimit(app);
                if (balance > limit)
                    BRWalletManager.getInstance(app).animateSavePhraseFlow();
            }
        }, 4000);
        BRWalletManager.refreshAddress();
        checkUnlockedTooLong();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appInBackground = true;
        CurrencyManager.getInstance(this).stopTimerTask();
        startStopReceiver(false);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
        BRAnimator.level = 0;

        unregisterScreenLockReceiver();
        //sync the kv stores
        if (PLATFORM_ON)
            APIClient.getInstance(this).syncKvStore();

        BRWalletManager.getInstance(this).setWalletCreated(false);

    }

    /**
     * Initializes all the views and components
     */

    private void initializeViews() {
        pay = (TextView) findViewById(R.id.main_button_pay);
        networkErrorBar = (RelativeLayout) findViewById(R.id.main_internet_status_bar);
        burgerButton = (Button) findViewById(R.id.main_button_burger);
        lockerPayFlipper = (ViewFlipper) findViewById(R.id.locker_pay_flipper);
        viewFlipper = (ViewFlipper) MainActivity.app.findViewById(R.id.middle_view_flipper);
        lockerButton = (Button) findViewById(R.id.main_button_locker);
        pageIndicator = (RelativeLayout) findViewById(R.id.main_pager_indicator);
        pageIndicatorLeft = (ImageView) findViewById(R.id.circle_indicator_left);
        syncProgressBar = (ProgressBar) findViewById(R.id.sync_progress_bar);
        syncProgressText = (TextView) findViewById(R.id.sync_progress_text);
        pageIndicatorRight = (ImageView) findViewById(R.id.circle_indicator_right);
        pageIndicatorLeft.setImageResource(R.drawable.circle_indicator);
        pageIndicatorRight.setImageResource(R.drawable.circle_indicator);
        CustomPagerAdapter pagerAdapter = new CustomPagerAdapter(getFragmentManager());
        burgerButtonMap = new HashMap<>();
        parallaxViewPager = ((ParallaxViewPager) findViewById(R.id.main_viewpager));
        parallaxViewPager.setOverlapPercentage(0.99f).setAdapter(pagerAdapter);
        parallaxViewPager.setBackgroundResource(R.drawable.backgroundmain);
        burgerButtonMap.put("burger", R.drawable.burger);
        burgerButtonMap.put("close", R.drawable.x);
        burgerButtonMap.put("back", R.drawable.navigationback);
        middleBubble1 = (BubbleTextView) findViewById(R.id.middle_bubble_tip1);
        middleBubble2 = (BubbleTextView) findViewById(R.id.middle_bubble_tip2);
        middleBubble2.setText(String.format(getString(R.string.middle_view_tip_second),
                BRConstants.bitcoinLowercase, BRConstants.bitcoinLowercase + "1,000,000"));

        middleBubbleBlocks = (BubbleTextView) findViewById(R.id.middle_bubble_blocks);
        qrBubble1 = (BubbleTextView) findViewById(R.id.qr_bubble1);
        qrBubble2 = (BubbleTextView) findViewById(R.id.qr_bubble2);
    }

    //check if the user hasn't used the passcode in 2 weeks or more and ask for it
    private void checkUnlockedTooLong() {
        String pass = KeyStoreManager.getPassCode(this);
        long passTime = KeyStoreManager.getLastPasscodeUsedTime(this);
        if (pass.length() == 4 && (passTime + BRConstants.PASS_CODE_TIME_LIMIT <= System.currentTimeMillis())) {
            ((BreadWalletApp) getApplication()).promptForAuthentication(this, BRConstants.AUTH_FOR_GENERAL, null, null, null, null, true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (BRAnimator.level > 1 || BRAnimator.scanResultFragmentOn || BRAnimator.decoderFragmentOn) {
                this.onBackPressed();
            } else if (BRAnimator.checkTheMultipressingAvailability()) {
                BRAnimator.pressMenuButton(app);
            }
        }
        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (BRAnimator.checkTheMultipressingAvailability()) {
            if (BRAnimator.wipeWalletOpen) {
                BRAnimator.pressWipeWallet(this, new FragmentSettings());
                activityButtonsEnable(true);
                return;
            }
            //switch the level of fragments creation.
            switch (BRAnimator.level) {
                case 0:
                    if (BRAnimator.decoderFragmentOn) {
                        BRAnimator.hideDecoderFragment();
                        break;
                    }
                    if (BRAnimator.scanResultFragmentOn) {
                        BRAnimator.hideScanResultFragment();
                        break;
                    }
                    super.onBackPressed();
                    break;
                case 1:
                    BRAnimator.pressMenuButton(this);
                    BRAnimator.hideDecoderFragment();
                    break;
                default:
                    BRAnimator.animateSlideToRight(this);
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
            BRAnimator.scaleView(pageIndicatorLeft, 1f, BRConstants.PAGE_INDICATOR_SCALE_UP, 1f, BRConstants.PAGE_INDICATOR_SCALE_UP);
            BRAnimator.scaleView(pageIndicatorRight, BRConstants.PAGE_INDICATOR_SCALE_UP, 1f, BRConstants.PAGE_INDICATOR_SCALE_UP, 1f);
        } else if (x == 1) {
            BRAnimator.scaleView(pageIndicatorRight, 1f, BRConstants.PAGE_INDICATOR_SCALE_UP, 1f, BRConstants.PAGE_INDICATOR_SCALE_UP);
            BRAnimator.scaleView(pageIndicatorLeft, BRConstants.PAGE_INDICATOR_SCALE_UP, 1f, BRConstants.PAGE_INDICATOR_SCALE_UP, 1f);
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
        else Log.e(TAG, "setBurgerButtonImage: item is null");
    }

    public void activityButtonsEnable(final boolean b) {
        Log.e(TAG, "activityButtonsEnable: " + b);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!BreadWalletApp.unlocked) {
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

    private void startStopReceiver(boolean b) {
        if (b) {
            this.registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        } else {
            this.unregisterReceiver(receiver);
        }
    }

    public void request(View view) {
        SpringAnimator.showAnimation(view);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent;
                String tempAmount = FragmentScanResult.instance.getBitcoinValue().value;
                BRWalletManager m = BRWalletManager.getInstance(MainActivity.this);
                int unit = BRConstants.CURRENT_UNIT_BITS;
                Activity context = MainActivity.app;
                String divideBy = "100";
                if (context != null)
                    unit = SharedPreferencesManager.getCurrencyUnit(context);
                if (unit == BRConstants.CURRENT_UNIT_MBITS) divideBy = "100000";
                if (unit == BRConstants.CURRENT_UNIT_BITCOINS) divideBy = "100000000";

                long minAmount = m.getMinOutputAmount();
                if (new BigDecimal(tempAmount).multiply(new BigDecimal(divideBy)).doubleValue() < minAmount) {
                    String placeHolder = BRConstants.bitcoinLowercase + new BigDecimal(minAmount).divide(new BigDecimal(divideBy)).toString();
                    final String bitcoinMinMessage = String.format(Locale.getDefault(), getString(R.string.bitcoin_payment_cant_be_less), placeHolder);
                    ((BreadWalletApp) getApplication()).showCustomDialog(getString(R.string.amount_too_small),
                            bitcoinMinMessage, getString(R.string.ok));
                    return;
                }
                String divideByForIntent = "1000000";
                if (unit == BRConstants.CURRENT_UNIT_MBITS) divideByForIntent = "1000";
                if (unit == BRConstants.CURRENT_UNIT_BITCOINS) divideByForIntent = "1";
                String strAmount = String.valueOf(new BigDecimal(tempAmount).divide(new BigDecimal(divideByForIntent)).toString());
                String address = SharedPreferencesManager.getReceiveAddress(MainActivity.this);
                intent = new Intent(MainActivity.this, RequestQRActivity.class);
                intent.putExtra(BRConstants.INTENT_EXTRA_REQUEST_AMOUNT, strAmount);
                intent.putExtra(BRConstants.INTENT_EXTRA_REQUEST_ADDRESS, address);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                BRAnimator.hideScanResultFragment();
            }
        }).start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BRConstants.PAY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuthenticationProcessor.getInstance().onPublishTxAuth(this, true);
                }
                break;
            case BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuthenticationProcessor.getInstance().onPaymentProtocolRequest(this, true);
                }
                break;
            case BRConstants.REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    CameraPlugin.handleCameraImageTaken(this, imageBitmap);
                } else {
                    CameraPlugin.handleCameraImageTaken(this, null);
                }
                break;
            case BRConstants.REQUEST_PHRASE_BITID:
                if (resultCode == RESULT_OK) {
                    RequestHandler.processBitIdResponse(this);
                }
                break;
            case BRConstants.SEND_BCH_REQUEST:
                if (resultCode == RESULT_OK) {
                    PostAuthenticationProcessor.getInstance().onSendBch(this, false, FragmentWithdrawBch.address);
                }
                break;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case BRConstants.CAMERA_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    BRAnimator.animateDecoderFragment();

                }
                return;
            }
            case BRConstants.GEO_REQUEST_ID: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    GeoLocationPlugin.handleGeoPermission(true);
                } else {
                    GeoLocationPlugin.handleGeoPermission(false);
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    public boolean isSoftKeyboardShown() {
        int[] location = new int[2];
        viewFlipper.getLocationOnScreen(location);
        return location[1] < 0;
    }


    @Override
    public void update(Observable observable, Object data) {
        MiddleViewAdapter.resetMiddleView(this, null);
    }

    private void unregisterScreenLockReceiver() {

        try {
            getApplicationContext().unregisterReceiver(mPowerKeyReceiver);
        } catch (IllegalArgumentException e) {
            mPowerKeyReceiver = null;
        }
    }

    public void hideAllBubbles() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BRAnimator.fadeScaleBubble(middleBubble1, middleBubble2, middleBubbleBlocks,
                        qrBubble2, qrBubble1, sendBubble1, sendBubble2);
            }
        });
    }

    public void showHideSyncProgressViews(boolean b) {
        if (syncProgressBar == null || syncProgressText == null) return;
        syncProgressBar.setVisibility(b ? View.VISIBLE : View.GONE);
        syncProgressText.setVisibility(b ? View.VISIBLE : View.GONE);
    }

    public class ToastUpdater extends Thread {
        public void run() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //first set, for when the internet is not available, fixes the blank toast
                    int latestBlockKnown = SharedPreferencesManager.getLastBlockHeight(MainActivity.this);
                    int currBlock = SharedPreferencesManager.getStartHeight(MainActivity.this);
                    String formattedBlockInfo = String.format(getString(R.string.blocks), currBlock, latestBlockKnown);
                    middleBubbleBlocks.setText(formattedBlockInfo);
                }
            });

            while (middleBubbleBlocks.getVisibility() == View.VISIBLE) {
                final int latestBlockKnown = BRPeerManager.getEstimatedBlockHeight();
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                final int currBlock = BRPeerManager.getCurrentBlockHeight();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String formattedBlockInfo = String.format(getString(R.string.blocks), currBlock, latestBlockKnown);
                        middleBubbleBlocks.setText(formattedBlockInfo);
                    }
                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public void setProgress(int progress, String progressText) {
        if (syncProgressBar == null || syncProgressText == null) return;
        syncProgressBar.setProgress(progress);
        syncProgressText.setText(progressText);
    }

}