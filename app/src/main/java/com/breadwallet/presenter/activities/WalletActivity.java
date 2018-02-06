package com.breadwallet.presenter.activities;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.TransitionManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRSearchBar;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.FontManager;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.SyncManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWallet;
import com.platform.HTTPServer;

import java.math.BigDecimal;

/**
 * Created by byfieldj on 1/16/18.
 * <p>
 * <p>
 * This activity will display pricing and transaction information for any currency the user has access to
 * (BTC, BCH, ETH)
 */

public class WalletActivity extends BRActivity implements InternetManager.ConnectionReceiverListener {
    private static final String TAG = WalletActivity.class.getName();
    BRText mCurrencyTitle;
    BRText mCurrencyPriceUsd;
    BRText mBalancePrimary;
    BRText mBalanceSecondary;
    Toolbar mToolbar;
    ImageButton mBackButton;
    BRButton mSendButton;
    BRButton mReceiveButton;
    BRButton mBuyButton;
    BRText mBalanceLabel;

    public ViewFlipper barFlipper;
    private BRSearchBar searchBar;
    private ImageButton mSearchIcon;
    private ImageButton mSwap;
    private ConstraintLayout toolBarConstraintLayout;

    private String mDefaultTextPrimary;
    private String mDefaultTextSecondary;
    private BaseWallet currentWallet;

    private static WalletActivity app;

    public static WalletActivity getApp() {
        return app;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wallet);

        mCurrencyTitle = findViewById(R.id.currency_label);
        mCurrencyPriceUsd = findViewById(R.id.currency_usd_price);
        mBalancePrimary = findViewById(R.id.balance_primary);
        mBalanceSecondary = findViewById(R.id.balance_secondary);
        mToolbar = findViewById(R.id.bread_bar);
        mBackButton = findViewById(R.id.back_icon);
        mSendButton = findViewById(R.id.send_button);
        mReceiveButton = findViewById(R.id.receive_button);
        mBuyButton = findViewById(R.id.buy_button);
        barFlipper = findViewById(R.id.tool_bar_flipper);
        searchBar = findViewById(R.id.search_bar);
        mSearchIcon = findViewById(R.id.search_icon);
        toolBarConstraintLayout = findViewById(R.id.bread_toolbar);
        mSwap = findViewById(R.id.swap);
        mBalanceLabel = findViewById(R.id.balance_label);

        setUpBarFlipper();

        TxManager.getInstance().init(this);

        BRAnimator.init(this);
        mBalancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);//make it the size it should be after animation to get the X
        mBalanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);//make it the size it should be after animation to get the X


        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showSendFragment(WalletActivity.this, null);

            }
        });

        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showReceiveFragment(WalletActivity.this, false);

            }
        });


        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BRAnimator.isClickAllowed()) return;
                barFlipper.setDisplayedChild(1); //search bar
                searchBar.onShow(true);
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

        t1Size = 28;
        t2Size = 14;

        mDefaultTextPrimary = mBalancePrimary.getText().toString();
        mDefaultTextSecondary = mBalanceSecondary.getText().toString();

        TxManager.getInstance().init(this);

    }

    private void updateUi() {
        BaseWallet currentWallet = WalletsMaster.getInstance(this).getCurrentWallet(this);
        Log.e(TAG, "updateUi: " + currentWallet.getIso(this));

        if (currentWallet.getUiConfiguration().buyVisible) {
            mBuyButton.setVisibility(View.VISIBLE);
            mBuyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(WalletActivity.this, WebViewActivity.class);
                    intent.putExtra("url", HTTPServer.URL_BUY);
                    Activity app = WalletActivity.this;
                    app.startActivity(intent);
                    app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
                }
            });

        } else {
            mBuyButton.setVisibility(View.GONE);
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.5f
            );
            mSendButton.setLayoutParams(param);
            mReceiveButton.setLayoutParams(param);
            mBuyButton.setLayoutParams(param);
        }

        String fiatIso = BRSharedPrefs.getPreferredFiatIso(this);
        String fiatRate = CurrencyUtils.getFormattedCurrencyString(this, fiatIso, new BigDecimal(CurrencyDataSource.getInstance(this).getCurrencyByIso(fiatIso).rate));
        String cryptoIso = currentWallet.getIso(this);

