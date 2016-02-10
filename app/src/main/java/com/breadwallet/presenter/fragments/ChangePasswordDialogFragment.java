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
import android.app.DialogFragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.PassCodeManager;

public class ChangePasswordDialogFragment extends DialogFragment {

    private static final String TAG = ChangePasswordDialogFragment.class.getName();
    private EditText passcodeEditText;
    private Button cancel;
    private DialogFragment dialogFragment;
    private static final int AUTH_MODE_CHECK_PASS = 0;
    private static final int AUTH_MODE_NEW_PASS = 1;
    private static final int AUTH_MODE_CONFIRM_PASS = 2;
    private static int currentMode = AUTH_MODE_CHECK_PASS;
    private static String tempPassToChange;
    private boolean firstTime = false; // if false then change;
    private boolean verifyOnly = false;


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_password_dialog, container);
        dialogFragment = this;
        passcodeEditText = (EditText) view.findViewById(R.id.edit_passcode);
        cancel = (Button) view.findViewById(R.id.button_password_cancel);

        final InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                getSystemService(Context.INPUT_METHOD_SERVICE);


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
                passcodeEditText.setText("");
                keyboard.hideSoftInputFromWindow(cancel.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        });

        getDialog().setTitle(getResources().getString(R.string.insert_old_passcode));

        if (firstTime) {
            cancel.setClickable(false);
            cancel.setVisibility(View.GONE);
            getDialog().setTitle(R.string.chose_a_passcode);
            currentMode = AUTH_MODE_NEW_PASS;
        }
        if (verifyOnly) {
            getDialog().setTitle(R.string.enter_passcode);
        }

        passcodeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() == 4) validatePassCode(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        getDialog().setCanceledOnTouchOutside(false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        passcodeEditText.setText("");
        passcodeEditText.post(
                new Runnable() {
                    public void run() {
                        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.toggleSoftInputFromWindow(passcodeEditText.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
                        passcodeEditText.requestFocus();
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

    public void setFirstTimeTrue() {
        firstTime = true;
        verifyOnly = false;
    }

    public void setVerifyOnlyTrue() {
        verifyOnly = true;
        firstTime = false;
    }

    private boolean validatePassCode(CharSequence s) {
        PassCodeManager passCodeManager = PassCodeManager.getInstance();

        if (firstTime) {
            switch (currentMode) {
                case AUTH_MODE_NEW_PASS:
                    tempPassToChange = s.toString();
                    currentMode = AUTH_MODE_CONFIRM_PASS;
                    getDialog().setTitle(getResources().getString(R.string.verify_passcode));
                    passcodeEditText.setText("");
                    break;
                case AUTH_MODE_CONFIRM_PASS:
                    String passToCheck = s.toString();
                    if (passToCheck.equals(tempPassToChange)) {
                        passCodeManager.setPassCode(tempPassToChange, getActivity());
                        tempPassToChange = "";
                        getDialog().cancel();
                        String tmp = CurrencyManager.getInstance(getActivity()).getCurrentBalanceText();
                        MiddleViewAdapter.resetMiddleView(getActivity(), tmp);
                        ((BreadWalletApp) getActivity().getApplicationContext()).setUnlocked(true);
                        InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                        keyboard.hideSoftInputFromWindow(cancel.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                        currentMode = AUTH_MODE_CHECK_PASS;
                    } else {
                        SpringAnimator.showAnimation(dialogFragment.getView());
                        passcodeEditText.setText("");
                        currentMode = AUTH_MODE_NEW_PASS;
                        getDialog().setTitle(getResources().getString(R.string.chose_a_new_passcode));
                    }
                    break;
            }
        } else if (verifyOnly) {
            if (passCodeManager.checkAuth(s.toString(), getActivity())) {
                BreadWalletApp.canceled = true;
                getDialog().cancel();
                MainActivity app = (MainActivity) getActivity();

                ((BreadWalletApp) getActivity().getApplicationContext()).setUnlocked(true);
                FragmentSettingsAll.refreshUI(getActivity());
                MiddleViewAdapter.resetMiddleView(getActivity(), null);
                app.softKeyboard.closeSoftKeyboard();
                ((BreadWalletApp) getActivity().getApplicationContext()).allowKeyStoreAccessForSeconds();
                getDialog().dismiss();
                passcodeEditText.setText("");
            } else {
                SpringAnimator.showAnimation(dialogFragment.getView());
                passcodeEditText.setText("");
            }

        } else {
            switch (currentMode) {
                case AUTH_MODE_CHECK_PASS:
                    if (passCodeManager.checkAuth(s.toString(), getActivity())) {
                        currentMode = AUTH_MODE_NEW_PASS;
                        getDialog().setTitle(getResources().getString(R.string.chose_a_new_passcode));
                        passcodeEditText.setText("");
                    } else {
                        Log.d(TAG, "Not equal, the text is: " + passcodeEditText.getText().toString());
                        SpringAnimator.showAnimation(dialogFragment.getView());
                        passcodeEditText.setText("");
                    }
                    break;
                case AUTH_MODE_NEW_PASS:
                    if (s.length() > 3 && s.length() < 12) {
                        tempPassToChange = s.toString();
                        currentMode = AUTH_MODE_CONFIRM_PASS;
                        getDialog().setTitle(getResources().getString(R.string.verify_passcode));
                        passcodeEditText.setText("");
                    } else {
                        SpringAnimator.showAnimation(dialogFragment.getView());
                        passcodeEditText.setText("");
                    }
                    break;
                case AUTH_MODE_CONFIRM_PASS:
                    String passToCheck = s.toString();
                    if (passToCheck.equals(tempPassToChange)) {
                        passCodeManager.setPassCode(tempPassToChange, getActivity());
                        tempPassToChange = "";
                        getDialog().cancel();
                        String tmp = CurrencyManager.getInstance(getActivity()).getCurrentBalanceText();
                        MiddleViewAdapter.resetMiddleView(getActivity(), tmp);
                        ((BreadWalletApp) getActivity().getApplicationContext()).setUnlocked(true);
                        InputMethodManager keyboard = (InputMethodManager) MainActivity.app.
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                        keyboard.hideSoftInputFromWindow(cancel.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                        currentMode = AUTH_MODE_CHECK_PASS;
                    } else {
                        SpringAnimator.showAnimation(dialogFragment.getView());
                        passcodeEditText.setText("");
                        currentMode = AUTH_MODE_NEW_PASS;
                        getDialog().setTitle(getResources().getString(R.string.chose_a_new_passcode));
                    }
                    break;
            }
        }
        return false;
    }
}