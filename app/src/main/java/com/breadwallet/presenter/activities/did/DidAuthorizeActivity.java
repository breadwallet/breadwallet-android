package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;

import org.wallet.library.AuthorizeManager;
import org.wallet.library.interfaces.VerifyCallback;
import org.wallet.library.login.LoginRequest;
import org.wallet.library.templates.Request;

public class DidAuthorizeActivity extends BaseSettingsActivity {
    private static final String TAG = "Author_test";

    @Override
    public int getLayoutId() {
        return R.layout.activity_author_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String value = intent.getStringExtra("result");
        AuthorizeManager.Server.verifyAuthorize(value, new VerifyCallback() {
            @Override
            public void callBack(Request request, boolean b) {
                LoginRequest loginRequest = (LoginRequest) request;
                Log.i(TAG, "name:"+loginRequest.name);
            }
        });
        AuthorizeManager.Server.verifyAuthorize(value, new VerifyCallback<LoginRequest>() {
            @Override
            public void callBack(LoginRequest request, boolean b) {
                Log.i(TAG, "did:"+request.did);
            }
        });
    }
}
