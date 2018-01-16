package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.activities.settings.WebViewActivity;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.wallet.BRWalletManager;

import java.util.List;

import static com.breadwallet.tools.util.Bip39Reader.getAllWordLists;
import static com.platform.HTTPServer.URL_SUPPORT;

public class InputWordsActivity extends BRActivity {
    private static final String TAG = InputWordsActivity.class.getName();
    private Button nextButton;

    private EditText word1;
    private EditText word2;
    private EditText word3;
    private EditText word4;
    private EditText word5;
    private EditText word6;
    private EditText word7;
    private EditText word8;
    private EditText word9;
    private EditText word10;
    private EditText word11;
    private EditText word12;

    private TextView title;
    private TextView description;
    public static boolean appVisible = false;
    private static InputWordsActivity app;

    public static InputWordsActivity getApp() {
        return app;
    }

    //will be true if this screen was called from the restore screen
    private boolean restore = false;
    private boolean resetPin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_words);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        nextButton = (Button) findViewById(R.id.send_button);

        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.paperKey);
            }
        });

        title = (TextView) findViewById(R.id.title);
        description = (TextView) findViewById(R.id.description);

        word1 = (EditText) findViewById(R.id.word1);
        word2 = (EditText) findViewById(R.id.word2);
        word3 = (EditText) findViewById(R.id.word3);
        word4 = (EditText) findViewById(R.id.word4);
        word5 = (EditText) findViewById(R.id.word5);
        word6 = (EditText) findViewById(R.id.word6);
        word7 = (EditText) findViewById(R.id.word7);
        word8 = (EditText) findViewById(R.id.word8);
        word9 = (EditText) findViewById(R.id.word9);
        word10 = (EditText) findViewById(R.id.word10);
        word11 = (EditText) findViewById(R.id.word11);
        word12 = (EditText) findViewById(R.id.word12);

        restore = getIntent().getExtras() != null && getIntent().getExtras().getBoolean("restore");
        resetPin = getIntent().getExtras() != null && getIntent().getExtras().getBoolean("resetPin");

        if (restore) {
            //change the labels
            title.setText(getString(R.string.MenuViewController_recoverButton));
            description.setText(getString(R.string.WipeWallet_instruction));
        } else if (resetPin) {
            //change the labels
            title.setText(getString(R.string.RecoverWallet_header_reset_pin));
            description.setText(getString(R.string.RecoverWallet_subheader_reset_pin));
        }

        word12.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    nextButton.performClick();
                }
                return false;
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                final Activity app = InputWordsActivity.this;
                String phraseToCheck = getPhrase();
                if (phraseToCheck == null) return;
                String cleanPhrase = Bip39Reader.cleanPhrase(app, phraseToCheck);

                if (BRWalletManager.getInstance().validatePhrase(app, cleanPhrase)) {

                    if (restore || resetPin) {
                        if (BRKeyStore.phraseIsValid(cleanPhrase, app)) {
                            Utils.hideKeyboard(app);
                            clearWords();
                            Intent intent;
                            if (restore) {
                                BRWalletManager m = BRWalletManager.getInstance();
                                m.wipeWalletButKeystore(app);
                                m.wipeKeyStore(app);
                                intent = new Intent(app, IntroActivity.class);
                            } else {
                                AuthManager.getInstance().setPinCode("", InputWordsActivity.this);
                                intent = new Intent(app, SetPinActivity.class);
                                intent.putExtra("noPin", true);
                            }

                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                            startActivity(intent);
                            if (!InputWordsActivity.this.isDestroyed()) finish();

                        } else {
                            BRDialog.showCustomDialog(app, "", getString(R.string.RecoverWallet_invalid), getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismissWithAnimation();
                                }
                            }, null, null, 0);
                        }

                    } else {
                        Utils.hideKeyboard(app);
                        BRWalletManager m = BRWalletManager.getInstance();
                        m.wipeWalletButKeystore(app);
                        m.wipeKeyStore(app);
                        PostAuth.getInstance().setPhraseForKeyStore(cleanPhrase);
                        PostAuth.getInstance().onRecoverWalletAuth(app, false);
                        BRSharedPrefs.putAllowSpend(app, false);
                    }

                } else {
                    String message = getResources().getString(R.string.RecoverWallet_invalid);

                    BRDialog.showCustomDialog(app, "", message, getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);

                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private String getPhrase() {
        boolean success = true;

        String w1 = word1.getText().toString().toLowerCase();
        String w2 = word2.getText().toString().toLowerCase();
        String w3 = word3.getText().toString().toLowerCase();
        String w4 = word4.getText().toString().toLowerCase();
        String w5 = word5.getText().toString().toLowerCase();
        String w6 = word6.getText().toString().toLowerCase();
        String w7 = word7.getText().toString().toLowerCase();
        String w8 = word8.getText().toString().toLowerCase();
        String w9 = word9.getText().toString().toLowerCase();
        String w10 = word10.getText().toString().toLowerCase();
        String w11 = word11.getText().toString().toLowerCase();
        String w12 = word12.getText().toString().toLowerCase();

        if (Utils.isNullOrEmpty(w1)) {
            SpringAnimator.failShakeAnimation(this, word1);
            success = false;
        }
        if (Utils.isNullOrEmpty(w2)) {
            SpringAnimator.failShakeAnimation(this, word2);
            success = false;
        }
        if (Utils.isNullOrEmpty(w3)) {
            SpringAnimator.failShakeAnimation(this, word3);
            success = false;
        }
        if (Utils.isNullOrEmpty(w4)) {
            SpringAnimator.failShakeAnimation(this, word4);
            success = false;
        }
        if (Utils.isNullOrEmpty(w5)) {
            SpringAnimator.failShakeAnimation(this, word5);
            success = false;
        }
        if (Utils.isNullOrEmpty(w6)) {
            SpringAnimator.failShakeAnimation(this, word6);
            success = false;
        }
        if (Utils.isNullOrEmpty(w7)) {
            SpringAnimator.failShakeAnimation(this, word7);
            success = false;
        }
        if (Utils.isNullOrEmpty(w8)) {
            SpringAnimator.failShakeAnimation(this, word8);
            success = false;
        }
        if (Utils.isNullOrEmpty(w9)) {
            SpringAnimator.failShakeAnimation(this, word9);
            success = false;
        }
        if (Utils.isNullOrEmpty(w10)) {
            SpringAnimator.failShakeAnimation(this, word10);
            success = false;
        }
        if (Utils.isNullOrEmpty(w11)) {
            SpringAnimator.failShakeAnimation(this, word11);
            success = false;
        }
        if (Utils.isNullOrEmpty(w12)) {
            SpringAnimator.failShakeAnimation(this, word12);
            success = false;
        }

        if (!success) return null;

        return w1 + " " + w2 + " " + w3 + " " + w4 + " " + w5 + " " + w6 + " " + w7 + " " + w8 + " " + w9 + " " + w10 + " " + w11 + " " + w12;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    PostAuth.getInstance().onRecoverWalletAuth(this, true);
                } else {
                    finish();
                }
                break;

        }

    }

    private void clearWords() {
        word1.setText("");
        word2.setText("");
        word3.setText("");
        word4.setText("");
        word5.setText("");
        word6.setText("");
        word7.setText("");
        word8.setText("");
        word9.setText("");
        word10.setText("");
        word11.setText("");
        word12.setText("");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

}
