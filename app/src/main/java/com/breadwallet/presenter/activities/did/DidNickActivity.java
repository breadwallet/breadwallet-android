package com.breadwallet.presenter.activities.did;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;

public class DidNickActivity extends BaseSettingsActivity {
    @Override
    public int getLayoutId() {
        return R.layout.activity_did_nick_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }
}
