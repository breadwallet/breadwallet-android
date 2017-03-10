package com.breadwallet.presenter.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRSoftKeyboard;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;

import static android.R.attr.duration;
import static com.breadwallet.R.color.dark_blue;
import static com.breadwallet.R.color.extra_light_grey;
import static com.breadwallet.R.color.white;

public class PinActivity extends Activity {
    private static final String TAG = PinActivity.class.getName();
    private BRSoftKeyboard keyboard;
    private LinearLayout pinLayout;
    private View dot1;
    private View dot2;
    private View dot3;
    private View dot4;
    private View dot5;
    private View dot6;
    private StringBuilder pin = new StringBuilder();
    private int pinLimit = 6;

    private ImageView unlockedImage;
    private TextView unlockedText;
    private TextView enterPinLabel;
    private LinearLayout offlineButtonsLayout;

    private Button leftButton;
    private Button rightButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);
        setStatusBarColor(android.R.color.transparent);
        keyboard = (BRSoftKeyboard) findViewById(R.id.brkeyboard);
        pinLayout = (LinearLayout) findViewById(R.id.pinLayout);

        unlockedImage = (ImageView) findViewById(R.id.unlocked_image);
        unlockedText = (TextView) findViewById(R.id.unlocked_text);
        enterPinLabel = (TextView) findViewById(R.id.enter_pin_label);
        offlineButtonsLayout = (LinearLayout) findViewById(R.id.buttons_layout);

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
        keyboard.setBRButtonBackgroundColor(R.color.white_trans);
        keyboard.setBRButtonTextColor(R.color.white);

        leftButton = (Button) findViewById(R.id.left_button);
        rightButton = (Button) findViewById(R.id.right_button);

        chooseWordsSize(true);

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(leftButton);
                chooseWordsSize(true);
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(rightButton);
                chooseWordsSize(false);
            }
        });

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

    public void startBreadActivity(Activity from) {
        Intent intent;
        intent = new Intent(from, BreadActivity.class);
        from.startActivity(intent);
        if (!from.isDestroyed()) {
            from.finish();
        }
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
        dot1.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_white : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot2.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_white : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot3.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_white : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot4.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_white : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot5.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_white : R.drawable.ic_pin_dot_black));
        selectedDots--;
        dot6.setBackground(getDrawable(selectedDots <= 0 ? R.drawable.ic_pin_dot_white : R.drawable.ic_pin_dot_black));

        if (pin.length() == 6) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    String actualPin = KeyStoreManager.getPassCode(PinActivity.this);
                    if (actualPin.equalsIgnoreCase(pin.toString())) {
                        unlockWallet();
                    } else {
                        showFailedToUnlock();
                    }

                }
            }, 100);

        }

    }

    private void unlockWallet() {
        pin = new StringBuilder("");
        int duration = 2000;
//        TranslateAnimation translateUp = new TranslateAnimation(offlineButtonsLayout.getX(), offlineButtonsLayout.getX(), offlineButtonsLayout.getY(), -200);
//        translateUp.setDuration(duration);
//        translateUp.setInterpolator(new AccelerateInterpolator());
//        setAnimationListenerToView(offlineButtonsLayout, translateUp);
//        offlineButtonsLayout.startAnimation(translateUp);
        offlineButtonsLayout.animate().translationY(-600).setInterpolator(new AccelerateInterpolator());
        pinLayout.animate().translationY(-2000).setInterpolator(new AccelerateInterpolator());
        enterPinLabel.animate().translationY(-1800).setInterpolator(new AccelerateInterpolator());
        keyboard.animate().translationY(600).setInterpolator(new AccelerateInterpolator());
        unlockedImage.animate().alpha(1f).setDuration(1000).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                Intent intent = new Intent(PinActivity.this, BreadActivity.class);
                startActivity(intent);
            }
        });
        unlockedText.animate().alpha(1f);

//        TranslateAnimation translateUp2 = new TranslateAnimation(pinLayout.getX(), pinLayout.getX(), pinLayout.getY(), -200);
//        translateUp2.setDuration(duration);
//        translateUp2.setInterpolator(new AccelerateInterpolator());
//        setAnimationListenerToView(pinLayout, translateUp2);
//        pinLayout.startAnimation(translateUp2);
//
//        TranslateAnimation translateUp3 = new TranslateAnimation(enterPinLabel.getX(), enterPinLabel.getX(), enterPinLabel.getY(), -200);
//        translateUp3.setDuration(duration);
//        translateUp3.setInterpolator(new AccelerateInterpolator());
//        setAnimationListenerToView(enterPinLabel, translateUp3);
//        enterPinLabel.startAnimation(translateUp3);
//
//        TranslateAnimation translateDown = new TranslateAnimation(keyboard.getX(), keyboard.getX(), keyboard.getY(), 2000);
//        translateDown.setDuration(duration);
//        translateDown.setInterpolator(new AccelerateInterpolator());
//        setAnimationListenerToView(keyboard, translateDown);
//        keyboard.startAnimation(translateDown);
//
//        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
//        alphaAnimation.setDuration(duration);
//        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//                Intent intent = new Intent(PinActivity.this, BreadActivity.class);
//                startActivity(intent);
//                unlockedImage.setAlpha(1f);
//                unlockedText.setAlpha(1f);
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//
//            }
//        });
//        unlockedText.startAnimation(alphaAnimation);
//        unlockedImage.startAnimation(alphaAnimation);


    }

    private void setAnimationListenerToView(final View v, Animation animation) {
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void showFailedToUnlock() {
        SpringAnimator.failShakeAnimation(PinActivity.this, pinLayout);
        pin = new StringBuilder("");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDots();
            }
        }, 1000);
    }

    private void chooseWordsSize(boolean isLeft) {
        int activeColor = getColor(white);
        int nonActiveColor = getColor(white);
        GradientDrawable leftDrawable = (GradientDrawable) leftButton.getBackground().getCurrent();
        GradientDrawable rightDrawable = (GradientDrawable) rightButton.getBackground().getCurrent();

        int rad = 30;
        int stoke = 2;

        leftDrawable.setCornerRadii(new float[]{rad, rad, 0, 0, 0, 0, rad, rad});
        rightDrawable.setCornerRadii(new float[]{0, 0, rad, rad, rad, rad, 0, 0});

        if (isLeft) {
            leftDrawable.setStroke(stoke, activeColor, 0, 0);
            rightDrawable.setStroke(stoke, nonActiveColor, 0, 0);
            leftButton.setTextColor(activeColor);
            rightButton.setTextColor(nonActiveColor);
        } else {
            leftDrawable.setStroke(stoke, nonActiveColor, 0, 0);
            rightDrawable.setStroke(stoke, activeColor, 0, 0);
            leftButton.setTextColor(nonActiveColor);
            rightButton.setTextColor(activeColor);
        }

    }
}
