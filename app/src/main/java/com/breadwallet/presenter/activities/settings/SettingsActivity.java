package com.breadwallet.presenter.activities.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.CurrencySettingsActivity;
import com.breadwallet.presenter.activities.ManageWalletsActivity;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.entities.BRSettingsItem;
import com.breadwallet.tools.adapter.SettingsAdapter;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.entities.TokenListMetaData;
import com.platform.tools.KVStoreManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseSettingsActivity {
    private static final String TAG = SettingsActivity.class.getName();
    private ListView mSettingsList;
    private List<BRSettingsItem> mSettingsItems;
    private static final String MARKET_URI = "market://details?id=com.breadwallet";
    private static  final String GOOGLE_PLAY_URI = "https://play.google.com/store/apps/details?id=com.breadwallet";

    @Override
    public int getLayoutId() {
        return R.layout.activity_settings;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettingsList = findViewById(R.id.settings_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSettingsItems == null)
            mSettingsItems = new ArrayList<>();
        mSettingsItems.clear();

        populateItems();
        mSettingsList.addFooterView(new View(this), null, true);
        mSettingsList.setAdapter(new SettingsAdapter(this, R.layout.settings_list_item, mSettingsItems));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void populateItems() {
       final  SettingsActivity activity = this;
        mSettingsItems.add(new BRSettingsItem(getString(R.string.Settings_wallet), "", null, true, 0));
        mSettingsItems.add(new BRSettingsItem(getString(R.string.TokenList_manageTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, ManageWalletsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


        mSettingsItems.add(new BRSettingsItem(getString(R.string.Settings_wipe), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, UnlinkActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


        mSettingsItems.add(new BRSettingsItem(getString(R.string.Settings_preferences), "", null, true, 0));

        mSettingsItems.add(new BRSettingsItem(getString(R.string.UpdatePin_updateTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, InputPinActivity.class);
                intent.putExtra(InputPinActivity.EXTRA_PIN_MODE_UPDATE, true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
            }
        }, false, R.drawable.chevron_right_light));

        mSettingsItems.add(new BRSettingsItem(getString(R.string.Settings_currency), BRSharedPrefs.getPreferredFiatIso(this), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, DisplayCurrencyActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


        mSettingsItems.add(new BRSettingsItem(getString(R.string.Settings_currencySettings), "", null, true, 0));

        final WalletBitcoinManager btcWallet = WalletBitcoinManager.getInstance(this);
        if (btcWallet.getSettingsConfiguration().mSettingList.size() > 0)
            mSettingsItems.add(new BRSettingsItem(btcWallet.getName(), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(activity, CurrencySettingsActivity.class);
                    BRSharedPrefs.putCurrentWalletIso(activity, btcWallet.getIso()); //change the current wallet to the one they enter settings to
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, R.drawable.chevron_right_light));
        final WalletBchManager bchWallet = WalletBchManager.getInstance(activity);
        if (bchWallet.getSettingsConfiguration().mSettingList.size() > 0)
            mSettingsItems.add(new BRSettingsItem(WalletBchManager.getInstance(activity).getName(), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(activity, CurrencySettingsActivity.class);
                    BRSharedPrefs.putCurrentWalletIso(activity, bchWallet.getIso());//change the current wallet to the one they enter settings to
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, R.drawable.chevron_right_light));
        final WalletEthManager ethWallet = WalletEthManager.getInstance(activity);
        if (ethWallet.getSettingsConfiguration().mSettingList.size() > 0)
            mSettingsItems.add(new BRSettingsItem(WalletEthManager.getInstance(activity).getName(), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(activity, CurrencySettingsActivity.class);
                    BRSharedPrefs.putCurrentWalletIso(activity, ethWallet.getIso());//change the current wallet to the one they enter settings to
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, R.drawable.chevron_right_light));


        mSettingsItems.add(new BRSettingsItem(getString(R.string.Tokens_Reset), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                resetToDefaultCurrencies();
            }
        }, false, 0));

        mSettingsItems.add(new BRSettingsItem(getString(R.string.Settings_other), "", null, true, 0));

        String shareAddOn = BRSharedPrefs.getShareData(activity) ? getString(R.string.PushNotifications_on) : getString(R.string.PushNotifications_off);

        mSettingsItems.add(new BRSettingsItem(getString(R.string.Settings_shareData), shareAddOn, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, ShareDataActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));

        mSettingsItems.add(new BRSettingsItem(getString(R.string.Settings_review), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI));
                    appStoreIntent.setPackage("com.android.vending");

                    startActivity(appStoreIntent);
                } catch (android.content.ActivityNotFoundException exception) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_URI)));
                }
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.arrow_leave));

        mSettingsItems.add(new BRSettingsItem(getString(R.string.About_title), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, AboutActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));

        mSettingsItems.add(new BRSettingsItem(getString(R.string.Settings_advancedTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, AdvancedActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


    }

    private void resetToDefaultCurrencies() {

        TokenListMetaData tokenMeta = KVStoreManager.getInstance().getTokenListMetaData(this);

        tokenMeta.enabledCurrencies = new ArrayList<>();

        TokenListMetaData.TokenInfo btc = new TokenListMetaData.TokenInfo("BTC", false, null);
        TokenListMetaData.TokenInfo bch = new TokenListMetaData.TokenInfo("BCH", false, null);
        TokenListMetaData.TokenInfo eth = new TokenListMetaData.TokenInfo("ETH", false, null);
        TokenListMetaData.TokenInfo brd = new TokenListMetaData.TokenInfo("BRD", true, null);

        tokenMeta.enabledCurrencies.add(btc);
        tokenMeta.enabledCurrencies.add(bch);
        tokenMeta.enabledCurrencies.add(eth);
        tokenMeta.enabledCurrencies.add(brd);


        // Publish the changes back to the KVStore
        KVStoreManager.getInstance().putTokenListMetaData(this, tokenMeta);

        // Notify WalletsMaster so the reset will be reflected on the Home Screen
        WalletsMaster.getInstance(this).updateWallets(this);

        // Go back to Home Screen
        onBackPressed();
    }

}
