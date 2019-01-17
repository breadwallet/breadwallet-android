package com.breadwallet.presenter.activities.did;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.tools.qrcode.QRUtils;

public class AuthorQrActivity extends BaseSettingsActivity {
    private ImageView mQRIv;

    @Override
    public int getLayoutId() {
        return R.layout.activity_author_qr_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        showQR();
    }

    private void initView(){
        mQRIv = findViewById(R.id.author_qr_iv);
    }

    private void showQR(){


        QRUtils.generateQR(this, "AuthorQrActivity", mQRIv);
    }
}
