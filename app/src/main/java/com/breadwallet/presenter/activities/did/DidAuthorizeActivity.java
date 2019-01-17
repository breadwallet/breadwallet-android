package com.breadwallet.presenter.activities.did;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.SwitchButton;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.util.CryptoUriParser;
import com.elastos.jni.Utility;
import com.google.gson.Gson;
import com.platform.tools.BRBitId;

import org.wallet.library.Constants;
import org.wallet.library.utils.HexUtils;

public class DidAuthorizeActivity extends BaseSettingsActivity {
    private static final String TAG = "author_test";

    private SwitchButton mNickNameSb;

    private SwitchButton mAddressSb;

    private Button mDenyBtn;

    private Button mAuthorizeBtn;

    private boolean mIsValid = false;



    @Override
    public int getLayoutId() {
        return R.layout.activity_author_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    private String json;
    private String packageName;
    private String activityCls;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(intent != null) {
            json = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.URI_EXTRA);
            packageName = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.PACKAGE_NAME);
            activityCls = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.ACTIVITY_CLASS);

            Log.i(TAG, "server json: "+json);
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
                mIsValid = verifySign(json);
                if(!mIsValid) return;

                SignWrapper signWrapper = getSignWrapper(json);
                final Entity entity = getEntity(signWrapper.content);

                BRSharedPrefs.putCurrentWalletIso(BreadApp.mContext, "ELA");
                if(entity.type == 0){//登录
                    String json = getResponseJson(0);
                    Intent intent = new Intent();
                    ComponentName componentName = new ComponentName(packageName, activityCls);
                    intent.putExtra(Constants.INTENT_EXTRA_KEY.URI_EXTRA, json);
                    intent.setComponent(componentName);
                    startActivityForResult(intent, RESULT_OK);
                    Log.i(TAG, "server login: "+json);
                } else {//支付

                    BRSharedPrefs.putCurrentWalletIso(BreadApp.mContext, "ELA");
//                    Intent newIntent = new Intent(BreadApp.mContext, WalletActivity.class);
//                    startActivity(newIntent);
//                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

                    UiUtils.showSendFragment(DidAuthorizeActivity.this, null);

                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            String result = "elastos:"+entity.rAddress+"?amount="+entity.amount;
                            Log.i(TAG, "server result: "+result);
                            if (CryptoUriParser.isCryptoUrl(DidAuthorizeActivity.this, result))
                                CryptoUriParser.processRequest(DidAuthorizeActivity.this, result,
                                        WalletsMaster.getInstance(DidAuthorizeActivity.this).getCurrentWallet(DidAuthorizeActivity.this));
                            else if (BRBitId.isBitId(result))
                                BRBitId.signBitID(DidAuthorizeActivity.this, result, null);
                            else
                                Log.e(TAG, "onActivityResult: not bitcoin address NOR bitID");
                        }
                    });

                    finish();
                }
            }
        });
    }

    static class SignWrapper {
        public String signed;
        public String content;
    }

    static class Entity {
        public int type; //0.登录 1.支付
        public String publicKey;
        public String message;
        public String didName;
        public String rAddress;
        public String nickName;
        public double amount;
    }

    private SignWrapper getSignWrapper(String json){
       return new Gson().fromJson(json, SignWrapper.class);
    }

    private Entity getEntity(String json){
        return new Gson().fromJson(json, Entity.class);
    }

    private boolean verifySign(String json){
        SignWrapper signWrapper = getSignWrapper(json);
        byte[] sign = HexUtils.hexToByteArray(signWrapper.signed);
        String content = signWrapper.content;
        Entity entity = getEntity(signWrapper.content);

        boolean isValid = Utility.getInstance(this).verify(entity.publicKey, content.getBytes(), sign);
        String did = Utility.getInstance(this).getDid(entity.publicKey);
        if(entity.didName.equals(did) && isValid){
            return true;
        }
        return false;
    }

    String pk;
    private String getResponseJson(int type){
        Entity entity = new Entity();
        byte[] phrase;
        String mn = null;
        try {
            phrase = BRKeyStore.getPhrase(this, 0);
            mn = new String(phrase);
            pk = Utility.getInstance(this).getSinglePrivateKey(mn);
            String PK = Utility.getInstance(this).getSinglePublicKey(mn);
            String did = Utility.getInstance(this).getDid(PK);

            entity.publicKey = PK;
            entity.didName = did;
            entity.message = "123456";
            entity.rAddress = Utility.getInstance(this).getAddress(PK);
            entity.type = type;
            entity.nickName = "elephant";
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        String entityJson = new Gson().toJson(entity);
        SignWrapper signWrapper = new SignWrapper();

        byte[] signed = Utility.getInstance(this).sign(pk, entityJson.getBytes());
        signWrapper.signed = HexUtils.bytesToHex(signed);
        signWrapper.content = entityJson;

        return new Gson().toJson(signWrapper);
    }
}
