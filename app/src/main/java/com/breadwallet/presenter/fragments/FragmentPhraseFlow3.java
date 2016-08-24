
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.activities.PhraseFlowActivity;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.DecelerateOvershootInterpolator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 7/14/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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

    private byte[] phrase;
    private String[] phraseWords;
    private TextView textFlow;
    private String wordToCheck;
    private Button word1;
    private Button word2;
    private Button word3;
    private Button word4;
    private Button word5;
    private Button word6;
    private Button doneButton;
    private Button sixButtons[];
    private List<String> sixWords;
    private View.OnClickListener listener;
    private TextView stepsTextView;
    private TableLayout tableLayout;
    private int step = 1;
    private boolean pressAvailable;
    private static final int STEPS_LIMIT = 2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_phrase_flow3, container, false);

        textFlow = (TextView) rootView.findViewById(R.id.textFlow3);
        stepsTextView = (TextView) rootView.findViewById(R.id.step);
        word1 = (Button) rootView.findViewById(R.id.word1);
        word2 = (Button) rootView.findViewById(R.id.word2);
        word3 = (Button) rootView.findViewById(R.id.word3);
        word4 = (Button) rootView.findViewById(R.id.word4);
        word5 = (Button) rootView.findViewById(R.id.word5);
        word6 = (Button) rootView.findViewById(R.id.word6);
        doneButton = (Button) rootView.findViewById(R.id.done_button);
        tableLayout = (TableLayout) rootView.findViewById(R.id.words_layout);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishFlow();
            }
        });

        return rootView;
    }

    public void setPhrase(byte[] phrase) {
        this.phrase = phrase;
        pressAvailable = true;
        step = 1;
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
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            nextTry();
                        }
                    }, 1500);
                } else {
                    b.setTextColor(getActivity().getColor(R.color.red_text));
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            goBack();
                        }
                    }, 1500);
                }
                SpringAnimator.showAnimation(b);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.setTextColor(getActivity().getColor(R.color.dark_blue));
                    }
                }, 1000);

            }
        };

        sixButtons[0] = word1;
        sixButtons[1] = word2;
        sixButtons[2] = word3;
        sixButtons[3] = word4;
        sixButtons[4] = word5;
        sixButtons[5] = word6;
        String cleanPhrase = new String(phrase);
        if (cleanPhrase.split(" ").length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            ((BreadWalletApp) getActivity().getApplication()).showCustomDialog(getString(R.string.warning),
                    getActivity().getString(R.string.phrase_error), getString(R.string.ok));
        }
        phraseWords = cleanPhrase.split(" ");
        startOver();
    }

    private void startOver() {
        wordToCheck = null;
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
        while (phraseWords[n].equalsIgnoreCase(wordToCheck)) {
            n = random.nextInt(10) + 1;
        }
        wordToCheck = phraseWords[n];
        String placeHolder;
        switch (n) {
            case 1:
                placeHolder = "second";
                break;
            case 2:
                placeHolder = "third";
                break;
            case 0:
                throw new IllegalArgumentException("Cannot be 0");
            case 11:
                throw new IllegalArgumentException("Cannot be 0");
            default:
                placeHolder = (n + 1) + "th";
                break;

        }
        textFlow.setText(String.format(getString(R.string.phrase_flow3), placeHolder));
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
            textFlow.setText(R.string.thanks_for_writing_phrase);
            tableLayout.setVisibility(View.GONE);
            stepsTextView.setVisibility(View.GONE);
            doneButton.setVisibility(View.VISIBLE);
            SharedPreferencesManager.putPhraseWroteDown(getActivity(), true);
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
            app.fragmentPhraseFlow2.setPhrase(phrase);
            app.animateSlide(app.fragmentPhraseFlow3, app.fragmentPhraseFlow2, IntroActivity.LEFT);
        }
    }

    private void updateStepsText(int steps) {
        stepsTextView.setText(String.format(Locale.getDefault(), "Steps %d/%d", steps, STEPS_LIMIT));
    }

    private void finishFlow() {
        PhraseFlowActivity app = (PhraseFlowActivity) getActivity();
        Intent intent;
        intent = new Intent(app, MainActivity.class);
        startActivity(intent);
        if (!app.isDestroyed()) {
            app.finish();
        }

    }

    public byte[] getPhrase() {
        return phrase;
    }

}
