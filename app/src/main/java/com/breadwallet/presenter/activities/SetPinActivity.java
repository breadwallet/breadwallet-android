package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.WebViewActivity;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.BRConstants;
import com.platform.HTTPServer;

public class SetPinActivity extends BRActivity {
    private static final String TAG = SetPinActivity.class.getName();
    private BRKeyboard keyboard;
    public static SetPinActivity introSetPitActivity;
    private View dot1;
    private View dot2;
    private View dot3;
    private View dot4;
    private View dot5;
    private View dot6;

    private ImageButton faq;
    private StringBuilder pin = new StringBuilder();
    private int pinLimit = 6;
    private boolean startingNextActivity;
    private TextView title;
    public static boolean appVisible = false;
    private static SetPinActivity app;

    public static SetPinActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_template);

        keyboard = (BRKeyboard) findViewById(R.id.brkeyboard);
        title = (TextView) findViewById(R.id.title);
        faq = (ImageButton) findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.setPin);
            }
        });

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
        introSetPitActivity = this;
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    private void handleClick(String key) {
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
            if (startingNextActivity) return;
            startingNextActivity = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SetPinActivity.this, ReEnterPinActivity.class);
                    intent.putExtra("pin", pin.toString());
                    intent.putExtra("noPin", getIntent().getBooleanExtra("noPin", false));
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                    pin = new StringBuilder("");
                    startingNextActivity = false;
                }
            }, 100);

        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }
}
