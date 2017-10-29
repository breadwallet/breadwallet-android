package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.TransitionManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.util.Bip39Reader;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class PaperKeyProveActivity extends BRActivity {
    private static final String TAG = PaperKeyProveActivity.class.getName();
    private Button submit;
    private EditText wordEditFirst;
    private EditText wordEditSecond;
    private TextView wordTextFirst;
    private TextView wordTextSecond;
    private SparseArray<String> sparseArrayWords = new SparseArray<>();
    public static boolean appVisible = false;
    private static PaperKeyProveActivity app;
    private ConstraintLayout constraintLayout;
    private ConstraintSet applyConstraintSet = new ConstraintSet();
    private ConstraintSet resetConstraintSet = new ConstraintSet();

    public static PaperKeyProveActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paper_key_prove);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        submit = (Button) findViewById(R.id.button_submit);
        wordEditFirst = (EditText) findViewById(R.id.word_edittext_first);
        wordEditSecond = (EditText) findViewById(R.id.word_edittext_second);
        wordTextFirst = (TextView) findViewById(R.id.word_number_first);
        wordTextSecond = (TextView) findViewById(R.id.word_number_second);

        constraintLayout = (ConstraintLayout) findViewById(R.id.constraintLayout);
        resetConstraintSet.clone(constraintLayout);
        applyConstraintSet.clone(constraintLayout);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                TransitionManager.beginDelayedTransition(constraintLayout);
                applyConstraintSet.setMargin(R.id.word_number_first, ConstraintSet.TOP, 8);
                applyConstraintSet.setMargin(R.id.line1, ConstraintSet.TOP, 16);
                applyConstraintSet.setMargin(R.id.line2, ConstraintSet.TOP, 16);
                applyConstraintSet.setMargin(R.id.word_number_second, ConstraintSet.TOP, 8);
                applyConstraintSet.applyTo(constraintLayout);


            }
        }, 500);


        wordEditSecond.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.submit || id == EditorInfo.IME_NULL) {
                    submit.performClick();
                    return true;
                }
                return false;
            }
        });


        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                String edit1 = wordEditFirst.getText().toString().replaceAll("[^a-zA-Z]", "");
                String edit2 = wordEditSecond.getText().toString().replaceAll("[^a-zA-Z]", "");

                if (edit1.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(0))) && edit2.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(1)))) {
                    Utils.hideKeyboard(PaperKeyProveActivity.this);
                    BRSharedPrefs.putPhraseWroteDown(PaperKeyProveActivity.this, true);
                    BRAnimator.showBreadSignal(PaperKeyProveActivity.this, getString(R.string.Alerts_paperKeySet), getString(R.string.Alerts_paperKeySetSubheader), R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                        @Override
                        public void onComplete() {
                            BRAnimator.startBreadActivity(PaperKeyProveActivity.this, false);
                            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                            finishAffinity();
                        }
                    });
                } else {
                    String languageCode = getString(R.string.lang);
                    List<String> list;
                    try {
                        list = Bip39Reader.getWordList(PaperKeyProveActivity.this, languageCode);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new NullPointerException("No word list");
                    }
                    if (!list.contains(edit1) || !edit1.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(0)))) {
                        SpringAnimator.failShakeAnimation(PaperKeyProveActivity.this, wordTextFirst);
                    }

                    if (!list.contains(edit2) || !edit2.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(1)))) {
                        SpringAnimator.failShakeAnimation(PaperKeyProveActivity.this, wordTextSecond);
                    }
                }

            }
        });
        String cleanPhrase = null;

        cleanPhrase = getIntent().getExtras() == null ? null : getIntent().getStringExtra("phrase");

        if (Utils.isNullOrEmpty(cleanPhrase)) {
            throw new RuntimeException(TAG + ": cleanPhrase is null");
        }

        String wordArray[] = cleanPhrase.split(" ");

        if (wordArray.length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title),
                    "Paper Key error, please contact support at @LTCFoundation", getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
            FirebaseCrash.report(new IllegalArgumentException("Paper Key error, please contact support at @LTCFoundation"));

        } else {
            randomWordsSetUp(wordArray);

        }

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

    private void randomWordsSetUp(String[] words) {
        final Random random = new Random();
        int n = random.nextInt(10) + 1;

        sparseArrayWords.append(n, words[n]);

        while (sparseArrayWords.get(n) != null) {
            n = random.nextInt(10) + 1;
        }

        sparseArrayWords.append(n, words[n]);

        wordTextFirst.setText(String.format(Locale.getDefault(), getString(R.string.ConfirmPaperPhrase_word), (sparseArrayWords.keyAt(0) + 1)));
        wordTextSecond.setText(String.format(Locale.getDefault(), getString(R.string.ConfirmPaperPhrase_word), (sparseArrayWords.keyAt(1) + 1)));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }
}
