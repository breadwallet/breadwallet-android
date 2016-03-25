package com.breadwallet.presenter.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.tools.WordsReader;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRWalletManager;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;

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
    public static final String TAG = IntroRecoverWalletFragment.class.getName();
    private Button recoverButton;
    private EditText editText;
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
                String[] words = new String[0];
                List<String> list;
                try {
                    list = WordsReader.getWordList(getActivity());
                    words = list.toArray(new String[list.size()]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String phraseToCheck = editText.getText().toString();
                if (words.length > 1000 && validateRecoveryPhrase(words, phraseToCheck)) {
                    String normalizedPhrase = Normalizer.normalize(phraseToCheck, Normalizer.Form.NFKD);
                    boolean success = KeyStoreManager.setKeyStoreString(normalizedPhrase, getActivity());
                    if(!success) throw new NullPointerException("Something went wrong when set the phrase into the KeyStore");
                    BRWalletManager m;
                    m = BRWalletManager.getInstance(getActivity());
//                    KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis() / 1000), getActivity());

                    String pubKey = m.getMasterPubKey(normalizedPhrase.toLowerCase());
                    KeyStoreManager.putMasterPublicKey(pubKey, getActivity());
                    Log.w(TAG, "The phrase from keystore is: " + KeyStoreManager.getKeyStoreString(getActivity()));
                    ((IntroActivity) getActivity()).startMainActivity();
                    getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                } else {
                    alertDialog.setTitle(getResources().getString(R.string.alert));
                    alertDialog.setMessage("\"" + phraseToCheck + "\" - " +
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

    private native boolean validateRecoveryPhrase(String[] words, String phrase);

    @Override
    public void onResume() {
        super.onResume();
    }
}
