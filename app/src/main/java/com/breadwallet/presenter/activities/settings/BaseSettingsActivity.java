package com.breadwallet.presenter.activities.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;

public abstract class BaseSettingsActivity extends BRActivity implements View.OnClickListener {
    private ImageButton mBackButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

    }

    @Override
    protected void onResume() {
        super.onResume();
        setupBackButton();
    }

    private void setupBackButton() {
        mBackButton = findViewById(getBackButtonId());
        mBackButton.setVisibility(View.VISIBLE);
        mBackButton.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBackButton.setOnClickListener(null);
    }

    @Override
    public void onClick(View v) {
        onBackPressed();
    }

    public abstract int getLayoutId();

    public abstract int getBackButtonId();

}
