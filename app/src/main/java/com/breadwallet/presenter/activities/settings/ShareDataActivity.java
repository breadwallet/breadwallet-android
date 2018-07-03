package com.breadwallet.presenter.activities.settings;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.breadwallet.R;
import com.breadwallet.tools.manager.BRSharedPrefs;

public class ShareDataActivity extends BaseSettingsActivity {
    private static final String TAG = ShareDataActivity.class.getName();
    private ToggleButton mToggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        mToggleButton = findViewById(R.id.toggleButton);
        mToggleButton.setChecked(BRSharedPrefs.getShareData(this));
        mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                BRSharedPrefs.putShareData(ShareDataActivity.this, isChecked);
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_share_data;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

}
