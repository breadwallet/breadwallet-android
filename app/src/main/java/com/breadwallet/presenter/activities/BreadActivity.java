package com.breadwallet.presenter.activities;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRNotificationBar;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.fragments.BuyTabFragment;
import com.breadwallet.presenter.history.HistoryFragment;
import com.breadwallet.presenter.spend.AuthBottomSheetDialogFragment;
import com.breadwallet.presenter.transfer.TransferFragment;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.TextSizeTransition;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.SyncManager;
import com.breadwallet.tools.security.BitcoinUrlHandler;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.tools.util.ExtensionKt;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.platform.APIClient;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

import static com.breadwallet.presenter.activities.ReEnterPinActivity.reEnterPinActivity;
import static com.breadwallet.presenter.activities.SetPinActivity.introSetPitActivity;
import static com.breadwallet.presenter.activities.intro.IntroActivity.introActivity;
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

public class BreadActivity extends BRActivity implements BRWalletManager.OnBalanceChanged, BRSharedPrefs.OnIsoChangedListener,
        TransactionDataSource.OnTxAddedListener, InternetManager.ConnectionReceiverListener {

    public static final Point screenParametersPoint = new Point();
    private static final float PRIMARY_TEXT_SIZE = 24f;
    private static final float SECONDARY_TEXT_SIZE = 12.8f;
    private int mSelectedBottomNavItem = -1;

    private InternetManager mConnectionReceiver;
    private Button primaryPrice;
    private Button secondaryPrice;
    private TextView equals;
    private ImageButton menuBut;
    private TextView ltcPriceLbl;
    private TextView ltcPriceDateLbl;
    private TextView balanceTxtV;

    public static boolean appVisible = false;
    public ViewFlipper barFlipper;
    private ConstraintLayout toolBarConstraintLayout;
    private boolean uiIsDone;

    private static BreadActivity app;
    private BottomNavigationView bottomNav;

    private Handler mHandler = new Handler();

    public static BreadActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bread);

        app = this;
        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);

        initializeViews();
        setPriceTags(BRSharedPrefs.getPreferredLTC(BreadActivity.this), false);
        setListeners();

        setUpBarFlipper();

        primaryPrice.setTextSize(PRIMARY_TEXT_SIZE);
        secondaryPrice.setTextSize(SECONDARY_TEXT_SIZE);

        if (introSetPitActivity != null) introSetPitActivity.finish();
        if (introActivity != null) introActivity.finish();
        if (reEnterPinActivity != null) reEnterPinActivity.finish();

        if (!BRSharedPrefs.getGreetingsShown(BreadActivity.this))
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BRAnimator.showGreetingsMessage(BreadActivity.this);
                    BRSharedPrefs.putGreetingsShown(BreadActivity.this, true);
                }
            }, 1000);

        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        updateUI();
        bottomNav.setSelectedItemId(R.id.nav_history);
    }

    private void addObservers() {
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        BRSharedPrefs.addIsoChangedListener(this);
    }

    private void removeObservers() {
        BRWalletManager.getInstance().removeListener(this);
        BRSharedPrefs.removeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //leave it empty, avoiding the os bug
    }

    private void setUrlHandler(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;
        String scheme = data.getScheme();
        if (scheme != null && (scheme.startsWith("litecoin") || scheme.startsWith("bitid"))) {
            String str = intent.getDataString();
            BitcoinUrlHandler.processRequest(this, str);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setUrlHandler(intent);
    }

    private void setListeners() {
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return handleNavigationItemSelected(item.getItemId());
            }
        });

        primaryPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swap();
            }
        });
        secondaryPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swap();
            }
        });
        menuBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.isClickAllowed()) {
                    BRAnimator.showMenuFragment(BreadActivity.this);
                }
            }
        });
    }

    public boolean handleNavigationItemSelected(int menuItemId) {
        if (mSelectedBottomNavItem == menuItemId) return true;
        mSelectedBottomNavItem = menuItemId;
        switch (menuItemId) {
            case R.id.nav_history:
                ExtensionKt.replaceFragment(BreadActivity.this, new HistoryFragment(), false, R.id.fragment_container);
                break;
            case R.id.nav_send:
                if (BRAnimator.isClickAllowed()) {
                    BRAnimator.showSendFragment(BreadActivity.this, null);
                }
                mSelectedBottomNavItem = 0;
                break;
            case R.id.nav_card:
                if (TextUtils.isEmpty(BRSharedPrefs.getLitecoinCardId(BreadActivity.this))) {
                    showAuthModal();
                } else {
                    ExtensionKt.replaceFragment(BreadActivity.this, new TransferFragment(), false, R.id.fragment_container);
                }
                break;
            case R.id.nav_receive:
                if (BRAnimator.isClickAllowed()) {
                    BRAnimator.showReceiveFragment(BreadActivity.this, true);
                }
                mSelectedBottomNavItem = 0;
                break;
            case R.id.nav_buy:
                ExtensionKt.replaceFragment(BreadActivity.this, new BuyTabFragment(), false, R.id.fragment_container);
                break;
        }
        return true;
    }

    public void showAuthModal() {
        BottomSheetDialogFragment fragment = new AuthBottomSheetDialogFragment();
        fragment.show(getSupportFragmentManager(), fragment.getTag());
        mSelectedBottomNavItem = 0;
    }

    private void swap() {
        if (!BRAnimator.isClickAllowed()) return;
        boolean b = !BRSharedPrefs.getPreferredLTC(this);
        setPriceTags(b, true);
        BRSharedPrefs.putPreferredLTC(this, b);
        BRSharedPrefs.notifyIsoChanged("");
    }

    private void setPriceTags(boolean ltcPreferred, boolean animate) {
        ConstraintSet set = new ConstraintSet();
        set.clone(toolBarConstraintLayout);

        if (animate) {
            TransitionSet textSizeTransition = new TransitionSet()
                    .setOrdering(TransitionSet.ORDERING_TOGETHER)
                    .addTransition(new TextSizeTransition())
                    .addTransition(new ChangeBounds());

            TransitionSet transition = new TransitionSet()
                    .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
                    .addTransition(new Fade(Fade.OUT))
                    .addTransition(textSizeTransition)
                    .addTransition(new Fade(Fade.IN));
            TransitionManager.beginDelayedTransition(toolBarConstraintLayout, transition);
        }

        primaryPrice.setTextSize(ltcPreferred ? PRIMARY_TEXT_SIZE : SECONDARY_TEXT_SIZE);
        secondaryPrice.setTextSize(ltcPreferred ? SECONDARY_TEXT_SIZE : PRIMARY_TEXT_SIZE);

        int[] ids = {primaryPrice.getId(), secondaryPrice.getId(), equals.getId()};
        // Clear views constraints
        for (int id : ids) {
            set.clear(id);
            set.constrainWidth(id, ConstraintSet.WRAP_CONTENT);
            set.constrainHeight(id, ConstraintSet.WRAP_CONTENT);
        }

        int dp16 = Utils.getPixelsFromDps(this, 16);
        int dp8 = Utils.getPixelsFromDps(this, 4);

        int leftId = ltcPreferred ? primaryPrice.getId() : secondaryPrice.getId();
        int rightId = ltcPreferred ? secondaryPrice.getId() : primaryPrice.getId();

        int[] chainViews = {leftId, equals.getId(), rightId};

        set.connect(leftId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dp16);
        set.connect(leftId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        set.connect(leftId, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dp16);
        set.setVerticalBias(leftId, 1.0f);

        set.connect(rightId, ConstraintSet.BASELINE, leftId, ConstraintSet.BASELINE);
        set.connect(equals.getId(), ConstraintSet.BASELINE, leftId, ConstraintSet.BASELINE);

        set.connect(equals.getId(), ConstraintSet.START, leftId, ConstraintSet.END, dp8);
        set.connect(equals.getId(), ConstraintSet.END, rightId, ConstraintSet.START, dp8);

        set.createHorizontalChain(leftId, ConstraintSet.LEFT, equals.getId(), ConstraintSet.RIGHT, chainViews, null, ConstraintSet.CHAIN_PACKED);

        // Apply the changes
        set.applyTo(toolBarConstraintLayout);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        }, toolBarConstraintLayout.getLayoutTransition().getDuration(LayoutTransition.CHANGING));
    }

    private void setUpBarFlipper() {
        barFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_enter));
        barFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_exit));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        app = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        addObservers();
        if (PLATFORM_ON) {
            APIClient.getInstance(this).updatePlatform();
        }

        setupNetworking();

        if (!BRWalletManager.getInstance().isCreated()) {
            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    BRWalletManager.getInstance().initWallet(BreadActivity.this);
                }
            });
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        }, 1000);

        BRWalletManager.getInstance().refreshBalance(this);
    }

    private void setupNetworking() {
        if (mConnectionReceiver == null) mConnectionReceiver = InternetManager.getInstance();
        IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectionReceiver, mNetworkStateFilter);
        InternetManager.addConnectionListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
        removeObservers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mConnectionReceiver);

        //sync the kv stores
        if (PLATFORM_ON) {
            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    APIClient.getInstance(BreadActivity.this).syncKvStore();
                }
            });
        }
    }

    private void initializeViews() {
        menuBut = findViewById(R.id.menuBut);
        bottomNav = findViewById(R.id.bottomNav);
        ltcPriceLbl = findViewById(R.id.price_change_text);
        ltcPriceDateLbl = findViewById(R.id.priceDateLbl);
        balanceTxtV = findViewById(R.id.balanceTxtV);

        primaryPrice = findViewById(R.id.primary_price);
        secondaryPrice = findViewById(R.id.secondary_price);
        equals = findViewById(R.id.equals);
        toolBarConstraintLayout = findViewById(R.id.bread_toolbar);

        barFlipper = findViewById(R.id.tool_bar_flipper);

        final ViewTreeObserver observer = primaryPrice.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (observer.isAlive()) {
                    observer.removeOnGlobalLayoutListener(this);
                }
                if (uiIsDone) return;
                uiIsDone = true;
                setPriceTags(BRSharedPrefs.getPreferredLTC(BreadActivity.this), false);
            }
        });

        ltcPriceLbl.setTextSize(PRIMARY_TEXT_SIZE);
        balanceTxtV.append(":");
    }

    @Override
    public void onBalanceChanged(final long balance) {
        updateUI();
    }

    public void updateUI() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName(Thread.currentThread().getName() + ":updateUI");
                //sleep a little in order to make sure all the commits are finished (like SharePreferences commits)
                String iso = BRSharedPrefs.getIso(BreadActivity.this);

                String formattedCurrency = null;
                CurrencyEntity currency = CurrencyDataSource.getInstance(BreadActivity.this).getCurrencyByIso(iso);
                if (currency != null) {
                    final BigDecimal roundedPriceAmount = new BigDecimal(currency.rate).multiply(new BigDecimal(100))
                            .divide(new BigDecimal(100), 2, BRConstants.ROUNDING_MODE);
                    formattedCurrency = BRCurrency.getFormattedCurrencyString(BreadActivity.this, iso, roundedPriceAmount);
                } else {
                    Timber.w("The currency related to %s is NULL", iso);
                }

                final String ltcPrice = formattedCurrency;

                //current amount in litoshis
                final BigDecimal amount = new BigDecimal(BRSharedPrefs.getCatchedBalance(BreadActivity.this));

                //amount in LTC units
                BigDecimal btcAmount = BRExchange.getBitcoinForSatoshis(BreadActivity.this, amount);
                final String formattedBTCAmount = BRCurrency.getFormattedCurrencyString(BreadActivity.this, "LTC", btcAmount);

                //amount in currency units
                final BigDecimal curAmount = BRExchange.getAmountFromSatoshis(BreadActivity.this, iso, amount);
                final String formattedCurAmount = BRCurrency.getFormattedCurrencyString(BreadActivity.this, iso, curAmount);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        primaryPrice.setText(formattedBTCAmount);
                        secondaryPrice.setText(String.format("%s", formattedCurAmount));
                        if (ltcPrice != null) {
                            ltcPriceLbl.setText(ltcPrice);
                            SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                            String pattern = df.toPattern().replaceAll("\\W?[Yy]+\\W?", " ");
                            ltcPriceDateLbl.setText("as of " + android.text.format.DateFormat.format(pattern, new Date()));
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onIsoChanged(String iso) {
        updateUI();
    }

    @Override
    public void onTxAdded() {
        BRWalletManager.getInstance().refreshBalance(BreadActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case BRConstants.CAMERA_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    BRAnimator.openScanner(this, BRConstants.SCANNER_REQUEST);
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
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        if (isConnected) {
            if (barFlipper != null) {
                if (barFlipper.getDisplayedChild() == 1) {
                    removeNotificationBar();
                }
            }
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    final double progress = BRPeerManager.syncProgress(BRSharedPrefs.getStartHeight(BreadActivity.this));
                    Timber.d("Sync Progress: %s", progress);
                    if (progress < 1 && progress > 0) {
                        SyncManager.getInstance().startSyncingProgressThread();
                    }
                }
            });
        } else {
            if (barFlipper != null) {
                addNotificationBar();
            }
            SyncManager.getInstance().stopSyncingProgressThread();
        }
    }

    public void removeNotificationBar() {
        if (barFlipper.getChildCount() == 1) return;
        barFlipper.removeViewAt(1);
        barFlipper.setDisplayedChild(0);
    }

    public void addNotificationBar() {
        if (barFlipper.getChildCount() == 2) return;
        BRNotificationBar view = new BRNotificationBar(this);
        barFlipper.addView(view);
        barFlipper.setDisplayedChild(1);
    }
}