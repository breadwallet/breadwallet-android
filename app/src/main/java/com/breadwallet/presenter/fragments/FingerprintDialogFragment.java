package com.breadwallet.presenter.fragments;/*
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

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.security.FingerprintUiHelper;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;

import static android.R.attr.dialogLayout;
import static com.breadwallet.R.id.keyboard;


/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
public class FingerprintDialogFragment extends Fragment
        implements FingerprintUiHelper.Callback {
    public static final String TAG = FingerprintDialogFragment.class.getName();

    private FingerprintManager.CryptoObject mCryptoObject;
    private FingerprintUiHelper mFingerprintUiHelper;
    private BRAuthCompletion completion;
    private TextView title;
    private TextView message;
    private LinearLayout fingerPrintLayout;
    private boolean authSucceeded;

    FingerprintUiHelper.FingerprintUiHelperBuilder mFingerprintUiHelperBuilder;

    public FingerprintDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Do not create a new Fragment when the Activity is re-created such as orientation changes. 
        setRetainInstance(true);
//        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);
//        getDialog().setTitle(R.string.fingerprint_auth);
        message = (TextView) v.findViewById(R.id.fingerprint_description);
        fingerPrintLayout = (LinearLayout) v.findViewById(R.id.fingerprint_layout);
//        title = (TextView) v....
        Bundle bundle = getArguments();
        String titleString = bundle.getString("title");
        String messageString = bundle.getString("message");
        if (!Utils.isNullOrEmpty(titleString)) {
            title.setText(titleString);
        }
        if (!Utils.isNullOrEmpty(messageString)) {
            message.setText(messageString);
        }
        FingerprintManager mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Activity.FINGERPRINT_SERVICE);
        mFingerprintUiHelperBuilder = new FingerprintUiHelper.FingerprintUiHelperBuilder(mFingerprintManager);
        mFingerprintUiHelper = mFingerprintUiHelperBuilder.build(
                (ImageView) v.findViewById(R.id.fingerprint_icon),
                (TextView) v.findViewById(R.id.fingerprint_status), this);
        View mFingerprintContent = v.findViewById(R.id.fingerprint_container);

        Button mCancelButton = (Button) v.findViewById(R.id.cancel_button);
        Button mSecondDialogButton = (Button) v.findViewById(R.id.second_dialog_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BRAnimator.isClickAllowed()) return;
//                if (!BRAnimator.scanResultFragmentOn && mode == BRConstants.AUTH_FOR_PAY && request.isAmountRequested) {
////                    FragmentScanResult.address = request.address[0];
//                    BRWalletManager.getInstance().offerToChangeTheAmount(getActivity(), "");
//                }
//                dismiss();
            }
        });
        mCancelButton.setText(R.string.cancel);
        mSecondDialogButton.setText(R.string.use_passcode);
        mFingerprintContent.setVisibility(View.VISIBLE);
        mSecondDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BRAnimator.isClickAllowed()) return;
                goToBackup();
            }
        });

        return v;
    }

    @Override
    public void onStop() {
        super.onStop();
        fingerPrintLayout.animate()
                .translationY(1000)
                .withLayer();
//        dialogLayout.animate()
//                .scaleY(0)
//                .scaleX(0).alpha(0);
//        mainLayout.animate().alpha(0);
        if (!authSucceeded)
            completion.onCancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintUiHelper.startListening(mCryptoObject);
        authSucceeded = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        mFingerprintUiHelper.stopListening();
    }

    /**
     * Switches to backup (password) screen. This either can happen when fingerprint is not
     * available or the user chooses to use the password authentication method by pressing the
     * button. This can also happen when the user had too many fingerprint attempts.
     */
    private void goToBackup() {
        Log.e(TAG, "goToBackup: ");
//        // Fingerprint is not used anymore. Stop listening for it.
//        if (getDialog() != null)
//            getDialog().cancel();
//        PasswordDialogFragment passwordDialogFragment = new PasswordDialogFragment();
//        passwordDialogFragment.setMode(mode);
//        passwordDialogFragment.setPaymentRequestEntity(request, paymentRequest);
//        passwordDialogFragment.setVerifyOnlyTrue();
//        passwordDialogFragment.setmMessage(message);
//        FragmentManager fm = getActivity().getFragmentManager();
//        passwordDialogFragment.show(fm, PasswordDialogFragment.class.getName());
        AuthManager.getInstance().authPrompt(getContext(), title.getText().toString(), message.getText().toString(), true, completion);
        if (mFingerprintUiHelper != null)
            mFingerprintUiHelper.stopListening();
    }

    @Override
    public void onAuthenticated() {
//        Dialog d = getDialog();
//        if (d == null) return;
//        d.cancel();
        authSucceeded = true;
        Log.e(TAG, "onAuthenticated: ");
//
//        ((BreadWalletApp) getActivity().getApplicationContext()).setUnlocked(true);
//        FragmentSettingsAll.refreshUI();
//        MiddleViewAdapter.resetMiddleView(getActivity(), null);
//        ((BreadWalletApp) getActivity().getApplicationContext()).allowKeyStoreAccessForSeconds();
//        getDialog().dismiss();
//        if (mode == BRConstants.AUTH_FOR_PHRASE) {
//            PhraseFlowActivity app = ((PhraseFlowActivity) getActivity());
//            if (SharedPreferencesManager.getPhraseWroteDown(app)) {
////                app.animateSlide(app.fragmentPhraseFlow1, app.fragmentRecoveryPhrase, IntroActivity.RIGHT);
////                app.fragmentRecoveryPhrase.setPhrase(FragmentPhraseFlow1.phrase);
//            } else {
////                app.animateSlide(app.fragmentPhraseFlow1, app.fragmentPhraseFlow2, IntroActivity.RIGHT);
////                app.fragmentPhraseFlow2.setPhrase(FragmentPhraseFlow1.phrase);
//            }
//        } else if (mode == BRConstants.AUTH_FOR_PAY && request != null) {
//            PostAuthenticationProcessor.getInstance().onPublishTxAuth((MainActivity) getActivity(),false);
//        } else if (mode == BRConstants.AUTH_FOR_PAYMENT_PROTOCOL && paymentRequest != null) {
//            if (paymentRequest.paymentURL == null || paymentRequest.paymentURL.isEmpty()) return;
//            new PaymentProtocolPostPaymentTask(paymentRequest).execute();
//        } else if (mode == AUTH_FOR_BIT_ID){
//            RequestHandler.processBitIdResponse(getActivity());
//        }
//        dismiss();
    }

    public void setCompletion(BRAuthCompletion completion) {
        this.completion = completion;
    }

    @Override
    public void onError() {
        Log.e(TAG, "onError: going to backup");
        goToBackup();
    }

}