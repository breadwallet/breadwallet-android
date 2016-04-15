
package com.breadwallet.presenter.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.TypesConverter;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.sqlite.SQLiteManager;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import java.nio.CharBuffer;
import java.text.Normalizer;
import java.util.Arrays;

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

public class FragmentWipeWallet extends Fragment {
    private static final String TAG = FragmentWipeWallet.class.getName();
    private Button close;
    private EditText recoveryPhraseEditText;
    private Button wipe;
    private BRWalletManager m;
    private boolean allowWipeButtonPress = true;

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
        recoveryPhraseEditText.setSingleLine();
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
                if (!allowWipeButtonPress) return;
                allowWipeButtonPress = false;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        allowWipeButtonPress = true;
                    }
                }, 500);
                if (phraseIsValid(recoveryPhraseEditText.getText().toString().trim().toLowerCase())) {
                    m.wipeWallet(getActivity());
                    startIntroActivity();
                    FragmentAnimator.resetFragmentAnimator();
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
        String normalizedPhrase = Normalizer.normalize(insertedPhrase.trim(), Normalizer.Form.NFKD);
        if (!BRWalletManager.getInstance(getActivity()).validatePhrase(getActivity(), normalizedPhrase))
            return false;
        String nullTerminatedPhrase = normalizedPhrase + '\0';
        byte[] pubKey = m.getMasterPubKey(nullTerminatedPhrase);
        byte[] pubKeyFromKeyStore = KeyStoreManager.getMasterPublicKey(getActivity());
        return Arrays.equals(pubKey, pubKeyFromKeyStore);
    }

    @Override
    public void onPause() {
        super.onPause();
        ((BreadWalletApp) getActivity().getApplication()).hideKeyboard(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        allowWipeButtonPress = true;
        MainActivity app = MainActivity.app;
        if (app != null)
            app.activityButtonsEnable(false);
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
