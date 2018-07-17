package com.breadwallet.presenter.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.BRSettingsItem;
import com.breadwallet.tools.adapter.SettingsAdapter;

import java.util.ArrayList;
import java.util.List;

public class AdvancedActivity extends BaseSettingsActivity {
    private static final String TAG = AdvancedActivity.class.getName();
    private ListView listView;
    public List<BRSettingsItem> items;
    public static boolean appVisible = false;
    private static AdvancedActivity app;

    public static AdvancedActivity getApp() {
        return app;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listView = findViewById(R.id.settings_list);

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_advanced;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        if (items == null)
            items = new ArrayList<>();
        items.clear();

        populateItems();
        listView.addFooterView(new View(this), null, true);
        listView.addHeaderView(new View(this), null, true);
        listView.setAdapter(new SettingsAdapter(this, R.layout.settings_list_item, items));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void populateItems() {

        items.add(new BRSettingsItem(getString(R.string.NodeSelector_title), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdvancedActivity.this, NodesActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.empty_300);

            }
        }, false, R.drawable.chevron_right_light));

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
