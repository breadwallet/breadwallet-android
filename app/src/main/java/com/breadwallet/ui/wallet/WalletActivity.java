package com.breadwallet.ui.wallet;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.model.PriceDataPoint;
import com.breadwallet.model.PriceChange;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRNotificationBar;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.fragments.FragmentSend;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.FontManager;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.SyncTestLogger;
import com.breadwallet.tools.util.TokenUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.ui.wallet.model.Balance;
import com.breadwallet.ui.wallet.model.TxFilter;
import com.breadwallet.ui.wallet.spark.SparkAdapter;
import com.breadwallet.ui.wallet.spark.SparkView;
import com.breadwallet.ui.wallet.spark.animation.LineSparkAnimator;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.util.SyncUpdateHandler;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.HTTPServer;
import com.platform.util.AppReviewPromptManager;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
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

    public static final String EXTRA_CRYPTO_REQUEST = "com.breadwallet.ui.wallet.WalletActivity.EXTRA_CRYPTO_REQUEST";
    protected static final String EXTRA_CURRENCY_CODE = "com.breadwallet.ui.wallet.WalletActivity.EXTRA_CURRENCY_CODE";
    private static final String SYNCED_THROUGH_DATE_FORMAT = "MM/dd/yy HH:mm";
    private static final String MARKET_CHART_DATE_WITH_HOUR = "MMM d, h:mm";
    private static final String MARKET_CHART_DATE_WITH_YEAR = "MMM d, YYYY";
    private static final float SYNC_PROGRESS_LAYOUT_ANIMATION_ALPHA = 0.0f;
    private static final int SEND_SHOW_DELAY = 300;
    private static final boolean RUN_LOGGER = false;
    private static final int MARKET_CHART_ANIMATION_DURATION = 500;
    private static final float MARKET_CHART_ANIMATION_ACCELERATION = 1.2f;

    private BaseTextView mCurrencyTitle;
    private BaseTextView mCurrencyPriceUsd;
    private BaseTextView mBalancePrimary;
    private BaseTextView mBalanceSecondary;
    private View mSwapIcon;
    private ConstraintLayout mToolbarHeader;
    private AppBarLayout mAppBar;
    private BRButton mSendButton;
    private BRButton mReceiveButton;
    private BRButton mSellButton;
    private LinearLayout mProgressLayout;
    private BaseTextView mSyncStatusLabel;
    private BaseTextView mProgressLabel;
    private BRSearchBar mSearchBar;
    private ConstraintLayout mToolBarConstraintLayout;
    private LinearLayout mWalletFooter;
    private View mDelistedTokenBanner;
    private TransactionListAdapter mAdapter;
    private WalletViewModel mViewModel;
    private SyncTestLogger mTestLogger;
    private String mCurrencyCode;
    private SparkView mSparkView;
    private BaseWalletManager mWallet;
    private BRNotificationBar mNotificationBar;
    private BaseTextView mChartLabel;

    // interval buttons
    private List<BaseTextView> mIntervalButtons;
    private BaseTextView mOneDay;
    private BaseTextView mOneWeek;
    private BaseTextView mOneMonth;
    private BaseTextView mThreeMonths;
    private BaseTextView mOneYear;
    private BaseTextView mThreeYears;

    private BaseTextView mCurrentlySelectedInterval;

    private SparkAdapter mPriceDataAdapter = new SparkAdapter();

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
        mCurrencyCode = getIntent().hasExtra(EXTRA_CURRENCY_CODE) ? getIntent().getStringExtra(EXTRA_CURRENCY_CODE)
                : WalletsMaster.getInstance().getCurrentWallet(this).getCurrencyCode(); // TODO USE A SINGLE SOURCE FOR CURRENCY

        mCurrencyTitle = findViewById(R.id.currency_label);
        mCurrencyPriceUsd = findViewById(R.id.currency_usd_price);
        mBalancePrimary = findViewById(R.id.balance_primary);
        mBalanceSecondary = findViewById(R.id.balance_secondary);
        mSwapIcon = findViewById(R.id.swap);
        mToolbarHeader = findViewById(R.id.bread_bar);
        mSendButton = findViewById(R.id.send_button);
        mReceiveButton = findViewById(R.id.receive_button);
        mSellButton = findViewById(R.id.sell_button);
        mSearchBar = findViewById(R.id.search_bar);
        mToolBarConstraintLayout = findViewById(R.id.bread_toolbar);
        mProgressLayout = findViewById(R.id.progress_layout);
        mSyncStatusLabel = findViewById(R.id.sync_status_label);
        mProgressLabel = findViewById(R.id.syncing_label);
        mWalletFooter = findViewById(R.id.bottom_toolbar_layout1);
        mDelistedTokenBanner = findViewById(R.id.delisted_token_layout);
        ImageButton backButton = findViewById(R.id.back_icon);
        ImageButton searchIcon = findViewById(R.id.search_icon);
        mSparkView = findViewById(R.id.spark_line);
        mAppBar = findViewById(R.id.appbar);
        mNotificationBar = findViewById(R.id.notification_bar);
        mChartLabel = findViewById(R.id.chart_label);

        mOneDay = findViewById(R.id.one_day);
        mOneDay.setOnClickListener(view -> {
            handleIntervalClick((BaseTextView) view);
            mViewModel.getInterval(Interval.ONE_DAY);
        });
        mOneWeek = findViewById(R.id.one_week);
        mOneWeek.setOnClickListener(view -> {
            handleIntervalClick((BaseTextView) view);

            mViewModel.getInterval(Interval.ONE_WEEK);
        });
        mOneMonth = findViewById(R.id.one_month);
        mOneMonth.setOnClickListener(view -> {
            handleIntervalClick((BaseTextView) view);
            mViewModel.getInterval(Interval.ONE_MONTH);
        });
        mThreeMonths = findViewById(R.id.three_months);
        mThreeMonths.setOnClickListener(view -> {
            handleIntervalClick((BaseTextView) view);
            mViewModel.getInterval(Interval.THREE_MONTHS);
        });
        mOneYear = findViewById(R.id.one_year);
        mOneYear.setOnClickListener(view -> {
            handleIntervalClick((BaseTextView) view);
            mViewModel.getInterval(Interval.ONE_YEAR);
        });
        mThreeYears = findViewById(R.id.three_years);
        mThreeYears.setOnClickListener(view -> {
            handleIntervalClick((BaseTextView) view);
            mViewModel.getInterval(Interval.THREE_YEARS);
        });

        mIntervalButtons = Arrays.asList(mOneDay, mOneWeek, mOneMonth, mThreeMonths, mOneYear, mThreeYears);
        handleIntervalClick(mOneYear);

        RecyclerView txList = findViewById(R.id.tx_list);
        txList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new TransactionListAdapter(this, null, item -> {
            UiUtils.showTransactionDetails(this, item);
        });
        txList.setAdapter(mAdapter);

        mViewModel = ViewModelProviders.of(this).get(WalletViewModel.class);
        mViewModel.setTargetCurrencyCode(mCurrencyCode);
        mViewModel.getBalanceLiveData().observe(this, balance -> {
            if (balance != null) {
                mCurrencyPriceUsd.setText(balance.getFiatExchangeRate());
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
        mViewModel.getProgressLiveData().observe(this, progress -> {
            if (progress != null && progress > SyncUpdateHandler.PROGRESS_START) {
                WalletActivity.this.updateSyncProgress(progress);
            } else {
                Log.e(TAG, "onChanged: Progress not set:" + progress);
            }
        });
        mViewModel.getRequestFeedbackLiveData().observe(this, showDialog -> {
            if (Boolean.TRUE == showDialog) {
                showRateAppDialog();
            }
        });
        LineSparkAnimator lineSparkAnimator = new LineSparkAnimator();
        lineSparkAnimator.setDuration(MARKET_CHART_ANIMATION_DURATION);
        lineSparkAnimator.setInterpolator(new AccelerateInterpolator(MARKET_CHART_ANIMATION_ACCELERATION));
        mSparkView.setAdapter(mPriceDataAdapter);
        mSparkView.setSparkAnimator(lineSparkAnimator);
        mSparkView.setScrubListener(value -> {
            if (value != null) {
                PriceDataPoint dataPoint = (PriceDataPoint) value;
                Log.d(TAG, "dataPoint: " + dataPoint.toString());
                showDataPoint(dataPoint);
                EventUtils.pushEvent(String.format(EventUtils.EVENT_WALLET_CHART_SCRUBBED, mCurrencyCode));
            } else {
                PriceChange priceChange = mViewModel.getPriceChange().getValue();
                mChartLabel.setText(priceChange != null ? priceChange.toString() : "");
                Balance balance = mViewModel.getBalanceLiveData().getValue();
                mCurrencyPriceUsd.setText(balance != null ? balance.getFiatExchangeRate() : "");
            }
        });
        // Set the background color for the app bar again when the layout has changed
        mAppBar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateUi());
        mViewModel.getPriceDataPoints().observe(this, priceDataPoints -> {
            if (priceDataPoints != null) {
                mPriceDataAdapter.setDataSet(priceDataPoints);
                mPriceDataAdapter.notifyDataSetChanged();
            }
        });
        mViewModel.getInterval(Interval.ONE_YEAR);
        mViewModel.getPriceChange().observe(this, priceChange -> {
            if (priceChange != null) {
                mChartLabel.setText(priceChange.toString());
            }
        });

        startSyncLoggerIfNeeded();

        mSendButton.setHasShadow(false);
        mSendButton.setOnClickListener(view -> showSendFragment(null));

        mSendButton.setHasShadow(false);
        mReceiveButton.setOnClickListener(view -> UiUtils.showReceiveFragment(WalletActivity.this, true));

        backButton.setOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        searchIcon.setOnClickListener(view -> showSearchBar());

        mSellButton.setOnClickListener(view -> UiUtils.startPlatformBrowser(WalletActivity.this, HTTPServer.getPlatformUrl(HTTPServer.URL_SELL)));

        mBalancePrimary.setOnClickListener(view -> swap());
        mBalanceSecondary.setOnClickListener(view -> swap());
        mSwapIcon.setOnClickListener(view -> swap());

        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        updateUi();
        mWallet = WalletsMaster.getInstance().getCurrentWallet(this);

        boolean cryptoPreferred = BRSharedPrefs.isCryptoPreferred(this);

        setPriceTags(cryptoPreferred, false);

        Button moreInfoButton = mDelistedTokenBanner.findViewById(R.id.more_info_button);
        moreInfoButton.setOnClickListener(view -> UiUtils.showSupportFragment(WalletActivity.this, BRConstants.FAQ_UNSUPPORTED_TOKEN, null));

        EventUtils.pushEvent(String.format(EventUtils.EVENT_WALLET_APPEARED, mCurrencyCode));
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
            mToolbarHeader.setBackground(gd);
            mAppBar.setBackground(gd);
        } else {
            //it's a solid color
            mToolbarHeader.setBackgroundColor(Color.parseColor(startColor));
            mAppBar.setBackgroundColor(Color.parseColor(startColor));
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

        mBalanceSecondary.setTextColor(getResources().getColor(cryptoPreferred
                ? R.color.white : R.color.currency_subheading_color, null));
        mBalancePrimary.setTextColor(getResources().getColor(cryptoPreferred
                ? R.color.currency_subheading_color : R.color.white, null));

        String circularBookFont = getString(R.string.Font_CircularPro_Book);
        mBalanceSecondary.setTypeface(FontManager.get(this, circularBookFont));
        mBalancePrimary.setTypeface(FontManager.get(this, circularBookFont));

        int balanceTextMargin = (int) getResources().getDimension(R.dimen.balance_text_margin);
        TypedValue primaryTextSize = new TypedValue();
        getResources().getValue(R.dimen.wallet_balance_primary_text_size, primaryTextSize, true);
        TypedValue secondaryTextSize = new TypedValue();
        getResources().getValue(R.dimen.wallet_balance_secondary_text_size, secondaryTextSize, true);
        // CRYPTO on RIGHT
        if (cryptoPreferred) {
            // Align crypto balance to the right parent
            // Align swap icon to left of crypto balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_secondary, ConstraintSet.START, balanceTextMargin);
            // Align usd balance to left of swap icon
            set.connect(R.id.balance_primary, ConstraintSet.END, R.id.swap, ConstraintSet.START, balanceTextMargin);
            mBalanceSecondary.setTextSize(primaryTextSize.getFloat());
            mBalancePrimary.setTextSize(secondaryTextSize.getFloat());
        } else {
            // CRYPTO on LEFT
            // Align primary to right of parent
            set.connect(R.id.balance_primary, ConstraintSet.END, ConstraintSet.PARENT_ID,
                    ConstraintSet.END, balanceTextMargin);
            // Align swap icon to left of usd balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_primary, ConstraintSet.START, balanceTextMargin);
            // Align secondary currency to the left of swap icon
            set.connect(R.id.balance_secondary, ConstraintSet.END, R.id.swap, ConstraintSet.START, balanceTextMargin);
            mBalanceSecondary.setTextSize(secondaryTextSize.getFloat());
            mBalancePrimary.setTextSize(primaryTextSize.getFloat());
        }
        set.applyTo(mToolBarConstraintLayout);
        mAdapter.notifyDataSetChanged();
    }

    private void handleIntervalClick(BaseTextView selectedView) {
        if (mCurrentlySelectedInterval == selectedView) {
            return;
        }
        EventUtils.pushEvent(String.format(EventUtils.EVENT_WALLET_CHART_AXIS_TOGGLE, mCurrencyCode));

        int deselectedColor = getColor(R.color.trans_white);
        int selectedColor = getColor(R.color.white);
        for (BaseTextView intervalButton : mIntervalButtons) {
            intervalButton.setTextColor(deselectedColor);
        }

        mCurrentlySelectedInterval = selectedView;
        selectedView.setTextColor(selectedColor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mViewModel.refreshBalanceAndTxList();
        InternetManager.registerConnectionReceiver(this, this);

        final BaseWalletManager wallet = WalletsMaster.getInstance().getCurrentWallet(this);
        if (wallet != null) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
                WalletEthManager.getInstance(getApplicationContext()).estimateGasPrice();
                if (wallet.getConnectStatus() != 2) {
                    wallet.connect(WalletActivity.this);
                }

            });

            mCurrencyCode = wallet.getCurrencyCode();

            if (!TokenUtil.isTokenSupported(mCurrencyCode)) {
                showDelistedTokenBanner();
            }
            wallet.addSyncListener(this);
        }

        SyncUpdateHandler.INSTANCE.startWalletSync(getApplicationContext(), mCurrencyCode);
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
    }

    /* SyncListener methods */
    @Override
    public void syncStopped(String error) {

    }

    @Override
    public void syncStarted() {
        SyncUpdateHandler.INSTANCE.startWalletSync(getApplicationContext(), mCurrencyCode);
    }
    /* SyncListener methods End*/

    @Override
    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged: isConnected: " + isConnected);
        if (isConnected) {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.GONE);
            }
        } else {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.VISIBLE);
                mNotificationBar.bringToFront();
            }
        }
    }

    @Override
    public void onBackPressed() {
        int entryCount = getFragmentManager().getBackStackEntryCount();
        if (entryCount > 0) {
            super.onBackPressed();
        } else {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
            if (!isDestroyed()) {
                finish();
            }
        }
    }

    public void updateSyncProgress(double progress) {
        if (progress != SyncUpdateHandler.PROGRESS_FINISH) {
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

    @Override
    public void onDismiss() {
        Animator searchBarAnimation = AnimatorInflater.loadAnimator(this, R.animator.to_top);
        searchBarAnimation.setTarget(mSearchBar);
        searchBarAnimation.start();
    }

    // endregion

    // region FragmentTxDetail.TxDetailListener

    @Override
    public void onDetailUpdate() {
        mViewModel.refreshBalanceAndTxList();
    }

    // endregion

    /**
     * Show the dialog to request a review in Google Play if there are no other fragment over the screen.
     */
    private void showRateAppDialog() {
        if (isFragmentOnTop()) {
            return;
        }

        EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_DISPLAYED);
        BRDialog.showCustomDialog(
                this,
                getString(R.string.RateAppPrompt_Title_Android),
                getString(R.string.RateAppPrompt_Body_Android),
                getString(R.string.RateAppPrompt_Button_RateApp_Android),
                getString(R.string.RateAppPrompt_Button_Dismiss_Android),
                brDialogView -> { // positiveButton
                    EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_GOOGLE_PLAY_TRIGGERED);
                    AppReviewPromptManager.INSTANCE.openGooglePlay(this);
                    brDialogView.dismiss();
                },
                brDialogView -> { // negativeButton
                    brDialogView.dismiss();
                },
                brDialogView -> { // onDismiss
                    EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_DISMISSED);
                    mViewModel.onRateAppPromptDismissed();
                },
                0
        );
    }

    /**
     * Check the back stack to see if send or receive fragment are present and look for FragmentTxDetail by tag.
     */
    private boolean isFragmentOnTop() {
        return getSupportFragmentManager().getBackStackEntryCount() > 0
                || getSupportFragmentManager().findFragmentByTag(FragmentTxDetails.TAG) != null;
    }

    private void showDataPoint(PriceDataPoint dataPoint) {
        DateFormat dateFormat;
        switch (mViewModel.getChartInterval().getValue()) {
            case ONE_DAY:
            case ONE_WEEK:
                dateFormat = new SimpleDateFormat(MARKET_CHART_DATE_WITH_HOUR, Locale.getDefault());
                break;
            default:
                dateFormat = new SimpleDateFormat(MARKET_CHART_DATE_WITH_YEAR, Locale.getDefault());
                break;
        }
        mChartLabel.setText(dateFormat.format(dataPoint.getTime()));
        mCurrencyPriceUsd.setText(String.format(Locale.getDefault(), "%.2f", dataPoint.getClosePrice()));
    }

    private void showSearchBar() {
        if (!UiUtils.isClickAllowed()) {
            return;
        }
        mSearchBar.onShow(true);
        Animator searchBarAnimation = AnimatorInflater.loadAnimator(this, R.animator.from_top);
        searchBarAnimation.setTarget(mSearchBar);
        searchBarAnimation.start();
    }

}
