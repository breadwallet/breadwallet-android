package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.customviews.PinLayout;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;


public class InputPinActivity extends BRActivity implements PinLayout.OnPinInserted {
    private static final String TAG = InputPinActivity.class.getName();

    private PinLayout mPinDigitViews;
    private BaseTextView mTitle;
    private String mNewPin;
    private PinMode mPinMode;
    private boolean mPinUpdateMode;
    public static final String EXTRA_PIN_MODE_UPDATE = "com.breadwallet.EXTRA_PIN_MODE_UPDATE";
    public static final String EXTRA_PIN_ACCEPTED = "com.breadwallet.EXTRA_PIN_ACCEPTED";
    public static final int SET_PIN_REQUEST_CODE = 274;
    private BRKeyboard mKeyboard;

    private enum PinMode {
        //Verify the old pin
        VERIFY,
        //Chose a new pin
        NEW,
        //Confirm the new pin
        CONFIRM
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_template);

        mKeyboard = findViewById(R.id.brkeyboard);

        mTitle = findViewById(R.id.title);

        ImageButton faq = findViewById(R.id.faq_button);
        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }
                BaseWalletManager walletManager = WalletsMaster.getInstance(InputPinActivity.this).getCurrentWallet(InputPinActivity.this);
                UiUtils.showSupportFragment(InputPinActivity.this, BRConstants.FAQ_SET_PIN, walletManager);
            }
        });
        int pinLength = BRKeyStore.getPinCode(this).length();
        mPinUpdateMode = getIntent().getBooleanExtra(EXTRA_PIN_MODE_UPDATE, false);
        if (pinLength > 0) {
            mPinMode = PinMode.VERIFY;
        } else {
            mPinMode = PinMode.NEW;
        }

        setModeUi();

        mPinDigitViews = findViewById(R.id.pin_digits);

        mKeyboard.setShowDecimal(false);

        int[] pinDigitButtonColors = getResources().getIntArray(R.array.pin_digit_button_colors);
        mKeyboard.setButtonTextColor(pinDigitButtonColors);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mPinDigitViews.setup(mKeyboard, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPinDigitViews.cleanUp();
    }

    @Override
    public void onPinInserted(String pin, boolean isPinCorrect) {
        switch (mPinMode) {
            case VERIFY:
                if (isPinCorrect) {
                    mPinMode = PinMode.NEW;
                    mPinDigitViews.setIsPinUpdating(true);
                    setModeUi();
                } else {
                    SpringAnimator.failShakeAnimation(this, mPinDigitViews);
                }
                break;
            case NEW:
                mNewPin = pin;
                mPinMode = PinMode.CONFIRM;
                setModeUi();
                break;

            case CONFIRM:
                if (pin.equals(mNewPin)) {
                    mPinDigitViews.setIsPinUpdating(false);
                    BRKeyStore.putPinCode(pin, this);
                    handleSuccess();
                } else {
                    SpringAnimator.failShakeAnimation(this, mPinDigitViews);
                    mNewPin = null;
                    mPinMode = PinMode.NEW;
                    setModeUi();
                }
                break;
        }
    }

    private void setModeUi() {
        switch (mPinMode) {
            case VERIFY:
                mTitle.setText(R.string.UpdatePin_enterCurrent);
                break;
            case NEW:
                if (mPinUpdateMode) {
                    mTitle.setText(R.string.UpdatePin_enterNew);
                } else {
                    mTitle.setText(R.string.UpdatePin_createTitle);
                }
                break;
            case CONFIRM:
                if (mPinUpdateMode) {
                    mTitle.setText(getString(R.string.UpdatePin_reEnterNew));
                } else {
                    mTitle.setText(getString(R.string.UpdatePin_createTitleConfirm));
                }
                break;
        }
    }

    private void handleSuccess() {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_PIN_ACCEPTED, true);
        setResult(RESULT_OK, intent);
        finish();
    }

}
