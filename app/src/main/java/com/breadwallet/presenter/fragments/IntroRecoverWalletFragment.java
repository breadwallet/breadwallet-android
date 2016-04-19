package com.breadwallet.presenter.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
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
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRWalletManager;

import java.text.Normalizer;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 9/15/15.
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
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {
                    recoverButton.performClick();
                }
                return false;
            }
        });
        alertDialog = new AlertDialog.Builder(getActivity()).create();
        recoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }

                String phraseToCheck = editText.getText().toString().trim().toLowerCase();
                String normalizedPhrase = Normalizer.normalize(phraseToCheck, Normalizer.Form.NFKD);
                String terminatedPhrase = normalizedPhrase + '\0';

                if (BRWalletManager.getInstance(getActivity()).validatePhrase(getActivity(), phraseToCheck)) {

                    BRWalletManager m = BRWalletManager.getInstance(getActivity());
                    m.wipeWalletButKeystore(getActivity());
                    m.wipeKeyStore();

                    boolean success = KeyStoreManager.putKeyStorePhrase(terminatedPhrase, getActivity(), 0);
                    boolean success2 = false;
                    if (success)
                        success2 = KeyStoreManager.putKeyStoreCanary(BRConstants.CANARY_STRING, getActivity(), 0);
//                    CharSequence sequence = CharBuffer.wrap(phraseToCheck);
//                    char[] normalizedPhrase = Normalizer.normalize(sequence, Normalizer.Form.NFKD).toCharArray();
                    if (!success || !success2)
                        return;
//                    KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis() / 1000), getActivity());

                    byte[] pubKey = m.getMasterPubKey(terminatedPhrase);
                    KeyStoreManager.putMasterPublicKey(pubKey, getActivity());
//                    Log.w(TAG, "The phrase from keystore is: " + KeyStoreManager.getKeyStoreString(getActivity()));
                    IntroActivity introActivity = (IntroActivity) getActivity();
                    getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    introActivity.startMainActivity();
                    if (!introActivity.isDestroyed()) introActivity.finish();
                } else {
                    alertDialog.setTitle(getResources().getString(R.string.alert));
                    //don't use
                    alertDialog.setMessage("\"" + normalizedPhrase + "\" - " +
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


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((BreadWalletApp) getActivity().getApplication()).hideKeyboard(getActivity());
    }
}
