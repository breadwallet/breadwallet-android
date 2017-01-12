
package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipboardManager;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.util.Arrays;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
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

public class FragmentRecoveryPhrase extends Fragment {
    public static final String TAG = FragmentRecoveryPhrase.class.getName();
    private TextView thePhrase;
    private byte[] phrase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_recovery_phrase, container, false);
        thePhrase = (TextView) rootView.findViewById(R.id.the_phrase);
        Button backButton = (Button) rootView.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().onBackPressed();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        Activity app = getActivity();
        if (app == null) app = MainActivity.app;
        if (app != null)
            ((BreadWalletApp) app.getApplication()).hideKeyboard(app);
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    public void setPhrase(byte[] phrase) {
        if (phrase == null) return;
        this.phrase = phrase;

        if (Utils.isEmulatorOrDebug(getActivity())) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        BRClipboardManager.copyToClipboard(getActivity(), thePhrase.getText().toString());
                        ((BreadWalletApp) getActivity().getApplication()).showCustomToast(getActivity(),
                                getActivity().getString(R.string.copied), 300, Toast.LENGTH_SHORT, 0);
                    }
                }
            }, 2000);
        }

        if (phrase.length == 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    getActivity().onBackPressed();
                }
            }, 10);
            return;
        }
        String cleanPhrase = new String(phrase);
        Arrays.fill(phrase, (byte) 0);
        if (cleanPhrase.split(" ").length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            ((BreadWalletApp) getActivity().getApplication()).showCustomDialog(getString(R.string.warning),
                    getActivity().getString(R.string.phrase_error), getString(R.string.ok));
            FirebaseCrash.report(new IllegalArgumentException(getActivity().getString(R.string.phrase_error)));
        }

        thePhrase.setText(cleanPhrase);
        if (cleanPhrase.charAt(0) > 0x3000)
            thePhrase.setText(cleanPhrase.replace(" ", "\u3000"));
    }

    public void releasePhrase() {
        if (phrase != null)
            Arrays.fill(phrase, (byte) 0);
    }
}
