package com.breadwallet.presenter.activities.did;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.did.AuthorInfo;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.SwitchButton;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.Utility;
import com.google.gson.Gson;

import org.wallet.library.AuthorizeManager;
import org.wallet.library.Constants;
import org.wallet.library.entity.LoginResponse;
import org.wallet.library.entity.SignWrapper;
import org.wallet.library.entity.UriFactory;

public class DidAuthorizeActivity extends BaseSettingsActivity {
    private static final String TAG = "author_test";

    private SwitchButton mNickNameSb;

    private SwitchButton mAddressSb;

    private Button mDenyBtn;

    private Button mAuthorizeBtn;

    @Override
    public int getLayoutId() {
        return R.layout.activity_author_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    private String mUri;
    private String packageName;
    private String activityCls;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(intent != null) {
            mUri = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.META_EXTRA);
            packageName = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.PACKAGE_NAME);
            activityCls = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.ACTIVITY_CLASS);

            Log.i(TAG, "server mUri: "+ mUri);
        }
        initView();
        initListener();
    }

    private void initView(){
        mNickNameSb = findViewById(R.id.nickname_switch_btn);
        mAddressSb = findViewById(R.id.receive_switch_btn);
        mDenyBtn = findViewById(R.id.deny_btn);
        mAuthorizeBtn = findViewById(R.id.authorize_btn);
    }

    private void initListener(){
        mDenyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mAuthorizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(StringUtil.isNullOrEmpty(pk)) {
                    Toast.makeText(DidAuthorizeActivity.this, "还未创建钱包", Toast.LENGTH_SHORT).show();
                    return;
                }

                UriFactory uriFactory = new UriFactory();
                uriFactory.parse(mUri);
                String did = uriFactory.getDID();
                String appId = uriFactory.getAppID();
                String sign = uriFactory.getSignature();
                String PK = uriFactory.getPublicKey();
                boolean isValid = AuthorizeManager.verify(DidAuthorizeActivity.this, did, PK, appId, sign);

                if(isValid){
                    String response = getResponseJson(isValid);
                    AuthorInfo authorInfo = new AuthorInfo();
                    authorInfo.setAppIcon("www.client.icon");
                    authorInfo.setAppName(packageName);
                    authorInfo.setDid(did);
                    authorInfo.setNickName("default");
                    authorInfo.setPK(PK);
                    authorInfo.setAuthorTime(System.currentTimeMillis()/1000);
                    DidDataSource.getInstance(DidAuthorizeActivity.this).putAuthorApp(authorInfo);

                    AuthorizeManager.startClientActivity(DidAuthorizeActivity.this, response, packageName, activityCls);
                }
                finish();
            }
        });
    }

    String pk;
    private String getResponseJson(boolean isValid){
        LoginResponse loginResponse = new LoginResponse();
        SignWrapper signWrapper = new SignWrapper();
        byte[] phrase;
        String mn;
        String did = null;
        String PK = null;
        String addr = null;
        try {
            phrase = BRKeyStore.getPhrase(this, 0);
            mn = new String(phrase);
            pk = Utility.getInstance(this).getSinglePrivateKey(mn);
            PK = Utility.getInstance(this).getSinglePublicKey(mn);
            did = Utility.getInstance(this).getDid(PK);
            addr = Utility.getInstance(this).getAddress(PK);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        if(isValid) {
            loginResponse.setStatus(200);
            loginResponse.setDID(did);
            loginResponse.setPublicKey(PK);
            loginResponse.setELAAddress(mAddressSb.isChecked()?addr:null);
            loginResponse.setNickName(mNickNameSb.isChecked()?BRSharedPrefs.getNickname(this):null);

            String loginJson = new Gson().toJson(loginResponse);
            String sign = AuthorizeManager.sign(this, pk, loginJson);
            signWrapper.setData(loginJson);
            signWrapper.setSign(sign);
        } else {
            loginResponse.setStatus(400);
            loginResponse.setMsg("参数错误");
            loginResponse.setDID(did);
            loginResponse.setPublicKey(PK);
            String loginJson = new Gson().toJson(loginResponse);
            String sign = AuthorizeManager.sign(this, pk, loginJson);
            signWrapper.setData(loginJson);
            signWrapper.setSign(sign);
        }
        return new Gson().toJson(signWrapper);
    }
}
