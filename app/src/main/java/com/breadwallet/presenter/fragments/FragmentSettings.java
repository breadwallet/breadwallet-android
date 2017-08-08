package com.breadwallet.presenter.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.adapter.CurrencyListAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.security.PassCodeManager;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import static com.breadwallet.tools.util.BRConstants.PLATFORM_ON;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
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

public class FragmentSettings extends Fragment {
    private static final String TAG = FragmentSettings.class.getName();
    private MainActivity app;
    private FragmentSettings fragmentSettings;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(
                R.layout.fragment_settings, container, false);

        app = MainActivity.app;
        fragmentSettings = this;
        initList();
        RelativeLayout about = (RelativeLayout) rootView.findViewById(R.id.about);
        TextView currencyName = (TextView) rootView.findViewById(R.id.three_letters_currency);
        RelativeLayout changePassword = (RelativeLayout) rootView.findViewById(R.id.change_password);
        final String tmp = SharedPreferencesManager.getIso(getActivity());
        currencyName.setText(tmp);
        RelativeLayout localCurrency = (RelativeLayout) rootView.findViewById(R.id.local_currency);
        RelativeLayout recoveryPhrase = (RelativeLayout) rootView.findViewById(R.id.recovery_phrase);
        RelativeLayout startRecoveryWallet = (RelativeLayout) rootView.findViewById(R.id.start_recovery_wallet);
        RelativeLayout fingerprintLimit = (RelativeLayout) rootView.findViewById(R.id.fingerprint_limit);
        RelativeLayout earlyAccess = (RelativeLayout) rootView.findViewById(R.id.early_access);
        RelativeLayout advanced = (RelativeLayout) rootView.findViewById(R.id.advanced);
        RelativeLayout line5 = (RelativeLayout) rootView.findViewById(R.id.settings_line_5);
        TextView theLimit = (TextView) rootView.findViewById(R.id.fingerprint_limit_text);
        RelativeLayout rescan = (RelativeLayout) rootView.findViewById(R.id.rescan_blockchain);


        theLimit.setText(BRStringFormatter.getFormattedCurrencyString("BTC", PassCodeManager.getInstance().getLimit(getActivity())));
        FingerprintManager mFingerprintManager;
        mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        boolean useFingerPrint;
        useFingerPrint = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.USE_FINGERPRINT)
                == PackageManager.PERMISSION_GRANTED && mFingerprintManager.isHardwareDetected() && mFingerprintManager.hasEnrolledFingerprints();

        if (!useFingerPrint) {
            fingerprintLimit.setVisibility(View.GONE);
            line5.setVisibility(View.GONE);
        }

        advanced.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    BRAnimator.animateSlideToLeft(app, new FragmentAdvanced(), fragmentSettings);
                }
            }
        });

        fingerprintLimit.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BRAnimator.checkTheMultipressingAvailability()) {
                            ((BreadWalletApp) getActivity().getApplicationContext()).promptForAuthentication(getActivity(),
                                    BRConstants.AUTH_FOR_LIMIT, null, null, null, null, false);
                        }
                    }
                });

        startRecoveryWallet.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BRAnimator.checkTheMultipressingAvailability()) {
                            BRAnimator.pressWipeWallet(app, new FragmentWipeWallet());
                            app.activityButtonsEnable(false);
                        }
                    }
                });
        recoveryPhrase.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BRWalletManager.getInstance(getActivity()).animateSavePhraseFlow();
                    }
                });
        about.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BRAnimator.checkTheMultipressingAvailability()) {
                            BRAnimator.animateSlideToLeft(app, new FragmentAbout(), fragmentSettings);
                        }
                    }
                });
        localCurrency.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BRAnimator.checkTheMultipressingAvailability()) {
                            BRAnimator.animateSlideToLeft(app, new FragmentCurrency(), fragmentSettings);
                        }
                    }
                });
        changePassword.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BRAnimator.checkTheMultipressingAvailability()) {
                            final android.app.FragmentManager fm = getActivity().getFragmentManager();
                            new PasswordDialogFragment().show(fm, PasswordDialogFragment.class.getName());
                        }
                    }
                }

        );

        rescan.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BRAnimator.checkTheMultipressingAvailability()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    BRAnimator.goToMainActivity(fragmentSettings);
                                    BRPeerManager.getInstance(getActivity()).rescan();
                                    SharedPreferencesManager.putStartHeight(getActivity(), 0);
                                }
                            }).start();
                        }

                    }
                }

        );
        //keep it hidden until finished
        if (!PLATFORM_ON) {
            earlyAccess.setVisibility(View.GONE);
            rootView.findViewById(R.id.early_access_separator).setVisibility(View.GONE);
            rootView.findViewById(R.id.early_access_separator2).setVisibility(View.GONE);

        } else {
            earlyAccess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BRAnimator.checkTheMultipressingAvailability()) {

                        BRAnimator.animateSlideToLeft(app, new FragmentWebView(), fragmentSettings);
                    }
                }
            });
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void initList() {
        CurrencyListAdapter.currencyListAdapter = CurrencyManager.getInstance(getActivity()).getCurrencyAdapterIfReady();
    }

}