package com.breadwallet.presenter.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.BRClipboardManager;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.wallet.BRWalletManager;

import java.util.Locale;

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

public class MainFragment extends Fragment {
    private static final String TAG = MainFragment.class.getName();
    public EditText addressEditText;

//    private AlertDialog alertDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        final Button scanQRButton = (Button) rootView.findViewById(R.id.main_button_scan_qr_code);
        RelativeLayout mainFragmentLayout = (RelativeLayout) rootView.findViewById(R.id.main_fragment);
        Button payAddressFromClipboardButton = (Button)
                rootView.findViewById(R.id.main_button_pay_address_from_clipboard);

        addressEditText = (EditText) rootView.findViewById(R.id.address_edit_text);
        addressEditText.setGravity(Gravity.CENTER_HORIZONTAL);
        addressEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addressEditText.setText(BRClipboardManager.readFromClipboard(getActivity()));
                MainActivity app = MainActivity.app;
                if (app != null) {
                    app.hideAllBubbles();
                }
            }
        });




        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BreadWalletApp) getActivity().getApplication()).hideKeyboard(getActivity());
                addressEditText.clearFocus();
                MainActivity app = MainActivity.app;
                if (app != null) {
                    app.hideAllBubbles();
                }
            }
        });

        mainFragmentLayout.setPadding(0, MainActivity.screenParametersPoint.y / 5, 0, 0);
        scanQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    FragmentAnimator.animateDecoderFragment();
                }
            }
        });

        payAddressFromClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog alert = null;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    String tempAddress = BRClipboardManager.readFromClipboard(getActivity());
                    if (!addressEditText.getText().toString().isEmpty()) {
                        tempAddress = addressEditText.getText().toString();
                    }
                    Log.e(TAG, "tempAddress: " + tempAddress);
                    final String finalAddress = tempAddress;
                    BRWalletManager wm = BRWalletManager.getInstance(getActivity());

                    if (wm.isValidBitcoinPrivateKey(finalAddress) || wm.isValidBitcoinBIP38Key(finalAddress)) {
                        BRWalletManager.getInstance(getActivity()).confirmSweep(getActivity(), finalAddress);
                        addressEditText.setText("");
                        return;
                    }

                    if (checkIfAddressIsValid(finalAddress)) {
                        if (finalAddress != null) {
                            BRWalletManager m = BRWalletManager.getInstance(getActivity());
                            if (m.addressContainedInWallet(finalAddress)) {

                                //builder.setTitle(getResources().getString(R.string.alert));
                                builder.setMessage(getResources().getString(R.string.address_already_in_your_wallet));
                                builder.setNeutralButton(getResources().getString(R.string.ok),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alert = builder.create();
                                alert.show();
                                BRClipboardManager.copyToClipboard(getActivity(), "");
                                addressEditText.setText("");
                            } else if (m.addressIsUsed(finalAddress)) {
                                builder.setTitle(getResources().getString(R.string.warning));

                                builder.setMessage(getResources().getString(R.string.address_already_used));
                                builder.setPositiveButton(getResources().getString(R.string.ignore),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                FragmentAnimator.animateScanResultFragment();
                                                FragmentScanResult.address = finalAddress;
                                            }
                                        });
                                builder.setNegativeButton(getResources().getString(R.string.cancel),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alert = builder.create();
                                alert.show();
                            } else {
                                FragmentAnimator.animateScanResultFragment();
                                FragmentScanResult.address = finalAddress;
                            }
                        } else {
                            throw new NullPointerException();
                        }
                    } else {
                        //builder.setTitle(getResources().getString(R.string.alert));
                        builder.setMessage(getResources().getString(R.string.mainfragment_clipboard_invalid_data));
                        builder.setNeutralButton(getResources().getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alert = builder.create();
                        alert.show();
                        BRClipboardManager.copyToClipboard(getActivity(), "");
                        addressEditText.setText("");
                    }
                }
            }

        });
        return rootView;
    }

    private boolean checkIfAddressIsValid(String str) {

        return BRWalletManager.validateAddress(str.trim());
    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager keyboard = (InputMethodManager) getActivity().
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            EditText editText = CustomPagerAdapter.adapter.
                    mainFragment.addressEditText;
            if (editText != null)
                keyboard.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }
}