package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ActivityUTILS;
import com.breadwallet.presenter.activities.ImportActivity;
import com.breadwallet.presenter.activities.RestoreActivity;
import com.breadwallet.presenter.entities.BRSettingsItem;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.security.AuthManager;
import com.platform.HTTPServer;

import java.util.ArrayList;
import java.util.List;

import static com.breadwallet.R.layout.settings_list_item;
import static com.breadwallet.R.layout.settings_list_section;

public class SettingsActivity extends AppCompatActivity {
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
        items = new ArrayList<>();

        populateItems();

        listView.setAdapter(new SettingsListAdapter(this, settings_list_item, items));
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
    }


    @Override
    public void onBackPressed() {
        BRAnimator.startBreadActivity(this, false);
    }

    private void populateItems() {

        items.add(new BRSettingsItem("Wallet", "", null, true));

        items.add(new BRSettingsItem("Import Wallet", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Import Wallet");
                Intent intent = new Intent(SettingsActivity.this, ImportActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

            }
        }, false));

        items.add(new BRSettingsItem("Restore Breadwallet", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Restore Breadwallet");
                Intent intent = new Intent(SettingsActivity.this, RestoreActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));

        items.add(new BRSettingsItem("Manage", "", null, true));

        items.add(new BRSettingsItem("Notifications", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, NotificationActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

            }
        }, false));
        if (AuthManager.isFingerPrintAvailable(this)) {
            items.add(new BRSettingsItem("FingerPrint Spending Limit", "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "onClick: FingerPrint Spending Limit");
                    Intent intent = new Intent(SettingsActivity.this, SpendLimitActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false));
        }

        items.add(new BRSettingsItem("Default Currency", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Default Currency");
                Intent intent = new Intent(SettingsActivity.this, DefaultCurrencyActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));
        items.add(new BRSettingsItem("Sync Blockchain", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, SyncBlockchainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));

        items.add(new BRSettingsItem("Bread", "", null, true));

        items.add(new BRSettingsItem("Share Anonymous Data", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Share Anonymous Data");
                Intent intent = new Intent(SettingsActivity.this, ShareDataActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));
        items.add(new BRSettingsItem("Join Early Access Program", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Join Early Access Program");
                Intent intent = new Intent(SettingsActivity.this, WebViewActivity.class);
                intent.putExtra("url", HTTPServer.URL_EA);
                Activity app = SettingsActivity.this;
                app.startActivity(intent);
                app.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));
        items.add(new BRSettingsItem("About", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: About");
                Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false));

    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }
}
