package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.util.Utils;

public class DisabledActivity extends Activity {

    private TextView untilLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locked);

        ActivityUTILS.setStatusBarColor(this, R.color.status_bar);
        untilLabel = (TextView) findViewById(R.id.until_label);

        long disabledUntil = getIntent().getLongExtra("until", 0);
        if (disabledUntil == 0) throw new IllegalArgumentException("can't be 0");

        String formattedDate = Utils.formatTimeStamp(disabledUntil, "ha");
        untilLabel.setText(String.format("Until: %s", formattedDate));

    }
}
