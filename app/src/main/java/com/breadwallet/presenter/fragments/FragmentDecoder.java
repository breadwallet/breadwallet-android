package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.qrcode.QRScannerView;
import com.breadwallet.tools.security.BRErrorPipe;
import com.breadwallet.tools.security.RequestHandler;
import com.breadwallet.tools.util.Utils;
import com.google.zxing.Result;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 4/14/16.
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

public class FragmentDecoder extends Fragment implements QRScannerView.ResultHandler {
    private QRScannerView mScannerView;
    public static final String TAG = FragmentDecoder.class.getName();
    public static boolean withdrawingBCH;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mScannerView = new QRScannerView(getActivity());
        final RelativeLayout blackPane = new RelativeLayout(getContext());
        blackPane.setLayoutParams(new RelativeLayout.LayoutParams(MainActivity.screenParametersPoint.x, MainActivity.screenParametersPoint.y));
        blackPane.setBackgroundColor(Color.BLACK);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.removeView(blackPane);
            }
        }, 1000);
        mScannerView.addView(blackPane);
        return mScannerView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mScannerView != null)
                    mScannerView.initViews();
            }
        }, 1000);

    }

    @Override
    public void onResume() {
        super.onResume();
        final FragmentDecoder instance = this;
        mScannerView.setResultHandler(instance);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.startCamera();
            }
        }, 400);

    }

    @Override
    public void handleResult(Result rawResult) {
        final String text = rawResult.getText();
        final MainActivity app = MainActivity.app;
        if (app == null) return;
        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!text.isEmpty()) {
                    BRAnimator.hideDecoderFragment();
                    if (withdrawingBCH) {
                        setUrl(text);
                    } else
                        RequestHandler.processRequest(app, text);

                }
            }
        });

    }

    public static void setUrl(String url) {
        Log.e(TAG, "setUrl: ");
        BRAnimator.goToMainActivity(null);
        boolean failed = false;
        if (Utils.isNullOrEmpty(url)) {
            failed = true;
        }
        Uri uri = null;
        try {
            uri = Uri.parse(url);
        } catch (Exception e) {
            failed = true;
        }

        if (failed) {
            ((BreadWalletApp) MainActivity.app.getApplication()).showCustomDialog("Invalid address", url + " is not a valid address.", "close");
        } else {
            Log.e(TAG, "setUrl: " + uri);
            String address = uri.getHost();
            Log.e(TAG, "setUrl: " + address);

            FragmentWithdrawBch.confirmSendingBCH(MainActivity.app, url);

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
        withdrawingBCH = false;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (MainActivity.app != null)
                    MainActivity.app.activityButtonsEnable(true);
            }
        }, 500);
    }
}
