package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.util.StringUtil;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import rufus.lzstring4java.LZString;

public class MultiSignQrActivity extends BRActivity {

    private final String TAG = "MultiSignQrActivity";
    private ImageView mQRCodeIv;

    private String mTransaction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sign_qr);

        Intent intent = getIntent();
        mTransaction = intent.getStringExtra("tx");

        initView();
        if (!StringUtil.isNullOrEmpty(mTransaction)) {
            fixView();
        }
    }

    private void initView() {
        mQRCodeIv = findViewById(R.id.multisign_qr_iv);
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        TextView shareQr = findViewById(R.id.multisign_qr_share_qr);
        TextView shareJson = findViewById(R.id.multisign_qr_share_json);
        TextView passOrSent = findViewById(R.id.multisign_pass_or_sent);
        TextView or = findViewById(R.id.multisign_qr_share_or);

        if (StringUtil.isNullOrEmpty(mTransaction)) {
            mQRCodeIv.setVisibility(View.INVISIBLE);
            shareQr.setVisibility(View.INVISIBLE);
            shareJson.setVisibility(View.INVISIBLE);
            or.setVisibility(View.INVISIBLE);
            passOrSent.setText(R.string.multisign_send_succeeded);
        } else {
            shareJson.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            shareQr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            passOrSent.setText(R.string.multisign_pass_next);
        }
    }

    private void fixView() {

        try {
            String url = "elaphant://multisign?sign=" + URLEncoder.encode(mTransaction, "utf-8");
            String utf16 = LZString.compressToUTF16(url);
            Log.d(TAG, "=== utf16 length: " + utf16.length());
            QRUtils.generateQR(this, url, mQRCodeIv);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

}
