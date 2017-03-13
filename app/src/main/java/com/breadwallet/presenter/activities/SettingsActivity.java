package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRSoftKeyboard;
import com.breadwallet.presenter.entities.BRSecurityCenterItem;
import com.breadwallet.presenter.entities.BRSettingsItem;

import java.util.ArrayList;
import java.util.List;

import static com.breadwallet.R.id.dot3;
import static com.breadwallet.R.id.dot5;
import static com.breadwallet.R.layout.settings_list_item;
import static com.breadwallet.R.layout.settings_list_section;
import static java.security.AccessController.getContext;

public class SettingsActivity extends Activity {
    private static final String TAG = SettingsActivity.class.getName();
    private ListView listView;
    public List<BRSettingsItem> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setStatusBarColor(android.R.color.transparent);
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

//            Log.e(TAG, "getView: pos: " + position + ", item: " + items.get(position));

            View v = convertView;
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
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        overridePendingTransition(R.anim.exit_to_bottom, 0);
    }

    private void setStatusBarColor(int color) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(color));
    }


    private void populateItems() {

        items.add(new BRSettingsItem("Wallet", "", null, true));

        items.add(new BRSettingsItem("Import Wallet", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Import Wallet");
            }
        }, false));

        items.add(new BRSettingsItem("Restore Breadwallet", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Restore Breadwallet");
            }
        }, false));

        items.add(new BRSettingsItem("Manage", "", null, true));

        items.add(new BRSettingsItem("Notifications", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Notifications");
            }
        }, false));
        items.add(new BRSettingsItem("FingerPrint Spending Limit", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: FingerPrint Spending Limit");
            }
        }, false));
        items.add(new BRSettingsItem("Default Currency", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Default Currency");
            }
        }, false));
        items.add(new BRSettingsItem("Sync Blockchain", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Sync Blockchain");
            }
        }, false));

        items.add(new BRSettingsItem("Bread", "", null, true));

        items.add(new BRSettingsItem("Share Anonymous Data", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Share Anonymous Data");
            }
        }, false));
        items.add(new BRSettingsItem("Join Early Access Program", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Join Early Access Program");
            }
        }, false));
        items.add(new BRSettingsItem("About", "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: About");
            }
        }, false));

    }

}
