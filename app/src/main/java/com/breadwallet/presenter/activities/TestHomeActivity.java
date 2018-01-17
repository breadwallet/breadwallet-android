package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RelativeLayout;

import com.breadwallet.R;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Dummy Home activity to simulate navigating to and from the new Currency screen
 */

public class TestHomeActivity extends Activity {

    RelativeLayout mBitcoinCard;
    RelativeLayout mBchCard;
    public static final String EXTRA_CURRENCY = "extra_currency";
    private static final String CURRENCY_BTC = "btc";
    private static final String CURRENCY_BCH = "bch";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_home);

        mBitcoinCard = findViewById(R.id.bitcoin_card);
        mBchCard = findViewById(R.id.bitcoin_cash_card);


        mBitcoinCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCurrencyActivity(CURRENCY_BTC);
            }
        });

        mBchCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCurrencyActivity(CURRENCY_BCH);
            }
        });
    }

    private void startCurrencyActivity(String currency) {

        Intent newIntent = new Intent(this, CurrencyActivity.class);
        newIntent.putExtra(EXTRA_CURRENCY, currency);
        startActivity(newIntent);
    }
}
