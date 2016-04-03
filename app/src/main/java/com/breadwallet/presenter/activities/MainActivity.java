package com.breadwallet.presenter.activities;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.MainFragmentQR;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.NetworkChangeReceiver;
import com.breadwallet.tools.SoftKeyboard;
import com.breadwallet.tools.adapter.AmountAdapter;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.adapter.ParallaxViewPager;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.sqlite.SQLiteManager;
import com.breadwallet.tools.threads.PassCodeTask;
import com.breadwallet.tools.threads.ToastBlockShowTask;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import java.util.HashMap;
import java.util.List;
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
    private static final String TAG = MainActivity.class.getName();
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final int BURGER = 0;
    public static final int CLOSE = 1;
    public static final int BACK = 2;
    private static final int DEBUG = 1;
    private static final int RELEASE = 2;
    private static final float PAGE_INDICATOR_SCALE_UP = 1.3f;

    public static MainActivity app;
    public static boolean decoderFragmentOn;
    public static boolean scanResultFragmentOn;
    public static RelativeLayout pageIndicator;
    private ImageView pageIndicatorLeft;
    private ImageView pageIndicatorRight;
    private Map<String, Integer> burgerButtonMap;
    private Button burgerButton;
    public Button lockerButton;
    public TextView pay;
    public ProgressBar syncProgressBar;
    public TextView syncProgressText;
    private static ParallaxViewPager parallaxViewPager;
    //    private ClipboardManager myClipboard;
    private boolean doubleBackToExitPressedOnce;
    public static boolean beenThroughSavedInstanceMethod = false;
    public ViewFlipper viewFlipper;
    public ViewFlipper lockerPayFlipper;
    private RelativeLayout networkErrorBar;
    private final NetworkChangeReceiver receiver = new NetworkChangeReceiver();
    public static final Point screenParametersPoint = new Point();
    private int middleViewPressedCount = 0;
    private BroadcastReceiver mPowerKeyReceiver = null;
    private String amountHolder;
    private String addressHolder;

    private static int MODE = RELEASE;
    private TextView testnet;
    public SoftKeyboard softKeyboard;
    private RelativeLayout mainLayout;
    private FingerprintManager fingerprintManager;
    private ToastBlockShowTask toastBlockShowTask;
    private int runCount = 0;
    boolean deleteTxs = false;

    public static boolean appInBackground = false;

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        beenThroughSavedInstanceMethod = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        app = this;
        initializeViews();

        printPhoneSpecs();

        setUpTheWallet();

        registerScreenLockReceiver();

        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        setUpApi23();

        if (((BreadWalletApp) getApplication()).isEmulatorOrDebug()) {
            MODE = DEBUG;
            Log.e(TAG, "DEBUG MODE!!!!!!");
        }
        testnet.setVisibility(MODE == DEBUG ? View.VISIBLE : View.GONE);

        InputMethodManager im = (InputMethodManager) getSystemService(Service.INPUT_METHOD_SERVICE);
        softKeyboard = new SoftKeyboard(mainLayout, im);

        pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pay();
            }
        });

        viewFlipper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MiddleViewAdapter.getSyncing()) {
                    ToastBlockShowTask.getInstance(app).startOneToast();
                    return;
                }
                if (FragmentAnimator.level == 0 && BreadWalletApp.unlocked) {
                    if (middleViewPressedCount % 2 == 0) {
                        ((BreadWalletApp) getApplication()).showCustomToast(app, getResources().
                                        getString(R.string.middle_view_tip_first),
                                (int) (screenParametersPoint.y * 0.7), Toast.LENGTH_LONG, 0);
                        middleViewPressedCount++;
                    } else {
                        ((BreadWalletApp) getApplication()).showCustomToast(app, getResources().
                                        getString(R.string.middle_view_tip_second),
                                (int) (screenParametersPoint.y * 0.8), Toast.LENGTH_LONG, 0);
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
                    if (FragmentAnimator.checkTheMultipressingAvailability()) {
                        FragmentAnimator.pressMenuButton(app, new FragmentSettingsAll());
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
                ((BreadWalletApp) getApplication()).promptForAuthentication(app, BRConstants.AUTH_FOR_GENERAL, null);

            }
        });
        scaleView(pageIndicatorLeft, 1f, PAGE_INDICATOR_SCALE_UP, 1f, PAGE_INDICATOR_SCALE_UP);

        //check the txAdded callback functionality
