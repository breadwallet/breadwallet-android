package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.breadwallet.R;
import com.breadwallet.did.AuthorInfo;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
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
        mPublickeyTx = findViewById(R.id.pub_key);
        mDidTx = findViewById(R.id.did);
    }

    private void initData(){
        if(mAuthorInfo == null) return;
        mAppNameTx.setText(mAuthorInfo.getAppName());
        mAuthorTimeTx.setText(longToStr(mAuthorInfo.getAuthorTime()));
//
//        mDidTx.setText("- DID "+mAuthorInfo.getDid());
//        mPublickeyTx.setText(""mAuthorInfo.getPK());
    }

    public static String longToStr(long ms){
        Date date = new Date(ms);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return format.format(date);
    }



}
