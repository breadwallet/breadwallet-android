package com.breadwallet.presenter.fragments;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/24/15.
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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.security.PassCodeManager;

public class ChangePasswordDialogFragment extends DialogFragment {

    public static final String TAG = "PasswordDialogFragment";
    public EditText passwordEditText;
    public Button cancel;
    public Button ok;
    public DialogFragment dialogFragment;
    private static final int AUTH_MODE_CHECK_PASS = 0;
    private static final int AUTH_MODE_NEW_PASS = 1;
    private static final int AUTH_MODE_CONFIRM_PASS = 2;
    private static int currentMode = AUTH_MODE_CHECK_PASS;
    private static String tempPassToChange;


    public ChangePasswordDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password_dialog, container);
        dialogFragment = this;
        passwordEditText = (EditText) view.findViewById(R.id.edit_password);
        cancel = (Button) view.findViewById(R.id.button_password_cancel);
        ok = (Button) view.findViewById(R.id.button_password_ok);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
                passwordEditText.setText("");
                InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.hideSoftInputFromWindow(cancel.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity app = MainActivity.app;
                Log.e(TAG, "clicked: " + currentMode);
                switch (currentMode) {
                    case AUTH_MODE_CHECK_PASS:
                        if (PassCodeManager.checkAuth(passwordEditText.getText().toString())) {
                            currentMode = AUTH_MODE_NEW_PASS;
                            getDialog().setTitle(getResources().getString(R.string.chose_a_new_passcode));
                            passwordEditText.setText("");
                            Log.e(TAG, "-----1-----");
                        } else {
                            Log.d(TAG, "Not equal, the text is: " + passwordEditText.getText().toString());
                            SpringAnimator.showAnimation(dialogFragment.getView());
                            passwordEditText.setText("");
                            Log.e(TAG, "-----2-----");
                        }
                        break;
                    case AUTH_MODE_NEW_PASS:
                        String pass = passwordEditText.getText().toString();
                        if (pass.length() > 3 && pass.length() < 12) {
                            Log.e(TAG, "-----3-----");
                            tempPassToChange = pass;
                            currentMode = AUTH_MODE_CONFIRM_PASS;
                            getDialog().setTitle(getResources().getString(R.string.verify_passcode));
                            passwordEditText.setText("");
                        } else {
                            Log.e(TAG, "-----4-----");
                            SpringAnimator.showAnimation(dialogFragment.getView());
                            passwordEditText.setText("");
                        }
                        break;
                    case AUTH_MODE_CONFIRM_PASS:
                        String passToCheck = passwordEditText.getText().toString();
                        if (passToCheck.equals(tempPassToChange)) {
                            Log.e(TAG, "-----5-----");
                            PassCodeManager.setPassCode(tempPassToChange);
                            tempPassToChange = "";
                            getDialog().cancel();
                            String tmp = CurrencyManager.getInstance(getActivity()).getCurrentBalanceText();
                            ((BreadWalletApp) getActivity().getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, tmp);
                            ((BreadWalletApp)getActivity().getApplicationContext()).setUnlocked(true);
                            InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                                    getSystemService(Context.INPUT_METHOD_SERVICE);
                            keyboard.hideSoftInputFromWindow(ok.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                            currentMode = AUTH_MODE_CHECK_PASS;
                        } else {
                            Log.e(TAG, "-----6-----");
                            SpringAnimator.showAnimation(dialogFragment.getView());
                            passwordEditText.setText("");
                            currentMode = AUTH_MODE_NEW_PASS;
                            getDialog().setTitle(getResources().getString(R.string.chose_a_new_passcode));
                        }
                        break;
                }

            }
        });
        getDialog().setTitle(getResources().getString(R.string.insert_old_passcode));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        passwordEditText.setText("");
        passwordEditText.post(
                new Runnable() {
                    public void run() {
                        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
                        inputMethodManager.toggleSoftInputFromWindow(passwordEditText.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                        passwordEditText.requestFocus();
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(CustomPagerAdapter.adapter.
                mainFragment.addressEditText.getWindowToken(), 0);
    }
}