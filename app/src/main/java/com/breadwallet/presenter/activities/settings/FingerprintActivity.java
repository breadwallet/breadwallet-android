package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ActivityUTILS;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.tools.util.Utils;

import java.math.BigDecimal;

import static android.R.attr.label;


public class FingerprintActivity extends AppCompatActivity {
    private static final String TAG = FingerprintActivity.class.getName();

    public RelativeLayout layout;
    public static boolean appVisible = false;
    private static FingerprintActivity app;
    private TextView limitExchange;
    private TextView limitInfo;

    private ToggleButton toggleButton;

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
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        limitExchange = (TextView) findViewById(R.id.limit_exchange);
        limitInfo = (TextView) findViewById(R.id.limit_info);

        toggleButton.setChecked(SharedPreferencesManager.getUseFingerprint(this));

        limitExchange.setText(getLimitText());

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Activity app = FingerprintActivity.this;
                if (isChecked && !Utils.isFingerprintEnrolled(app)) {
                    Log.e(TAG, "onCheckedChanged: fingerprint not setup");
                    BreadDialog.showCustomDialog(app, "Fingerprint Not Setup", "You have not setup any fingerprints on this device. Go to Settings -> Security to setup a fingerprint.", "OK", null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                    buttonView.setChecked(false);
                    return;
                }
                SharedPreferencesManager.putUseFingerprint(app, isChecked);

            }
        });
        SpannableString ss = new SpannableString("You can customize your Fingerprint Spending Limit from the Fingerprint Spending Limit screen");
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Intent intent = new Intent(FingerprintActivity.this, SpendLimitActivity.class);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                startActivity(intent);
                finish();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        ss.setSpan(clickableSpan, limitInfo.getText().length() - 33, limitInfo.getText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        limitInfo.setText(ss);
        limitInfo.setMovementMethod(LinkMovementMethod.getInstance());
        limitInfo.setHighlightColor(Color.TRANSPARENT);

    }

    private String getLimitText() {
        String iso = SharedPreferencesManager.getIso(this);
        //amount in satoshis
        BigDecimal satoshis = new BigDecimal(KeyStoreManager.getSpendLimit(this));
        //amount in BTC, mBTC or bits
        BigDecimal amount = BRExchange.getAmountFromSatoshis(this, "BTC", satoshis);
        //amount in user preferred ISO (e.g. USD)
        BigDecimal curAmount = BRExchange.getAmountFromSatoshis(this, iso, satoshis);
        //formatted string for the label
        return String.format("Spending Limit: %s (%s)", BRCurrency.getFormattedCurrencyString(this, "BTC", amount), BRCurrency.getFormattedCurrencyString(this, iso, curAmount));
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
