
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.activities.PhraseFlowActivity;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRStringFormatter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;


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

public class FragmentPhraseFlow1 extends Fragment {
    FragmentPhraseFlow1 fragmentPhraseFlow1;
    public static byte[] phrase;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_phrase_flow1, container, false);
        Button nextButton = (Button) rootView.findViewById(R.id.next_button);
        fragmentPhraseFlow1 = this;
        if (KeyStoreManager.getPassCode(getActivity()).isEmpty()) {
            PhraseFlowActivity app = ((PhraseFlowActivity) getActivity());
            app.showHideFragments(app.fragmentPhraseFlow2);
            app.fragmentPhraseFlow2.setPhrase(phrase);
        }

        int limit = SharedPreferencesManager.getLimit(getActivity());

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhraseFlowActivity app = ((PhraseFlowActivity) getActivity());
                if (app == null) return;

                if (BRAnimator.checkTheMultipressingAvailability()) {
                    ((BreadWalletApp) app.getApplicationContext()).promptForAuthentication(app, BRConstants.AUTH_FOR_PHRASE, null, null, null, null,false);
                }
            }
        });

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        if (CurrencyManager.getInstance(getActivity()).getBALANCE() > limit
                && !SharedPreferencesManager.getPhraseWroteDown(getActivity())) {
            TextView title = (TextView) rootView.findViewById(R.id.warning_flow);
            TextView textFlow1 = (TextView) rootView.findViewById(R.id.textFlow1);
            TextView textFlow2 = (TextView) rootView.findViewById(R.id.textFlow2);
            if (title == null || textFlow1 == null || textFlow2 == null) return rootView;
            title.setText(R.string.write_down_your_recovery_phrase);
            String iso = SharedPreferencesManager.getIso(getActivity());
            double rate = SharedPreferencesManager.getRate(getActivity());
            String limitText = String.format("%s(%s)", BRStringFormatter.getFormattedCurrencyString("BTC", limit),
                    BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(limit), getActivity()));
            textFlow1.setText(String.format(Locale.getDefault(), "your account balance is above %s\n\n", limitText));
            textFlow1.setTypeface(null, Typeface.BOLD);
            textFlow2.setText(R.string.protect_your_wallet);
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void setPhrase(final byte[] thePhrase) {
        phrase = thePhrase;

    }

    public void releasePhrase() {
        if (phrase != null)
            Arrays.fill(phrase, (byte) 0);
    }
}
