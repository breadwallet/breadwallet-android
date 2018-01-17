package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.breadwallet.R;

/**
 * Created by byfieldj on 1/17/18.
 *
 * Dummy Home activity to simulate navigating to and from the new Currency screen
 */

public class TestHomeActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_home);
    }
}
