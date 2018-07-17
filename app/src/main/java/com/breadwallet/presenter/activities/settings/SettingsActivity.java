package com.breadwallet.presenter.activities.settings;

import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.BRSettingsItem;
import com.breadwallet.tools.adapter.SettingsAdapter;
import com.breadwallet.tools.util.SettingsUtil;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseSettingsActivity {
    private static final String TAG = SettingsActivity.class.getName();
    public static final String EXTRA_MODE = "com.breadwallet.presenter.activities.settings.EXTRA_MODE";
    public static final String MODE_SETTINGS = "settings";
    public static final String MODE_PREFERENCES = "preferences";
    public static final String MODE_SECURITY = "security";
    public static final String MODE_CURRENCY_SETTINGS = "currency_settings";
    private boolean mIsButtonBackArrow;

    @Override
    public int getLayoutId() {
        return R.layout.activity_settings;
    }

    @Override
    public int getBackButtonId() {
        return mIsButtonBackArrow ? R.id.back_button : R.id.close_button;
    }

    @Override
    protected void onResume() {
        setTitleAndList();
        //call super on resume after the child's to allow for the mode to be detected
        super.onResume();
    }

    private void setTitleAndList() {
        BaseTextView title = findViewById(R.id.title);
        ListView settingsList = findViewById(R.id.settings_list);
        List<BRSettingsItem> settingsItems = new ArrayList<>();
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) {
            throw new IllegalArgumentException("Need mode for the settings activity");
        }
        switch (mode) {
            case MODE_SETTINGS:
                settingsItems = SettingsUtil.getMainSettings(this);
                title.setText(getString(R.string.Settings_title));
                mIsButtonBackArrow = false;
                break;
            case MODE_PREFERENCES:
                settingsItems = SettingsUtil.getPreferencesSettings(this);
                title.setText(getString(R.string.Settings_preferences));
                mIsButtonBackArrow = true;
                break;
            case MODE_SECURITY:
                settingsItems = SettingsUtil.getSecuritySettings(this);
                title.setText(getString(R.string.MenuButton_security));
                mIsButtonBackArrow = true;
                break;
            case MODE_CURRENCY_SETTINGS:
                BaseWalletManager walletManager = WalletsMaster.getInstance(this).getCurrentWallet(this);
                settingsItems = walletManager.getSettingsConfiguration().getSettingsList();
                String currencySettingsLabel = String.format("%s %s", walletManager.getName(), getString(R.string.Settings_title));
                title.setText(currencySettingsLabel);
                mIsButtonBackArrow = true;
                break;
        }

        settingsList.setAdapter(new SettingsAdapter(this, R.layout.settings_list_item, settingsItems));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mIsButtonBackArrow) {
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        } else {
            overridePendingTransition(R.anim.empty_300, R.anim.exit_to_bottom);
        }
    }
}
