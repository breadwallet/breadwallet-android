package com.breadwallet.presenter.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/15/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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
public class IntroRecoverWalletFragment extends Fragment {
    public Button recoverButton;
    public EditText editText;
    private AlertDialog alertDialog;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.intro_fragment_recover_wallet, container, false);
        recoverButton = (Button) rootView.findViewById(R.id.recover_button);
        editText = (EditText) rootView.findViewById(R.id.recover_wallet_edit_text);
        editText.setText("");
        alertDialog = new AlertDialog.Builder(getActivity()).create();
        recoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
                if (validateRecoveryPhrase(editText.getText().toString())) {
                    ((IntroActivity) getActivity()).startMainActivity();
                    getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                } else {

                    alertDialog.setTitle(getResources().getString(R.string.alert));
                    alertDialog.setMessage("\"" + editText.getText().toString() + "\" - " +
                            getResources().getString(R.string.dialog_recovery_phrase_invalid));
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            }
        });
        return rootView;
    }

    boolean validateRecoveryPhrase(String phrase) {
        //mock test
        int count = 0;
        int length = phrase.length();
        for (int i = 0; i < length; i++) {
            if (phrase.charAt(i) == ' ') count++;
        }
        return count == 12 || count == 11 || count == 10;

    }

}
