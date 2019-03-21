package com.breadwallet.ui.recovery;

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
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
import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.mvvm.Resource;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.SmartValidator;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.ArrayList;
import java.util.List;

public class RecoveryKeyActivity extends BRActivity implements View.OnFocusChangeListener, BREdit.EditTextEventListener {
    private static final String TAG = RecoveryKeyActivity.class.getName();
    private Button mNextButton;

    private static final int NUMBER_OF_WORDS = 12;
    private static final int LAST_WORD_INDEX = 11;

    public static final String EXTRA_UNLINK = "com.breadwallet.EXTRA_UNLINK";
    public static final String EXTRA_RESET_PIN = "com.breadwallet.EXTRA_RESET_PIN";

    private List<BREdit> mEditTextWords = new ArrayList<>(NUMBER_OF_WORDS);

    //will be true if this screen was called from the restore screen
    private boolean mIsUnlinking = false;
    private boolean mIsResettingPin = false;
    private TypedValue mTypedValue = new TypedValue();
    private RecoveryKeyViewModel mViewModel;
    private Observer<Resource<Void>> mRecoverWalletObserver = resource -> {
        if (resource == null) {
            return;
        }
        switch (resource.getStatus()) {
            case SUCCESS:
                Intent intent = new Intent(this, InputPinActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
                break;
            case LOADING:
                findViewById(R.id.loading_view).setVisibility(View.VISIBLE);
                break;
            case ERROR:
                findViewById(R.id.loading_view).setVisibility(View.GONE);
                break;
            default:
                break;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_words);

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
                BaseWalletManager wm = WalletsMaster.getInstance().getCurrentWallet(RecoveryKeyActivity.this);
                UiUtils.showSupportFragment(RecoveryKeyActivity.this, BRConstants.FAQ_PAPER_KEY, wm);
            }
        });

        TextView title = findViewById(R.id.title);
        TextView description = findViewById(R.id.description);

        mEditTextWords.add((BREdit) findViewById(R.id.word1));
        mEditTextWords.add((BREdit) findViewById(R.id.word2));
        mEditTextWords.add((BREdit) findViewById(R.id.word3));
        mEditTextWords.add((BREdit) findViewById(R.id.word4));
        mEditTextWords.add((BREdit) findViewById(R.id.word5));
        mEditTextWords.add((BREdit) findViewById(R.id.word6));
        mEditTextWords.add((BREdit) findViewById(R.id.word7));
        mEditTextWords.add((BREdit) findViewById(R.id.word8));
        mEditTextWords.add((BREdit) findViewById(R.id.word9));
        mEditTextWords.add((BREdit) findViewById(R.id.word10));
        mEditTextWords.add((BREdit) findViewById(R.id.word11));
        mEditTextWords.add((BREdit) findViewById(R.id.word12));

        mViewModel = ViewModelProviders.of(this).get(RecoveryKeyViewModel.class);

        for (EditText editText : mEditTextWords) {
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

        mEditTextWords.get(LAST_WORD_INDEX).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    mNextButton.performClick();
                }
                return false;
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateAllWords();
                final Activity app = RecoveryKeyActivity.this;
                String phraseToCheck = getPhrase();
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
                                BRDialog.showCustomDialog(RecoveryKeyActivity.this, getString(R.string.WipeWallet_alertTitle),
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
                                AuthManager.getInstance().setPinCode(RecoveryKeyActivity.this, "");
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
                        mViewModel.recoverWallet(RecoveryKeyActivity.this, cleanPhrase)
                                .observe(RecoveryKeyActivity.this, mRecoverWalletObserver);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) {
            mEditTextWords.get(0).addEditTextEventListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (BuildConfig.DEBUG) {
            mEditTextWords.get(0).removeEditTextEventListener(this);
        }
    }

    private String getPhrase() {
        boolean success = true;

        StringBuilder paperKeyStringBuilder = new StringBuilder();
        for (EditText editText : mEditTextWords) {
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
            BRReportsManager.reportBug(new IllegalArgumentException("Paper key contains " + numberOfWords + "words"));
            return null;
        }

        return paperKey;
    }

    private String clean(String word) {
        return word.trim().replaceAll(" ", "");
    }

    private void clearWords() {
        for (EditText editText : mEditTextWords) {
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

    private void validateAllWords() {
        for (int i = 0; i < mEditTextWords.size(); i++) {
            BREdit editText = mEditTextWords.get(i);
            validateWord(editText);
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
        } else if (requestCode == BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mViewModel.recoverWallet(this).observe(this, mRecoverWalletObserver);
            } else {
                finish();
            }
        }
    }

    @Override
    public void onEvent(BREdit.EditTextEvent editTextEvent) {
        switch (editTextEvent) {
            case PASTE:
                String clipboardText = BRClipboardManager.getClipboard(this);
                String[] potentialWords = clipboardText.split("\\s+");  // Uses any whitespace as a delimiter.
                for (int i = 0; i < potentialWords.length && i < mEditTextWords.size(); i++) {
                    BREdit firstEditText = mEditTextWords.get(i);
                    firstEditText.setText(potentialWords[i]);
                    validateWord(firstEditText);
                }
                break;
        }
    }
}
