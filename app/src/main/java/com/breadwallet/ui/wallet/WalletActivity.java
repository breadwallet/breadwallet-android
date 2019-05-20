package com.breadwallet.ui.wallet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.fragments.FragmentSend;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.FontManager;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.services.SyncService;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.SyncTestLogger;
import com.breadwallet.tools.util.TokenUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.ui.wallet.model.TxFilter;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.HTTPServer;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by byfieldj on 1/16/18.
 * <p>
 * <p>
 * This activity will display pricing and transaction information for any currency the user has access to
 * (BTC, BCH, ETH)
 */

public class WalletActivity extends BRActivity implements InternetManager.ConnectionReceiverListener, SyncListener,
        BRSearchBar.FilterListener, FragmentTxDetails.TxDetailListener {
    private static final String TAG = WalletActivity.class.getName();
    private static final String URBAN_APP_PACKAGE_NAME = "com.urbandroid.lux";

    public static final String EXTRA_CRYPTO_REQUEST = "com.breadwallet.ui.wallet.WalletActivity.EXTRA_CRYPTO_REQUEST";
    protected static final String EXTRA_CURRENCY_CODE = "com.breadwallet.ui.wallet.WalletActivity.EXTRA_CURRENCY_CODE";
    private static final String SYNCED_THROUGH_DATE_FORMAT = "MM/dd/yy HH:mm";
    private static final float SYNC_PROGRESS_LAYOUT_ANIMATION_ALPHA = 0.0f;
    private static final int SEND_SHOW_DELAY = 300;
    private static final boolean RUN_LOGGER = false;

    protected enum FlipperViewState {
        BALANCE, SEARCH, NETWORK
    }

    private BaseTextView mCurrencyTitle;
    private BaseTextView mCurrencyPriceUsd;
    private BaseTextView mBalancePrimary;
    private BaseTextView mBalanceSecondary;
    private Toolbar mToolbar;
    private BRButton mSendButton;
    private BRButton mReceiveButton;
    private BRButton mSellButton;
    private LinearLayout mProgressLayout;
    private BaseTextView mSyncStatusLabel;
    private BaseTextView mProgressLabel;
    public ViewFlipper mBarFlipper;
    private BRSearchBar mSearchBar;
    private ConstraintLayout mToolBarConstraintLayout;
    private LinearLayout mWalletFooter;
    private View mDelistedTokenBanner;
    private TransactionListAdapter mAdapter;
    private WalletViewModel mViewModel;
    private SyncTestLogger mTestLogger;
    private SyncNotificationBroadcastReceiver mSyncNotificationBroadcastReceiver;
    private String mCurrentWalletIso;
    private BaseWalletManager mWallet;

    /**
     * Start the wallet activity for the given currency.
     *
     * @param callerActivity Activity from where WalletActivity is started.
     * @param currencyCode   The currency code of the wallet to be shown.
     */
    public static void start(Activity callerActivity, String currencyCode) {
        Intent intent = new Intent(callerActivity, WalletActivity.class);
        intent.putExtra(EXTRA_CURRENCY_CODE, currencyCode);
        callerActivity.startActivity(intent);
    }

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
        mSendButton = findViewById(R.id.send_button);
        mReceiveButton = findViewById(R.id.receive_button);
        mSellButton = findViewById(R.id.sell_button);
        mBarFlipper = findViewById(R.id.tool_bar_flipper);
        mSearchBar = findViewById(R.id.search_bar);
        mToolBarConstraintLayout = findViewById(R.id.bread_toolbar);
        mProgressLayout = findViewById(R.id.progress_layout);
        mSyncStatusLabel = findViewById(R.id.sync_status_label);
        mProgressLabel = findViewById(R.id.syncing_label);
        mWalletFooter = findViewById(R.id.bottom_toolbar_layout1);
        mDelistedTokenBanner = findViewById(R.id.delisted_token_layout);
        ImageButton backButton = findViewById(R.id.back_icon);
        ImageButton searchIcon = findViewById(R.id.search_icon);

        RecyclerView txList = findViewById(R.id.tx_list);
        txList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new TransactionListAdapter(this, null, item -> {
            UiUtils.showTransactionDetails(this, item);
        });
        txList.setAdapter(mAdapter);

        mCurrentWalletIso = getIntent().hasExtra(EXTRA_CURRENCY_CODE) ? getIntent().getStringExtra(EXTRA_CURRENCY_CODE)
                : WalletsMaster.getInstance().getCurrentWallet(this).getCurrencyCode(); // TODO USE A SINGLE SOURCE FOR CURRENCY

        mViewModel = ViewModelProviders.of(this).get(WalletViewModel.class);
        mViewModel.setCurrencyToWatch(mCurrentWalletIso);
        mViewModel.getBalanceLiveData().observe(this, balance -> {
            if (balance != null) {
                BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(this);
                mCurrencyPriceUsd.setText(String.format(getString(R.string.Account_exchangeRate),
                        balance.getFiatExchangeRate(), walletManager.getCurrencyCode()));
                mBalancePrimary.setText(balance.getFiatBalance());
                mBalanceSecondary.setText(balance.getCryptoBalance());

                if (Utils.isNullOrZero(balance.getExchangeRate())) {
                    mCurrencyPriceUsd.setVisibility(View.INVISIBLE);
                } else {
                    mCurrencyPriceUsd.setVisibility(View.VISIBLE);
                }
            }
        });
        mViewModel.getTxListLiveData().observe(this, newTxList -> {
            if (newTxList != null) {
                mAdapter.setItems(newTxList);
                mAdapter.notifyDataSetChanged();
            }
        });

        startSyncLoggerIfNeeded();

        setUpBarFlipper();

        mSendButton.setHasShadow(false);
        mSendButton.setOnClickListener(view -> showSendFragment(null));

        mSendButton.setHasShadow(false);
        mReceiveButton.setOnClickListener(view -> UiUtils.showReceiveFragment(WalletActivity.this, true));

        backButton.setOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        searchIcon.setOnClickListener(view -> {
            if (!UiUtils.isClickAllowed()) {
                return;
            }
            mBarFlipper.setDisplayedChild(FlipperViewState.SEARCH.ordinal());
            mSearchBar.onShow(true);
        });

        mSellButton.setOnClickListener(view -> UiUtils.startWebActivity(WalletActivity.this, HTTPServer.getPlatformUrl(HTTPServer.URL_SELL)));

        mBalancePrimary.setOnClickListener(view -> swap());
        mBalanceSecondary.setOnClickListener(view -> swap());

        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        updateUi();
        mWallet = WalletsMaster.getInstance().getCurrentWallet(this);

        // Check if the "Twilight" screen altering app is currently running
        if (Utils.checkIfScreenAlteringAppIsRunning(this, URBAN_APP_PACKAGE_NAME)) {
            BRDialog.showSimpleDialog(this, getString(R.string.Alert_ScreenAlteringAppDetected),
                    getString(R.string.Android_screenAlteringMessage));
        }

        boolean cryptoPreferred = BRSharedPrefs.isCryptoPreferred(this);

        setPriceTags(cryptoPreferred, false);

        Button moreInfoButton = mDelistedTokenBanner.findViewById(R.id.more_info_button);
        moreInfoButton.setOnClickListener(view -> UiUtils.showSupportFragment(WalletActivity.this, BRConstants.FAQ_UNSUPPORTED_TOKEN, null));
    }

    /**
     * This token is no longer supported by the BRD app, notify the user.
     */
    private void showDelistedTokenBanner() {
        mDelistedTokenBanner.setVisibility(View.VISIBLE);
    }

    private void startSyncLoggerIfNeeded() {
        if (BuildConfig.DEBUG && RUN_LOGGER) {
            if (mTestLogger != null) {
                mTestLogger.interrupt();
            }
            mTestLogger = new SyncTestLogger(this); //Sync logger
            mTestLogger.start();
        }
    }

    private void updateUi() {
        final BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(this);
        if (walletManager == null) {
            Log.e(TAG, "updateUi: wallet is null");
            return;
        }

        mCurrencyTitle.setText(walletManager.getName());
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
        } else if (endColor != null) {
            mSendButton.setColor(Color.parseColor(endColor));
            mReceiveButton.setColor(Color.parseColor(endColor));
            mSellButton.setColor(Color.parseColor(endColor));
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
    }

    private void swap() {
        if (!UiUtils.isClickAllowed()) {
            return;
        }
        EventUtils.pushEvent(EventUtils.EVENT_AMOUNT_SWAP_CURRENCY);
        BRSharedPrefs.setIsCryptoPreferred(WalletActivity.this,
                !BRSharedPrefs.isCryptoPreferred(WalletActivity.this));
        setPriceTags(BRSharedPrefs.isCryptoPreferred(WalletActivity.this), true);
    }


    private void setPriceTags(final boolean cryptoPreferred, boolean animate) {
        ConstraintSet set = new ConstraintSet();
        set.clone(mToolBarConstraintLayout);
        if (animate) {
            TransitionManager.beginDelayedTransition(mToolBarConstraintLayout);
        }
        int balanceTextMargin = (int) getResources().getDimension(R.dimen.balance_text_margin);
        int balancePrimaryPadding = (int) getResources().getDimension(R.dimen.balance_primary_padding);
        int balanceSecondaryPadding = (int) getResources().getDimension(R.dimen.balance_secondary_padding);
        TypedValue primaryTextSize = new TypedValue();
        getResources().getValue(R.dimen.wallet_balance_primary_text_size, primaryTextSize, true);
        TypedValue secondaryTextSize = new TypedValue();
        getResources().getValue(R.dimen.wallet_balance_secondary_text_size, secondaryTextSize, true);
        // CRYPTO on RIGHT
        if (cryptoPreferred) {
            // Align crypto balance to the right parent
            set.connect(R.id.balance_secondary, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END, balanceTextMargin);
            mBalancePrimary.setPadding(0, balancePrimaryPadding, 0, 0);
            mBalanceSecondary.setPadding(0, balanceSecondaryPadding, 0, 0);

            // Align swap icon to left of crypto balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_secondary, ConstraintSet.START, balanceTextMargin);

            // Align usd balance to left of swap icon
            set.connect(R.id.balance_primary, ConstraintSet.END, R.id.swap, ConstraintSet.START, balanceTextMargin);

            mBalanceSecondary.setTextSize(primaryTextSize.getFloat());
            mBalancePrimary.setTextSize(secondaryTextSize.getFloat());

            set.applyTo(mToolBarConstraintLayout);

        } else {
            // CRYPTO on LEFT
            // Align primary to right of parent
            set.connect(R.id.balance_primary, ConstraintSet.END, ConstraintSet.PARENT_ID,
                    ConstraintSet.END, balanceTextMargin);

            // Align swap icon to left of usd balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_primary, ConstraintSet.START, balanceTextMargin);


            // Align secondary currency to the left of swap icon
            set.connect(R.id.balance_secondary, ConstraintSet.END, R.id.swap, ConstraintSet.START, balanceTextMargin);
            mBalancePrimary.setPadding(0, balanceSecondaryPadding, 0, 0);
            mBalanceSecondary.setPadding(0, balancePrimaryPadding, 0, 0);

            mBalanceSecondary.setTextSize(secondaryTextSize.getFloat());
            mBalancePrimary.setTextSize(primaryTextSize.getFloat());

            set.applyTo(mToolBarConstraintLayout);
        }
        mBalanceSecondary.setTextColor(getResources().getColor(cryptoPreferred
                ? R.color.white : R.color.currency_subheading_color, null));
        mBalancePrimary.setTextColor(getResources().getColor(cryptoPreferred
                ? R.color.currency_subheading_color : R.color.white, null));
        String circularBoldFont = getString(R.string.Font_CircularPro_Bold);
        String circularBookFont = getString(R.string.Font_CircularPro_Book);
        mBalanceSecondary.setTypeface(FontManager.get(this, cryptoPreferred ? circularBoldFont : circularBookFont));
        mBalancePrimary.setTypeface(FontManager.get(this, cryptoPreferred ? circularBookFont : circularBoldFont));

        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mViewModel.refreshBalanceAndTxList();
        InternetManager.registerConnectionReceiver(this, this);

        final BaseWalletManager wallet = WalletsMaster.getInstance().getCurrentWallet(this);
        if (wallet != null) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
                WalletEthManager.getInstance(WalletActivity.this).estimateGasPrice();
                if (wallet.getConnectStatus() != 2) {
                    wallet.connect(WalletActivity.this);
                }

            });

            mCurrentWalletIso = wallet.getCurrencyCode();

            if (!TokenUtil.isTokenSupported(mCurrentWalletIso)) {
                showDelistedTokenBanner();
            }
            wallet.addSyncListener(this);
        }

        mSyncNotificationBroadcastReceiver = new SyncNotificationBroadcastReceiver();
        SyncService.registerSyncNotificationBroadcastReceiver(getApplicationContext(),
                mSyncNotificationBroadcastReceiver);
        SyncService.startService(getApplicationContext(), mCurrentWalletIso);

        showSendIfNeeded(getIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showSendIfNeeded(intent);
    }

    private synchronized void showSendIfNeeded(final Intent intent) {
        final CryptoRequest request = (CryptoRequest) intent.getSerializableExtra(EXTRA_CRYPTO_REQUEST);
        intent.removeExtra(EXTRA_CRYPTO_REQUEST);
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
        SyncService.unregisterSyncNotificationBroadcastReceiver(getApplicationContext(),
                mSyncNotificationBroadcastReceiver);
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
        mBarFlipper.setDisplayedChild(FlipperViewState.BALANCE.ordinal());
    }


    @Override
    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged: isConnected: " + isConnected);
        if (isConnected) {
            if (mBarFlipper != null && mBarFlipper.getDisplayedChild() == FlipperViewState.NETWORK.ordinal()) {
                mBarFlipper.setDisplayedChild(FlipperViewState.BALANCE.ordinal());
            }
            SyncService.startService(getApplicationContext(), mCurrentWalletIso);

        } else {
            if (mBarFlipper != null) {
                mBarFlipper.setDisplayedChild(FlipperViewState.NETWORK.ordinal());
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

    public void updateSyncProgress(double progress) {
        if (progress != SyncService.PROGRESS_FINISH) {
            StringBuffer labelText = new StringBuffer(getString(R.string.SyncingView_syncing));
            labelText.append(' ')
                    .append(NumberFormat.getPercentInstance().format(progress));
            mProgressLabel.setText(labelText);
            mProgressLayout.setVisibility(View.VISIBLE);

            if (mWallet instanceof BaseBitcoinWalletManager) {
                BaseBitcoinWalletManager baseBitcoinWalletManager = (BaseBitcoinWalletManager) mWallet;
                long syncThroughDateInMillis = baseBitcoinWalletManager.getPeerManager()
                        .getLastBlockTimestamp() * DateUtils.SECOND_IN_MILLIS;
                String syncedThroughDate = new SimpleDateFormat(SYNCED_THROUGH_DATE_FORMAT,
                        Locale.getDefault()).format(syncThroughDateInMillis);
                mSyncStatusLabel.setText(String.format(getString(R.string.SyncingView_syncedThrough),
                        syncedThroughDate));
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

    /**
     * The {@link SyncNotificationBroadcastReceiver} is responsible for receiving updates from the
     * {@link SyncService} and updating the UI accordingly.
     */
    private class SyncNotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SyncService.ACTION_SYNC_PROGRESS_UPDATE.equals(intent.getAction())) {
                String intentWalletIso = intent.getStringExtra(SyncService.EXTRA_WALLET_CURRENCY_CODE);
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
        // TODO: Find a better solution.
        if (FragmentSend.isIsSendShown()) {
            return;
        }
        FragmentSend.setIsSendShown(true);
        new Handler().postDelayed(() -> {
            FragmentSend fragmentSend = (FragmentSend) getSupportFragmentManager()
                    .findFragmentByTag(FragmentSend.class.getName());
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
        }, SEND_SHOW_DELAY);

    }

    // region BRSearchBar.FilterListener

    @Override
    public void onFilterChanged(TxFilter filter) {
        mViewModel.setFilter(filter);
    }

    // endregion

    // region FragmentTxDetail.TxDetailListener

    @Override
    public void onDetailUpdate() {
        mViewModel.refreshBalanceAndTxList();
    }

    // endregion
}
