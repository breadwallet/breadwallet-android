package com.breadwallet.presenter.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.customviews.PinLayout;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.wallet.WalletsMaster;


public class LoginActivity extends BRActivity implements PinLayout.OnPinInserted {
    private static final String TAG = LoginActivity.class.getName();
    private BRKeyboard mKeyboard;
    private LinearLayout mPinLayout;
    private ImageView mUnlockedImage;
    private ImageButton mFingerPrint;
    private PinLayout mPinDigitViews;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        mPinDigitViews = findViewById(R.id.pin_digits);
        mKeyboard = findViewById(R.id.brkeyboard);
        mPinLayout = findViewById(R.id.pinLayout);
        mFingerPrint = findViewById(R.id.fingerprint_icon);
        mUnlockedImage = findViewById(R.id.unlocked_image);

        String pin = BRKeyStore.getPinCode(this);
        if (pin.isEmpty()) {
            Intent intent = new Intent(LoginActivity.this, InputPinActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
            return;
        }
        if (!PinLayout.isPinLengthValid(pin.length())) {
            throw new IllegalArgumentException("Pin length illegal: " + pin.length());
        }

        mKeyboard.setShowDecimal(false);
        mKeyboard.setDeleteButtonBackgroundColor(getColor(android.R.color.transparent));
        mKeyboard.setDeleteImage(R.drawable.ic_delete_dark);

        int[] pinDigitButtonColors = getResources().getIntArray(R.array.pin_digit_button_colors);
        mKeyboard.setButtonTextColor(pinDigitButtonColors);

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                WalletsMaster.getInstance(LoginActivity.this).getAllWallets(LoginActivity.this);
            }
        });

        final boolean useFingerprint = AuthManager.isFingerPrintAvailableAndSetup(this) && BRSharedPrefs.getUseFingerprint(this);
        mFingerPrint.setVisibility(useFingerprint ? View.VISIBLE : View.GONE);

        if (useFingerprint) {
            mFingerPrint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AuthManager.getInstance().authPrompt(LoginActivity.this,
                            "", "", false, true, new BRAuthCompletion() {
                                @Override
                                public void onComplete() {
                                    unlockWallet();
                                }

                                @Override
                                public void onCancel() {

                                }
                            });
                }
            });
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mFingerPrint != null && useFingerprint) {
                    mFingerPrint.performClick();
                }

            }
        }, DateUtils.SECOND_IN_MILLIS / 2);

    }

    @Override
    protected void onResume() {
        super.onResume();

        mPinDigitViews.setup(mKeyboard, this);
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("BG:" + TAG + ":initLastWallet");
                WalletsMaster.getInstance(LoginActivity.this).initLastWallet(LoginActivity.this);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mPinDigitViews.cleanUp();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            finishAffinity();
        }
    }

    private void unlockWallet() {
        mFingerPrint.setVisibility(View.INVISIBLE);
        mPinLayout.animate().translationY(-R.dimen.animation_long).setInterpolator(new AccelerateInterpolator());
        mKeyboard.animate().translationY(R.dimen.animation_long).setInterpolator(new AccelerateInterpolator());
        mUnlockedImage.animate().alpha(1f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        boolean showHomeActivity = (BRSharedPrefs.wasAppBackgroundedFromHome(LoginActivity.this))
                                || BRSharedPrefs.isNewWallet(LoginActivity.this);

                        Class toGo = showHomeActivity ? HomeActivity.class : WalletActivity.class;
                        Intent intent = new Intent(LoginActivity.this, toGo);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
                        if (!LoginActivity.this.isDestroyed()) {
                            LoginActivity.this.finish();
                        }
                    }
                }, DateUtils.SECOND_IN_MILLIS / 2);
            }
        });
        EventUtils.pushEvent(EventUtils.EVENT_LOGIN_SUCCESS);
    }

    private void showFailedToUnlock() {
        SpringAnimator.failShakeAnimation(LoginActivity.this, mPinLayout);
        EventUtils.pushEvent(EventUtils.EVENT_LOGIN_FAILED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == InputPinActivity.SET_PIN_REQUEST_CODE && resultCode == RESULT_OK) {

            boolean isPinAccepted = data.getBooleanExtra(InputPinActivity.EXTRA_PIN_ACCEPTED, false);
            if (isPinAccepted) {
                UiUtils.startBreadActivity(this, false);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BRConstants.CAMERA_REQUEST_ID) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                UiUtils.openScanner(this);
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
            } else {
                Log.e(TAG, "onRequestPermissionsResult: permission isn't granted for: " + requestCode);
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onPinInserted(String pin, boolean isPinCorrect) {
        if (isPinCorrect) {
            unlockWallet();
        } else {
            showFailedToUnlock();
        }
    }
}
