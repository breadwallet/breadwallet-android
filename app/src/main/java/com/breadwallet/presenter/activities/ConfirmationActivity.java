package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.fragments.FragmentLinkWallet;
import com.breadwallet.presenter.fragments.FragmentPaymentConfirmation;
import com.breadwallet.protocols.messageexchange.MessageExchangeService;
import com.breadwallet.protocols.messageexchange.entities.LinkMetaData;
import com.breadwallet.protocols.messageexchange.entities.RequestMetaData;
import com.breadwallet.tools.util.Utils;

/**
 * Created by Jade Byfield <jade@breadwallet.com> on  7/24/18.
 * Copyright (c) 2018 breadwallet LLC
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

/**
 * This Activity is used to confirm a request.  Currently it supports Link, Payment and Call requests from the
 * {@link MessageExchangeService}.  Accordingly, it uses either {@link FragmentLinkWallet} or
 * {@link FragmentPaymentConfirmation}.
 */
public class ConfirmationActivity extends FragmentActivity {
    private static final String TAG = ConfirmationActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        final Parcelable metaData = getIntent().getParcelableExtra(MessageExchangeService.EXTRA_METADATA);
        String action = getIntent().getAction();

        BRButton positiveButton = findViewById(R.id.positive_button);
        BRButton negativeButton = findViewById(R.id.negative_button);
        View headerView = findViewById(R.id.header);

        if (!Utils.isNullOrEmpty(action) && action == MessageExchangeService.ACTION_GET_USER_CONFIRMATION) {
            if (metaData != null) {
                Log.d(TAG, "Found metaData!");
                if (metaData instanceof LinkMetaData) {

                    // Handles link messages.
                    FragmentLinkWallet linkWalletFragment = FragmentLinkWallet.newInstance((LinkMetaData) metaData);
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, linkWalletFragment).commit();
                    Log.d(TAG, "ConfirmationType LINK");

                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleLinkApproved();
                        }
                    });

                    negativeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handleLinkDeclined();
                        }
                    });

                }
                // Handles payment and call requests.
                else if (metaData instanceof RequestMetaData) {
                    headerView.setVisibility(View.VISIBLE);
                    // Display FragmentPaymentConfirmation and set up new listeners for positive
                    // and negative buttons
                    FragmentPaymentConfirmation paymentConfirmationFragment = FragmentPaymentConfirmation.newInstance((RequestMetaData) metaData);
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, paymentConfirmationFragment).commit();

                    positiveButton.setText(getResources().getString(R.string.Button_buy));
                    positiveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handlePaymentApproved((RequestMetaData) metaData);
                        }
                    });

                    negativeButton.setText(getResources().getString(R.string.Button_cancel));
                    negativeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            handlePaymentCanceled((RequestMetaData) metaData);
                        }
                    });

                } else {
                    Log.d(TAG, "Found unknown metadata type!");
                }
            }
        }
    }

    public void handleLinkDeclined() {
        Log.d(TAG, "handleLinkDeclined()");
        MessageExchangeService.enqueueWork(this,MessageExchangeService.createIntent(this, false));
        finish();
    }

    public void handleLinkApproved() {
        Log.d(TAG, "handleLinkApproved()");
        MessageExchangeService.enqueueWork(this, MessageExchangeService.createIntent(this, true));
        finish();
    }

    private void handlePaymentApproved(RequestMetaData metaData) {
        Log.d(TAG, "handlePaymentApproved()");
        MessageExchangeService.enqueueWork(this, MessageExchangeService.createIntent(this, metaData, true));
        finish();
    }

    private void handlePaymentCanceled(RequestMetaData metaData) {
        Log.d(TAG, "handlePaymentCanceled()");
        MessageExchangeService.enqueueWork(this, MessageExchangeService.createIntent(this, metaData, false));
        finish();
    }
}