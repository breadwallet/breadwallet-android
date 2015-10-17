/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.breadwallet.tools.auth;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.PassCodeManager;

/**
 * A dialog which uses password to authentication if fingerprint is not available.
 */
public class PasswordAuthenticationDialogFragment extends DialogFragment
        implements TextView.OnEditorActionListener, FingerprintUiHelper.Callback {

    private Button mCancelButton;
    private Button mSecondDialogButton;
    private View mFingerprintContent;
    private View mBackupContent;
    private EditText mPassword;

    InputMethodManager mInputMethodManager;


    public PasswordAuthenticationDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
        mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle("need authentication");
        View v = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);
        mCancelButton = (Button) v.findViewById(R.id.cancel_button);
        mSecondDialogButton = (Button) v.findViewById(R.id.second_dialog_button);
        mFingerprintContent = v.findViewById(R.id.fingerprint_container);
        mBackupContent = v.findViewById(R.id.backup_container);
        mPassword = (EditText) v.findViewById(R.id.password);
        mPassword.setOnEditorActionListener(this);
        mPassword.postDelayed(mShowKeyboardRunnable, 500);
        mPassword.requestFocus();
        v.findViewById(R.id.use_fingerprint_in_future_check);
        v.findViewById(R.id.new_fingerprint_enrolled_description);
        mCancelButton.setText(R.string.cancel);
        mSecondDialogButton.setText(R.string.ok);
        mFingerprintContent.setVisibility(View.GONE);
        mBackupContent.setVisibility(View.VISIBLE);

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mSecondDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verifyPassword();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

//    /**
//     * Sets the crypto object to be passed in when authenticating with fingerprint.
//     */
//    public void setCryptoObject(FingerprintManager.CryptoObject cryptoObject) {
//        mCryptoObject = cryptoObject;
//    }


    /**
     * Checks whether the current entered password is correct, and dismisses the the dialog and
     * let's the activity know about the result.
     */
    private void verifyPassword() {
        if (!checkPassword(mPassword.getText().toString())) {
            SpringAnimator.showAnimation(mPassword);
            return;
        }
        mPassword.setText("");
        onAuthenticated();
        dismiss();
    }

    /**
     * @return true if {@code password} is correct, false otherwise
     */
    private boolean checkPassword(String password) {
        mPassword.setText("");
        // Assume the password is always correct.
        // In the real world situation, the password needs to be verified in the server side.
        return PassCodeManager.checkAuth(password);
    }

    private final Runnable mShowKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            mInputMethodManager.showSoftInput(mPassword, 0);
        }
    };

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            verifyPassword();
            return true;
        }
        return false;
    }

    @Override
    public void onAuthenticated() {
        getDialog().cancel();
        String tmp = CurrencyManager.getCurrentBalanceText();
        ((BreadWalletApp) getActivity().getApplication()).setTopMiddleView(BreadWalletApp.BREAD_WALLET_TEXT, tmp);
        ((MainActivity) getActivity()).setUnlocked(true);
        MainActivity.app.softKeyboard.closeSoftKeyboard();
        // Callback from FingerprintUiHelper. Let the activity know that authentication was
        // successful.
//        mActivity.onPurchased(true /* withFingerprint */);
        dismiss();
    }

    @Override
    public void onError() {
    }

}
