package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;

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
                if (AuthManager.getInstance().isWalletDisabled(DisabledActivity.this)) {
                    SpringAnimator.failShakeAnimation(DisabledActivity.this, disabled);
                    AuthManager.getInstance().authSuccess(DisabledActivity.this);//todo DELETE
                } else {
                    BRAnimator.startBreadActivity(DisabledActivity.this, true);
                }
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
    }
}
