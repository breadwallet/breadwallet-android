
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.PhraseFlowActivity;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentPhraseFlow2 extends Fragment {
    private TextView thePhrase;
    private Button continueButton;
    private Button backButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_phrase_flow2, container, false);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        thePhrase = (TextView) rootView.findViewById(R.id.the_phrase);
        continueButton = (Button) rootView.findViewById(R.id.continue_button);
        TextView step1 = (TextView) rootView.findViewById(R.id.step1);
        step1.setText(String.format(getString(R.string.step_holder), 1, 3));
        backButton = (Button) rootView.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });

        return rootView;
    }

    public void setPhrase(final byte[] phrase) {
        if(phrase.length == 0) {
            throw new RuntimeException("phrase is empty what??");
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String cleanPhrase = new String(phrase);
                if (cleanPhrase.split(" ").length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
                    ((BreadWalletApp) getActivity().getApplication()).showCustomDialog(getString(R.string.warning),
                            getActivity().getString(R.string.phrase_error), getString(R.string.ok));
                }
                thePhrase.setText(cleanPhrase);
                if (cleanPhrase.charAt(0) > 0x3000)
                    thePhrase.setText(cleanPhrase.replace(" ", "\u3000"));

                continueButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PhraseFlowActivity app = ((PhraseFlowActivity) getActivity());
                        if(app == null) return;
                        app.animateSlide(app.fragmentPhraseFlow2, app.fragmentPhraseFlow3, IntroActivity.RIGHT);
                        app.fragmentPhraseFlow3.setPhrase(phrase);
                    }
                });
            }
        }, 50);

    }


}
