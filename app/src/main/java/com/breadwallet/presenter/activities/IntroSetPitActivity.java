package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRSoftKeyboard;

public class IntroSetPitActivity extends Activity {
    private static final String TAG = IntroSetPitActivity.class.getName();
    private BRSoftKeyboard keyboard;
    private View dot1;
    private View dot2;
    private View dot3;
    private View dot4;
    private View dot5;
    private View dot6;
    private StringBuilder pin = new StringBuilder();
    private int pinLimit = 6;
    private boolean startingNextActivity;
    private TextView title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_template);
        setStatusBarColor(android.R.color.transparent);
        keyboard = (BRSoftKeyboard) findViewById(R.id.brkeyboard);
        title = (TextView) findViewById(R.id.title);

        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);
        dot5 = findViewById(R.id.dot5);
        dot6 = findViewById(R.id.dot6);

        keyboard.addOnInsertListener(new BRSoftKeyboard.OnInsertListener() {
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
    }

    private void handleClick(String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }
        if (key.length() > 1) {
            Log.e(TAG, "handleClick: key is longer: " + key);
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key));
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

    private void setStatusBarColor(int color) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(color));
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
                    Log.e(TAG, "updateDots: starting new activity: " + pin.toString());
                    Intent intent = new Intent(IntroSetPitActivity.this, IntroReEnterPinActivity.class);
                    intent.putExtra("pin", pin.toString());
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                    pin = new StringBuilder("");
                    startingNextActivity = false;
                }
            }, 100);

        }

    }
}
