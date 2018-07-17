package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.Locale;


public class DisabledActivity extends BRActivity {
    private static final String TAG = DisabledActivity.class.getName();
    private TextView untilLabel;
    private TextView disabled;
    //    private TextView attempts;
    private ConstraintLayout layout;
    private Button resetButton;
    private CountDownTimer timer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disabled);

        untilLabel = findViewById(R.id.until_label);
        layout = findViewById(R.id.layout);
        disabled = findViewById(R.id.disabled);
        resetButton = findViewById(R.id.reset_button);

        ImageButton faq = findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                BaseWalletManager wm = WalletsMaster.getInstance(DisabledActivity.this).getCurrentWallet(DisabledActivity.this);
                UiUtils.showSupportFragment(DisabledActivity.this, BRConstants.FAQ_WALLET_DISABLE, wm);
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DisabledActivity.this, InputWordsActivity.class);
                intent.putExtra(InputWordsActivity.EXTRA_RESET_PIN, true);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        untilLabel.setText("");

    }

    private void refresh() {
        if (AuthManager.getInstance().isWalletDisabled(DisabledActivity.this)) {
            SpringAnimator.failShakeAnimation(DisabledActivity.this, disabled);
        } else {
            UiUtils.startBreadActivity(DisabledActivity.this, true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        long disabledUntil = AuthManager.getInstance().disabledUntil(this);
//        Log.e(TAG, "onResume: disabledUntil: " + disabledUntil + ", diff: " + (disabledUntil - BRSharedPrefs.getSecureTime(this)));
        long disabledTime = disabledUntil - System.currentTimeMillis();
        int seconds = (int) disabledTime / 1000;
        timer = new CountDownTimer(seconds * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long durationSeconds = (millisUntilFinished / 1000);
                untilLabel.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", durationSeconds / 3600,
                        (durationSeconds % 3600) / 60, (durationSeconds % 60)));
            }

            public void onFinish() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                }, 2000);
                long durationSeconds = 0;
                untilLabel.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", durationSeconds / 3600,
                        (durationSeconds % 3600) / 60, (durationSeconds % 60)));
            }
        }.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();

    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else if (AuthManager.getInstance().isWalletDisabled(DisabledActivity.this)) {
            SpringAnimator.failShakeAnimation(DisabledActivity.this, disabled);
        } else {
            UiUtils.startBreadActivity(DisabledActivity.this, true);
        }
        overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }
}
