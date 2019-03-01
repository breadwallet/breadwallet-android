package com.breadwallet.presenter.activities;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.TransitionManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.intro.OnBoardingActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.SmartValidator;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.util.Bip39Reader;

import java.util.Locale;
import java.util.Random;

public class PaperKeyProveActivity extends BRActivity {
    private static final String TAG = PaperKeyProveActivity.class.getName();
    public static final String EXTRA_PAPER_KEY = "com.breadwallet.presenter.activities.EXTRA_PAPER_KEY";
    public static final String EXTRA_DONE_ACTION = "com.breadwallet.presenter.activities.EXTRA_DONE_ACTION";
    public static final int WORD_COUNT = 12;
    public static final char NULL_TERMINATOR = '\0';
    public static final int RANDOM_MAX_DIGIT = 10;
    private Button mSubmitButton;
    private EditText mWordEditTextFirst;
    private EditText mWordEditTextSecond;
    private TextView mWordTextViewFirst;
    private TextView mWordTextViewSecond;
    private ImageView mCheckMark1;
    private ImageView mCheckMark2;
    private SparseArray<String> mWordsSparseArray = new SparseArray<>();
    private ConstraintLayout mMainConstraintLayout;
    private ConstraintSet mModifiedConstraintSet = new ConstraintSet();
    private ConstraintSet mDefaultConstraintSet = new ConstraintSet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paper_key_prove);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        mSubmitButton = findViewById(R.id.button_submit);
        mWordEditTextFirst = findViewById(R.id.word_edittext_first);
        mWordEditTextSecond = findViewById(R.id.word_edittext_second);
        mWordTextViewFirst = findViewById(R.id.word_number_first);
        mWordTextViewSecond = findViewById(R.id.word_number_second);

        mCheckMark1 = findViewById(R.id.check_mark_1);
        mCheckMark2 = findViewById(R.id.check_mark_2);

        mWordEditTextFirst.addTextChangedListener(new BRTextWatcher());
        mWordEditTextSecond.addTextChangedListener(new BRTextWatcher());

        mMainConstraintLayout = findViewById(R.id.constraintLayout);
        mDefaultConstraintSet.clone(mMainConstraintLayout);
        mModifiedConstraintSet.clone(mMainConstraintLayout);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(mMainConstraintLayout);
                Resources resources = getResources();
                int verticalMarginSmall = (int) resources.getDimension(R.dimen.activity_vertical_margin_small);
                int verticalMargin = (int) resources.getDimension(R.dimen.activity_vertical_margin);
                mModifiedConstraintSet.setMargin(R.id.word_number_first, ConstraintSet.TOP, verticalMarginSmall);
                mModifiedConstraintSet.setMargin(R.id.line1, ConstraintSet.TOP, verticalMargin);
                mModifiedConstraintSet.setMargin(R.id.line2, ConstraintSet.TOP, verticalMargin);
                mModifiedConstraintSet.setMargin(R.id.word_number_second, ConstraintSet.TOP, verticalMarginSmall);
                mModifiedConstraintSet.applyTo(mMainConstraintLayout);
            }
        }, DateUtils.SECOND_IN_MILLIS / 2);

        mWordEditTextSecond.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_SEND || id == EditorInfo.IME_NULL) {
                    mSubmitButton.performClick();
                    return true;
                }
                return false;
            }
        });

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }
                if (isWordCorrect(true) && isWordCorrect(false)) {
                    Utils.hideKeyboard(PaperKeyProveActivity.this);
                    BRSharedPrefs.putPhraseWroteDown(PaperKeyProveActivity.this, true);
                    UiUtils.showBreadSignal(PaperKeyProveActivity.this, getString(R.string.Alerts_paperKeySet), getString(R.string.Alerts_paperKeySetSubheader), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                        @Override
                        public void onComplete() {
                            String extraDoneAction = getIntent().getExtras() == null
                                    ? null
                                    : getIntent().getStringExtra(PaperKeyProveActivity.EXTRA_DONE_ACTION);
                            PaperKeyActivity.DoneAction action = extraDoneAction == null
                                    ? null : PaperKeyActivity.DoneAction.valueOf(extraDoneAction);
                            if (action != null && action.equals(PaperKeyActivity.DoneAction.SHOW_BUY_SCREEN)) {
                                OnBoardingActivity.showBuyScreen(PaperKeyProveActivity.this);
                            } else {
                                UiUtils.startBreadActivity(PaperKeyProveActivity.this, false);
                                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                            }
                            finishAffinity();
                        }
                    });
                } else {
                    if (!isWordCorrect(true)) {
                        mWordEditTextFirst.setTextColor(getColor(R.color.red_text));
                        SpringAnimator.failShakeAnimation(PaperKeyProveActivity.this, mWordEditTextFirst);
                    }
                    if (!isWordCorrect(false)) {
                        mWordEditTextSecond.setTextColor(getColor(R.color.red_text));
                        SpringAnimator.failShakeAnimation(PaperKeyProveActivity.this, mWordEditTextSecond);
                    }
                }

            }
        });
        String cleanPhrase = getIntent().getExtras() == null ? null : getIntent().getStringExtra(EXTRA_PAPER_KEY);

        if (Utils.isNullOrEmpty(cleanPhrase)) {
            throw new RuntimeException(TAG + ": cleanPhrase is null");
        }

        String[] wordArray = cleanPhrase.split(" ");

        if (wordArray.length == WORD_COUNT && cleanPhrase.charAt(cleanPhrase.length() - 1) == NULL_TERMINATOR) {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title),
                    getString(R.string.Alert_keystore_generic_android), getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
            BRReportsManager.reportBug(new IllegalArgumentException("Paper Key error, please contact support at breadwallet.com"), false);
        } else {
            randomWordsSetUp(wordArray);
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void randomWordsSetUp(String[] words) {
        final Random random = new Random();
        int n = random.nextInt(RANDOM_MAX_DIGIT) + 1;

        mWordsSparseArray.append(n, words[n]);

        while (mWordsSparseArray.get(n) != null) {
            n = random.nextInt(RANDOM_MAX_DIGIT) + 1;
        }

        mWordsSparseArray.append(n, words[n]);

        mWordTextViewFirst.setText(String.format(Locale.getDefault(), getString(R.string.ConfirmPaperPhrase_word), (mWordsSparseArray.keyAt(0) + 1)));
        mWordTextViewSecond.setText(String.format(Locale.getDefault(), getString(R.string.ConfirmPaperPhrase_word), (mWordsSparseArray.keyAt(1) + 1)));

    }

    private boolean isWordCorrect(boolean first) {
        if (first) {
            String edit = Bip39Reader.cleanWord(mWordEditTextFirst.getText().toString());
            return SmartValidator.isWordValid(PaperKeyProveActivity.this, edit) && edit.equalsIgnoreCase(mWordsSparseArray.get(mWordsSparseArray.keyAt(0)));
        } else {
            String edit = Bip39Reader.cleanWord(mWordEditTextSecond.getText().toString());
            return SmartValidator.isWordValid(PaperKeyProveActivity.this, edit) && edit.equalsIgnoreCase(mWordsSparseArray.get(mWordsSparseArray.keyAt(1)));
        }
    }


    private void validateWord(EditText view) {
        String word = view.getText().toString();
        boolean valid = SmartValidator.isWordValid(this, word);
        view.setTextColor(getColor(valid ? R.color.light_gray : R.color.red_text));
        if (isWordCorrect(true)) {
            mCheckMark1.setVisibility(View.VISIBLE);
        } else {
            mCheckMark1.setVisibility(View.INVISIBLE);
        }

        if (isWordCorrect(false)) {
            mCheckMark2.setVisibility(View.VISIBLE);
        } else {
            mCheckMark2.setVisibility(View.INVISIBLE);
        }
    }

    private class BRTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            validateWord(mWordEditTextFirst);
            validateWord(mWordEditTextSecond);

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

}
