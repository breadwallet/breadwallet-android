package com.breadwallet.presenter.fragments;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/24/15.
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

import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRClipboardManager;

import java.io.File;

public class FragmentSharing extends DialogFragment {

    private static final String TAG = FragmentSharing.class.getName();
    private boolean customToastAvailable = true;
    private String theAddress;

    public FragmentSharing() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sharing_dialog, container);
        TextView copyAddress = (TextView) view.findViewById(R.id.copy_address);
        TextView sendEmail = (TextView) view.findViewById(R.id.send_email);
        TextView sendMessage = (TextView) view.findViewById(R.id.send_message);
        TextView requestAmount = (TextView) view.findViewById(R.id.request_amount);
        TextView sharingTitle = (TextView) view.findViewById(R.id.sharing_title);
        sharingTitle.setText(String.format(getString(R.string.at_this_address), "\n" + theAddress));

        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog);

        copyAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    BRClipboardManager.copyToClipboard(getActivity(), theAddress);
                    if (customToastAvailable) {
                        customToastAvailable = false;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                customToastAvailable = true;
                            }
                        }, 2000);
                        ((BreadWalletApp) getActivity().getApplicationContext()).
                                showCustomToast(getActivity(), getResources().getString(R.string.toast_address_copied), 360, Toast.LENGTH_SHORT, 0);
                        getDialog().cancel();
                    }
                }
            }

        });

        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto", "", null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.bitcoin_address));
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "bitcoin:" + theAddress);
                    File qrFile = MainFragmentQR.getQRImageFile(getActivity());
                    Uri uri = qrFile == null ? null : Uri.fromFile(qrFile);
                    if (uri != null)
                        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.bitcoin_address)));
                    getDialog().cancel();
                }
            }
        });

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    if (theAddress == null || theAddress.isEmpty()) return;
                    Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                    sendIntent.setData(Uri.parse("sms:"));
                    sendIntent.putExtra("sms_body", "bitcoin:" + theAddress);
                    sendIntent.putExtra("exit_on_sent", true);
                    startActivity(sendIntent);
                    getDialog().cancel();

                }
            }
        });

        requestAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    getDialog().cancel();
                    FragmentScanResult.isARequest = true;
                    BRAnimator.animateScanResultFragment();
                }
            }
        });

        return view;
    }

    public void setTheAddress(String address) {
        theAddress = address;
    }

}