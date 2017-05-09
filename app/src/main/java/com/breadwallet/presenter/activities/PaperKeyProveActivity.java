package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.util.WordsReader;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static java.security.AccessController.getContext;


public class PaperKeyProveActivity extends Activity {
    private static final String TAG = PaperKeyProveActivity.class.getName();
    private Button submit;
    private EditText wordEditFirst;
    private EditText wordEditSecond;
    private TextView wordTextFirst;
    private TextView wordTextSecond;
    private SparseArray<String> sparseArrayWords = new SparseArray<>();
    public static boolean appVisible = false;
    private static PaperKeyProveActivity app;

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

        final InputMethodManager keyboard = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        wordEditFirst.requestFocus();
        keyboard.showSoftInput(wordEditFirst, 0);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                String edit1 = wordEditFirst.getText().toString().replaceAll("[^a-zA-Z]", "");
                String edit2 = wordEditSecond.getText().toString().replaceAll("[^a-zA-Z]", "");

                if (edit1.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(0))) && edit2.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(1)))) {
                    Log.e(TAG, "onClick: Success!");
                    SharedPreferencesManager.putPhraseWroteDown(PaperKeyProveActivity.this, true);
                    BRAnimator.showBreadSignal(PaperKeyProveActivity.this, "Paper Key Set", "Awesome!", R.drawable.ic_check_mark_white, new BROnSignalCompletion() {
                        @Override
                        public void onComplete() {
                            BRAnimator.startBreadActivity(PaperKeyProveActivity.this, false);
                            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                        }
                    });
                } else {
                    String languageCode = getString(R.string.lang_Android);
                    List<String> list;
                    try {
                        list = WordsReader.getWordList(PaperKeyProveActivity.this, languageCode);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new NullPointerException("No word list");
                    }
                    Log.e(TAG, "onClick: FAIL");
                    if (!list.contains(edit1) || edit1.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(0)))) {
                        SpringAnimator.failShakeAnimation(PaperKeyProveActivity.this, wordTextFirst);
                    }

                    if (!list.contains(edit2) || edit2.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(1)))) {
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
        Log.e(TAG, "onCreate: " + cleanPhrase);

        String wordArray[] = cleanPhrase.split(" ");

        if (wordArray.length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            BreadDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title),
                    getString(R.string.RecoveryPhrase_paperKeyError_Android), getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
            FirebaseCrash.report(new IllegalArgumentException(getString(R.string.RecoveryPhrase_paperKeyError_Android)));

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

        wordTextFirst.setText("Word " + (sparseArrayWords.keyAt(0) + 1));
        wordTextSecond.setText("Word " + (sparseArrayWords.keyAt(1) + 1));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }
}
