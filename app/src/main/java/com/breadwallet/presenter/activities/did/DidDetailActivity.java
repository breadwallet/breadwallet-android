package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.did.AuthorInfo;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
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
    private BaseTextView mNicknameTx;
    private BaseTextView mPublickeyTx;
    private BaseTextView mDidTx;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String did = intent.getStringExtra("did");
        if(!StringUtil.isNullOrEmpty(did)) {
            mAuthorInfo = DidDataSource.getInstance(this).getInfoByDid(did);
        }
        initView();
        initData();
    }

    private void initView() {
        mAppNameTx = findViewById(R.id.app_name);
        mAuthorTimeTx = findViewById(R.id.auth_time);
        mNicknameTx = findViewById(R.id.nick_name);
        mExpTimeTx = findViewById(R.id.expiration);
        mPublickeyTx = findViewById(R.id.pub_key);
        mDidTx = findViewById(R.id.did);
    }

    private void initData(){
        if(mAuthorInfo == null) return;
        Log.i("xidaokun", "authorTime:"+mAuthorInfo.getAuthorTime());
        Log.i("xidaokun", "expTime:"+mAuthorInfo.getExpTime());
        mAppNameTx.setText(mAuthorInfo.getAppName());
        mAuthorTimeTx.setText(BRDateUtil.getAuthorDate(mAuthorInfo.getAuthorTime()==0 ? System.currentTimeMillis() : (mAuthorInfo.getAuthorTime())));
        mExpTimeTx.setText(BRDateUtil.getAuthorDate(mAuthorInfo.getExpTime()==0 ? System.currentTimeMillis() : (mAuthorInfo.getExpTime())));
    }




}
