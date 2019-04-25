package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.breadwallet.R;
import com.breadwallet.did.AuthorInfo;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.MaxHeightLv;
import com.breadwallet.presenter.customviews.RoundImageView;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRDateUtil;
import com.breadwallet.tools.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class DidDetailActivity extends BaseSettingsActivity {
    @Override
    public int getLayoutId() {
        return R.layout.activity_did_detail_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    private AuthorInfo mAuthorInfo;
    private BaseTextView mAppNameTx;
    private BaseTextView mAuthorTimeTx;
    private BaseTextView mExpTimeTx;
    private CheckBox mAuthorCbox;
    private RoundImageView mAppIcon;
    private MaxHeightLv mAuthInfoLv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String did = intent.getStringExtra("did");
        String appId = intent.getStringExtra("appId");
        if(!StringUtil.isNullOrEmpty(did)) {
            mAuthorInfo = DidDataSource.getInstance(this).getInfoByDid(did);
        }
        initView();
        initData();
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
    }

    private void initView() {
        mAppNameTx = findViewById(R.id.app_name);
        mAuthorTimeTx = findViewById(R.id.auth_time);
        mExpTimeTx = findViewById(R.id.expiration);
        mAuthorCbox = findViewById(R.id.auto_checkbox);
        mAppIcon = findViewById(R.id.app_icon);
        mAuthInfoLv = findViewById(R.id.author_info_detail_list);
        boolean isAuto = BRSharedPrefs.isAuthorAuto(this, mAuthorInfo.getDid());
        mAuthorCbox.setButtonDrawable(isAuto?R.drawable.ic_author_check:R.drawable.ic_author_uncheck);

        mAuthorCbox.setText(String.format(getString(R.string.Author_Auto_Check), mAuthorInfo.getAppName()));
        mAuthorCbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(mAuthorInfo == null) return;
                mAuthorCbox.setButtonDrawable(b?R.drawable.ic_author_check:R.drawable.ic_author_uncheck);
                BRSharedPrefs.setIsAuthorAuto(DidDetailActivity.this, mAuthorInfo.getDid(), b);
            }
        });
    }

    private void initData(){
        if(mAuthorInfo == null) return;
        mAppNameTx.setText(mAuthorInfo.getAppName());
        mAuthorTimeTx.setText(BRDateUtil.getAuthorDate(mAuthorInfo.getAuthorTime()==0 ? System.currentTimeMillis() : (mAuthorInfo.getAuthorTime())));
        mExpTimeTx.setText(BRDateUtil.getAuthorDate(mAuthorInfo.getExpTime()==0 ? System.currentTimeMillis() : (mAuthorInfo.getExpTime())));

        List<String> infoSb = new ArrayList<>();
        infoSb.add(getString(R.string.Did_Detail_DID));
        infoSb.add(getString(R.string.Did_Detail_Public_Key));

        String requestInfo = BRSharedPrefs.getRequestInfo(this);
        if(!StringUtil.isNullOrEmpty(requestInfo)) {
            if(requestInfo.contains("Nickname".toLowerCase())){
                infoSb.add(getString(R.string.Did_Detail_Nick_Name));
            }
            if(requestInfo.contains("ELAAddress".toLowerCase())){
                infoSb.add(getString(R.string.Did_Detail_Ela_Address));
            }
            if(requestInfo.contains("BTCAddress".toLowerCase())){
                infoSb.add(getString(R.string.Did_Detail_Btc_Address));
            }
            if(requestInfo.contains("ETHAddress".toLowerCase())){
                infoSb.add(getString(R.string.Did_Detail_Eth_Address));
            }
            if(requestInfo.contains("BCHAddress".toLowerCase())){
                infoSb.add(getString(R.string.Did_Detail_Bch_Address));
            }
            if(requestInfo.contains("IOEXAddress".toLowerCase())){
                infoSb.add(getString(R.string.Did_Detail_Ioex_Address));
            }
            if(requestInfo.contains("PhoneNumber".toLowerCase())){
                infoSb.add(getString(R.string.Did_Detail_Phone_Number));
            }
            if(requestInfo.contains("Email".toLowerCase())){
                infoSb.add(getString(R.string.Did_Detail_Email));
            }
            if(requestInfo.contains("ChineseIDCard".toLowerCase())){
                infoSb.add(getString(R.string.Did_Detail_Chinese_Id_Card));
            }
        }

        mAuthInfoLv.setAdapter(new AuthorDetailAdapter(this, infoSb));
    }




}
