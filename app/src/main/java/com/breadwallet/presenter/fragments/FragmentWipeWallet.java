
package com.breadwallet.presenter.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.util.WordsReader;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.util.List;

import static android.content.Context.INPUT_METHOD_SERVICE;

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

public class FragmentWipeWallet extends Fragment {
    private static final String TAG = FragmentWipeWallet.class.getName();
    private EditText recoveryPhraseEditText;
    private Button continueButton;
    private Button cancelButton;
    private Button wipe;
    private BRWalletManager m;
    private boolean allowWipeButtonPress = true;
    private AlertDialog alertDialog;
    private InputMethodChangeReceiver mReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragment_wipe_wallet, container, false);
        m = BRWalletManager.getInstance(getActivity());
        Button close = (Button) rootView.findViewById(R.id.wipe_wallet_close);
        recoveryPhraseEditText = (EditText) rootView.findViewById(R.id.editText_phrase);
        wipe = (Button) rootView.findViewById(R.id.wipe_wallet_wipe);
        continueButton = (Button) rootView.findViewById(R.id.continue_button);
        cancelButton = (Button) rootView.findViewById(R.id.cancel_button);
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
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
        alertDialog = new AlertDialog.Builder(getActivity()).create();
        wipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (alertDialog.isShowing()) {
                    alertDialog.dismiss();
                }
                if (!allowWipeButtonPress) return;
                allowWipeButtonPress = false;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        allowWipeButtonPress = true;
                    }
                }, 500);
                String cleanPhrase = WordsReader.cleanPhrase(getActivity(), recoveryPhraseEditText.getText().toString().trim().toLowerCase());
                if (KeyStoreManager.phraseIsValid(cleanPhrase, getActivity())) {
                    m.wipeKeyStore(getActivity());
                    m.wipeWalletButKeystore(getActivity());
                    BRPeerManager.stopSyncingProgressThread();
                    startIntroActivity();
                    BRAnimator.resetFragmentAnimator();
                    SharedPreferencesManager.putTipsShown(getActivity(), true);
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

                    //don't use
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

        return rootView;
    }


    @Override
    public void onPause() {
        super.onPause();
        ((BreadWalletApp) getActivity().getApplication()).hideKeyboard(getActivity());
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        allowWipeButtonPress = true;
        MainActivity app = MainActivity.app;
        if (app != null)
            app.activityButtonsEnable(false);
        if (recoveryPhraseEditText != null && !Utils.isUsingCustomInputMethod(getActivity())) {
            (new Handler()).postDelayed(new Runnable() {

                public void run() {
                    recoveryPhraseEditText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                    recoveryPhraseEditText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));

                }
            }, 100);

        }

        if (Utils.isUsingCustomInputMethod(getActivity())) {
            disableEditText();
        } else {
            enableEditText();
        }
    }

    public void disableEditText() {

        continueButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectInputMethod();
            }
        });
        recoveryPhraseEditText.setEnabled(false);
        recoveryPhraseEditText.setHint(getString(R.string.insecure_keyboard_message));
        IntentFilter filter = new IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED);
        mReceiver = new InputMethodChangeReceiver();
        getActivity().registerReceiver(mReceiver, filter);
        showKeyBoard(false);

    }

    private void showSelectInputMethod() {
        InputMethodManager imeManager = (InputMethodManager) getActivity().getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
        if (imeManager != null) {
            imeManager.showInputMethodPicker();
        } else {
            FirebaseCrash.report(new RuntimeException("error showing the input method choosing dialog"));
            Toast.makeText(getActivity(), "error showing the input method choosing dialog", Toast.LENGTH_LONG).show();
        }
    }

    private void enableEditText() {
        recoveryPhraseEditText.setEnabled(true);
        recoveryPhraseEditText.setHint("");
        recoveryPhraseEditText.setText("");
        continueButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
        showKeyBoard(true);
        try {
            getActivity().unregisterReceiver(mReceiver);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showKeyBoard(boolean b) {
        if (b && !Utils.isUsingCustomInputMethod(getActivity())) {
            if (recoveryPhraseEditText != null) {
                recoveryPhraseEditText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                recoveryPhraseEditText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
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
                if (Utils.isUsingCustomInputMethod(getActivity())) {
                    showSelectInputMethod();
                } else {
                    enableEditText();
                }
            }
        }
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
