package com.breadwallet.presenter.activities.did;

import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.security.BRKeyStore;
import com.elastos.jni.Utility;

import org.wallet.library.Constants;
import org.wallet.library.login.LoginRequest;

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
        LoginRequest loginRequest = new LoginRequest();
        try {
            loginRequest.type = Constants.REQUEST_TYPE_VALUE.TYPE_LOGIN;
            loginRequest.serialNumber = 0;
            byte[] phrase = BRKeyStore.getPhrase(this, 0);
            String publickey = Utility.getInstance(this).getSinglePublicKey(new String(phrase));
            loginRequest.did = Utility.getInstance(this).getDid(publickey);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        QRUtils.generateQR(this, "AuthorQrActivity", mQRIv);
    }
}
