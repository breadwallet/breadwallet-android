package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.RequestHandler;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 1/15/16.
 * Copyright (c) 2016 breadwallet LLC
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

public class RequestQRActivity extends Activity {

    public static final String TAG = RequestQRActivity.class.getName();
    private ImageView qrcode;
    public boolean activityIsInBackground = false;
    public static RequestQRActivity requestApp;
    public Button close;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_qr);
        requestApp = this;

        String requestAddrs = getIntent().getExtras().getString(BRConstants.INTENT_EXTRA_REQUEST_ADDRESS);
        String requestAmount = getIntent().getExtras().getString(BRConstants.INTENT_EXTRA_REQUEST_AMOUNT);

        String finalAddress = "bitcoin:" + requestAddrs + "?amount=" + requestAmount;
        qrcode = (ImageView) findViewById(R.id.request_image_qr_code);
        close = (Button) findViewById(R.id.request_close);
        TextView requestAmountText = (TextView) findViewById(R.id.request_amount_text);
        TextView requestAddressText = (TextView) findViewById(R.id.request_address_text);
        RelativeLayout addressLayout = (RelativeLayout) findViewById(R.id.request_address_layout);

        BRWalletManager.getInstance(this).generateQR(finalAddress,qrcode);
        String address = "";
        String amount = "";
        RequestObject obj = RequestHandler.getRequestFromString(finalAddress);
        address = obj.address;
        final String iso = SharedPreferencesManager.getIso(this);
        final float rate = SharedPreferencesManager.getRate(this);
        amount = BRStringFormatter.getBitsAndExchangeString(rate, iso, new BigDecimal(obj.amount), this);
        final Intent intent = new Intent(this, MainActivity.class);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
                onBackPressed();
                if (MainActivity.app != null) {
                    onBackPressed();
                } else {
                    startActivity(intent);
                }
                if (!RequestQRActivity.this.isDestroyed()) {
                    finish();
                }

            }
        });
        requestAddressText.setText(address);
        requestAmountText.setText(amount);
        final BreadWalletApp breadWalletApp = (BreadWalletApp) getApplication();
        addressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                breadWalletApp.cancelToast();
            }
        });

    }



    @Override
    protected void onResume() {
        super.onResume();
        activityIsInBackground = false;
        requestApp = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityIsInBackground = true;
    }
}
