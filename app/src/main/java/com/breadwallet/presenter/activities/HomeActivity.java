package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.wallet.WalletsMaster;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Dummy Home activity to simulate navigating to and from the new Currency screen
 */

public class HomeActivity extends Activity {

    private RelativeLayout mBitcoinCard;

    private RelativeLayout mBchCard;
    private static HomeActivity app;

    public static HomeActivity getApp() {
        return app;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mBitcoinCard = findViewById(R.id.bitcoin_card);
        mBchCard = findViewById(R.id.bitcoin_cash_card);

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                WalletsMaster.getInstance(HomeActivity.this).initWallets(HomeActivity.this);
            }
        });

        mBitcoinCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRSharedPrefs.putCurrentWalletIso(HomeActivity.this, "BTC");
                Intent newIntent = new Intent(HomeActivity.this, WalletActivity.class);
                startActivity(newIntent);
            }
        });

        mBchCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRSharedPrefs.putCurrentWalletIso(HomeActivity.this, "BCH");
                Intent newIntent = new Intent(HomeActivity.this, WalletActivity.class);
                startActivity(newIntent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;
        BRSharedPrefs.putCurrentWalletIso(HomeActivity.this, "");
    }

}
