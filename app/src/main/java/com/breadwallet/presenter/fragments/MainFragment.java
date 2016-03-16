package com.breadwallet.presenter.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.BRClipboardManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.wallet.BRWalletManager;

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
    private static final String TAG = MainFragment.class.getName();
    public EditText addressEditText;
    private AlertDialog alertDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        Button scanQRButton = (Button) rootView.findViewById(R.id.main_button_scan_qr_code);
        LinearLayout mainFragmentLayout = (LinearLayout) rootView.findViewById(R.id.main_fragment);
        Button payAddressFromClipboardButton = (Button)
                rootView.findViewById(R.id.main_button_pay_address_from_clipboard);
        addressEditText = (EditText) rootView.findViewById(R.id.address_edit_text);

        alertDialog = new AlertDialog.Builder(getActivity()).create();
        addressEditText.setGravity(Gravity.CENTER_HORIZONTAL);

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
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    if (alertDialog.isShowing()) alertDialog.dismiss();
                    String address = BRClipboardManager.readFromClipboard(getActivity());
                    if (checkIfAddressIsValid(address)) {
                        if (address != null) {
                            BRWalletManager m = BRWalletManager.getInstance(getActivity());
                            if (!m.addressContainedInWallet(address)) {
                                FragmentAnimator.animateScanResultFragment();
                                FragmentScanResult.address = address;
                            } else {
                                alertDialog.setTitle(getResources().getString(R.string.alert));
                                alertDialog.setMessage(getResources().getString(R.string.address_already_in_your_wallet));
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.ok),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                        } else {
                            throw new NullPointerException();
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
        return rootView;
    }

    private boolean checkIfAddressIsValid(String str) {
        BRWalletManager m = BRWalletManager.getInstance(getActivity());

        return m.validateAddress(str);
    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity.app.softKeyboard.closeSoftKeyboard();
    }
}