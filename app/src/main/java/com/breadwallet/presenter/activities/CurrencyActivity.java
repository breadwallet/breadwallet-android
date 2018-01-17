package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRText;

import static com.breadwallet.presenter.activities.TestHomeActivity.EXTRA_CURRENCY;

/**
 * Created by byfieldj on 1/16/18.
 * <p>
 * <p>
 * This activity will display pricing and transaction information for any currency the user has access to
 */

public class CurrencyActivity extends Activity {

    BRText mCurrencyTitle;
    BRText mCurrencyPriceUsd;
    BRText mBalanceAmountUsd;
    BRText mBalanceAmountCurrency;
    Toolbar mToolbar;
    ImageButton mBackButton;
    BRButton mSendButton;
    BRButton mReceiveButton;
    BRButton mBuyButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_currency);


        mCurrencyTitle = findViewById(R.id.currency_label);
        mCurrencyPriceUsd = findViewById(R.id.currency_usd_price);
        mBalanceAmountUsd = findViewById(R.id.balance_display);
        mBalanceAmountCurrency = findViewById(R.id.balance_count);
        mToolbar = findViewById(R.id.bread_bar);
        mBackButton = findViewById(R.id.back_icon);
        mSendButton = findViewById(R.id.send_button);
        mReceiveButton = findViewById(R.id.receive_button);
        mBuyButton = findViewById(R.id.buy_button);


        if (getIntent() != null) {
            String currency = getIntent().getStringExtra(EXTRA_CURRENCY);
            if (currency.equals("btc")) {
                // Do nothing, BTC display is the default display
            } else if (currency.equals("bch")) {
                mCurrencyTitle.setText("BitcoinCash");
                mCurrencyPriceUsd.setText("$2,665.41 per BCH");
                mBalanceAmountUsd.setText("$4,177.74");
                mBalanceAmountCurrency.setText("1.56739 BCH");
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
    }


}
