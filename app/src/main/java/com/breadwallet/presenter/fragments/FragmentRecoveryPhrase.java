
package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.security.KeyStoreManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/22/15.
 * Copyright (c) 2016 Mihail Gutan <mihail@breadwallet.com>
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
    private ImageView checkBox;
    private RelativeLayout checkBoxlayout;
    private boolean checked = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_recovery_phrase, container, false);
        thePhrase = (TextView) rootView.findViewById(R.id.the_phrase);
        checkBox = (ImageView) rootView.findViewById(R.id.write_down_check_box);
        checkBoxlayout = (RelativeLayout) rootView.findViewById(R.id.write_down_notice_layout);

        boolean phraseWroteDown = SharedPreferencesManager.getPhraseWroteDown(getActivity());

        //TODO delete this code below which is for testing reasons only
//        thePhrase.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                BRClipboardManager.copyToClipboard(getActivity(), thePhrase.getText().toString());
//                ((BreadWalletApp) getActivity().getApplication()).showCustomToast(getActivity(),
//                        getString(R.string.copied), 300, Toast.LENGTH_SHORT, 0);
//            }
//        });

        if (!phraseWroteDown) {
            checkBoxlayout.setVisibility(View.VISIBLE);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setCheckBoxImage();

                }
            });
        }

        String phrase = KeyStoreManager.getKeyStorePhrase(getActivity(), BRConstants.SHOW_PHRASE_REQUEST_CODE);
        if (phrase == null || phrase.isEmpty()) return rootView;
        if (phrase.charAt(phrase.length() - 1) == '\0') {
            ((BreadWalletApp) getActivity().getApplication()).showCustomDialog(getString(R.string.warning),
                    getActivity().getString(R.string.phrase_error), getString(R.string.close));
        }

        thePhrase.setText(phrase);
        if(phrase.charAt(0) > 0x3000)
            thePhrase.setText(phrase.replace(" ", "\u3000"));
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

    private void setCheckBoxImage() {
        checkBox.setImageResource(!checked ? R.drawable.checkbox_checked : R.drawable.checkbox_empty);
        checked = !checked;
        SharedPreferencesManager.putCheckBoxRecoveryPhraseFragment(getActivity(), checked);
    }
}
