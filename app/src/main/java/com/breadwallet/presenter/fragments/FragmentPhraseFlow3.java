
package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.activities.PhraseFlowActivity;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */


public class FragmentPhraseFlow3 extends Fragment {

    public static final String TAG = FragmentPhraseFlow3.class.getName();

    private byte[] phrase;
    private String[] phraseWords;
    private TextView textFlow;
    private String wordToCheck = "";
    private Button word1;
    private Button word2;
    private Button word3;
    private Button word4;
    private Button word5;
    private Button word6;
    private Button sixButtons[];
    private List<String> sixWords;
    private View.OnClickListener listener;
    private TextView stepsTextView;
    private TableLayout tableLayout;
    private int step = 1;
    private boolean pressAvailable;
    private static final int STEPS_LIMIT = 3;
    private Button backButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_phrase_flow3, container, false);
        Button skipButton = (Button) rootView.findViewById(R.id.skip_button);
        textFlow = (TextView) rootView.findViewById(R.id.textFlow3);
        stepsTextView = (TextView) rootView.findViewById(R.id.step);
        word1 = (Button) rootView.findViewById(R.id.word1);
        word2 = (Button) rootView.findViewById(R.id.word2);
        word3 = (Button) rootView.findViewById(R.id.word3);
        word4 = (Button) rootView.findViewById(R.id.word4);
        word5 = (Button) rootView.findViewById(R.id.word5);
        word6 = (Button) rootView.findViewById(R.id.word6);
        tableLayout = (TableLayout) rootView.findViewById(R.id.words_layout);
        backButton = (Button) rootView.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        if (CurrencyManager.getInstance(getActivity()).getBALANCE() > SharedPreferencesManager.getLimit(getActivity())) {
            skipButton.setVisibility(View.GONE);
        }
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                Activity app = getActivity();
                intent = new Intent(app, MainActivity.class);
                startActivity(intent);
                if (!app.isDestroyed()) {
                    app.finish();
                }
            }
        });

        return rootView;
    }

    public void setPhrase(byte[] phrase) {
        this.phrase = phrase;
        pressAvailable = true;
        step = 2;
        updateStepsText(step);
        sixWords = new ArrayList<>();
        sixButtons = new Button[6];
        listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!pressAvailable) return;
                pressAvailable = false;
                final Button b = ((Button) view);
                if (b.getText().toString().equalsIgnoreCase(wordToCheck)) {
                    b.setTextColor(getActivity().getColor(R.color.green_text));
                    SpringAnimator.showAnimation(b);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            nextTry();
                        }
                    }, 800);
                } else {
                    b.setTextColor(getActivity().getColor(R.color.red_text));
                    SpringAnimator.failShakeAnimation(getActivity(), b);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            goBack();
                        }
                    }, 800);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.setTextColor(getActivity().getColor(R.color.dark_blue));
                    }
                }, 500);

            }
        };

        sixButtons[0] = word1;
        sixButtons[1] = word2;
        sixButtons[2] = word3;
        sixButtons[3] = word4;
        sixButtons[4] = word5;
        sixButtons[5] = word6;
        String cleanPhrase = phrase == null? "" : new String(phrase);
        if (cleanPhrase.split(" ").length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            ((BreadWalletApp) getActivity().getApplication()).showCustomDialog(getString(R.string.warning),
                    getActivity().getString(R.string.phrase_error), getString(R.string.ok));
        }
        phraseWords = cleanPhrase.split(" ");
        startOver();
    }

    private void startOver() {
        sixWords.clear();
        setTextWithRandomNr();
        fillWordsArray();
        setButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BreadWalletApp) getActivity().getApplication()).hideKeyboard(getActivity());
    }


    private void setTextWithRandomNr() {
        if (textFlow == null) return;
        final Random random = new Random();
        int n = random.nextInt(10) + 1;
        String oldWord = wordToCheck;
        wordToCheck = phraseWords[n];
        while (oldWord.equalsIgnoreCase(wordToCheck)) {
            n = random.nextInt(10) + 1;
            wordToCheck = phraseWords[n];
        }
        String placeHolder;
        switch (n) {
            case 0:
                throw new IllegalArgumentException("Cannot be 0");
            case 11:
                throw new IllegalArgumentException("Cannot be 11");
            case 1:
                textFlow.setText(getText(R.string.word_nr2));
                break;
            case 2:
                textFlow.setText(getText(R.string.word_nr3));
                break;
            case 3:
                textFlow.setText(getText(R.string.word_nr4));
                break;
            case 4:
                textFlow.setText(getText(R.string.word_nr5));
                break;
            case 5:
                textFlow.setText(getText(R.string.word_nr6));
                break;
            case 6:
                textFlow.setText(getText(R.string.word_nr7));
                break;
            case 7:
                textFlow.setText(getText(R.string.word_nr8));
                break;
            case 8:
                textFlow.setText(getText(R.string.word_nr9));
                break;
            case 9:
                textFlow.setText(getText(R.string.word_nr10));
                break;
            case 10:
                textFlow.setText(getText(R.string.word_nr11));
                break;
            default:
                throw new IllegalArgumentException("cannot be other");

        }

    }

    private void fillWordsArray() {
        final Random random = new Random();
        sixWords.add(wordToCheck);
        for (int i = 1; i < 6; i++) {
            int n = random.nextInt(12);
            String randWord = phraseWords[n];
            while (sixWords.contains(randWord)) {
                int w = random.nextInt(12);
                randWord = phraseWords[w];
            }
            sixWords.add(randWord);
        }
        Collections.shuffle(sixWords);

    }

    private void setButtons() {
        for (int i = 0; i < 6; i++) {
            String word = sixWords.get(i);
            sixButtons[i].setText(word);
            sixButtons[i].setOnClickListener(listener);
        }
    }

    private void nextTry() {
        if (step == STEPS_LIMIT) {
            SharedPreferencesManager.putPhraseWroteDown(getActivity(), true);
            finishFlow();
            return;
        }
        stepsTextView.setVisibility(View.GONE);
        updateStepsText(++step);
        for (int i = 0; i < 6; i++) {
            sixButtons[i].setVisibility(View.GONE);
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                startOver();
                for (int i = 0; i < 6; i++) {
                    sixButtons[i].setVisibility(View.VISIBLE);
                }
                for (int i = 0; i < 6; i++) {
                    SpringAnimator.showAnimation(sixButtons[i]);
                }
                stepsTextView.setVisibility(View.VISIBLE);
                SpringAnimator.showAnimation(stepsTextView);
                SpringAnimator.showAnimation(textFlow);
                pressAvailable = true;
            }
        });

    }

    private void goBack() {
        if (this.isVisible()) {
            PhraseFlowActivity app = (PhraseFlowActivity) getActivity();
            app.animateSlide(app.fragmentPhraseFlow3, app.fragmentPhraseFlow2, IntroActivity.LEFT);
            app.fragmentPhraseFlow2.setPhrase(phrase);
        }
    }

    private void updateStepsText(int steps) {
        stepsTextView.setText(String.format(getString(R.string.step_holder), steps, STEPS_LIMIT));
    }

    private void finishFlow() {
        Toast toast = new Toast(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast,
                (ViewGroup) getActivity().findViewById(R.id.toast_layout_root));
        layout.setBackgroundResource(R.drawable.toast_layout_black);
        TextView text = (TextView) layout.findViewById(R.id.toast_text);
        text.setText(R.string.recovery_phrase_set);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        text.setPadding(20, 40, 20, 40);
        toast.setGravity(Gravity.BOTTOM, 0, PhraseFlowActivity.screenParametersPoint.y / 2);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                PhraseFlowActivity app = (PhraseFlowActivity) getActivity();
                Intent intent;
                intent = new Intent(app, MainActivity.class);
                startActivity(intent);
                if (!app.isDestroyed()) {
                    app.finish();
                }
            }
        }, 1000);

    }

    public void releasePhrase() {
        if (phrase != null)
            Arrays.fill(phrase, (byte) 0);
    }

    public byte[] getPhrase() {
        return phrase;
    }

}
