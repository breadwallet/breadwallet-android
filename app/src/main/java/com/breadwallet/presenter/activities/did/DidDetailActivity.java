package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    }




}
