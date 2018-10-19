package com.breadwallet.presenter.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRSearchBar;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.fragments.FragmentSend;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.AppEntryPointHandler;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.FontManager;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.services.SyncService;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.SyncTestLogger;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.BalanceUpdateListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.HTTPServer;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

/**
 * Created by byfieldj on 1/16/18.
 * <p>
 * <p>
 * This activity will display pricing and transaction information for any currency the user has access to
 * (BTC, BCH, ETH)
 */

public class WalletActivity extends BRActivity implements InternetManager.ConnectionReceiverListener,
        OnTxListModified, RatesDataSource.OnDataChanged, SyncListener, BalanceUpdateListener {
    private static final String TAG = WalletActivity.class.getName();

    public static final String EXTRA_CRYPTO_REQUEST = "com.breadwallet.presenter.activities.WalletActivity.EXTRA_CRYPTO_REQUEST";

    private static final String SYNCED_THROUGH_DATE_FORMAT = "MM/dd/yy HH:mm";
    private static final float SYNC_PROGRESS_LAYOUT_ANIMATION_ALPHA = 0.0f;

    private BaseTextView mCurrencyTitle;
    private BaseTextView mCurrencyPriceUsd;
    private BaseTextView mBalancePrimary;
    private BaseTextView mBalanceSecondary;
    private Toolbar mToolbar;
    private ImageButton mBackButton;
    private BRButton mSendButton;
    private BRButton mReceiveButton;
    private BRButton mSellButton;
    private LinearLayout mProgressLayout;
    private BaseTextView mSyncStatusLabel;
    private BaseTextView mProgressLabel;
    public ViewFlipper mBarFlipper;
    private BRSearchBar mSearchBar;
    private ImageButton mSearchIcon;
    private ConstraintLayout mToolBarConstraintLayout;
    private LinearLayout mWalletFooter;

    private static final float PRIMARY_TEXT_SIZE = 30;
    private static final float SECONDARY_TEXT_SIZE = 16;

    private static final boolean RUN_LOGGER = false;

    private SyncTestLogger mTestLogger;

    private SyncNotificationBroadcastReceiver mSyncNotificationBroadcastReceiver;
    private String mCurrentWalletIso;

    private BaseWalletManager mWallet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wallet);

        BRSharedPrefs.putIsNewWallet(this, false);

        mCurrencyTitle = findViewById(R.id.currency_label);
        mCurrencyPriceUsd = findViewById(R.id.currency_usd_price);
        mBalancePrimary = findViewById(R.id.balance_primary);
        mBalanceSecondary = findViewById(R.id.balance_secondary);
        mToolbar = findViewById(R.id.bread_bar);
        mBackButton = findViewById(R.id.back_icon);
        mSendButton = findViewById(R.id.send_button);
        mReceiveButton = findViewById(R.id.receive_button);
        mSellButton = findViewById(R.id.sell_button);
        mBarFlipper = findViewById(R.id.tool_bar_flipper);
        mSearchBar = findViewById(R.id.search_bar);
        mSearchIcon = findViewById(R.id.search_icon);
        mToolBarConstraintLayout = findViewById(R.id.bread_toolbar);
        mProgressLayout = findViewById(R.id.progress_layout);
        mSyncStatusLabel = findViewById(R.id.sync_status_label);
        mProgressLabel = findViewById(R.id.syncing_label);
        mWalletFooter = findViewById(R.id.bottom_toolbar_layout1);

        startSyncLoggerIfNeeded();

        setUpBarFlipper();
        mBalancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, PRIMARY_TEXT_SIZE);
        mBalanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, SECONDARY_TEXT_SIZE);

        mSendButton.setHasShadow(false);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSendFragment(null);
            }
        });

        mSendButton.setHasShadow(false);
        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiUtils.showReceiveFragment(WalletActivity.this, true);

            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });

        mSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }
                mBarFlipper.setDisplayedChild(1); //search bar
                mSearchBar.onShow(true);
            }
        });

        mSellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.startWebActivity(WalletActivity.this, HTTPServer.URL_SELL);
            }
        });


        mBalancePrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swap();
            }
        });
        mBalanceSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swap();
            }
        });

        TxManager.getInstance().init(this);

        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        updateUi();
        mWallet = WalletsMaster.getInstance(this).getCurrentWallet(this);

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("BG:" + TAG + ":refreshBalances and address");
                Activity app = WalletActivity.this;
                WalletsMaster.getInstance(app).refreshBalances(app);
                if (mWallet != null) {
                    mWallet.refreshAddress(app);
                }
            }
        });

        // Check if the "Twilight" screen altering app is currently running
        if (Utils.checkIfScreenAlteringAppIsRunning(this, "com.urbandroid.lux")) {
            BRDialog.showSimpleDialog(this, "Screen Altering App Detected", getString(R.string.Android_screenAlteringMessage));
        }


        boolean cryptoPreferred = BRSharedPrefs.isCryptoPreferred(this);

        setPriceTags(cryptoPreferred, false);

    }

    private void startSyncLoggerIfNeeded() {
        if (Utils.isEmulatorOrDebug(this) && RUN_LOGGER) {
            if (mTestLogger != null) {
                mTestLogger.interrupt();
            }
            mTestLogger = new SyncTestLogger(this); //Sync logger
            mTestLogger.start();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //since we have one instance of activity at all times, this is needed to know when a new intent called upon this activity
        AppEntryPointHandler.processDeepLink(this, intent);
        showSendIfNeeded(intent);
    }

    private void updateUi() {
        final BaseWalletManager walletManager = WalletsMaster.getInstance(this).getCurrentWallet(this);
        if (walletManager == null) {
            Log.e(TAG, "updateUi: wallet is null");
            return;
        }

        BigDecimal bigExchangeRate = walletManager.getFiatExchangeRate(this);

        String fiatExchangeRate = CurrencyUtils.getFormattedAmount(this, BRSharedPrefs.getPreferredFiatIso(this), bigExchangeRate);
        String fiatBalance = CurrencyUtils.getFormattedAmount(this, BRSharedPrefs.getPreferredFiatIso(this), walletManager.getFiatBalance(this));
        String cryptoBalance = CurrencyUtils.getFormattedAmount(this, walletManager.getIso(), walletManager.getCachedBalance(this), walletManager.getUiConfiguration().getMaxDecimalPlacesForUi());

        mCurrencyTitle.setText(walletManager.getName());
        mCurrencyPriceUsd.setText(String.format("%s per %s", fiatExchangeRate, walletManager.getIso()));
        mBalancePrimary.setText(fiatBalance);
        mBalanceSecondary.setText(cryptoBalance);

        if (Utils.isNullOrZero(bigExchangeRate)) {
            mCurrencyPriceUsd.setVisibility(View.INVISIBLE);
        } else {
            mCurrencyPriceUsd.setVisibility(View.VISIBLE);
        }

        String startColor = walletManager.getUiConfiguration().getStartColor();
        String endColor = walletManager.getUiConfiguration().getEndColor();
        int currentTheme = UiUtils.getThemeId(this);

        if (currentTheme == R.style.AppTheme_Dark) {
            mSendButton.setColor(getColor(R.color.wallet_footer_button_color_dark));
            mReceiveButton.setColor(getColor(R.color.wallet_footer_button_color_dark));
            mSellButton.setColor(getColor(R.color.wallet_footer_button_color_dark));

            if (endColor != null) {
                mWalletFooter.setBackgroundColor(Color.parseColor(endColor));
            }
        } else {
            if (endColor != null) {
                mSendButton.setColor(Color.parseColor(endColor));
                mReceiveButton.setColor(Color.parseColor(endColor));
                mSellButton.setColor(Color.parseColor(endColor));
            }
        }

        if (endColor != null) {
            //it's a gradient
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{Color.parseColor(startColor), Color.parseColor(endColor)});
            gd.setCornerRadius(0f);
            mToolbar.setBackground(gd);
        } else {
            //it's a solid color
            mToolbar.setBackgroundColor(Color.parseColor(startColor));
        }

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("BG:" + TAG + ":updateTxList");
                TxManager.getInstance().updateTxList(WalletActivity.this);
            }
        });

    }

    private void swap() {
        if (!UiUtils.isClickAllowed()) {
            return;
        }
        BRSharedPrefs.setIsCryptoPreferred(WalletActivity.this, !BRSharedPrefs.isCryptoPreferred(WalletActivity.this));
        setPriceTags(BRSharedPrefs.isCryptoPreferred(WalletActivity.this), true);
    }


    private void setPriceTags(final boolean cryptoPreferred, boolean animate) {
        ConstraintSet set = new ConstraintSet();
        set.clone(mToolBarConstraintLayout);
        if (animate) {
            TransitionManager.beginDelayedTransition(mToolBarConstraintLayout);
        }
        int px8 = Utils.getPixelsFromDps(this, 8);

        // CRYPTO on RIGHT
        if (cryptoPreferred) {
            // Align crypto balance to the right parent
            set.connect(R.id.balance_secondary, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, px8);
            mBalancePrimary.setPadding(0, Utils.getPixelsFromDps(WalletActivity.this, 22), 0, 0);
            mBalanceSecondary.setPadding(0, Utils.getPixelsFromDps(WalletActivity.this, 12), 0, 0);

            // Align swap icon to left of crypto balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_secondary, ConstraintSet.START, px8);

            // Align usd balance to left of swap icon
            set.connect(R.id.balance_primary, ConstraintSet.END, R.id.swap, ConstraintSet.START, px8);

            mBalanceSecondary.setTextSize(PRIMARY_TEXT_SIZE);
            mBalancePrimary.setTextSize(SECONDARY_TEXT_SIZE);

            set.applyTo(mToolBarConstraintLayout);

        } else {
            // CRYPTO on LEFT
            // Align primary to right of parent
            set.connect(R.id.balance_primary, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, px8);

            // Align swap icon to left of usd balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_primary, ConstraintSet.START, px8);


            // Align secondary currency to the left of swap icon
            set.connect(R.id.balance_secondary, ConstraintSet.END, R.id.swap, ConstraintSet.START, px8);
            mBalancePrimary.setPadding(0, Utils.getPixelsFromDps(WalletActivity.this, 12), 0, 0);
            mBalanceSecondary.setPadding(0, Utils.getPixelsFromDps(WalletActivity.this, 22), 0, 0);


            mBalanceSecondary.setTextSize(SECONDARY_TEXT_SIZE);
            mBalancePrimary.setTextSize(PRIMARY_TEXT_SIZE);

            set.applyTo(mToolBarConstraintLayout);
        }
        mBalanceSecondary.setTextColor(getResources().getColor(cryptoPreferred ? R.color.white : R.color.currency_subheading_color, null));
        mBalancePrimary.setTextColor(getResources().getColor(cryptoPreferred ? R.color.currency_subheading_color : R.color.white, null));
        mBalanceSecondary.setTypeface(FontManager.get(this, cryptoPreferred ? "CircularPro-Bold.otf" : "CircularPro-Book.otf"));
        mBalancePrimary.setTypeface(FontManager.get(this, !cryptoPreferred ? "CircularPro-Bold.otf" : "CircularPro-Book.otf"));

        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();

        InternetManager.registerConnectionReceiver(this, this);

        TxManager.getInstance().onResume(this);

        RatesDataSource.getInstance(this).addOnDataChangedListener(this);
        final BaseWalletManager wallet = WalletsMaster.getInstance(this).getCurrentWallet(this);
        if (wallet != null) {
            wallet.addTxListModifiedListener(this);
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    WalletEthManager.getInstance(WalletActivity.this).estimateGasPrice();
                    wallet.refreshCachedBalance(WalletActivity.this);
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            updateUi();
                        }
                    });
                    if (wallet.getConnectStatus() != 2) {
                        wallet.connect(WalletActivity.this);
                    }

                }
            });

            wallet.addBalanceChangedListener(this);

            mCurrentWalletIso = wallet.getIso();

            wallet.addSyncListener(this);
        }

        mSyncNotificationBroadcastReceiver = new SyncNotificationBroadcastReceiver();
        SyncService.registerSyncNotificationBroadcastReceiver(getApplicationContext(), mSyncNotificationBroadcastReceiver);
        SyncService.startService(getApplicationContext(), mCurrentWalletIso);

        AppEntryPointHandler.processDeepLink(this, getIntent());
        showSendIfNeeded(getIntent());

    }

    private void showSendIfNeeded(final Intent intent) {
        final CryptoRequest request = (CryptoRequest) intent.getSerializableExtra(EXTRA_CRYPTO_REQUEST);
        if (request != null) {
            showSendFragment(request);
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        InternetManager.unregisterConnectionReceiver(this, this);
        if (mWallet != null) {
            mWallet.removeSyncListener(this);
        }
        SyncService.unregisterSyncNotificationBroadcastReceiver(getApplicationContext(), mSyncNotificationBroadcastReceiver);
    }

    /* SyncListener methods */
    @Override
    public void syncStopped(String error) {

    }

    @Override
    public void syncStarted() {
        SyncService.startService(getApplicationContext(), mCurrentWalletIso);
    }
    /* SyncListener methods End*/

    private void setUpBarFlipper() {
        mBarFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_enter));
        mBarFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_exit));
    }

    public void resetFlipper() {
        mBarFlipper.setDisplayedChild(0);
    }


    @Override
    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged: isConnected: " + isConnected);
        if (isConnected) {
            if (mBarFlipper != null && mBarFlipper.getDisplayedChild() == 2) {
                mBarFlipper.setDisplayedChild(0);
            }
            SyncService.startService(getApplicationContext(), mCurrentWalletIso);

        } else {
            if (mBarFlipper != null) {
                mBarFlipper.setDisplayedChild(2);
            }

        }
    }

    @Override
    public void onBackPressed() {
        int c = getFragmentManager().getBackStackEntryCount();
        if (c > 0) {
            super.onBackPressed();
            return;
        }
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        if (!isDestroyed()) {
            finish();
        }
    }

    @Override
    public void txListModified(String hash) {
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("UI:" + TAG + ":updateUi");
                updateUi();
            }
        });
    }

    public void updateSyncProgress(double progress) {
        if (progress != SyncService.PROGRESS_FINISH) {
            StringBuffer labelText = new StringBuffer(getString(R.string.SyncingView_syncing));
            labelText.append(' ')
                    .append(NumberFormat.getPercentInstance().format(progress));
            mProgressLabel.setText(labelText);
            mProgressLayout.setVisibility(View.VISIBLE);

            if (mWallet instanceof BaseBitcoinWalletManager) {
                BaseBitcoinWalletManager baseBitcoinWalletManager = (BaseBitcoinWalletManager) mWallet;
                long syncThroughDateInMillis = baseBitcoinWalletManager.getPeerManager().getLastBlockTimestamp() * DateUtils.SECOND_IN_MILLIS;
                String syncedThroughDate = new SimpleDateFormat(SYNCED_THROUGH_DATE_FORMAT).format(syncThroughDateInMillis);
                mSyncStatusLabel.setText(String.format(getString(R.string.SyncingView_syncedThrough), syncedThroughDate));
            }
        } else {
            mProgressLayout.animate()
                    .translationY(-mProgressLayout.getHeight())
                    .alpha(SYNC_PROGRESS_LAYOUT_ANIMATION_ALPHA)
                    .setDuration(DateUtils.SECOND_IN_MILLIS)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mProgressLayout.setVisibility(View.GONE);
                        }
                    });
        }
    }

    @Override
    public void onChanged() {
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateUi();
            }
        });
    }

    @Override
    public void onBalanceChanged(BigDecimal newBalance) {
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateUi();
            }
        });
    }

    /**
     * The {@link SyncNotificationBroadcastReceiver} is responsible for receiving updates from the
     * {@link SyncService} and updating the UI accordingly.
     */
    private class SyncNotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SyncService.ACTION_SYNC_PROGRESS_UPDATE.equals(intent.getAction())) {
                String intentWalletIso = intent.getStringExtra(SyncService.EXTRA_WALLET_ISO);
                double progress = intent.getDoubleExtra(SyncService.EXTRA_PROGRESS, SyncService.PROGRESS_NOT_DEFINED);
                if (mCurrentWalletIso.equals(intentWalletIso)) {
                    if (progress >= SyncService.PROGRESS_START) {
                        WalletActivity.this.updateSyncProgress(progress);
                    } else {
                        Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: Progress not set:" + progress);
                    }
                } else {
                    Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: Wrong wallet. Expected:"
                            + mCurrentWalletIso + " Actual:" + intentWalletIso + " Progress:" + progress);
                }
            }
        }
    }

    public void showSendFragment(final CryptoRequest request) {
        FragmentSend fragmentSend = (FragmentSend) getSupportFragmentManager().findFragmentByTag(FragmentSend.class.getName());
        if (fragmentSend == null) {
            fragmentSend = new FragmentSend();
        }

        Bundle arguments = new Bundle();
        arguments.putSerializable(EXTRA_CRYPTO_REQUEST, request);
        fragmentSend.setArguments(arguments);
        if (!fragmentSend.isAdded()) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                    .add(android.R.id.content, fragmentSend, FragmentSend.class.getName())
                    .addToBackStack(FragmentSend.class.getName()).commit();
        }

    }

}
