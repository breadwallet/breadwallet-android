package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.tools.manager.BRSharedPrefs;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Dummy Home activity to simulate navigating to and from the new Currency screen
 */

public class TestHomeActivity extends Activity {

    private RelativeLayout mBitcoinCard;

    private RelativeLayout mBchCard;
    private static TestHomeActivity app;

    public static TestHomeActivity getApp() {
        return app;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_home);

        mBitcoinCard = findViewById(R.id.bitcoin_card);
        mBchCard = findViewById(R.id.bitcoin_cash_card);

        mBitcoinCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRSharedPrefs.putCurrentWalletIso(TestHomeActivity.this, "BTC");
                Intent newIntent = new Intent(TestHomeActivity.this, WalletActivity.class);
                startActivity(newIntent);
            }
        });

        mBchCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRSharedPrefs.putCurrentWalletIso(TestHomeActivity.this, "BCH");
                Intent newIntent = new Intent(TestHomeActivity.this, WalletActivity.class);
                startActivity(newIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;
        BRSharedPrefs.putCurrentWalletIso(TestHomeActivity.this, "");
    }

}
