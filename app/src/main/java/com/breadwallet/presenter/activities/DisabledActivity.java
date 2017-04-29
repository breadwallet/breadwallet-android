package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.util.Utils;

import java.util.Locale;


public class DisabledActivity extends Activity {

    private TextView untilLabel;
    private TextView disabled;
    private TextView attempts;
    private ConstraintLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disabled);

        ActivityUTILS.init(this);
        untilLabel = (TextView) findViewById(R.id.until_label);
        layout = (ConstraintLayout) findViewById(R.id.layout);
        disabled = (TextView) findViewById(R.id.disabled);
        attempts = (TextView) findViewById(R.id.attempts_label);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.failShakeAnimation(DisabledActivity.this, disabled);
            }
        });

        double waitTimeMinutes = getIntent().getDoubleExtra("waitTimeMinutes", 0);
        if (waitTimeMinutes == 0) throw new IllegalArgumentException("can't be 0");
        int failCount = KeyStoreManager.getFailCount(this);
        String formattedDate = Utils.formatTimeStamp((long) (System.currentTimeMillis() + waitTimeMinutes * 60 * 1000), "hh:mm a");
        untilLabel.setText(String.format("until: %s", formattedDate));
        attempts.setText(String.format(Locale.getDefault(), "%d attempts remaining", 8 - failCount));

    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
