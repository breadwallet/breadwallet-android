package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRNotificationBar;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.adapter.WalletListAdapter;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.AppEntryPointHandler;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.PromptManager;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BalanceUpdateListener;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.platform.APIClient;
import com.platform.HTTPServer;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Home activity that will show a list of a user's wallets
 */

public class HomeActivity extends BRActivity implements InternetManager.ConnectionReceiverListener, RatesDataSource.OnDataChanged, BalanceUpdateListener {

    private static final String TAG = HomeActivity.class.getSimpleName();
    public static final String EXTRA_DATA = "com.breadwallet.presenter.activities.WalletActivity.EXTRA_DATA";

    public static final String CCC_CURRENCY_CODE = "CCC";
    public static final int MAX_NUMBER_OF_CHILDREN = 2;

    private RecyclerView mWalletRecycler;
    private WalletListAdapter mAdapter;
    private BaseTextView mFiatTotal;
    private BRNotificationBar mNotificationBar;
    private ConstraintLayout mBuyLayout;
    private LinearLayout mTradeLayout;
    private LinearLayout mMenuLayout;
    private LinearLayout mListGroupLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mWalletRecycler = findViewById(R.id.rv_wallet_list);
        mFiatTotal = findViewById(R.id.total_assets_usd);
        mNotificationBar = findViewById(R.id.notification_bar);
        mBuyLayout = findViewById(R.id.buy_layout);
        mTradeLayout = findViewById(R.id.trade_layout);
        mMenuLayout = findViewById(R.id.menu_layout);
        mListGroupLayout = findViewById(R.id.list_group_layout);

        mBuyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = String.format(BRConstants.CURRENCY_PARAMETER_STRING_FORMAT, HTTPServer.URL_BUY,
                        WalletBitcoinManager.getInstance(HomeActivity.this).getCurrencyCode());
                UiUtils.startWebActivity(HomeActivity.this, url);
            }
        });
        mTradeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiUtils.startWebActivity(HomeActivity.this, HTTPServer.URL_TRADE);
            }
        });
        mMenuLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_SETTINGS);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        mWalletRecycler.setLayoutManager(new LinearLayoutManager(this));
        mWalletRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, mWalletRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if (position >= mAdapter.getItemCount() || position < 0) return;
                if (mAdapter.getItemViewType(position) == 0) {
                    BRSharedPrefs.putCurrentWalletCurrencyCode(HomeActivity.this, mAdapter.getItemAt(position).getCurrencyCode());
                    Intent newIntent = new Intent(HomeActivity.this, WalletActivity.class);
                    startActivity(newIntent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                } else {
                    Intent intent = new Intent(HomeActivity.this, AddWalletsActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {
            }
        }));
        processIntentData(getIntent());

        ImageView buyBell = findViewById(R.id.buy_bell);
        boolean isBellNeeded = BRSharedPrefs.getFeatureEnabled(this, APIClient.FeatureFlags.BUY_NOTIFICATION.toString())
                && CurrencyUtils.isBuyNotificationNeeded(this);
        buyBell.setVisibility(isBellNeeded ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntentData(intent);
    }

    private synchronized void processIntentData(Intent intent) {
        String data = intent.getStringExtra(EXTRA_DATA);
        if (Utils.isNullOrEmpty(data)) {
            data = intent.getDataString();
        }
        if (data != null) {
            AppEntryPointHandler.processDeepLink(this, data);
        }
    }

    private void showNextPromptIfNeeded() {
        PromptManager.PromptItem toShow = PromptManager.getInstance().nextPrompt(this);
        if (toShow != null) {
            View promptView = PromptManager.promptInfo(this, toShow);
            if (mListGroupLayout.getChildCount() >= MAX_NUMBER_OF_CHILDREN) {
                mListGroupLayout.removeViewAt(0);
            }
            mListGroupLayout.addView(promptView, 0);
            EventUtils.pushEvent(EventUtils.EVENT_PROMPT_PREFIX
                    + PromptManager.getPromptName(toShow) + EventUtils.EVENT_PROMPT_SUFFIX_DISPLAYED);
        } else {
            Log.i(TAG, "showNextPrompt: nothing to show");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showNextPromptIfNeeded();
        WalletsMaster.getInstance(this).addBalanceUpdateListener(this);
        populateWallets();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.startObserving();
            }
        }, DateUtils.SECOND_IN_MILLIS / 2);
        InternetManager.registerConnectionReceiver(this, this);
        updateUi();
        RatesDataSource.getInstance(this).addOnDataChangedListener(this);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("BG:" + TAG + ":refreshBalances and address");
                WalletsMaster.getInstance(HomeActivity.this).refreshBalances(HomeActivity.this);
                BaseWalletManager walletManager = WalletsMaster.getInstance(HomeActivity.this).getCurrentWallet(HomeActivity.this);
                if (walletManager != null) {
                    walletManager.refreshAddress(HomeActivity.this);
                }
            }
        });
        onConnectionChanged(InternetManager.getInstance().isConnected(this));
    }

    private void populateWallets() {
        ArrayList<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(this).getAllWallets(this));
        mAdapter = new WalletListAdapter(this, list);
        mWalletRecycler.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        InternetManager.unregisterConnectionReceiver(this, this);
        mAdapter.stopObserving();
        WalletsMaster.getInstance(this).removeBalanceUpdateListener(this);
    }

    private void updateUi() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final BigDecimal fiatTotalAmount = WalletsMaster.getInstance(HomeActivity.this).getAggregatedFiatBalance(HomeActivity.this);
                if (fiatTotalAmount == null) {
                    Log.e(TAG, "updateUi: fiatTotalAmount is null");
                    return;
                }
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        mFiatTotal.setText(CurrencyUtils.getFormattedAmount(HomeActivity.this,
                                BRSharedPrefs.getPreferredFiatIso(HomeActivity.this), fiatTotalAmount));
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged: isConnected: " + isConnected);
        if (isConnected) {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.GONE);
            }
            if (mAdapter != null) {
                mAdapter.startObserving();
            }
        } else {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.VISIBLE);
                mNotificationBar.bringToFront();
            }
        }
    }

    public void closeNotificationBar() {
        mNotificationBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onChanged() {
        updateUi();
    }

    @Override
    public void onBalanceChanged(BigDecimal balance) {
        updateUi();
    }
}