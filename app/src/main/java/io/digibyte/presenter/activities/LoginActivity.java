package io.digibyte.presenter.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.digibyte.R;
import io.digibyte.presenter.activities.camera.ScanQRActivity;
import io.digibyte.presenter.activities.util.ActivityUTILS;
import io.digibyte.presenter.activities.util.BRActivity;
import io.digibyte.presenter.customviews.BRDialogView;
import io.digibyte.presenter.customviews.BRKeyboard;
import io.digibyte.presenter.interfaces.BRAuthCompletion;
import io.digibyte.tools.animation.BRAnimator;
import io.digibyte.tools.animation.BRDialog;
import io.digibyte.tools.animation.SpringAnimator;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.security.AuthManager;
import io.digibyte.tools.security.BRKeyStore;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.util.BRConstants;
import io.digibyte.tools.util.Utils;
import io.digibyte.wallet.BRWalletManager;
import com.platform.APIClient;


import static io.digibyte.R.color.white;
import static io.digibyte.tools.util.BRConstants.PLATFORM_ON;
import static io.digibyte.tools.util.BRConstants.SCANNER_REQUEST;

public class LoginActivity extends BRActivity {
    private static final String TAG = LoginActivity.class.getName();
    private BRKeyboard keyboard;
    private LinearLayout pinLayout;
    private View dot1;
    private View dot2;
    private View dot3;
    private View dot4;
    private View dot5;
    private View dot6;
    private StringBuilder pin = new StringBuilder();
    private int pinLimit = 6;
    private static LoginActivity app;

    private ImageView unlockedImage;
    private TextView unlockedText;
    private TextView enterPinLabel;
    private LinearLayout offlineButtonsLayout;

    private ImageButton fingerPrint;
    public static boolean appVisible = false;
    private boolean inputAllowed = true;

    private Button leftButton;
    private Button rightButton;


