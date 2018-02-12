package com.breadwallet.presenter.activities;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.TransitionManager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.WebViewActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRSearchBar;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.SyncManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.platform.HTTPServer;

import java.math.BigDecimal;

import static com.breadwallet.presenter.activities.TestHomeActivity.EXTRA_CURRENCY;
import static com.breadwallet.tools.animation.BRAnimator.t1Size;
import static com.breadwallet.tools.animation.BRAnimator.t2Size;

/**
 * Created by byfieldj on 1/16/18.
 * <p>
 * <p>
 * This activity will display pricing and transaction information for any currency the user has access to
 * (BTC, BCH, ETH)
 *
 */

public class CurrencyActivity extends BreadActivity implements InternetManager.ConnectionReceiverListener {

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



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_currency);


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


        BRAnimator.init(this);
        mBalancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);//make it the size it should be after animation to get the X
        mBalanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);//make it the size it should be after animation to get the X


        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showSendFragment(CurrencyActivity.this, null);

            }
        });

        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showReceiveFragment(CurrencyActivity.this, false);

            }
        });


        if (getIntent() != null) {
            String currency = getIntent().getStringExtra(EXTRA_CURRENCY);
            if (currency.equals("btc")) {
                // Do nothing, BTC display is the default display

                mBuyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent intent = new Intent(CurrencyActivity.this, WebViewActivity.class);
                        intent.putExtra("url", HTTPServer.URL_BUY);
                        Activity app = CurrencyActivity.this;
                        app.startActivity(intent);
                        app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
                    }
                });

            } else if (currency.equals("bch")) {
                mCurrencyTitle.setText("BitcoinCash");
                mCurrencyPriceUsd.setText("$2,665.41 per BCH");
                mBalancePrimary.setText("$4,177.74");
                mBalanceSecondary.setText("1.56739 BCH");
                mToolbar.setBackgroundColor(getResources().getColor(R.color.bitcoin_cash_row_color, null));
                mSendButton.setColor(R.color.bitcoin_cash_row_color);
                mReceiveButton.setColor(R.color.bitcoin_cash_row_color);
                mBuyButton.setVisibility(View.GONE);

                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.5f
                );
                mSendButton.setLayoutParams(param);
                mReceiveButton.setLayoutParams(param);


            }

        }

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
    }

    private void swap() {
        if (!BRAnimator.isClickAllowed()) return;
        boolean b = !BRSharedPrefs.getPreferredBTC(this);
        setPriceTags(b, true);
        BRSharedPrefs.putPreferredBTC(this, b);
    }

    private void setPriceTags(boolean btcPreferred, boolean animate) {
        //mBalanceSecondary.setTextSize(!btcPreferred ? t1Size : t2Size);
        //mBalancePrimary.setTextSize(!btcPreferred ? t2Size : t1Size);
        ConstraintSet set = new ConstraintSet();
        set.clone(toolBarConstraintLayout);
        if (animate)
            TransitionManager.beginDelayedTransition(toolBarConstraintLayout);
        int px4 = Utils.getPixelsFromDps(this, 4);
        int px16 = Utils.getPixelsFromDps(this, 16);
        //align to parent left
        set.connect(!btcPreferred ? R.id.balance_secondary : R.id.balance_primary, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.END, px16);
        //align swap symbol after the first item
        set.connect(R.id.swap, ConstraintSet.START, !btcPreferred ? mBalanceSecondary.getId() : mBalancePrimary.getId(), ConstraintSet.END, px4);
        //align second item after swap symbol
        set.connect(!btcPreferred ? R.id.balance_primary : R.id.balance_secondary, ConstraintSet.START, mSwap.getId(), ConstraintSet.END, px4);

//      align the "Balance" text to remain at the top of the USD balance
        //set.connect(R.id.balance_display , ConstraintSet.TOP, R.id.balance_label, ConstraintSet.BOTTOM, px16);



        // Apply the changes
        set.applyTo(toolBarConstraintLayout);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        }, toolBarConstraintLayout.getLayoutTransition().getDuration(LayoutTransition.CHANGING));
    }

    /*public void updateUI() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName(Thread.currentThread().getName() + ":updateUI");
                //sleep a little in order to make sure all the commits are finished (like SharePreferences commits)
                String iso = BRSharedPrefs.getIso(CurrencyActivity.this);

                //current amount in satoshis
                final BigDecimal amount = new BigDecimal(BRSharedPrefs.getCatchedBalance(CurrencyActivity.this));

                //amount in BTC units
                BigDecimal btcAmount = BRExchange.getBitcoinForSatoshis(CurrencyActivity.this, amount);
                final String formattedBTCAmount = BRCurrency.getFormattedCurrencyString(CurrencyActivity.this, "BTC", btcAmount);

                //amount in currency units
                BigDecimal curAmount = BRExchange.getAmountFromSatoshis(CurrencyActivity.this, iso, amount);
                final String formattedCurAmount = BRCurrency.getFormattedCurrencyString(CurrencyActivity.this, iso, curAmount);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBalancePrimary.setText(formattedBTCAmount);
                        mBalanceSecondary.setText(String.format("%s", formattedCurAmount));

                    }
                });
                TxManager.getInstance().updateTxList(CurrencyActivity.this);
            }
        });
    }*/


    private void setUpBarFlipper() {
        barFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_enter));
        barFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_exit));
    }

    public void resetFlipper(){
        barFlipper.setDisplayedChild(0);
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        if (isConnected) {
            if (barFlipper != null) {
                if (barFlipper.getDisplayedChild() == 2)
                    barFlipper.setDisplayedChild(0);
            }
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    final double progress = BRPeerManager.syncProgress(BRSharedPrefs.getStartHeight(CurrencyActivity.this));
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
