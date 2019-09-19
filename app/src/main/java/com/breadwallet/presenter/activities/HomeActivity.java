/**
 * BreadWallet
 * <p/>
 * Created by byfieldj on <jade@breadwallet.com> 1/17/18.
 * Copyright (c) 2019 breadwallet LLC
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

package com.breadwallet.presenter.activities;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.model.Experiments;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRNotificationBar;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.viewmodels.HomeViewModel;
import com.breadwallet.repository.ExperimentsRepositoryImpl;
import com.breadwallet.tools.adapter.WalletListAdapter;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.AppEntryPointHandler;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.PromptManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.ui.notification.InAppNotificationActivity;
import com.breadwallet.ui.wallet.WalletActivity;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.ethereum.WalletTokenManager;
import com.platform.HTTPServer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Home activity that will show a list of a user's wallets
 */

public class HomeActivity extends BRActivity implements InternetManager.ConnectionReceiverListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    public static final String EXTRA_DATA = "com.breadwallet.presenter.activities.HomeActivity.EXTRA_DATA";
    public static final String EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID = "com.breadwallet.presenter.activities.HomeActivity.EXTRA_PUSH_CAMPAIGN_ID";
    private static final String NETWORK_TESTNET = "TESTNET";
    private static final String NETWORK_MAINNET = "MAINNET";

    private RecyclerView mWalletRecycler;
    private WalletListAdapter mAdapter;
    private BaseTextView mFiatTotal;
    private BRNotificationBar mNotificationBar;
    private ConstraintLayout mBuyLayout;
    private LinearLayout mTradeLayout;
    private LinearLayout mMenuLayout;
    private LinearLayout mListGroupLayout;
    private HomeViewModel mViewModel;
    private BaseTextView mBuyMenuLabel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Show build info as a watermark on non prod builds like: TESTNET 3.10.1 build 1
        setUpBuildInfoLabel();

        mWalletRecycler = findViewById(R.id.rv_wallet_list);
        mFiatTotal = findViewById(R.id.total_assets_usd);
        mNotificationBar = findViewById(R.id.notification_bar);
        mBuyLayout = findViewById(R.id.buy_layout);
        mTradeLayout = findViewById(R.id.trade_layout);
        mMenuLayout = findViewById(R.id.menu_layout);
        mListGroupLayout = findViewById(R.id.list_group_layout);
        mBuyMenuLabel = findViewById(R.id.buy_text_view);

        boolean showBuyAndSell = BRConstants.USD.equals(BRSharedPrefs.getPreferredFiatIso(this)) &&
                ExperimentsRepositoryImpl.INSTANCE.isExperimentActive(Experiments.BUY_SELL_MENU_BUTTON);
        mBuyMenuLabel.setText(showBuyAndSell ? R.string.HomeScreen_buyAndSell : R.string.HomeScreen_buy);
        Map<String, String> eventAttributes = new HashMap<>();
        eventAttributes.put(EventUtils.EVENT_ATTRIBUTE_SHOW, Boolean.toString(showBuyAndSell));
        EventUtils.pushEvent(EventUtils.EVENT_EXPERIMENT_BUY_SELL_MENU_BUTTON, eventAttributes);
        mBuyLayout.setOnClickListener(view -> {
            Map<String, String> clickAttributes = new HashMap<>();
            eventAttributes.put(EventUtils.EVENT_ATTRIBUTE_BUY_AND_SELL, Boolean.toString(showBuyAndSell));
            EventUtils.pushEvent(EventUtils.EVENT_HOME_DID_TAP_BUY, clickAttributes);
            String url = String.format(BRConstants.CURRENCY_PARAMETER_STRING_FORMAT,
                    HTTPServer.getPlatformUrl(HTTPServer.URL_BUY),
                    WalletBitcoinManager.getInstance(HomeActivity.this).getCurrencyCode());
            UiUtils.startPlatformBrowser(HomeActivity.this, url);
        });
        mTradeLayout.setOnClickListener(view -> UiUtils.startPlatformBrowser(HomeActivity.this, HTTPServer.getPlatformUrl(HTTPServer.URL_TRADE)));
        mMenuLayout.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_SETTINGS);
            startActivity(intent);
            overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
        });
        mWalletRecycler.setLayoutManager(new LinearLayoutManager(this));
        mWalletRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, mWalletRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if (position >= mAdapter.getItemCount() || position < 0) {
                    return;
                }
                if (mAdapter.getItemViewType(position) == 0) {
                    String currencyCode = mAdapter.getItemAt(position).getCurrencyCode();
                    BRSharedPrefs.putCurrentWalletCurrencyCode(HomeActivity.this, currencyCode);
                    // Use BrdWalletActivity to show rewards view and animation if BRD and not shown yet.
                    if (WalletTokenManager.BRD_CURRENCY_CODE.equalsIgnoreCase(currencyCode)) {
                        if (!BRSharedPrefs.getRewardsAnimationShown(HomeActivity.this)) {
                            Map<String, String> attributes = new HashMap<>();
                            attributes.put(EventUtils.EVENT_ATTRIBUTE_CURRENCY, WalletTokenManager.BRD_CURRENCY_CODE);
                            EventUtils.pushEvent(EventUtils.EVENT_REWARDS_OPEN_WALLET, attributes);
                        }
                        BrdWalletActivity.start(HomeActivity.this, currencyCode);
                    } else {
                        WalletActivity.start(HomeActivity.this, currencyCode);
                    }
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
        boolean isBellNeeded = ExperimentsRepositoryImpl.INSTANCE.isExperimentActive(Experiments.BUY_NOTIFICATION)
                && CurrencyUtils.isBuyNotificationNeeded(this);
        buyBell.setVisibility(isBellNeeded ? View.VISIBLE : View.INVISIBLE);

        mAdapter = new WalletListAdapter(this);
        mWalletRecycler.setAdapter(mAdapter);

        // Get ViewModel, observe updates to Wallet and aggregated balance data
        mViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        mViewModel.getWallets().observe(this, wallets -> mAdapter.setWallets(wallets));

        mViewModel.getAggregatedFiatBalance().observe(this, aggregatedFiatBalance -> {
            if (aggregatedFiatBalance == null) {
                Log.e(TAG, "fiatTotalAmount is null");
                return;
            }
            mFiatTotal.setText(CurrencyUtils.getFormattedAmount(HomeActivity.this,
                    BRSharedPrefs.getPreferredFiatIso(HomeActivity.this), aggregatedFiatBalance));
        });
        mViewModel.getNotificationLiveData().observe(this, notification -> {
            if (notification != null) {
                InAppNotificationActivity.Companion.start(HomeActivity.this, notification);
            }
        });
        mViewModel.checkForInAppNotification();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntentData(intent);
    }

    private synchronized void processIntentData(Intent intent) {
        if (intent.hasExtra(EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID)) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put(EventUtils.EVENT_ATTRIBUTE_CAMPAIGN_ID, intent.getStringExtra(EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID));
            EventUtils.pushEvent(EventUtils.EVENT_MIXPANEL_APP_OPEN, attributes);
            EventUtils.pushEvent(EventUtils.EVENT_PUSH_NOTIFICATION_OPEN);
        }

        String data = intent.getStringExtra(EXTRA_DATA);
        if (Utils.isNullOrEmpty(data)) {
            data = intent.getDataString();
        }
        if (data != null) {
            AppEntryPointHandler.processDeepLink(this, data);
        }
    }

    private void showNextPromptIfNeeded() {
        PromptManager.PromptItem toShow = PromptManager.nextPrompt(this);
        if (toShow != null) {
            View promptView = PromptManager.promptInfo(this, toShow);
            if (mListGroupLayout.getChildCount() > 0) {
                mListGroupLayout.removeAllViews();
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
        InternetManager.registerConnectionReceiver(this, this);
        onConnectionChanged(InternetManager.getInstance().isConnected(this));
        mViewModel.refreshWallets();
    }

    @Override
    protected void onPause() {
        super.onPause();
        InternetManager.unregisterConnectionReceiver(this, this);
    }

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

    public void closeNotificationBar() {
        mNotificationBar.setVisibility(View.INVISIBLE);
    }

    private void setUpBuildInfoLabel() {
        TextView buildInfoTextView = findViewById(R.id.testnet_label);
        String network = BuildConfig.BITCOIN_TESTNET ? NETWORK_TESTNET : NETWORK_MAINNET;
        String buildInfo = network + " " + BuildConfig.VERSION_NAME + " build " + BuildConfig.BUILD_VERSION;
        buildInfoTextView.setText(buildInfo);
        buildInfoTextView.setVisibility(BuildConfig.BITCOIN_TESTNET || BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
    }
}