    public static LoginActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);
        String pin = BRKeyStore.getPinCode(this);
        if (pin.isEmpty() || (pin.length() != 6 && pin.length() != 4)) {
            Intent intent = new Intent(this, SetPinActivity.class);
            intent.putExtra("noPin", true);
            startActivity(intent);
            if (!LoginActivity.this.isDestroyed()) finish();
            return;
        }

        if (BRKeyStore.getPinCode(this).length() == 4) pinLimit = 4;

        keyboard = (BRKeyboard) findViewById(R.id.brkeyboard);
        pinLayout = (LinearLayout) findViewById(R.id.pinLayout);
        fingerPrint = (ImageButton) findViewById(R.id.fingerprint_icon);

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

        keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                handleClick(key);
            }
        });
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_trans_button);
        keyboard.setBRButtonTextColor(R.color.white);
        keyboard.setShowDot(false);
        keyboard.setBreadground(getDrawable(R.drawable.bread_gradient));
        keyboard.setCustomButtonBackgroundColor(10, getColor(android.R.color.transparent));
        keyboard.setDeleteImage(getDrawable(R.drawable.ic_delete_white));

        leftButton = (Button) findViewById(R.id.left_button);
        rightButton = (Button) findViewById(R.id.right_button);

        setUpOfflineButtons();

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showReceiveFragment(LoginActivity.this, false);
//                chooseWordsSize(true);
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                try {
                    // Check if the camera permission is granted
                    if (ContextCompat.checkSelfPermission(app,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Should we show an expgetString(R.string.ConfirmPaperPhrase_word)lanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(app,
                                Manifest.permission.CAMERA)) {
                            BRDialog.showCustomDialog(app, getString(R.string.Send_cameraUnavailabeTitle_android),
                                    getString(R.string.Send_cameraUnavailabeMessage_android), getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                        @Override
                                        public void onClick(BRDialogView brDialogView) {
                                            brDialogView.dismiss();
                                        }
                                    }, null, null, 0);
                        } else {
                            // No explanation needed, we can request the permission.
                            ActivityCompat.requestPermissions(app,
                                    new String[]{Manifest.permission.CAMERA},
                                    BRConstants.CAMERA_REQUEST_ID);
                        }
                    } else {
                        // Permission is granted, open camera
                        Intent intent = new Intent(app, ScanQRActivity.class);
                        app.startActivityForResult(intent, SCANNER_REQUEST);
                        app.overridePendingTransition(R.anim.fade_up, 0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final boolean useFingerprint = AuthManager.isFingerPrintAvailableAndSetup(this) && BRSharedPrefs.getUseFingerprint(this);
//        Log.e(TAG, "onCreate: isFingerPrintAvailableAndSetup: " + useFingerprint);
        fingerPrint.setVisibility(useFingerprint ? View.VISIBLE : View.GONE);

        if (useFingerprint)
            fingerPrint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AuthManager.getInstance().authPrompt(LoginActivity.this, "", "", false, true, new BRAuthCompletion() {
                        @Override
                        public void onComplete() {
//                            AuthManager.getInstance().authSuccess(LoginActivity.this);
                            unlockWallet();
                        }

                        @Override
                        public void onCancel() {

                        }
                    });
                }
            });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (fingerPrint != null && useFingerprint)
                    fingerPrint.performClick();
            }
        }, 500);

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDots();

        appVisible = true;
        app = this;
        inputAllowed = true;
        if (!BRWalletManager.getInstance().isCreated()) {
            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    BRWalletManager.getInstance().initWallet(LoginActivity.this);
                }
            });
        }
        if (PLATFORM_ON)
            APIClient.getInstance(this).updatePlatform();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    private void handleClick(String key) {
        if (!inputAllowed) {
            Log.e(TAG, "handleClick: input not allowed");
            return;
        }
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
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
        } else {
            finishAffinity();
        }
    }

    private void unlockWallet() {
        pin = new StringBuilder("");
        offlineButtonsLayout.animate().translationY(-600).setInterpolator(new AccelerateInterpolator());
        pinLayout.animate().translationY(-2000).setInterpolator(new AccelerateInterpolator());
        enterPinLabel.animate().translationY(-1800).setInterpolator(new AccelerateInterpolator());
        keyboard.animate().translationY(2000).setInterpolator(new AccelerateInterpolator());
        unlockedImage.animate().alpha(1f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(LoginActivity.this, BreadActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
                        if (!LoginActivity.this.isDestroyed()) {
                            LoginActivity.this.finish();
                        }
                    }
                }, 400);
            }
        });
        unlockedText.animate().alpha(1f);
    }

    private void showFailedToUnlock() {
        SpringAnimator.failShakeAnimation(LoginActivity.this, pinLayout);
        pin = new StringBuilder("");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                inputAllowed = true;
                updateDots();
            }
        }, 1000);
    }

    private void updateDots() {
        AuthManager.getInstance().updateDots(this, pinLimit, pin.toString(), dot1, dot2, dot3, dot4, dot5, dot6, R.drawable.ic_pin_dot_white,
                new AuthManager.OnPinSuccess() {
                    @Override
                    public void onSuccess() {
                        inputAllowed = false;
                        if (AuthManager.getInstance().checkAuth(pin.toString(), LoginActivity.this)) {
                            AuthManager.getInstance().authSuccess(LoginActivity.this);
                            unlockWallet();
                        } else {
                            AuthManager.getInstance().authFail(LoginActivity.this);
                            showFailedToUnlock();
                        }
                    }
                });
    }

    private void setUpOfflineButtons() {
        int activeColor = getColor(white);
        GradientDrawable leftDrawable = (GradientDrawable) leftButton.getBackground().getCurrent();
        GradientDrawable rightDrawable = (GradientDrawable) rightButton.getBackground().getCurrent();

        int rad = Utils.getPixelsFromDps(this, (int) getResources().getDimension(R.dimen.radius) / 2);
        int stoke = 2;

        leftDrawable.setCornerRadii(new float[]{rad, rad, 0, 0, 0, 0, rad, rad});
        rightDrawable.setCornerRadii(new float[]{0, 0, rad, rad, rad, rad, 0, 0});

        leftDrawable.setStroke(stoke, activeColor, 0, 0);
        rightDrawable.setStroke(stoke, activeColor, 0, 0);
        leftButton.setTextColor(activeColor);
        rightButton.setTextColor(activeColor);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case BRConstants.CAMERA_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    BRAnimator.openScanner(this, BRConstants.SCANNER_REQUEST);
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Log.e(TAG, "onRequestPermissionsResult: permission isn't granted for: " + requestCode);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

}
