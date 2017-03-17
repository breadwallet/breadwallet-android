package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroReEnterPinActivity;
import com.breadwallet.presenter.customviews.BRSoftKeyboard;
import com.breadwallet.tools.manager.SharedPreferencesManager;

public class NotificationActivity extends Activity {
    private static final String TAG = NotificationActivity.class.getName();
    private ToggleButton toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        setStatusBarColor(android.R.color.transparent);

        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setChecked(SharedPreferencesManager.getShowNotification(this));
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferencesManager.putShowNotification(NotificationActivity.this, isChecked);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void setStatusBarColor(int color) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(color));
    }
}
