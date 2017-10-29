package com.breadwallet.presenter.activities;

import android.animation.LayoutTransition;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.TransitionManager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRSearchBar;
import com.breadwallet.presenter.fragments.FragmentManage;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.ConnectionManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.security.BitcoinUrlHandler;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.tools.util.NetworkChangeReceiver;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.APIClient;
import com.platform.HTTPServer;

import java.math.BigDecimal;

import static com.breadwallet.presenter.activities.intro.IntroActivity.introActivity;
import static com.breadwallet.presenter.activities.ReEnterPinActivity.reEnterPinActivity;
import static com.breadwallet.presenter.activities.SetPinActivity.introSetPitActivity;
import static com.breadwallet.tools.animation.BRAnimator.t1Size;
import static com.breadwallet.tools.animation.BRAnimator.t2Size;
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

public class BreadActivity extends BRActivity implements BRWalletManager.OnBalanceChanged,
        BRPeerManager.OnTxStatusUpdate, BRSharedPrefs.OnIsoChangedListener,
        TransactionDataSource.OnTxAddedListener, FragmentManage.OnNameChanged, ConnectionManager.ConnectionReceiverListener {

    private static final String TAG = BreadActivity.class.getName();

    private LinearLayout sendButton;
    private LinearLayout receiveButton;
    private LinearLayout menuButton;
    public static final Point screenParametersPoint = new Point();

    private NetworkChangeReceiver mNetworkStateReceiver;

    private TextView primaryPrice;
    private TextView secondaryPrice;
    private TextView equals;
    private TextView priceChange;

    private TextView manageText;
    private ImageView walletName;
    private TextView emptyTip;
    private ConstraintLayout walletProgressLayout;

    private RelativeLayout mainLayout;
    private LinearLayout toolbarLayout;
    private Toolbar toolBar;
    private int progress = 0;
    public static boolean appVisible = false;
    private ImageButton searchIcon;
    public ViewFlipper barFlipper;
    private BRSearchBar searchBar;
    //    private boolean isSwapped;
    private ConstraintLayout toolBarConstraintLayout;
    private String savedFragmentTag;
    private boolean uiIsDone;

    private static BreadActivity app;

    public static BreadActivity getApp() {
        return app;
    }

    static {
        System.loadLibrary("core");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bread);
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        BRPeerManager.getInstance().addStatusUpdateListener(this);
        BRPeerManager.setOnSyncFinished(new BRPeerManager.OnSyncSucceeded() {
            @Override
            public void onFinished() {
                //put some here
            }
        });
        BRSharedPrefs.addIsoChangedListener(this);

        app = this;
        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);
        // Always cast your custom Toolbar here, and set it as the ActionBar.
        toolBar = (Toolbar) findViewById(R.id.bread_bar);

        initializeViews();

        setListeners();

        toolbarLayout.removeView(walletProgressLayout);

        setUpBarFlipper();

        BRAnimator.init(this);
        primaryPrice.setTextSize(TypedValue.COMPLEX_UNIT_PX, t1Size);//make it the size it should be after animation to get the X
        secondaryPrice.setTextSize(TypedValue.COMPLEX_UNIT_PX, t2Size);//make it the size it should be after animation to get the X

        if (introSetPitActivity != null) introSetPitActivity.finish();
        if (introActivity != null) introActivity.finish();
        if (reEnterPinActivity != null) reEnterPinActivity.finish();

        updateUI();

        TxManager.getInstance().init(this);

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
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSendFragment(BreadActivity.this, null);

            }
        });

        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showReceiveFragment(BreadActivity.this, true);
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                //start the server for Buy Bitcoin
                BRAnimator.showMenuFragment(BreadActivity.this);
                WebView view = new WebView(BreadActivity.this);
                view.loadUrl(HTTPServer.URL_SUPPORT);

            }
        });
        manageText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
                FragmentManage fragmentManage = new FragmentManage();
                fragmentManage.setOnNameChanged(BreadActivity.this);
                transaction.add(android.R.id.content, fragmentManage, FragmentManage.class.getName());
                transaction.addToBackStack(null);
                transaction.commit();
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

        searchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                barFlipper.setDisplayedChild(1); //search bar
                searchBar.onShow(true);

            }
        });


    }

    private void swap() {
        if (!BRAnimator.isClickAllowed()) return;
        boolean b = !BRSharedPrefs.getPreferredBTC(this);
        setPriceTags(b, true);
        BRSharedPrefs.putPreferredBTC(this, b);
    }

    private void setPriceTags(boolean btcPreferred, boolean animate) {
        secondaryPrice.setTextSize(!btcPreferred ? t1Size : t2Size);
        primaryPrice.setTextSize(!btcPreferred ? t2Size : t1Size);
        ConstraintSet set = new ConstraintSet();
        set.clone(toolBarConstraintLayout);
        if (animate)
            TransitionManager.beginDelayedTransition(toolBarConstraintLayout);
        int px4 = Utils.getPixelsFromDps(this, 4);
        int px16 = Utils.getPixelsFromDps(this, 16);
        //align to parent left
        set.connect(!btcPreferred ? R.id.secondary_price : R.id.primary_price, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, px16);
        //align equals after the first item
        set.connect(R.id.equals, ConstraintSet.START, !btcPreferred ? secondaryPrice.getId() : primaryPrice.getId(), ConstraintSet.END, px4);
        //align second item after equals
        set.connect(!btcPreferred ? R.id.primary_price : R.id.secondary_price, ConstraintSet.START, equals.getId(), ConstraintSet.END, px4);
        //align the second item to the baseline of the first
//        set.connect(!btcPreferred ? R.id.primary_price : R.id.secondary_price, ConstraintSet.BASELINE, btcPreferred ? R.id.primary_price : R.id.secondary_price, ConstraintSet.BASELINE, 0);
        // Apply the changes
        set.applyTo(toolBarConstraintLayout);

        new Handler().postDelayed(new Runnable() {
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
        ActivityUTILS.init(this);
        if (PLATFORM_ON)
            APIClient.getInstance().updatePlatform();

        setupNetworking();

        if (!BRWalletManager.getInstance().isCreated()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BRWalletManager.getInstance().setUpTheWallet(BreadActivity.this);
                }
            }).start();

        }

        BRAnimator.showFragmentByTag(this, savedFragmentTag);
        savedFragmentTag = null;

        TxManager.getInstance().onResume(BreadActivity.this);

    }

    private void setupNetworking() {
        if (mNetworkStateReceiver == null) mNetworkStateReceiver = new NetworkChangeReceiver();
        IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkStateReceiver, mNetworkStateFilter);
        ConnectionManager.addConnectionListener(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
        saveVisibleFragment();

    }

    @Override
    protected void onStop() {
        super.onStop();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mNetworkStateReceiver);

        //sync the kv stores
        if (PLATFORM_ON) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    APIClient.getInstance().syncKvStore();
                }
            }).start();

        }

    }

    private void initializeViews() {
        sendButton = (LinearLayout) findViewById(R.id.send_layout);
        receiveButton = (LinearLayout) findViewById(R.id.receive_layout);
        manageText = (TextView) findViewById(R.id.manage_text);
        walletName = (ImageView) findViewById(R.id.wallet_logo);
        menuButton = (LinearLayout) findViewById(R.id.menu_layout);
        primaryPrice = (TextView) findViewById(R.id.primary_price);
        secondaryPrice = (TextView) findViewById(R.id.secondary_price);
        equals = (TextView) findViewById(R.id.equals);
        priceChange = (TextView) findViewById(R.id.price_change_text);
        emptyTip = (TextView) findViewById(R.id.empty_tx_tip);
//        syncLabel = (TextView) findViewById(R.id.syncing_label);
//        syncDate = (TextView) findViewById(R.id.sync_date);
//        loadProgressBar = (ProgressBar) findViewById(R.id.load_wallet_progress);
//        syncProgressBar = (ProgressBar) findViewById(R.id.sync_progress);
        toolBarConstraintLayout = (ConstraintLayout) findViewById(R.id.bread_toolbar);
        walletProgressLayout = (ConstraintLayout) findViewById(R.id.loading_wallet_layout);

        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        toolbarLayout = (LinearLayout) findViewById(R.id.toolbar_layout);
//        syncingLayout = (ConstraintLayout) findViewById(R.id.syncing_layout);
//        recyclerLayout = (LinearLayout) findViewById(R.id.recycler_layout);
        searchIcon = (ImageButton) findViewById(R.id.search_icon);
        barFlipper = (ViewFlipper) findViewById(R.id.tool_bar_flipper);
        searchBar = (BRSearchBar) findViewById(R.id.search_bar);

        final ViewTreeObserver observer = primaryPrice.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (observer.isAlive())
                    observer.removeOnGlobalLayoutListener(this);
                if (uiIsDone) return;
                uiIsDone = true;
                setPriceTags(BRSharedPrefs.getPreferredBTC(BreadActivity.this), false);
            }
        });

    }

    private void saveVisibleFragment() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            return;
        }
        savedFragmentTag = getFragmentManager().getBackStackEntryAt(getFragmentManager().getBackStackEntryCount() - 1).getName();
    }

    //returns x-pos relative to root layout
    private float getRelativeX(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getX();
        else
            return myView.getX() + getRelativeX((View) myView.getParent());
    }

    //returns y-pos relative to root layout
    private float getRelativeY(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getY();
        else
            return myView.getY() + getRelativeY((View) myView.getParent());
    }

    //0 crypto is left, 1 crypto is right
    private int getSwapPosition() {
        if (primaryPrice == null || secondaryPrice == null) {
            return 0;
        }
        return getRelativeX(primaryPrice) < getRelativeX(secondaryPrice) ? 0 : 1;
    }

    @Override
    public void onBalanceChanged(final long balance) {
        updateUI();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void updateUI() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //sleep a little in order to make sure all the commits are finished (like SharePreferences commits)
                String iso = BRSharedPrefs.getIso(BreadActivity.this);

                //current amount in satoshis
                final BigDecimal amount = new BigDecimal(BRSharedPrefs.getCatchedBalance(BreadActivity.this));

                //amount in BTC units
                BigDecimal btcAmount = BRExchange.getBitcoinForSatoshis(BreadActivity.this, amount);
                final String formattedBTCAmount = BRCurrency.getFormattedCurrencyString(BreadActivity.this, "LTC", btcAmount);

                //amount in currency units
                BigDecimal curAmount = BRExchange.getAmountFromSatoshis(BreadActivity.this, iso, amount);
                final String formattedCurAmount = BRCurrency.getFormattedCurrencyString(BreadActivity.this, iso, curAmount);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        boolean preferredBtc = BRSharedPrefs.getPreferredBTC(BreadActivity.this);
//                        if (!isSwapped) {
                        primaryPrice.setText(formattedBTCAmount);
                        secondaryPrice.setText(String.format("%s", formattedCurAmount));
//                        } else {
//                            primaryPrice.setText(String.format(" = %s", preferredBtc ? formattedCurAmount : formattedBTCAmount));
//                            secondaryPrice.setText((preferredBtc ? formattedBTCAmount : formattedCurAmount));
//                        }

                    }
                });
                TxManager.getInstance().updateTxList(BreadActivity.this);
            }
        }).start();

    }

    @Override
    public void onStatusUpdate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                TxManager.getInstance().updateTxList(BreadActivity.this);
            }
        }).start();

    }

    @Override
    public void onIsoChanged(String iso) {
        updateUI();
    }

    @Override
    public void onTxAdded() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                TxManager.getInstance().updateTxList(BreadActivity.this);
            }
        }).start();
    }

    //    private void setWalletLoading() {
//        loadProgressBar.setProgress(progress);
//
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                while (loadProgressBar.getProgress() < 100) {
//                    try {
//                        Thread.sleep(60);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    loadProgressBar.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            progress += 5;
//                            loadProgressBar.setProgress(progress);
//                        }
//                    });
//
//                }
//                walletProgressLayout.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        toolbarLayout.removeView(walletProgressLayout);
//                    }
//                });
//            }
//        }).start();
//    }
//
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
    public void onNameChanged(String name) {
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (isConnected) {
            if (barFlipper != null) {
                if (barFlipper.getDisplayedChild() == 2)
                    barFlipper.setDisplayedChild(0);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {

                    final double progress = BRPeerManager.syncProgress(BRSharedPrefs.getStartHeight(BreadActivity.this));
//                    Log.e(TAG, "run: " + progress);
                    if (progress < 1 && progress > 0) {
                        BRPeerManager.getInstance().startSyncingProgressThread();
                    }
                }
            }).start();

        } else {
            if (barFlipper != null)
                barFlipper.setDisplayedChild(2);
            BRPeerManager.getInstance().stopSyncingProgressThread();
        }

    }


}