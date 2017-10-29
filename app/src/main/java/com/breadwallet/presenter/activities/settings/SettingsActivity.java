package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.ImportActivity;
import com.breadwallet.presenter.activities.RestoreActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.entities.BRSettingsItem;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.platform.APIClient;
import com.platform.HTTPServer;

import java.util.ArrayList;
import java.util.List;

import static com.breadwallet.R.layout.settings_list_item;
import static com.breadwallet.R.layout.settings_list_section;

public class SettingsActivity extends BRActivity {
    private static final String TAG = SettingsActivity.class.getName();
    private ListView listView;
    public List<BRSettingsItem> items;
    public static boolean appVisible = false;
    private static SettingsActivity app;

    public static SettingsActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        listView = (ListView) findViewById(R.id.settings_list);

    }


    public class SettingsListAdapter extends ArrayAdapter<String> {

        private List<BRSettingsItem> items;
        private Context mContext;

        public SettingsListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<BRSettingsItem> items) {
            super(context, resource);
            this.items = items;
            this.mContext = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            View v;
            BRSettingsItem item = items.get(position);
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();

            if (item.isSection) {
                v = inflater.inflate(settings_list_section, parent, false);
            } else {
                v = inflater.inflate(settings_list_item, parent, false);
                TextView addon = (TextView) v.findViewById(R.id.item_addon);
                addon.setText(item.addonText);
                v.setOnClickListener(item.listener);
            }

            TextView title = (TextView) v.findViewById(R.id.item_title);
            title.setText(item.title);
            return v;

        }

        @Override
        public int getCount() {
            return items == null ? 0 : items.size();
        }

        @Override
        public int getItemViewType(int position) {
            return super.getItemViewType(position);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);
        if (items == null)
            items = new ArrayList<>();
        items.clear();

        populateItems();

        listView.setAdapter(new SettingsListAdapter(this, R.layout.settings_list_item, items));
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_up, R.anim.exit_to_bottom);
    }

    private void populateItems() {

        items.add(new BRSettingsItem(getString(R.string.Settings_wallet), "", null, true));

        items.add(new BRSettingsItem(getString(R.string.Settings_importTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, ImportActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);

            }
        }, false));

        items.add(new BRSettingsItem(getString(R.string.Settings_wipe), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, RestoreActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        }, false));

        items.add(new BRSettingsItem(getString(R.string.Settings_manage), "", null, true));

        items.add(new BRSettingsItem(getString(R.string.Settings_notifications), BRSharedPrefs.getShowNotification(this) ?
                getString(R.string.PushNotifications_on) : getString(R.string.PushNotifications_off), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, NotificationActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

            }
        }, false));

        if (AuthManager.isFingerPrintAvailableAndSetup(this)) {
            items.add(new BRSettingsItem(getString(R.string.Settings_fingerprintLimit_Android), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AuthManager.getInstance().authPrompt(SettingsActivity.this, null, getString(R.string.VerifyPin_continueBody), true, new BRAuthCompletion() {
                        @Override
                        public void onComplete() {
                            Intent intent = new Intent(SettingsActivity.this, SpendLimitActivity.class);
                            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                            startActivity(intent);
                        }

                        @Override
                        public void onCancel() {

                        }
                    });

                }
            }, false));
        }

        items.add(new BRSettingsItem(getString(R.string.Settings_currency), BRSharedPrefs.getIso(this), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, DisplayCurrencyActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));

        items.add(new BRSettingsItem(getString(R.string.Settings_sync), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, SyncBlockchainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));

        items.add(new BRSettingsItem("Loaf", "", null, true));

        items.add(new BRSettingsItem(getString(R.string.Settings_shareData), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, ShareDataActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));
        boolean eaEnabled = APIClient.getInstance().isFeatureEnabled(APIClient.FeatureFlags.EARLY_ACCESS.toString());
        if (eaEnabled)
            items.add(new BRSettingsItem(getString(R.string.Settings_earlyAccess), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SettingsActivity.this, WebViewActivity.class);
                    intent.putExtra("url", HTTPServer.URL_EA);
                    Activity app = SettingsActivity.this;
                    app.startActivity(intent);
                    app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
                }
            }, false));
        items.add(new BRSettingsItem(getString(R.string.Settings_about), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));
        items.add(new BRSettingsItem("", "", null, true));
        items.add(new BRSettingsItem(getString(R.string.Settings_advancedTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, AdvancedActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));

        items.add(new BRSettingsItem("", "", null, true)); //just for a blank space

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }
}