//        String fiatBalance = CurrencyUtils.getFormattedCurrencyString(this, BRSharedPrefs.getPreferredFiatIso(this), new BigDecimal(ExchangeUtils....(currentWallet.getCachedBalance(this)));
        String cryptoBalance = CurrencyUtils.getFormattedCurrencyString(this, currentWallet.getIso(this), new BigDecimal(currentWallet.getCachedBalance(this)));

        String rateString = String.format("%s per %s", fiatRate, cryptoIso);

        mCurrencyTitle.setText(currentWallet.getName(this));
        mCurrencyPriceUsd.setText(rateString);
        mBalancePrimary.setText("-------");
        mBalanceSecondary.setText(cryptoBalance);
        String color = currentWallet.getUiConfiguration().colorHex;
        Log.e(TAG, "onResume: " + currentWallet.getName(this));
        Log.e(TAG, "onResume: color:" + color);
        mToolbar.setBackgroundColor(Color.parseColor(color));
        mSendButton.setColor(Color.parseColor(color));
        mReceiveButton.setColor(Color.parseColor(color));
        mBuyButton.setColor(Color.parseColor(color));
    }

    private void swap() {
        if (!BRAnimator.isClickAllowed()) return;
        boolean b = !BRSharedPrefs.isCryptoPreferred(this);
        setPriceTags(b, true);
        BRSharedPrefs.setIsCryptoPreferred(this, b);
    }

    private void setPriceTags(final boolean btcPreferred, boolean animate) {
        //mBalanceSecondary.setTextSize(!btcPreferred ? t1Size : t2Size);
        //mBalancePrimary.setTextSize(!btcPreferred ? t2Size : t1Size);
        ConstraintSet set = new ConstraintSet();
        set.clone(toolBarConstraintLayout);
        if (animate)
            TransitionManager.beginDelayedTransition(toolBarConstraintLayout);
        int px8 = Utils.getPixelsFromDps(this, 8);
        int px16 = Utils.getPixelsFromDps(this, 14);
        //align to parent left
        set.connect(!btcPreferred ? R.id.balance_secondary : R.id.balance_primary, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, px16);
        //align swap symbol after the first item
        set.connect(R.id.swap, ConstraintSet.START, !btcPreferred ? mBalanceSecondary.getId() : mBalancePrimary.getId(), ConstraintSet.END, px8);
        //align second item after swap symbol
        set.connect(!btcPreferred ? R.id.balance_primary : R.id.balance_secondary, ConstraintSet.START, mSwap.getId(), ConstraintSet.END, px8);

        //set.connect(R.id.balance_secondary, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, px8);
        //set.connect(R.id.balance_primary, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, px8);


        if (!btcPreferred) {
            mBalanceSecondary.setTextColor(getResources().getColor(R.color.white, null));
            mBalancePrimary.setTextColor(getResources().getColor(R.color.currency_subheading_color, null));
            mBalanceSecondary.setTypeface(FontManager.get(this, "CircularPro-Bold.otf"));

        } else {
            mBalanceSecondary.setTextColor(getResources().getColor(R.color.currency_subheading_color, null));
            mBalancePrimary.setTextColor(getResources().getColor(R.color.white, null));
            mBalanceSecondary.setTypeface(FontManager.get(this, "CircularPro-Book.otf"));


        }

        // Apply the changes
        set.applyTo(toolBarConstraintLayout);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUI();

                mBalanceSecondary.setTextSize(!btcPreferred ? t1Size : t2Size);
                mBalancePrimary.setTextSize(!btcPreferred ? t2Size : t1Size);

            }
        }, toolBarConstraintLayout.getLayoutTransition().getDuration(LayoutTransition.CHANGE_APPEARING));
    }

    public void updateUI() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName(Thread.currentThread().getName() + ":updateUI");
                //sleep a little in order to make sure all the commits are finished (like SharePreferences commits)
                String iso = BRSharedPrefs.getPreferredFiatIso(WalletActivity.this);

                //current amount in satoshis
                final BigDecimal amount = new BigDecimal(BRSharedPrefs.getCatchedBalance(WalletActivity.this));

                //amount in BTC units
                BigDecimal btcAmount = BRExchange.getBitcoinForSatoshis(WalletActivity.this, amount);
                final String formattedBTCAmount = BRCurrency.getFormattedCurrencyString(WalletActivity.this, "BTC", btcAmount);

                //amount in currency units
                BigDecimal curAmount = BRExchange.getAmountFromSatoshis(WalletActivity.this, iso, amount);
                final String formattedCurAmount = BRCurrency.getFormattedCurrencyString(WalletActivity.this, iso, curAmount);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBalancePrimary.setText(mDefaultTextPrimary);
                        mBalanceSecondary.setText(mDefaultTextSecondary);
                        //mBalancePrimary.setTextColor(getResources().getColor(R.color.white_trans, null));
                        //mBalanceSecondary.setTextColor(getResources().getColor(R.color.white, null));


                    }
                });
                TxManager.getInstance().updateTxList(WalletActivity.this);
            }
        });

        TxManager.getInstance().updateTxList(CurrencyActivity.this);

    }

    @Override
    public void onStatusUpdate() {
        super.onStatusUpdate();

        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                TxManager.getInstance().updateTxList(CurrencyActivity.this);
            }
        });
    }

    @Override
    public void onTxAdded() {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                TxManager.getInstance().updateTxList(CurrencyActivity.this);
            }
        });
        com.breadwallet.core.test.BRWalletManager.getInstance().refreshBalance(CurrencyActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        }, 1000);*/

        TxManager.getInstance().onResume(CurrencyActivity.this);

    }

    private void setUpBarFlipper() {
        barFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_enter));
        barFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_exit));
    }

    public void resetFlipper() {
        barFlipper.setDisplayedChild(0);
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        final BaseWallet wallet = WalletsMaster.getInstance(this).getCurrentWallet(this);
        if (isConnected) {
            if (barFlipper != null) {
                if (barFlipper.getDisplayedChild() == 2)
                    barFlipper.setDisplayedChild(0);
            }
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    final double progress = wallet.getPeerManager().getSyncProgress(BRSharedPrefs.getStartHeight(WalletActivity.this,
                            BRSharedPrefs.getCurrentWalletIso(WalletActivity.this)));
//                    Log.e(TAG, "run: " + progress);
                    if (progress < 1 && progress > 0) {
                        SyncManager.getInstance().startSyncingProgressThread();
                    }
                }
            });

        } else {
            if (barFlipper != null)
                barFlipper.setDisplayedChild(2);
            SyncManager.getInstance().stopSyncingProgressThread();
        }
    }
}