//        BRWalletManager m = BRWalletManager.getInstance(app);
//        m.testWalletCallbacks();

//        Log.e(TAG, "the pubkey length is: " + m.getPublicKeyBuff().length);
//                Log.e(TAG, "FROM KEYSTORE PUBKEY: " + KeyStoreManager.getMasterPublicKey(app));
//                Log.e(TAG, "FROM KEYSTORE PHRASE: " + KeyStoreManager.getKeyStoreString(app));
//                Log.e(TAG, "FROM KEYSTORE CREATION TIME: " + KeyStoreManager.getWalletCreationTime(app));
//        createInvisibleLayoutTips();
    }

    @Override
    protected void onRestart() {
        super.onRestart();  // Always call the superclass method first
        app = this;
        // Activity being restarted from stopped state
    }

    @Override
    protected void onResume() {
        super.onResume();
        appInBackground = false;
        app = this;
        amountHolder = null;
        addressHolder = null;
        CurrencyManager currencyManager = CurrencyManager.getInstance(this);
        currencyManager.startTimer();
        currencyManager.deleteObservers();
        currencyManager.addObserver(this);
        MiddleViewAdapter.resetMiddleView(this, null);
        boolean isNetworkAvailable = CurrencyManager.getInstance(this).isNetworkAvailable(this);
        Log.e(TAG, "isNetworkAvailable: " + isNetworkAvailable);
        networkErrorBar.setVisibility(isNetworkAvailable ? View.GONE : View.VISIBLE);
        startStopReceiver(true);
        double currentSyncProgress = BRPeerManager.syncProgress();
        if (currentSyncProgress > 0 && currentSyncProgress < 1) {
            Log.e(TAG, "Worked! restarted the syncing!");
            BRPeerManager.startSyncingProgressThread();
        }
        askForPasscode();


//        else {
//            BRPeerManager.stopSyncingProgressThread();
//        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        appInBackground = true;
        Log.e(TAG, "Activity onPause");
        startStopReceiver(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        CurrencyManager.getInstance(this).stopTimerTask();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        clearCMemory();
        finish();
        FragmentAnimator.level = 0;
        CurrencyManager.getInstance(this).stopTimerTask();
        Log.e(TAG, "Activity Destroyed!");
        softKeyboard.unRegisterSoftKeyboardCallback();
        unregisterScreenLockReceiver();

    }

    /**
     * Initializes all the views and components
     */

    private void initializeViews() {
        pay = (TextView) findViewById(R.id.main_button_pay);
        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        testnet = (TextView) findViewById(R.id.testnet);
        networkErrorBar = (RelativeLayout) findViewById(R.id.main_internet_status_bar);
        burgerButton = (Button) findViewById(R.id.main_button_burger);
        lockerPayFlipper = (ViewFlipper) findViewById(R.id.locker_pay_flipper);
        viewFlipper = (ViewFlipper) MainActivity.app.findViewById(R.id.middle_view_flipper);
        lockerButton = (Button) findViewById(R.id.main_button_locker);
        pageIndicator = (RelativeLayout) findViewById(R.id.main_pager_indicator);
        pageIndicatorLeft = (ImageView) findViewById(R.id.circle_indicator_left);
        syncProgressBar = (ProgressBar) findViewById(R.id.sync_progress_bar);
        syncProgressText = (TextView) findViewById(R.id.sync_progress_text);
//        middleView = findViewById(R.id.main_label_breadwallet);
        pageIndicatorRight = (ImageView) findViewById(R.id.circle_indicator_right);
        CustomPagerAdapter pagerAdapter = new CustomPagerAdapter(getFragmentManager());
        burgerButtonMap = new HashMap<>();
        parallaxViewPager = ((ParallaxViewPager) findViewById(R.id.main_viewpager));
        parallaxViewPager.setOverlapPercentage(0.99f).setAdapter(pagerAdapter);
        parallaxViewPager.setBackgroundResource(R.drawable.backgroundmain);
        burgerButtonMap.put("burger", R.drawable.burger);
        burgerButtonMap.put("close", R.drawable.x);
        burgerButtonMap.put("back", R.drawable.navigationback);
//        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (FragmentAnimator.level > 1 || scanResultFragmentOn || decoderFragmentOn) {
                this.onBackPressed();
            } else if (FragmentAnimator.checkTheMultipressingAvailability()) {
                FragmentAnimator.pressMenuButton(app, new FragmentSettingsAll());
            }
        }
        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (FragmentAnimator.checkTheMultipressingAvailability()) {
            Log.e(TAG, "onBackPressed!");
            if (FragmentAnimator.wipeWalletOpen) {
                FragmentAnimator.pressWipeWallet(this, new FragmentSettings());
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
                            Toast.LENGTH_SHORT, 0);
                    makeDoubleBackToExitPressedOnce();
                    break;
                case 1:
                    FragmentAnimator.pressMenuButton(this, new FragmentSettingsAll());
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

    private void scaleView(View v, float startScaleX, float endScaleX, float startScaleY, float endScaleY) {
        Animation anim = new ScaleAnimation(
                startScaleX, endScaleX, // Start and end values for the X axis scaling
                startScaleY, endScaleY, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        v.startAnimation(anim);
    }

    private void makeDoubleBackToExitPressedOnce() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 1000);
    }

    private void startStopReceiver(boolean b) {
        if (b) {
            this.registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        } else {
            this.unregisterReceiver(receiver);
        }
    }


    public void pay() {

        amountHolder = FragmentScanResult.currentCurrencyPosition == FragmentScanResult.BITCOIN_RIGHT ?
                AmountAdapter.getRightValue() : AmountAdapter.getLeftValue();
        addressHolder = FragmentScanResult.address;
        final Double amountAsDouble = Double.parseDouble(amountHolder);
        if (addressHolder == null) return;
        if (addressHolder.length() < 20) return;
        if (amountAsDouble <= 0) return;
        Log.e(TAG, "*********Sending: " + amountHolder + " to: " + addressHolder);
        CurrencyManager cm = CurrencyManager.getInstance(this);

        if (cm.isNetworkAvailable(this)) {
            BRWalletManager m = BRWalletManager.getInstance(this);
            boolean txPossible = m.tryTransaction(addressHolder, cm.getSatoshisFromBits(Math.round(amountAsDouble)));
            if (!txPossible && amountAsDouble <= cm.getBitsFromSatoshi(cm.getBALANCE()) && amountAsDouble > 0) {
                final double maxAmountDouble = cm.getBitsFromSatoshi(m.getMaxOutputAmount());
                Log.e(TAG, "maxAmountDouble: " + maxAmountDouble);
                final double amountToReduce = amountAsDouble - maxAmountDouble;
                String strToReduce = String.valueOf(amountToReduce);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(String.format("reduce payment amount by\n%s?", amountToReduce))
                        .setTitle("insufficient funds for bitcoin network fee")
                        .setCancelable(false)
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton(strToReduce, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, Math.round(amountAsDouble - amountToReduce), null));
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                return;
            }
            long feeForTx = cm.getBitsFromSatoshi(m.feeForTransaction(addressHolder, cm.getSatoshisFromBits(Math.round(amountAsDouble))));
            Log.e(TAG, "pay >>>> feeForTx: " + feeForTx + ", amountAsDouble: " + amountAsDouble +
                    ", CurrencyManager.getInstance(this).getBALANCE(): " + cm.getBitsFromSatoshi(cm.getBALANCE()));
            if (feeForTx != 0 && amountAsDouble + feeForTx < cm.getBALANCE()) {

                confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, Math.round(amountAsDouble), null));
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(String.format("insufficient funds to send: ƀ%d", Math.round(amountAsDouble)))
                        .setCancelable(false)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("No internet connection!")
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

    public void request(View view) {
        SpringAnimator.showAnimation(view);
        Intent intent;
        String tempAmount = FragmentScanResult.currentCurrencyPosition == FragmentScanResult.BITCOIN_RIGHT ?
                AmountAdapter.getRightValue() : AmountAdapter.getLeftValue();
        SharedPreferences prefs = getSharedPreferences(MainFragmentQR.RECEIVE_ADDRESS_PREFS, Context.MODE_PRIVATE);
        String testTemp = prefs.getString(MainFragmentQR.RECEIVE_ADDRESS, "");

        intent = new Intent(this, RequestQRActivity.class);
        intent.putExtra(BRConstants.INTENT_EXTRA_REQUEST_AMOUNT, tempAmount);
        intent.putExtra(BRConstants.INTENT_EXTRA_REQUEST_ADDRESS, testTemp);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        FragmentAnimator.hideScanResultFragment();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            // Challenge completed, proceed with using cipher
            if (resultCode == RESULT_OK) {
                Log.e(TAG, "Auth for phrase was accepted");
//                if (tryEncrypt()) {
//                    showPurchaseConfirmation();
//                }
            } else {
                Log.e(TAG, "Auth for phrase was rejected");
                // The user canceled or didnât complete the lock screen
                // operation. Go to error/cancellation flow.
            }
        }
        //when starting another activity that will return a result (ex: auth)

