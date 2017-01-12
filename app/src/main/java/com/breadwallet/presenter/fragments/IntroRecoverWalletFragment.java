package com.breadwallet.presenter.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.util.WordsReader;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.util.List;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.breadwallet.R.string.show;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/15/15.
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

public class IntroRecoverWalletFragment extends Fragment {
    public static final String TAG = IntroRecoverWalletFragment.class.getName();
    private Button recoverButton;
    private EditText editText;
    private AlertDialog alertDialog;
    private Button continueButton;
    private Button cancelButton;
    private InputMethodChangeReceiver mReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.intro_fragment_recover_wallet, container, false);

        recoverButton = (Button) rootView.findViewById(R.id.recover_button);
        editText = (EditText) rootView.findViewById(R.id.recover_wallet_edit_text);
        continueButton = (Button) rootView.findViewById(R.id.continue_button);
        cancelButton = (Button) rootView.findViewById(R.id.cancel_button);
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

                String phraseToCheck = editText.getText().toString().toLowerCase();
                String cleanPhrase = WordsReader.cleanPhrase(getActivity(), phraseToCheck);

                if (BRWalletManager.getInstance(getActivity()).validatePhrase(getActivity(), cleanPhrase)) {
                    showKeyBoard(false);
                    BRWalletManager m = BRWalletManager.getInstance(getActivity());
                    m.wipeWalletButKeystore(getActivity());
                    m.wipeKeyStore(getActivity());
                    PostAuthenticationProcessor.getInstance().setPhraseForKeyStore(cleanPhrase);
                    PostAuthenticationProcessor.getInstance().onRecoverWalletAuth((IntroActivity) getActivity(), false);
                    SharedPreferencesManager.putAllowSpend(getActivity(), false);

                } else {
                    String message = getResources().getString(R.string.bad_recovery_phrase);
                    String[] words = cleanPhrase.split(" ");
                    if (words.length != 12) {
                        message = String.format(getActivity().getString(R.string.recovery_phrase_must_have_12_words), 12);
                    } else {
                        List<String> allWords = WordsReader.getAllWordLists(getActivity());

                        for (String word : words) {
                            if (!allWords.contains(word)) {
                                message = String.format(getActivity().getString(R.string.not_a_recovery_phrase_word), word);
                            }
                        }
                    }

                    alertDialog.setMessage(message);
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
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        return rootView;
    }

    public void disableEditText() {
        cancelButton.setVisibility(View.VISIBLE);
        continueButton.setVisibility(View.VISIBLE);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectInputMethod();
            }
        });
        editText.setEnabled(false);
        editText.setHint(getString(R.string.insecure_keyboard_message));
        IntentFilter filter = new IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED);
        mReceiver = new InputMethodChangeReceiver();
        getActivity().registerReceiver(mReceiver, filter);
        showKeyBoard(false);

    }

    private void showSelectInputMethod(){
        InputMethodManager imeManager = (InputMethodManager) getActivity().getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
        if (imeManager != null) {
            imeManager.showInputMethodPicker();
        } else {
            FirebaseCrash.report(new RuntimeException("error showing the input method choosing dialog"));
            Toast.makeText(getActivity(), "error showing the input method choosing dialog", Toast.LENGTH_LONG).show();
        }
    }

    private void enableEditText() {
        editText.setEnabled(true);
        editText.setHint("");
        editText.setText("");
        continueButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        showKeyBoard(true);
        try {
            getActivity().unregisterReceiver(mReceiver);
        } catch (Exception ex){
            Log.e(TAG, "enableEditText: " + ex.getMessage());
        }

    }

    @Override
    public void onResume() {
        if (Utils.isUsingCustomInputMethod(getActivity()) && this.isVisible()) {
            disableEditText();
        } else {
            enableEditText();
        }

        super.onResume();
    }

    public void showKeyBoard(boolean b) {
        if (b && !Utils.isUsingCustomInputMethod(getActivity())) {
            if (editText != null) {
                editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
            }
        } else {
            ((BreadWalletApp) getActivity().getApplication()).hideKeyboard(getActivity());
        }

    }

    public class InputMethodChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_INPUT_METHOD_CHANGED)) {
                if(Utils.isUsingCustomInputMethod(getActivity())){
                    showSelectInputMethod();
                } else {
                    enableEditText();
                }
            }
        }
    }

}
