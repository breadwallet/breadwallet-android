package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.security.SmartValidator;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;

import java.util.ArrayList;
import java.util.List;

public class InputWordsActivity extends BRActivity implements View.OnFocusChangeListener {
    private static final String TAG = InputWordsActivity.class.getName();
    private Button mNextButton;

    private static final int NUMBER_OF_WORDS = 12;
    private static final int LAST_WORD_INDEX = 11;

    public static final String EXTRA_UNLINK = "com.breadwallet.EXTRA_UNLINK";
    public static final String EXTRA_RESET_PIN = "com.breadwallet.EXTRA_RESET_PIN";

    private List<EditText> mEditWords = new ArrayList<>(NUMBER_OF_WORDS);

    private String mDebugPhrase;

    //will be true if this screen was called from the restore screen
    private boolean mIsUnlinking = false;
    private boolean mIsResettingPin = false;
    private TypedValue mTypedValue = new TypedValue();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_words);

//        if (Utils.isEmulatorOrDebug(this)) {
//            //japanese
//            mDebugPhrase = "こせき　ぎじにってい　けっこん　せつぞく　うんどう　ふこう　にっすう　こせい　きさま　なまみ　たきび　はかい";
//            //english
//            mDebugPhrase = "blush wear arctic fruit unique quantum because mammal entry country school curtain";
//            //french
//            mDebugPhrase = "eyebrow elbow weasel again gate organ mobile then behind name debate joke";
//            //spanish
//            mDebugPhrase = "zorro turismo mezcla nicho morir chico blanco pájaro alba esencia roer repetir";
//            //chinese
//            mDebugPhrase = "怨 贪 旁 扎 吹 音 决 廷 十 助 畜 怒";
//        }

        mNextButton = findViewById(R.id.send_button);

        getTheme().resolveAttribute(R.attr.input_words_text_color, mTypedValue, true);

        if (Utils.isUsingCustomInputMethod(this)) {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title), getString(R.string.Alert_customKeyboard_android),
                    getString(R.string.Button_ok), getString(R.string.JailbreakWarnings_close), new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            InputMethodManager imeManager = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
                            imeManager.showInputMethodPicker();
                            brDialogView.dismissWithAnimation();
                        }
                    }, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, 0);
        }

        ImageButton faq = findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                BaseWalletManager wm = WalletsMaster.getInstance(InputWordsActivity.this).getCurrentWallet(InputWordsActivity.this);
                UiUtils.showSupportFragment(InputWordsActivity.this, BRConstants.FAQ_PAPER_KEY, wm);
            }
        });

        TextView title = findViewById(R.id.title);
        TextView description = findViewById(R.id.description);

        mEditWords.add((EditText) findViewById(R.id.word1));
        mEditWords.add((EditText) findViewById(R.id.word2));
        mEditWords.add((EditText) findViewById(R.id.word3));
        mEditWords.add((EditText) findViewById(R.id.word4));
        mEditWords.add((EditText) findViewById(R.id.word5));
        mEditWords.add((EditText) findViewById(R.id.word6));
        mEditWords.add((EditText) findViewById(R.id.word7));
        mEditWords.add((EditText) findViewById(R.id.word8));
        mEditWords.add((EditText) findViewById(R.id.word9));
        mEditWords.add((EditText) findViewById(R.id.word10));
        mEditWords.add((EditText) findViewById(R.id.word11));
        mEditWords.add((EditText) findViewById(R.id.word12));

        for (EditText editText : mEditWords) {
            editText.setOnFocusChangeListener(this);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mIsUnlinking = extras.getBoolean(EXTRA_UNLINK);
            mIsResettingPin = extras.getBoolean(EXTRA_RESET_PIN);
        }

        if (mIsUnlinking) {
            //change the labels
            title.setText(getString(R.string.MenuViewController_recoverButton));
            description.setText(getString(R.string.WipeWallet_instruction));
        } else if (mIsResettingPin) {
            //change the labels
            title.setText(getString(R.string.RecoverWallet_header_reset_pin));
            description.setText(getString(R.string.RecoverWallet_subheader_reset_pin));
        }


        mEditWords.get(LAST_WORD_INDEX).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    mNextButton.performClick();
                }
                return false;
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                final Activity app = InputWordsActivity.this;
                String phraseToCheck = getPhrase();
                if (Utils.isEmulatorOrDebug(app) && !Utils.isNullOrEmpty(mDebugPhrase)) {
                    phraseToCheck = mDebugPhrase;
                }
                if (phraseToCheck == null) {
                    return;
                }
                String cleanPhrase = SmartValidator.cleanPaperKey(app, phraseToCheck);
                if (Utils.isNullOrEmpty(cleanPhrase)) {
                    BRReportsManager.reportBug(new NullPointerException("cleanPhrase is null or empty!"));
                    return;
                }
                if (SmartValidator.isPaperKeyValid(app, cleanPhrase)) {

                    if (mIsUnlinking || mIsResettingPin) {
                        if (SmartValidator.isPaperKeyCorrect(cleanPhrase, app)) {
                            Utils.hideKeyboard(app);
                            clearWords();

                            if (mIsUnlinking) {
                                BRDialog.showCustomDialog(InputWordsActivity.this, getString(R.string.WipeWallet_alertTitle),
                                        getString(R.string.WipeWallet_alertMessage), getString(R.string.WipeWallet_wipe), getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                BreadApp.clearApplicationUserData();
                                            }
                                        }, new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismissWithAnimation();
                                            }
                                        }, null, 0);

                            } else {
                                AuthManager.getInstance().setPinCode(InputWordsActivity.this, "");
                                Intent intent = new Intent(app, InputPinActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                                startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
                            }


                        } else {
                            BRDialog.showCustomDialog(app, "", getString(R.string.RecoverWallet_invalid),
                                    getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                        @Override
                                        public void onClick(BRDialogView brDialogView) {
                                            brDialogView.dismissWithAnimation();
                                        }
                                    }, null, null, 0);
                        }

                    } else {
                        // Recover Wallet
                        Utils.hideKeyboard(app);
                        WalletsMaster m = WalletsMaster.getInstance(InputWordsActivity.this);
                        PostAuth.getInstance().setCachedPaperKey(cleanPhrase);
                        //Disallow BTC and BCH sending.
                        BRSharedPrefs.putAllowSpend(app, BaseBitcoinWalletManager.BITCASH_CURRENCY_CODE, false);
                        BRSharedPrefs.putAllowSpend(app, BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE, false);

                        PostAuth.getInstance().onRecoverWalletAuth(app, false);
                    }

                } else {
                    BRDialog.showCustomDialog(app, "", getResources().getString(R.string.RecoverWallet_invalid),
                            getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private String getPhrase() {
        boolean success = true;

        StringBuilder paperKeyStringBuilder = new StringBuilder();
        for (EditText editText : mEditWords) {
            String cleanedWords = clean(editText.getText().toString().toLowerCase());
            if (Utils.isNullOrEmpty(cleanedWords)) {
                SpringAnimator.failShakeAnimation(this, editText);
                success = false;
            } else {
                paperKeyStringBuilder.append(cleanedWords);
                paperKeyStringBuilder.append(' ');
            }
        }

        if (!success) {
            return null;
        }

        // Ensure the paper key is 12 words.
        String paperKey = paperKeyStringBuilder.toString().trim();
        int numberOfWords = paperKey.split(" ").length;
        if (numberOfWords != NUMBER_OF_WORDS) {
            BRReportsManager.reportBug(new IllegalArgumentException("Paper key contains " + numberOfWords + " words"));
            return null;
        }

        return paperKey;
    }

    private String clean(String word) {
        return word.trim().replaceAll(" ", "");
    }

    private void clearWords() {
        for (EditText editText : mEditWords) {
            editText.setText("");
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            validateWord((EditText) v);
        } else {
            ((EditText) v).setTextColor(getColor(mTypedValue.resourceId));
        }
    }

    private void validateWord(EditText view) {
        String word = view.getText().toString();
        boolean valid = SmartValidator.isWordValid(this, word);
        view.setTextColor(getColor(valid ? mTypedValue.resourceId : R.color.red_text));
        if (!valid) {
            SpringAnimator.failShakeAnimation(this, view);
        }
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

}
