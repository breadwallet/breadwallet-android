package com.breadwallet.presenter.activities.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ActivityUTILS;
import com.breadwallet.presenter.entities.BRSecurityCenterItem;
import com.breadwallet.tools.animation.BRAnimator;

import java.util.ArrayList;
import java.util.List;

public class FingerprintActivity extends AppCompatActivity {
    private static final String TAG = FingerprintActivity.class.getName();

    public ListView mListView;
    public RelativeLayout layout;
    public List<BRSecurityCenterItem> itemList;
    public static boolean appVisible = false;
    private static FingerprintActivity app;

    public static FingerprintActivity getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);

        itemList = new ArrayList<>();
        mListView = (ListView) findViewById(R.id.menu_listview);

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
        super.onBackPressed();
        BRAnimator.startBreadActivity(this, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        appVisible = false;
    }

}
