package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.util.Random;


public class IntroPhraseProveActivity extends Activity {
    private static final String TAG = IntroPhraseProveActivity.class.getName();
    private Button submit;
    private EditText wordEditFirst;
    private EditText wordEditSecond;
    private TextView wordTextFirst;
    private TextView wordTextSecond;
    private SparseArray<String> sparseArrayWords = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phrase_prove);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setStatusBarColor(android.R.color.transparent);
        submit = (Button) findViewById(R.id.button_submit);
        wordEditFirst = (EditText) findViewById(R.id.word_edittext_first);
        wordEditSecond = (EditText) findViewById(R.id.word_edittext_second);
        wordTextFirst = (TextView) findViewById(R.id.word_number_first);
        wordTextSecond = (TextView) findViewById(R.id.word_number_second);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String edit1 = wordEditFirst.getText().toString().replaceAll("[^a-zA-Z]", "");
                String edit2 = wordEditSecond.getText().toString().replaceAll("[^a-zA-Z]", "");
                if (edit1.isEmpty() || edit2.isEmpty()) {
                    if (edit1.isEmpty()) {
                        SpringAnimator.failShakeAnimation(IntroPhraseProveActivity.this, wordTextFirst);
                    }
                    if (edit2.isEmpty()) {
                        SpringAnimator.failShakeAnimation(IntroPhraseProveActivity.this, wordTextSecond);
                    }
                } else {
                    if (edit1.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(0))) && edit2.equalsIgnoreCase(sparseArrayWords.get(sparseArrayWords.keyAt(1)))) {
                        Log.e(TAG, "onClick: Success!");
                        BRAnimator.showBreadDialog(IntroPhraseProveActivity.this, "Paper Key Set", "Awesome!", R.drawable.ic_check_mark_white);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BRWalletManager.getInstance().startBreadActivity(IntroPhraseProveActivity.this);
                                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                            }
                        }, 1000);
                    } else {
                        Log.e(TAG, "onClick: FAIL");
                    }
                }
            }
        });
        String cleanPhrase = null;

        try {
            cleanPhrase = new String(KeyStoreManager.getKeyStorePhrase(this, 0));
            //todo DELETE THIS LOG
            Log.e(TAG, "onCreate: success: " + cleanPhrase);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }
        if (Utils.isNullOrEmpty(cleanPhrase)) {
            throw new RuntimeException(TAG + ": cleanPhrase is null");
        }

        String wordArray[] = cleanPhrase.split(" ");

        if (wordArray.length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            BreadDialog.showCustomDialog(this, getString(R.string.warning),
                    getString(R.string.phrase_error), getString(R.string.ok), null, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }, null, null, 0);
            FirebaseCrash.report(new IllegalArgumentException(getString(R.string.phrase_error)));

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
        int n = random.nextInt(10) + 1;

        sparseArrayWords.append(n, words[n]);

        while (sparseArrayWords.get(n) != null) {
            n = random.nextInt(10) + 1;
        }

        sparseArrayWords.append(n, words[n]);

        wordTextFirst.setText("Word " + (sparseArrayWords.keyAt(0) + 1));
        wordTextSecond.setText("Word " + (sparseArrayWords.keyAt(1) + 1));
    }

    private void setStatusBarColor(int color) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(color));
    }

}
