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
import com.breadwallet.tools.manager.BRSharedPrefs;
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
    private CheckBox mAuthorCbox;

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
        mAuthorCbox = findViewById(R.id.auto_checkbox);
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
