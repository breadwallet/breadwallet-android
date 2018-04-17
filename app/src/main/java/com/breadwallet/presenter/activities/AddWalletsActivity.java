package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;

public class AddWalletsActivity extends BRActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wallets);
    }
}
