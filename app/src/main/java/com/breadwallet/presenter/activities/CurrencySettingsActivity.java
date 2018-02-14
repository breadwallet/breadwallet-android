package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;

/**
 * Created by byfieldj on 2/5/18.
 */

public class CurrencySettingsActivity extends Activity {

    public static final String EXTRA_CURRENCY_BTC = "btc";
    public static final String EXTRA_CURRENCY_BCH = "bch";
    public static final String EXTRA_CURRENCY = "currency";

    private BRText mTitle;
    private ImageButton mBackButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency_settings);

        mTitle = findViewById(R.id.title);
        mBackButton = findViewById(R.id.back_button);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });


       String currency =  getIntent().getStringExtra(EXTRA_CURRENCY);

       if(currency.equals(EXTRA_CURRENCY_BTC)){
           mTitle.setText("Bitcoin Settings");
       }
       else if(currency.equals(EXTRA_CURRENCY_BCH)){
           mTitle.setText("BitcoinCash Settings");
       }
    }
}
