package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.breadwallet.R;

/**
 * Created by byfieldj on 1/16/18.
 *
 *
 * This activity will display pricing and transaction information for any currency the user has access to
 */

public class CurrencyActivity extends Activity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency);
    }


}
