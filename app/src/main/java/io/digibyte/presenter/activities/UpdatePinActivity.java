package io.digibyte.presenter.activities;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.digibyte.R;
import io.digibyte.presenter.activities.util.ActivityUTILS;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.customviews.BRKeyboard;
import io.digibyte.presenter.interfaces.BROnSignalCompletion;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.security.AuthManager;
import io.digibyte.tools.security.BRKeyStore;
import io.digibyte.tools.util.BRConstants;

public class UpdatePinActivity extends BRActivity {
    private static final String TAG = UpdatePinActivity.class.getName();
    private BRKeyboard keyboard;
    private View dot1;
    private View dot2;
    private View dot3;
    private View dot4;
    private View dot5;
    private View dot6;
    private StringBuilder pin = new StringBuilder();
    private int pinLimit = 6;
    //    private boolean allowInserting = true;
    private TextView title;
    private TextView description;
    int mode = ENTER_PIN;
    public static final int ENTER_PIN = 1;
    public static final int ENTER_NEW_PIN = 2;
    public static final int RE_ENTER_NEW_PIN = 3;

    private ImageButton faq;
    private LinearLayout pinLayout;
    private String curNewPin = "";
    public static boolean appVisible = false;
    private static UpdatePinActivity app;

    public static UpdatePinActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_template);

        keyboard = (BRKeyboard) findViewById(R.id.brkeyboard);
        title = (TextView) findViewById(R.id.title);
        description = (TextView) findViewById(R.id.description);
        pinLayout = (LinearLayout) findViewById(R.id.pinLayout);
        if (BRKeyStore.getPinCode(this).length() == 4) pinLimit = 4;
        setMode(ENTER_PIN);
        title.setText(getString(R.string.UpdatePin_updateTitle));
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);
        dot5 = findViewById(R.id.dot5);
        dot6 = findViewById(R.id.dot6);

        /* faq = (ImageButton) findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.setPin);
            }
        }); */


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

        AuthManager.getInstance().updateDots(this, pinLimit, pin.toString(), dot1, dot2, dot3, dot4, dot5, dot6, R.drawable.ic_pin_dot_gray, new AuthManager.OnPinSuccess() {
            @Override
            public void onSuccess() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        goNext();
                    }
                }, 100);
            }
        });

    }

    private void goNext() {
        switch (mode) {
            case ENTER_PIN:
                if (AuthManager.getInstance().checkAuth(pin.toString(), this)) {
                    setMode(ENTER_NEW_PIN);
                    pinLimit = 6;
                } else {
                    SpringAnimator.failShakeAnimation(this, pinLayout);
                }
                pin = new StringBuilder("");
                updateDots();
                break;
            case ENTER_NEW_PIN:
                setMode(RE_ENTER_NEW_PIN);
                curNewPin = pin.toString();
                pin = new StringBuilder("");
                updateDots();
                break;

            case RE_ENTER_NEW_PIN:
                if (curNewPin.equalsIgnoreCase(pin.toString())) {
                    AuthManager.getInstance().setPinCode(pin.toString(), this);
                    BRAnimator.showBreadSignal(this, getString(R.string.Alerts_pinSet), getString(R.string.UpdatePin_caption), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                        @Override
                        public void onComplete() {
                            BRAnimator.startBreadActivity(UpdatePinActivity.this, false);
                        }
                    });
                } else {
                    SpringAnimator.failShakeAnimation(this, pinLayout);
                    setMode(ENTER_NEW_PIN);
                    pinLimit = BRKeyStore.getPinCode(this).length();
                }
                pin = new StringBuilder("");
                updateDots();
                break;
        }
    }

    private void setMode(int mode) {
        String text = "";
        this.mode = mode;
        switch (mode) {
            case ENTER_PIN:
                text = getString(R.string.UpdatePin_enterCurrent);
                break;
            case ENTER_NEW_PIN:
                text = getString(R.string.UpdatePin_enterNew);
                break;
            case RE_ENTER_NEW_PIN:
                text = getString(R.string.UpdatePin_reEnterNew);
                break;
        }
        description.setText(text);
        SpringAnimator.springView(description);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }
}
