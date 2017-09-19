package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.WebViewActivity;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.platform.HTTPServer;

public class ReEnterPinActivity extends BRActivity {
    private static final String TAG = ReEnterPinActivity.class.getName();
    private BRKeyboard keyboard;
    public static ReEnterPinActivity reEnterPinActivity;
    private View dot1;
    private View dot2;
    private View dot3;
    private View dot4;
    private View dot5;
    private View dot6;
    private StringBuilder pin = new StringBuilder();
    private TextView title;
    private int pinLimit = 6;
    private String firstPIN;
    private boolean isPressAllowed = true;
    private LinearLayout pinLayout;
    public static boolean appVisible = false;
    private static ReEnterPinActivity app;

    public static ReEnterPinActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_template);

        keyboard = (BRKeyboard) findViewById(R.id.brkeyboard);
        pinLayout = (LinearLayout) findViewById(R.id.pinLayout);

        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.setPin);
            }
        });

        title = (TextView) findViewById(R.id.title);
        title.setText(getString(R.string.UpdatePin_createTitleConfirm));
        firstPIN = getIntent().getExtras().getString("pin");
        if (Utils.isNullOrEmpty(firstPIN)) {
            throw new RuntimeException("first PIN is required");
        }
        reEnterPinActivity = this;

        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);
        dot5 = findViewById(R.id.dot5);
        dot6 = findViewById(R.id.dot6);

        keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                handleClick(key);
            }
        });
        keyboard.setShowDot(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDots();
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);
        isPressAllowed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    private void handleClick(String key) {
        if (!isPressAllowed) return;
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else {
            Log.e(TAG, "handleClick: oops: " + key);
        }
    }


    private void handleDigitClick(Integer dig) {
        if (pin.length() < pinLimit)
            pin.append(dig);
        updateDots();
    }

    private void handleDeleteClick() {
        if (pin.length() > 0)
            pin.deleteCharAt(pin.length() - 1);
        updateDots();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void updateDots() {
        int selectedDots = pin.length();

        dot1.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_gray : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot2.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_gray : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot3.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_gray : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot4.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_gray : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot5.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_gray : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot6.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_gray : R.drawable.ic_pin_dot_black));

        if (pin.length() == 6) {
            verifyPin();
        }

    }

    private void verifyPin() {
        if (firstPIN.equalsIgnoreCase(pin.toString())) {
            AuthManager.getInstance().authSuccess(this);
//            Log.e(TAG, "verifyPin: SUCCESS");
            isPressAllowed = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    pin = new StringBuilder("");
                    updateDots();
                }
            }, 200);
            AuthManager.getInstance().setPinCode(pin.toString(), this);
            if (getIntent().getBooleanExtra("noPin", false)) {
                BRAnimator.startBreadActivity(this, false);
            } else {
                BRAnimator.showBreadSignal(this, getString(R.string.Alerts_pinSet), getString(R.string.UpdatePin_createInstruction), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                    @Override
                    public void onComplete() {
                        PostAuth.getInstance().onCreateWalletAuth(ReEnterPinActivity.this, false);

                    }
                });
            }

        } else {
            AuthManager.getInstance().authFail(this);
            Log.e(TAG, "verifyPin: FAIL: firs: " + firstPIN + ", reEnter: " + pin.toString());
//            title.setText("Wrong PIN,\nplease try again");
            SpringAnimator.failShakeAnimation(this, pinLayout);
            pin = new StringBuilder();
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }
}
