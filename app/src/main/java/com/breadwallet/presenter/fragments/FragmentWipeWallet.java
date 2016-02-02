
package com.breadwallet.presenter.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/14/15.
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

public class FragmentWipeWallet extends Fragment {
    private static final String TAG = FragmentWipeWallet.class.getName();
    private Button close;
    private EditText recoveryPhraseEditText;
    private Button wipe;
    private BRWalletManager m;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragment_wipe_wallet, container, false);
        m = BRWalletManager.getInstance(getActivity());
        close = (Button) rootView.findViewById(R.id.wipe_wallet_close);
        recoveryPhraseEditText = (EditText) rootView.findViewById(R.id.editText_phrase);
        wipe = (Button) rootView.findViewById(R.id.wipe_wallet_wipe);
        recoveryPhraseEditText.setText("");
        recoveryPhraseEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {
                    wipe.performClick();
                }
                return false;
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        wipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (phraseIsValid(recoveryPhraseEditText.getText().toString())) {
                    m.sweepPrivateKey();
                    startIntroActivity();
                    getActivity().finish();
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.attention))
                            .setMessage(getString(R.string.wipewallet_not_valid_phrase))
                            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            }
        });

        return rootView;
    }

    private boolean phraseIsValid(String insertedPhrase) {
        String thePhrase = KeyStoreManager.getKeyStoreString(getActivity());
        if (thePhrase == null) throw new NullPointerException("Phrase is null! weird behaviour");
//        Log.e(TAG,"Inserted:" +  insertedPhrase);
//        Log.e(TAG,"Actual:" +  thePhrase);
        return insertedPhrase.equalsIgnoreCase(thePhrase);
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity.app.softKeyboard.closeSoftKeyboard();
    }

    private void startIntroActivity() {
        Intent intent;
        intent = new Intent(getActivity(), IntroActivity.class);
        startActivity(intent);
        if (!getActivity().isDestroyed()) {
            getActivity().finish();
        }
    }
}
