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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.others.MyClipboardManager;

public class PasswordDialogFragment extends DialogFragment {

    public static final String TAG = "SharingFragment";
    private TextView copyAddress;
    private TextView sendEmail;
    private TextView sendMessage;
    private TextView requestAmount;
    private boolean customToastAvailable = true;
    private String theAddress;

    public PasswordDialogFragment() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sharing_options, container);
        copyAddress = (TextView) view.findViewById(R.id.copy_address);
        sendEmail = (TextView) view.findViewById(R.id.send_email);
        sendMessage = (TextView) view.findViewById(R.id.send_message);
        requestAmount = (TextView) view.findViewById(R.id.request_amount);
        getDialog().setTitle(getResources().getString(R.string.dialog_receive_bitcoins));

        copyAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                    MyClipboardManager.copyToClipboard(getActivity(), theAddress);
                    if (customToastAvailable) {
                        customToastAvailable = false;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                customToastAvailable = true;
                            }
                        }, 2000);
                        ((BreadWalletApp) getActivity().getApplicationContext()).
                                showCustomToast(getActivity(), getResources().getString(R.string.toast_address_copied), 360, Toast.LENGTH_SHORT);
                        getDialog().cancel();
                    }
                }
            }

        });

        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto", "", null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.bitcoin_address));
                    emailIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.sharing_message) + " " + theAddress);
                    Uri uri = Uri.parse("file://" + MainFragmentQR.qrCodeImageFile.getAbsolutePath());
                    Log.e(TAG, "The qrCodeImageFile.getAbsolutePath(): " + MainFragmentQR.qrCodeImageFile.getAbsolutePath());
                    emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.dialog_email_title)));
                    getDialog().cancel();
                }
            }
        });

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                    Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                    sendIntent.setData(Uri.parse("sms:"));
                    sendIntent.putExtra("sms_body", getResources().getString(R.string.sharing_message) + " " + theAddress);
                    startActivity(sendIntent);
                    getDialog().cancel();
                }
            }
        });

        requestAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(FragmentAnimator.checkTheMultipressingAvailability(300)){
                    //TODO: actions on requesting amount
                }
            }
        });

        return view;
    }

    public void setTheAddress(String address) {
        theAddress = address;
    }

}