//        if (resultCode == RESULT_OK) {
//            ((BreadWalletApp) getApplicationContext()).setUnlocked(true);
//            String tmp = CurrencyManager.getInstance(this).getCurrentBalanceText();
//            ((BreadWalletApp) getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, tmp);
//            softKeyboard.closeSoftKeyboard();
//        } else {
//            ((BreadWalletApp) getApplicationContext()).setUnlocked(false);
//            ((BreadWalletApp) getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_IMAGE, null);
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.e(TAG, "********************** onActivityResult >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + requestCode);
        switch (requestCode) {
            case BRConstants.CAMERA_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    FragmentAnimator.animateDecoderFragment();
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    public boolean isSoftKeyboardShown() {
        int[] location = new int[2];
        viewFlipper.getLocationOnScreen(location);
        boolean isShown = location[1] < 0;
        Log.e(TAG, "The keyboard is shown: " + isShown + " y location: " + location[1]);
        return isShown;
    }

    private void setUpApi23() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        }
    }

    public void confirmPay(final PaymentRequestEntity request) {
        SharedPreferences settings;
        boolean certified = false;
        if (request.cn != null && request.cn.length() != 0) {
            certified = true;
        }
        StringBuilder allAddresses = new StringBuilder();
        for (String s : request.addresses) {
            allAddresses.append(s + ", ");
        }
        allAddresses.delete(allAddresses.length() - 2, allAddresses.length());
        String certification = "";
        if (certified) {
            certification = "certified: " + request.cn + "\n";
            allAddresses = new StringBuilder();
        }

        //DecimalFormat decimalFormat = new DecimalFormat("0.00");
        settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
        float rate = settings.getFloat(FragmentCurrency.RATE, 1.0f);
        String amount = String.valueOf(request.amount);
        CurrencyManager m = CurrencyManager.getInstance(this);
        final String message = certification + allAddresses.toString() + "\n\n" + "amount: " + m.getFormattedCurrencyString("BTC", String.valueOf(request.amount))
                + " (" + m.getExchangeForAmount(rate, iso, amount) + ")";

//        ((BreadWalletApp) getApplication()).showCustomDialog("payment info", certification + allAddresses.toString() +
//                "\n\n" + "amount " + CurrencyManager.getInstance(this).getFormattedCurrencyString("BTC", String.valueOf(request.amount / 100))
//                + " (" + CurrencyManager.getInstance(this).getFormattedCurrencyString(iso, amount) + ")", "send");
        long minOutput = BRWalletManager.getInstance(this).getMinOutputAmount() / 100;
        if (request.amount < minOutput) {
            final String bitcoinMinMessage = String.format("bitcoin payments can't be less than ƀ%d", minOutput);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new android.app.AlertDialog.Builder(app)
                            .setTitle(getString(R.string.payment_failed))
                            .setMessage(bitcoinMinMessage)
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            });

            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new android.app.AlertDialog.Builder(app)
                        .setTitle(getString(R.string.payment_info))
                        .setMessage(message)
                        .setPositiveButton(getString(R.string.send), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ((BreadWalletApp) getApplicationContext()).promptForAuthentication(app, BRConstants.AUTH_FOR_PAY, request);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    @Override
    public void update(Observable observable, Object data) {
        MiddleViewAdapter.resetMiddleView(this, null);
    }

//    public void testSQLiteConnectivity(Activity context) {
//        // Test MerkleBlock Table
////        BRMerkleBlockEntity merkleBlockEntity = new BRMerkleBlockEntity();
////        merkleBlockEntity.setBuff();
////
////        MerkleBlockDataSource MBdataSource;
////        MBdataSource = new MerkleBlockDataSource(this);
////        MBdataSource.open();
////        MBdataSource.createMerkleBlock(merkleBlockEntity);
////        List<BRMerkleBlockEntity> values = MBdataSource.getAllMerkleBlocks();
////        Iterator<BRMerkleBlockEntity> merkleBlockEntityIterator = values.iterator();
////        while (merkleBlockEntityIterator.hasNext()) {
////            BRMerkleBlockEntity tmp = merkleBlockEntityIterator.next();
////            Log.e(TAG, "The merkleBlock: " + tmp.getId() + " " + tmp.getBlockHash() + " " + tmp.getFlags() +
////                    " " + tmp.getHashes() + " " + tmp.getHeight() + " " + tmp.getMerkleRoot() + " " +
////                    tmp.getNonce() + " " + tmp.getPrevBlock() + " " + tmp.getTarget() + " " +
////                    tmp.getTimeStamp() + tmp.getTotalTransactions() + " " + tmp.getVersion());
////
////        }
//
//        // Test Transaction Table
//        byte[] pretendToBeATx = "some transaction".getBytes();
//        byte[] pretendToBeATx2 = "some other transaction".getBytes();
//        BRTransactionEntity transactionEntity = new BRTransactionEntity(pretendToBeATx, 2, 4);
//
//        BRTransactionEntity transactionEntity2 = new BRTransactionEntity(pretendToBeATx2, 53, 542);
//
//        TransactionDataSource TXdataSource = new TransactionDataSource(this);
//        TXdataSource.open();
//        TXdataSource.deleteAllTransactions();
//        TXdataSource.createTransaction(transactionEntity);
//        TXdataSource.createTransaction(transactionEntity2);
//        List<BRTransactionEntity> txValues = TXdataSource.getAllTransactions();
////        for (BRTransactionEntity transactionEntity1 : txValues) {
////            Log.e(TAG, "The transaction: " + transactionEntity1.getId()
////                            + " " + new String(transactionEntity1.getBuff())
////            );
//
////        }
//        TXdataSource.close();
//    }

    private void setUpTheWallet() {

        BRWalletManager m = BRWalletManager.getInstance(this);
        final BRPeerManager pm = BRPeerManager.getInstance(this);


//        String phrase = KeyStoreManager.getKeyStoreString(this);
//        if (phrase == null) return;
//        String normalizedPhrase = Normalizer.normalize(phrase, Normalizer.Form.NFKD);
//        m.getMasterPubKey(normalizedPhrase);

        SQLiteManager sqLiteManager = SQLiteManager.getInstance(this);

        List<BRTransactionEntity> transactions = sqLiteManager.getTransactions();
        List<BRMerkleBlockEntity> blocks = sqLiteManager.getBlocks();
        List<BRPeerEntity> peers = sqLiteManager.getPeers();

        final int transactionsCount = transactions.size();
        final int blocksCount = blocks.size();
        final int peersCount = peers.size();


//        CustomLogger.LogThis("setUpTheWallet: number of transactions from sqlite: ",
//                String.valueOf(transactions.size()),
//                " transactionCount: ", String.valueOf(transactionsCount), " blocksCount: ",
//                String.valueOf(blocksCount), " peersCount: ", String.valueOf(peersCount));

        if (!m.isCreated()) {

            if (transactionsCount > 0) {
                m.createTxArrayWithCount(transactionsCount);
                for (BRTransactionEntity entity : transactions) {
                    m.putTransaction(entity.getBuff(), entity.getBlockheight(), entity.getTimestamp());
                }
            }

            String pubkeyEncoded = KeyStoreManager.getMasterPublicKey(this);
            int r = pubkeyEncoded.length() == 0 ? 0 : 1;
            m.createWallet(transactionsCount, pubkeyEncoded, r);

        }

        if (!pm.isCreated()) {
            if (blocksCount > 0) {
                pm.createBlockArrayWithCount(blocksCount);
                for (BRMerkleBlockEntity entity : blocks) {
                    pm.putBlock(entity.getBuff(), entity.getBlockHeight());
                }
            }

            if (peersCount > 0) {
                pm.createPeerArrayWithCount(peersCount);
                for (BRPeerEntity entity : peers) {
                    pm.putPeer(entity.getAddress(), entity.getPort(), entity.getTimeStamp());
                }
            }

            Log.e(TAG, "blocksCount before connecting: " + blocksCount);
            Log.e(TAG, "peersCount before connecting: " + peersCount);

//        earliestKeyTime = 1456796244;
            String walletTimeString = KeyStoreManager.getWalletCreationTime(this);
            final long earliestKeyTime = !walletTimeString.isEmpty() ? Long.valueOf(walletTimeString) : 0;
            Log.e(TAG, "earliestKeyTime before connecting: " + earliestKeyTime);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    pm.createAndConnect(earliestKeyTime > 0 ? earliestKeyTime : 0, blocksCount, peersCount);
                }
            }).start();

        }

    }

    private void registerScreenLockReceiver() {
        final IntentFilter theFilter = new IntentFilter();
        /** System Defined Broadcast */
        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);

        mPowerKeyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String strAction = intent.getAction();

                if (strAction.equals(Intent.ACTION_SCREEN_OFF)) {
                    ((BreadWalletApp) getApplicationContext()).setUnlocked(false);
                    Log.e(TAG, ">>>>>>>>onReceive>>>>>>>>> the screen is locked!");
                }
            }
        };

        getApplicationContext().registerReceiver(mPowerKeyReceiver, theFilter);
    }

    private void unregisterScreenLockReceiver() {
        int apiLevel = Build.VERSION.SDK_INT;

        if (apiLevel >= 7) {
            try {
                getApplicationContext().unregisterReceiver(mPowerKeyReceiver);
            } catch (IllegalArgumentException e) {
                mPowerKeyReceiver = null;
            }
        } else {
            getApplicationContext().unregisterReceiver(mPowerKeyReceiver);
            mPowerKeyReceiver = null;
        }
    }

    private void askForPasscode() {
        final String pass = KeyStoreManager.getPassCode(app);
        if (pass == null || pass.isEmpty()) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    if (app != null) {
                        Log.e(TAG, "PASSCODE: " + pass);
                        new PassCodeTask(app).start();
                    }
                }
            });
        }

    }

    private native void clearCMemory();

    private native void cTests();

