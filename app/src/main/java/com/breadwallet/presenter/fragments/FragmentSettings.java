
package com.breadwallet.presenter.fragments;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.FragmentAnimator;

/**
 * BreadWallet
 *
 * Created by Mihail on 6/29/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentSettings extends Fragment {
    public static final String TAG = "FragmentSettings";
    private RelativeLayout about;
    private RelativeLayout localCurrency;
    private RelativeLayout recoveryPhrase;
    private RelativeLayout startRecoveryWallet;
    private MainActivity app;
    private FragmentSettings fragmentSettings;
    private TextView currencyName;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragment_settings, container, false);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        app = MainActivity.app;
        fragmentSettings = this;
        about = (RelativeLayout) getView().findViewById(R.id.about);
        currencyName = (TextView) getView().findViewById(R.id.three_letters_currency);
        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        final String tmp = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
        currencyName.setText(tmp);
        localCurrency = (RelativeLayout) getView().findViewById(R.id.local_currency);
        recoveryPhrase = (RelativeLayout) getView().findViewById(R.id.recovery_phrase);
        startRecoveryWallet = (RelativeLayout) getView().findViewById(R.id.start_recovery_wallet);
        startRecoveryWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAnimator.pressWipeWallet(app, app.fragmentWipeWallet);
                app.activityButtonsEnable(false);
            }
        });
        recoveryPhrase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getResources().getString(R.string.warning))
                        .setMessage(getResources().getString(R.string.dialog_do_not_let_anyone))
                        .setPositiveButton(getResources().getString(R.string.show), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                FragmentAnimator.animateSlideToLeft(app, app.fragmentRecoveryPhrase, fragmentSettings);
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Canceled the view of the phrase!");
                            }
                        })
                        .show();

            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAnimator.animateSlideToLeft(app, app.fragmentAbout, fragmentSettings);
            }
        });
        localCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAnimator.animateSlideToLeft(app, app.fragmentCurrency, fragmentSettings);
            }
        });
    }

}
