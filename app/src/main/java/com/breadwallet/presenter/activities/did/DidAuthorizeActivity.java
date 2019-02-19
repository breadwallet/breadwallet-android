package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.did.AuthorInfo;
import com.breadwallet.did.CallbackData;
import com.breadwallet.did.CallbackEntity;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.presenter.customviews.SwitchButton;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRDateUtil;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.Utility;
import com.google.gson.Gson;

import org.wallet.library.AuthorizeManager;
import org.wallet.library.Constants;
import org.wallet.library.entity.UriFactory;

import java.util.Calendar;
import java.util.Date;

public class DidAuthorizeActivity extends BaseSettingsActivity {
    private static final String TAG = "author_test";

    private SwitchButton mNickNameSb;

    private SwitchButton mAddressSb;

    private Button mDenyBtn;

    private Button mAuthorizeBtn;

    private BaseTextView mAppNameTv;

    private CheckBox mAuthorCbox;

    private BaseTextView mWillTv;

    @Override
    public int getLayoutId() {
        return R.layout.activity_author_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    private String mUri;

    private LoadingDialog mLoadingDialog;

    private UriFactory uriFactory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(intent != null) {
            String action = intent.getAction();
            if(!StringUtil.isNullOrEmpty(action) && action.equals(Intent.ACTION_VIEW)){
                Uri uri = intent.getData();
                Log.i(TAG, "server mUri: "+ uri.toString());
                mUri = uri.toString();
            } else {
                mUri = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.META_EXTRA);
            }
        }
//        mUri = "elaphant://identity?CallbackUrl=http%3A%2F%2Fdevsite.matrixyz.cn%2Fb.php%3Fidww%3D1&ReturnUrl=http%3A%2F%2Fdevsite.matrixyz.cn%2Fa.php%3Fida%3D1&Description=developerSite&AppID=223311&PublicKey=0290985E607823C7C8E1559588C5A07D62D2A0073EF042AE86EFDDCBA9687958D6&Signature=6E6ABE5B8733D8AEA09C6D813A50BF5DE7AF9E70446467CADE62FFB3D8AE5EF7013F01AC8232893701DA676D2EB13C992830025EE8BC1F35F7A75EF4C94A2B72&DID=ifjPYkMBfxFUes8DETpzRoM76bkVXg9DXv&RandomNumber=1548641817&AppName=developerSitaees";

        initView();
        initListener();

        mLoadingDialog = new LoadingDialog(this, R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);

        if(StringUtil.isNullOrEmpty(mUri)) return;
        uriFactory = new UriFactory();
        uriFactory.parse(mUri);

        mAppNameTv.setText(uriFactory.getAppName());
        mWillTv.setText(String.format(getString(R.string.Did_Will_Get), uriFactory.getAppName()));
        mAuthorCbox.setText(String.format(getString(R.string.Author_Auto_Check), uriFactory.getAppName()));

        Log.i("xidaokun", "did:"+uriFactory.getDID());
        boolean isAuto = BRSharedPrefs.isAuthorAuto(this, uriFactory.getDID());
        mAuthorCbox.setButtonDrawable(isAuto?R.drawable.ic_author_check:R.drawable.ic_author_uncheck);

