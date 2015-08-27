package com.breadwallet.presenter.fragments;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.others.MyClipboardManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/14/15.
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

public class MainFragment extends Fragment {
    public static final String TAG = "MainFragment";
    private Button scanQRButton;
    private Button payAddressFromClipboardButton;
    public EditText addressEditText;
    private AlertDialog alertDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        scanQRButton = (Button) getActivity().findViewById(R.id.main_button_scan_qr_code);
        payAddressFromClipboardButton = (Button) getActivity().findViewById(R.id.main_button_pay_address_from_clipboard);
        alertDialog = new AlertDialog.Builder(getActivity()).create();
        scanQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                    FragmentAnimator.animateDecoderFragment();
                }
            }
        });
        addressEditText = (EditText) getView().findViewById(R.id.address_edit_text);
        addressEditText.setGravity(Gravity.CENTER_HORIZONTAL);
        payAddressFromClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                    if (alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                    String address = MyClipboardManager.readFromClipboard(getActivity());
                    Log.e(TAG, "The address before check: " + address);
                    if (checkIfAddressIsValid(address)) {
                        payAddressFromClipboardButton.setBackgroundResource(R.drawable.buttonbluepressed);
                        addressEditText.setText(address);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                payAddressFromClipboardButton.setBackgroundResource(R.drawable.buttonblue);
                            }
                        }, 50);
                        if (MainFragmentDecoder.accessGranted) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    MainFragmentDecoder.accessGranted = true;
                                }
                            }, 300);
                            MainFragmentDecoder.accessGranted = false;
                            Log.e(TAG, "The address: " + address);
                            if (address != null) {
                                FragmentScanResult.address = address;
                                FragmentAnimator.animateScanResultFragment();
                            } else {
                                throw new NullPointerException();
                            }
                        }

                    } else {
                        alertDialog.setTitle(getResources().getString(R.string.alert));
                        alertDialog.setMessage(getResources().getString(R.string.mainfragment_clipboard_invalid_data));
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
                }
            }

        });

    }

    public boolean checkIfAddressIsValid(String str) {
        int length = str.length();
        if (length != 34) {
            return false;
        } else {
            for (int i = 0; i < length; i++) {
                if (str.charAt(i) < 48) {
                    Log.e(TAG, "Bad address, char: " + str.charAt(i));
                    return false;
                } else {
                    if (str.charAt(i) > 57 && str.charAt(i) < 65) {
                        Log.e(TAG, "Bad address, char: " + str.charAt(i));
                        return false;
                    }
                    if (str.charAt(i) > 90 && str.charAt(i) < 61) {
                        Log.e(TAG, "Bad address, char: " + str.charAt(i));
                        return false;
                    }
                    if (str.charAt(i) > 122) {
                        Log.e(TAG, "Bad address, char: " + str.charAt(i));
                        return false;
                    }
                }

            }
        }
        return true;
    }
}