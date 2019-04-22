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
import android.widget.ListView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.did.AuthorInfo;
import com.breadwallet.did.CallbackData;
import com.breadwallet.did.CallbackEntity;
import com.breadwallet.did.ChineseIDCard;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.did.PhoneNumber;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.presenter.customviews.RoundImageView;
import com.breadwallet.presenter.entities.AuthorInfoItem;
import com.breadwallet.tools.adapter.AuthorInfoAdapter;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRDateUtil;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.Utility;
import com.google.gson.Gson;

import org.wallet.library.AuthorizeManager;
import org.wallet.library.Constants;
import org.wallet.library.entity.UriFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DidAuthorizeActivity extends BaseSettingsActivity {
    private static final String TAG = "author_test";

    private Button mDenyBtn;

    private Button mAuthorizeBtn;

    private BaseTextView mAppNameTv;

    private CheckBox mAuthorCbox;

    private BaseTextView mWillTv;

    private RoundImageView mAppIcon;

    private ListView mAuthorInfoLv;

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
        if (intent != null) {
            String action = intent.getAction();
            if (!StringUtil.isNullOrEmpty(action) && action.equals(Intent.ACTION_VIEW)) {
                Uri uri = intent.getData();
                Log.i(TAG, "server mUri: " + uri.toString());
                mUri = uri.toString();
            } else {
                mUri = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.META_EXTRA);
            }
        }
        initView();
        initListener();
        initData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_VIEW)) {
                Uri uri = intent.getData();
                Log.i(TAG, "server mUri: " + uri.toString());
                mUri = uri.toString();
            } else {
                mUri = intent.getStringExtra(Constants.INTENT_EXTRA_KEY.META_EXTRA);
            }
        }
    }

    private void initView() {
        mAppNameTv = findViewById(R.id.app_name);
        mDenyBtn = findViewById(R.id.deny_btn);
        mAuthorizeBtn = findViewById(R.id.authorize_btn);
        mAuthorCbox = findViewById(R.id.auto_checkbox);
        mWillTv = findViewById(R.id.auth_info);
        mAppIcon = findViewById(R.id.app_icon);
        mAuthorInfoLv = findViewById(R.id.author_info_list);
    }

    private void initData(){
        mLoadingDialog = new LoadingDialog(this, R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);

        if (StringUtil.isNullOrEmpty(mUri)) return;
        uriFactory = new UriFactory();
        uriFactory.parse(mUri);

        mAppNameTv.setText(uriFactory.getAppName());
        mWillTv.setText(String.format(getString(R.string.Did_Will_Get), uriFactory.getAppName()));
        mAuthorCbox.setText(String.format(getString(R.string.Author_Auto_Check), uriFactory.getAppName()));

        boolean isAuto = BRSharedPrefs.isAuthorAuto(this, uriFactory.getDID());
        mAuthorCbox.setButtonDrawable(isAuto ? R.drawable.ic_author_check : R.drawable.ic_author_uncheck);

        String appId = uriFactory.getAppID();
        int iconResourceId = getResources().getIdentifier("unknow", BRConstants.DRAWABLE, getPackageName());
        if(!StringUtil.isNullOrEmpty(appId)) {
            if(appId.equals(BRConstants.REA_PACKAGE_ID)){
                iconResourceId = getResources().getIdentifier("redpackage", BRConstants.DRAWABLE, getPackageName());
            } else if(appId.equals(BRConstants.DEVELOPER_WEBSITE)){
                iconResourceId = getResources().getIdentifier("developerweb", BRConstants.DRAWABLE, getPackageName());
            } else if(appId.equals(BRConstants.HASH_ID)){
                iconResourceId = getResources().getIdentifier("hash", BRConstants.DRAWABLE, getPackageName());
            }
        }
        mAppIcon.setImageDrawable(getDrawable(iconResourceId));

        List infos = createInfoList();
        AuthorInfoAdapter authorAdapter = new AuthorInfoAdapter(this, infos);
        mAuthorInfoLv.setAdapter(authorAdapter);

        if (isAuto) author();
    }

    AuthorInfoItem nickNameItem;
    AuthorInfoItem elaAddressItem;
    AuthorInfoItem btcAddressItem;
    AuthorInfoItem ethAddressItem;
    AuthorInfoItem bchAddressItem;
    AuthorInfoItem phoneNumberItem;
    AuthorInfoItem emailItem;
    AuthorInfoItem idcardItem;

    private List<AuthorInfoItem> createInfoList(){
        List<AuthorInfoItem> infos = new ArrayList<>();
        AuthorInfoItem didItem = new AuthorInfoItem(AuthorInfoItem.DID, getString(R.string.Did_Elastos_DID), "required");
        infos.add(didItem);
        AuthorInfoItem publicKeyItem = new AuthorInfoItem(AuthorInfoItem.PUBLIC_KEY, getString(R.string.Did_Public_Key), "required");
        infos.add(publicKeyItem);

        final String requestInfo = uriFactory.getRequestInfo();
        if(StringUtil.isNullOrEmpty(requestInfo)) return infos;
        if(requestInfo.contains("Nickname".toLowerCase())){
            nickNameItem = new AuthorInfoItem(AuthorInfoItem.NICK_NAME, getString(R.string.Did_Nick_Name), "check");
            infos.add(nickNameItem);
        }

        if(requestInfo.contains("ELAAddress".toLowerCase())){
            elaAddressItem = new AuthorInfoItem(AuthorInfoItem.ELA_ADDRESS, getString(R.string.Did_Ela_Address), "check");
            infos.add(elaAddressItem);
        }

        if(requestInfo.contains("Nickname".toLowerCase())){
            nickNameItem = new AuthorInfoItem(AuthorInfoItem.NICK_NAME, getString(R.string.Did_Nick_Name), "check");
            infos.add(nickNameItem);
        }

        if(requestInfo.contains("BTCAddress".toLowerCase())) {
            btcAddressItem = new AuthorInfoItem(AuthorInfoItem.BTC_ADDRESS, getString(R.string.Did_Btc_Address), "check");
            infos.add(btcAddressItem);
        }

        if(requestInfo.contains("ETHAddress".toLowerCase())){
            ethAddressItem = new AuthorInfoItem(AuthorInfoItem.ETH_ADDRESS, getString(R.string.Did_Eth_Address), "check");
            infos.add(ethAddressItem);
        }

        if(requestInfo.contains("BCHAddress".toLowerCase())){
            bchAddressItem = new AuthorInfoItem(AuthorInfoItem.BCH_ADDRESS, getString(R.string.Did_Bch_Address), "check");
            infos.add(bchAddressItem);
        }

        if(requestInfo.contains("PhoneNumber".toLowerCase())){
            phoneNumberItem = new AuthorInfoItem(AuthorInfoItem.PHONE_NUMBER, getString(R.string.Did_PhoneNumber), "check");
            infos.add(phoneNumberItem);
        }

        if(requestInfo.contains("Email".toLowerCase())){
            emailItem = new AuthorInfoItem(AuthorInfoItem.EMAIL, getString(R.string.Did_Email), "check");
            infos.add(emailItem);
        }

        if(requestInfo.contains("ChineseIDCard".toLowerCase())){
            idcardItem = new AuthorInfoItem(AuthorInfoItem.CHINESE_ID_CARD, getString(R.string.Did_Chinese_ID), "check");
            infos.add(idcardItem);
        }

        return infos;
    }

    private void initListener() {
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
                if (uriFactory == null) return;
                mAuthorCbox.setButtonDrawable(b ? R.drawable.ic_author_check : R.drawable.ic_author_uncheck);
                BRSharedPrefs.setIsAuthorAuto(DidAuthorizeActivity.this, uriFactory.getDID(), b);
            }
        });
    }

    private void author() {
        String mn = getMn();
        if (StringUtil.isNullOrEmpty(mn)) {
            Toast.makeText(DidAuthorizeActivity.this, "还未创建钱包", Toast.LENGTH_SHORT).show();
            return;
        }

        if (StringUtil.isNullOrEmpty(mUri)) {
            Toast.makeText(DidAuthorizeActivity.this, "参数无效", Toast.LENGTH_SHORT).show();
            return;
        }

        final String did = uriFactory.getDID();
        final String appId = uriFactory.getAppID();
        String sign = uriFactory.getSignature();
        String PK = uriFactory.getPublicKey();
        String randomNumber = uriFactory.getRandomNumber();

        final String backurl = uriFactory.getCallbackUrl();
        final String returnUrl = uriFactory.getReturnUrl();
        boolean isValid = AuthorizeManager.verify(DidAuthorizeActivity.this, did, PK, appId, sign);
        if (!isValid) {
            Toast.makeText(this, "invalid params", Toast.LENGTH_SHORT);
            finish();
        }

        if (isValid) {
            cacheAuthorInfo(uriFactory);
            final CallbackEntity entity = new CallbackEntity();
            String pk = Utility.getInstance(DidAuthorizeActivity.this).getSinglePrivateKey(mn);
            String myPK = Utility.getInstance(DidAuthorizeActivity.this).getSinglePublicKey(mn);
            final String myDid = Utility.getInstance(DidAuthorizeActivity.this).getDid(myPK);

            CallbackData callbackData = new CallbackData();
            //default
            callbackData.DID = myDid;
            callbackData.PublicKey = myPK;
            callbackData.RandomNumber = randomNumber;
            callbackData.PhoneNumber = new PhoneNumber();
            //request info
            callbackData.Nickname = (nickNameItem!=null)?nickNameItem.getValue(this)[0] : null;
            callbackData.ELAAddress = (elaAddressItem!=null)?elaAddressItem.getValue(this)[0] : null;
            callbackData.BTCAddress = (btcAddressItem!=null)?btcAddressItem.getValue(this)[0] : null;
            callbackData.ETHAddress = (ethAddressItem!=null)?ethAddressItem.getValue(this)[0] : null;
            callbackData.BCHAddress = (bchAddressItem!=null)?bchAddressItem.getValue(this)[0] : null;
            callbackData.Email = (emailItem!=null)?emailItem.getValue(this)[0] : null;
            if(phoneNumberItem != null){
                PhoneNumber phoneNumber = new PhoneNumber();
                phoneNumber.PhoneNumber = phoneNumberItem.getValue(this)[1];
                phoneNumber.CountryCode = phoneNumberItem.getValue(this)[0];
                callbackData.PhoneNumber = phoneNumber;
            }
            if(idcardItem != null){
                callbackData.ChineseIDCard = new ChineseIDCard();
                callbackData.ChineseIDCard.RealName = idcardItem.getValue(this)[0];
                callbackData.ChineseIDCard.IDNumber = idcardItem.getValue(this)[1];
            }

            entity.Data = new Gson().toJson(callbackData);
            entity.PublicKey = myPK;
            entity.Sign = AuthorizeManager.sign(DidAuthorizeActivity.this, pk, entity.Data);


            if (!isFinishing()) mLoadingDialog.show();
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(!StringUtil.isNullOrEmpty(backurl)){
                            String ret = DidDataSource.getInstance(DidAuthorizeActivity.this).callBackUrl(backurl, entity);
                            if ((StringUtil.isNullOrEmpty(ret) || StringUtil.isNullOrEmpty(ret) || ret.contains("err code:"))) {
                                toast("callback return error");
                            }
                        }

                        if (!StringUtil.isNullOrEmpty(returnUrl)) {
                            String url;
                            if (returnUrl.contains("?")) {
                                url = returnUrl + "&did=" + myDid + "&response=" + Uri.encode(new Gson().toJson(entity));
                            } else {
                                url = returnUrl + "?did=" + myDid + "&response=" + Uri.encode(new Gson().toJson(entity));
                            }

                            if(BRConstants.REA_PACKAGE_ID.equals(appId) || BRConstants.DPOS_VOTE_ID.equals(appId)){
                                UiUtils.startWebviewActivity(DidAuthorizeActivity.this, url);
                            } else {
                                UiUtils.openUrlByBrowser(DidAuthorizeActivity.this, url);
                            }

//                            if (returnUrl.contains("target=\"internal\"") || returnUrl.contains("target=internal")) {
//                                UiUtils.startWebviewActivity(DidAuthorizeActivity.this, url);
//                            } else {
//                                UiUtils.openUrlByBrowser(DidAuthorizeActivity.this, url);
//                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        dialogDismiss();
                        finish();
                    }
                }
            });
        }
    }

    private void toast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DidAuthorizeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dialogDismiss() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing())
                    mLoadingDialog.dismiss();
            }
        });
    }

    private void cacheAuthorInfo(UriFactory uriFactory) {
        if (uriFactory == null) return;
        AuthorInfo info = new AuthorInfo();
        info.setAuthorTime(getAuthorTime(0));
        info.setPK(uriFactory.getPublicKey());
        info.setAppId(uriFactory.getAppID());
        info.setNickName(uriFactory.getAppName());
        info.setDid(uriFactory.getDID());
        info.setAppName(uriFactory.getAppName());
        info.setExpTime(getAuthorTime(30));
        info.setAppIcon("www.elstos.org");
        info.setRequestInfo(uriFactory.getRequestInfo());
        DidDataSource.getInstance(this).putAuthorApp(info);
    }

    private long getAuthorTime(int day) {
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(calendar.DATE, day);
        date = calendar.getTime();
        long time = date.getTime();

        return time;
    }

    private String timeTest(long time) {
        return BRDateUtil.getAuthorDate(time);
    }

    private String getMn() {
        byte[] phrase = null;
        try {
            phrase = BRKeyStore.getPhrase(this, 0);
            if (phrase != null) {
                return new String(phrase);
            }
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