        if(isAuto) author();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent != null) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_VIEW)){
                Uri uri = intent.getData();
                Log.i(TAG, "server mUri: "+ uri.toString());
                mUri = uri.toString();
            } else {
                mUri = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.META_EXTRA);
            }
        }
    }

    private void initView(){
        mAppNameTv = findViewById(R.id.app_name);
        mNickNameSb = findViewById(R.id.nickname_switch_btn);
        mAddressSb = findViewById(R.id.receive_switch_btn);
        mDenyBtn = findViewById(R.id.deny_btn);
        mAuthorizeBtn = findViewById(R.id.authorize_btn);
        mAuthorCbox = findViewById(R.id.auto_checkbox);
        mWillTv = findViewById(R.id.auth_info);
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
                author();
            }
        });
        mAuthorCbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(uriFactory == null) return;
                mAuthorCbox.setButtonDrawable(b?R.drawable.ic_author_check:R.drawable.ic_author_uncheck);
                BRSharedPrefs.setIsAuthorAuto(DidAuthorizeActivity.this, uriFactory.getDID(), b);
            }
        });
    }

    private void author(){
        String mn = getMn();
        if(StringUtil.isNullOrEmpty(mn)) {
            Toast.makeText(DidAuthorizeActivity.this, "还未创建钱包", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!mAddressSb.isChecked()){
            Toast.makeText(DidAuthorizeActivity.this, "需要获取public key", Toast.LENGTH_SHORT).show();
            return;
        }

        if(StringUtil.isNullOrEmpty(mUri)){
            Toast.makeText(DidAuthorizeActivity.this, "参数无效", Toast.LENGTH_SHORT).show();
            return;
        }

        final String did = uriFactory.getDID();
        String appId = uriFactory.getAppID();
        String sign = uriFactory.getSignature();
        String PK = uriFactory.getPublicKey();
        final String backurl = uriFactory.getCallbackUrl();
        final String returnUrl = uriFactory.getReturnUrl();
        boolean isValid = AuthorizeManager.verify(DidAuthorizeActivity.this, did, PK, appId, sign);

        if(isValid){
            cacheAuthorInfo(uriFactory);
            if(!StringUtil.isNullOrEmpty(backurl)){
                final CallbackEntity entity = new CallbackEntity();
                String pk = Utility.getInstance(DidAuthorizeActivity.this).getSinglePrivateKey(mn);
                String myPK = Utility.getInstance(DidAuthorizeActivity.this).getSinglePublicKey(mn);
                String myAddress = Utility.getInstance(DidAuthorizeActivity.this).getAddress(myPK);
                final String myDid = Utility.getInstance(DidAuthorizeActivity.this).getDid(myPK);
                CallbackData callbackData = new CallbackData();
                callbackData.NickName = BRSharedPrefs.getNickname(DidAuthorizeActivity.this);
                callbackData.ELAAddress = myAddress;
                entity.Data = new Gson().toJson(callbackData);
                entity.PublicKey = myPK;
                entity.Sign = AuthorizeManager.sign(DidAuthorizeActivity.this, pk, entity.Data);


                mLoadingDialog.show();
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        String ret = DidDataSource.getInstance(DidAuthorizeActivity.this).callBackUrl(backurl, entity);
                        if(ret != null) {
                            if(ret.contains("err code:")) {
                                dialogDismiss();
                            } else {
                                try {
                                    if(StringUtil.isNullOrEmpty(returnUrl) || returnUrl.equals("null")) {
                                        dialogDismiss();
                                        return;
                                    }
                                    Uri uri = Uri.parse(returnUrl+"&did="+myDid);
                                    Log.i("xidaokun", "did:"+uri.toString());
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    startActivity(intent);
                                    finish();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    dialogDismiss();
                                }
                            }
                        }
                    }
                });
            }

//                    AuthorizeManager.startClientActivity(DidAuthorizeActivity.this, response, packageName, activityCls);
        }
    }

    private void dialogDismiss(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingDialog.dismiss();
            }
        });
    }

    private void cacheAuthorInfo(UriFactory uriFactory){
        if(uriFactory == null) return;
        AuthorInfo info = new AuthorInfo();
        info.setAuthorTime(getAuthorTime(0));
        info.setPK(uriFactory.getPublicKey());
        info.setNickName(uriFactory.getAppName());
        info.setDid(uriFactory.getDID());
        info.setAppName(uriFactory.getAppName());
        info.setExpTime(getAuthorTime(30));
        info.setAppIcon("www.elstos.org");
        Log.i("xidaokun", "cache AuthorTime:"+getAuthorTime(0)+ " ExpTime:"+getAuthorTime(30));
        DidDataSource.getInstance(this).putAuthorApp(info);
    }

    private long getAuthorTime(int day){
        Date date = new Date();
        Calendar calendar  =   Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(calendar.DATE, day);
        date=calendar.getTime();
        long time = date.getTime();

        return time;
    }

    private String timeTest(long time){
        return BRDateUtil.getAuthorDate(time);
    }

    private String getMn(){
        byte[] phrase = null;
        try {
            phrase = BRKeyStore.getPhrase(this, 0);
            if(phrase != null) {
               return new String(phrase);
            }
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
