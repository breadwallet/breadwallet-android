package com.breadwallet.presenter.activities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
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
import com.breadwallet.presenter.fragments.PasswordDialogFragment;
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
import com.breadwallet.tools.sqlite.TransactionDataSource;
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
//    private int tipsCount;

    private boolean deleteTxs = false;

    //TODO Test everything with the KILL ACTIVITY feature in the developer settings
    //loading the native library
//    static {
//        System.loadLibrary("core");
//    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        beenThroughSavedInstanceMethod = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e(TAG, "MainActivity created!");

        app = this;
        initializeViews();

        //TODO delete the core testing
//        cTests();
        printPhoneSpecs();

        deleteTxs = true;
//        testTxAdding(2);
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                BRWalletManager.getInstance(app).testTransactionAdding(34643634);
//            }
//        }, 10000);


        setUpTheWallet();
        registerScreenLockReceiver();
//        testSQLiteConnectivity(this);

        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        setUpApi23();

        // Start lengthy operation in a background thread
//        new Thread(new Runnable() {
//            int mProgressStatus = 0;
//
//            public void run() {
//                while (mProgressStatus < 100) {
//                    Random r = new Random();
//                    try {
//                        Thread.sleep(r.nextInt(500));
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    mProgressStatus += 2;
//
//                    // Update the progress bar
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            syncProgressBar.setProgress(mProgressStatus);
//                            //TODO use resource string with place holders, everywhere
//                            syncProgressText.setText(String.valueOf(mProgressStatus) + "%");
//                        }
//                    });
//                }
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mProgressStatus >= 100) {
//                            syncProgressBar.setVisibility(View.INVISIBLE);
//                            syncProgressText.setVisibility(View.INVISIBLE);
//                        }
//                    }
//                });
//
//            }
//        }).start();

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
        askForPasscode();

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
//                    if (!decoderFragmentOn) {
                    FragmentAnimator.pressMenuButton(this, new FragmentSettingsAll());
//                    }
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
//        if (auth == 1) {
//            if (addressHolder == null || amountHolder == null) return;
//            if (Long.valueOf(amountHolder) <= 0 || addressHolder.length() < 30) return;
//            BRWalletManager walletManager = BRWalletManager.getInstance(this);
//            walletManager.pay(addressHolder, Long.valueOf(amountHolder) * 100);
//            final MediaPlayer mp = MediaPlayer.create(this, R.raw.coinflip);
//            mp.start();
//            FragmentAnimator.hideScanResultFragment();
//            return;
//        }

        amountHolder = FragmentScanResult.currentCurrencyPosition == FragmentScanResult.BITCOIN_RIGHT ?
                AmountAdapter.getRightValue() : AmountAdapter.getLeftValue();
        addressHolder = FragmentScanResult.address;
        if (addressHolder == null) return;
        if (addressHolder.length() < 20) return;
        if (Long.valueOf(amountHolder) <= 0) return;
        Log.e(TAG, "*********Sending: " + amountHolder + " to: " + addressHolder);

        if (CurrencyManager.getInstance(this).isNetworkAvailable(this)) {
            if (Long.valueOf(amountHolder) < CurrencyManager.getInstance(this).getBALANCE()) {
                confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, Long.valueOf(amountHolder), null));
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("insufficient funds to send: " + amountHolder)
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
        //TODO make sure the address changes on txAdded
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
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Permissions", "Permission Granted: " + permissions[i]);
            } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Log.d("Permissions", "Permission Denied: " + permissions[i]);
            }
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

    public void testSQLiteConnectivity(Activity context) {
        // Test MerkleBlock Table
//        BRMerkleBlockEntity merkleBlockEntity = new BRMerkleBlockEntity();
//        merkleBlockEntity.setBuff();
//
//        MerkleBlockDataSource MBdataSource;
//        MBdataSource = new MerkleBlockDataSource(this);
//        MBdataSource.open();
//        MBdataSource.createMerkleBlock(merkleBlockEntity);
//        List<BRMerkleBlockEntity> values = MBdataSource.getAllMerkleBlocks();
//        Iterator<BRMerkleBlockEntity> merkleBlockEntityIterator = values.iterator();
//        while (merkleBlockEntityIterator.hasNext()) {
//            BRMerkleBlockEntity tmp = merkleBlockEntityIterator.next();
//            Log.e(TAG, "The merkleBlock: " + tmp.getId() + " " + tmp.getBlockHash() + " " + tmp.getFlags() +
//                    " " + tmp.getHashes() + " " + tmp.getHeight() + " " + tmp.getMerkleRoot() + " " +
//                    tmp.getNonce() + " " + tmp.getPrevBlock() + " " + tmp.getTarget() + " " +
//                    tmp.getTimeStamp() + tmp.getTotalTransactions() + " " + tmp.getVersion());
//
//        }

        // Test Transaction Table
        byte[] pretendToBeATx = "some transaction".getBytes();
        byte[] pretendToBeATx2 = "some other transaction".getBytes();
        BRTransactionEntity transactionEntity = new BRTransactionEntity(pretendToBeATx, 2, 4);

        BRTransactionEntity transactionEntity2 = new BRTransactionEntity(pretendToBeATx2, 53, 542);

        TransactionDataSource TXdataSource = new TransactionDataSource(this);
        TXdataSource.open();
        TXdataSource.deleteAllTransactions();
        TXdataSource.createTransaction(transactionEntity);
        TXdataSource.createTransaction(transactionEntity2);
        List<BRTransactionEntity> txValues = TXdataSource.getAllTransactions();
//        for (BRTransactionEntity transactionEntity1 : txValues) {
//            Log.e(TAG, "The transaction: " + transactionEntity1.getId()
//                            + " " + new String(transactionEntity1.getBuff())
//            );

//        }
        TXdataSource.close();
    }

    private void setUpTheWallet() {
        //TODO deleting all txs for testing only
        if (deleteTxs) {
            TransactionDataSource TXdataSource = new TransactionDataSource(this);
            TXdataSource.open();
            TXdataSource.deleteAllTransactions();
            TXdataSource.close();
        }

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

        int transactionsCount = transactions.size();
        final int blocksCount = blocks.size();
        final int peersCount = peers.size();

//        CustomLogger.LogThis("setUpTheWallet: number of transactions from sqlite: ",
//                String.valueOf(transactions.size()),
//                " transactionCount: ", String.valueOf(transactionsCount), " blocksCount: ",
//                String.valueOf(blocksCount), " peersCount: ", String.valueOf(peersCount));

        if (transactionsCount > 0) {
            m.createTxArrayWithCount(transactionsCount);
            for (BRTransactionEntity entity : transactions) {
                m.putTransaction(entity.getBuff());
            }
        }

        if (blocksCount > 0) {
            pm.createBlockArrayWithCount(blocksCount);
            for (BRMerkleBlockEntity entity : blocks) {
                pm.putBlock(entity.getBuff());
            }
        }

        if (peersCount > 0) {
            pm.createPeerArrayWithCount(peersCount);
            for (BRPeerEntity entity : peers) {
                pm.putPeer(entity.getAddress(), entity.getPort(), entity.getTimeStamp());
            }
        }

        String pubkeyEncoded = KeyStoreManager.getMasterPublicKey(this);
        int r = pubkeyEncoded.length() == 0 ? 0 : 1;

        m.createWallet(transactionsCount, pubkeyEncoded, r);

        final long earliestKeyTime = KeyStoreManager.getWalletCreationTime(this);
        Log.e(TAG, "earliestKeyTime from keystore: " + earliestKeyTime);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                pm.connect(earliestKeyTime, blocksCount, peersCount);
//            }
//        }).start();
        pm.connect(earliestKeyTime, blocksCount, peersCount);

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

    private void testTxAdding(final int number) {
        for (int i = 1; i <= number; i++) {
            final int finalVar = i;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    BRWalletManager.getInstance(app).testTransactionAdding(finalVar * 100);
                    BRWalletManager.getInstance(app).testTransactionAdding(1000000000);
                }
            }, 10000 + finalVar * 1000);
        }
    }

    private void askForPasscode() {
        String pass = KeyStoreManager.getPassCode(this);
        Log.e(TAG, "PASSCODE: " + pass);
        if (pass == null || pass.isEmpty()) {
            new PassCodeTask().start();
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

    private class PassCodeTask extends Thread {
        PasswordDialogFragment passwordDialogFragment;
        String pass = "";

        @Override
        public void run() {
            super.run();

            final FragmentManager fm = getFragmentManager();
            passwordDialogFragment = new PasswordDialogFragment();
            passwordDialogFragment.setFirstTimeTrue();
            passwordDialogFragment.show(fm, PasswordDialogFragment.class.getName());
            while (pass != null && pass.isEmpty()) {
                Log.e(TAG, "in the while");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pass = KeyStoreManager.getPassCode(getApplicationContext());
                        Log.e(TAG, "in the run of the UI");
                        if (!passwordDialogFragment.isAdded()) {
                            Log.e(TAG, "in the !passwordDialogFragment.isAdded()");
                            passwordDialogFragment = new PasswordDialogFragment();
                            passwordDialogFragment.setFirstTimeTrue();
                            passwordDialogFragment.show(fm, PasswordDialogFragment.class.getName());
                        }
                    }
                });


                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

    }

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