//    private void createInvisibleLayoutTips() {
//        // Creating a new RelativeLayout
//        final RelativeLayout mask = new RelativeLayout(this);
//
//        // Defining the RelativeLayout layout parameters.
//        // In this case I want to fill its parent
//        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
//                RelativeLayout.LayoutParams.MATCH_PARENT,
//                RelativeLayout.LayoutParams.MATCH_PARENT);
//
//        // Adding the TextView to the RelativeLayout as a child
//        mask.setLayoutParams(rlp);
//        int position = 0;  // position of the tab you want
//        CustomPagerAdapter.adapter.getItem()
//        showTip();
//
//        mask.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (tipsCount >= 6) {
//                    mainLayout.removeView(mask);
//                }
//                showTip();
//            }
//        });
//        mainLayout.addView(mask);
//
//    }
//
//    private void showTip() {
//        if (tipsCount == 0)
//            ((BreadWalletApp) getApplicationContext()).showCustomToast(this, getString(R.string.middle_view_tip_first),
//                    MainActivity.screenParametersPoint.y / 5, Toast.LENGTH_LONG, 0);
//        if (tipsCount == 1)
//            ((BreadWalletApp) getApplicationContext()).showCustomToast(this, getString(R.string.middle_view_tip_second),
//                    MainActivity.screenParametersPoint.y / 4, Toast.LENGTH_LONG, 0);
//        if (tipsCount == 2)
//            ((BreadWalletApp) getApplicationContext()).showCustomToast(this, getString(R.string.toast_qr_tip),
//                    MainActivity.screenParametersPoint.y / 3, Toast.LENGTH_LONG, 0);
//        if (tipsCount == 3)
//            ((BreadWalletApp) getApplicationContext()).showCustomToast(this, getString(R.string.toast_address_tip),
//                    MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
//        if (tipsCount == 4)
//            ((BreadWalletApp) getApplicationContext()).showCustomToast(this, getString(R.string.scan_qr_code_tip),
//                    MainActivity.screenParametersPoint.y, Toast.LENGTH_LONG, 0);
//        if (tipsCount == 5)
//            ((BreadWalletApp) getApplicationContext()).showCustomToast(this, getString(R.string.clipboard_tip),
//                    MainActivity.screenParametersPoint.y / 5, Toast.LENGTH_LONG, 0);
//        tipsCount++;
//    }


    private void printPhoneSpecs() {
        String specsTag = "PHONE SPECS";
        Log.e(specsTag, "");
        Log.e(specsTag, "***************************PHONE SPECS***************************");

        Log.e(specsTag, "* screen X: " + screenParametersPoint.x + " , screen Y: " + screenParametersPoint.y);

        Log.e(specsTag, "* Build.CPU_ABI: " + Build.CPU_ABI);

        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        Log.e(specsTag, "* maxMemory:" + Long.toString(maxMemory));
        Log.e(specsTag, "----------------------------PHONE SPECS----------------------------");
        Log.e(specsTag, "");
    }

}