package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.SettingsUtil;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.ValidatorUtil;

public class KYCEditActivity extends BaseSettingsActivity {
    @Override
    public int getLayoutId() {
        return R.layout.activity_did_nick_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }


    private TextView mTitleTv;
    private TextView mSaveTv;
    private EditText mNicknameEdt;
    private TextView mNickCleanTv;

    private EditText mEmailEdt;
    private TextView mEmailCleanTv;

    private TextView mAreaTv;
    private EditText mMobileEdt;
    private TextView mMobileCleanTv;

    private EditText mRealNameEdt;
    private TextView mRealCleanTv;

    private EditText mIDEdt;
    private TextView mIDCleanTv;

    private int WHRER_FROM;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initListener();
        WHRER_FROM = getIntent().getIntExtra(SettingsUtil.KYC_FROME_KEY, -1);
        selectView(WHRER_FROM);
    }

    private void initView() {
        mTitleTv = findViewById(R.id.title);
        mNicknameEdt = findViewById(R.id.did_nickname_edt);
        mNickCleanTv = findViewById(R.id.did_nickname_clean);
        mSaveTv = findViewById(R.id.close_button);
        mNicknameEdt.setText(BRSharedPrefs.getNickname(this));

        mMobileEdt = findViewById(R.id.did_mobile_edt);
        mMobileCleanTv = findViewById(R.id.did_mobile_clean);
        mAreaTv = findViewById(R.id.did_mobile_area);

        mEmailEdt = findViewById(R.id.did_email_edt);
        mEmailCleanTv = findViewById(R.id.did_email_clean);

        mIDEdt = findViewById(R.id.did_id_edt);
        mIDCleanTv = findViewById(R.id.did_id_clean);
    }

    private void initListener() {
        mNickCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNicknameEdt.setText("");
            }
        });
        mMobileCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMobileEdt.setText("");
            }
        });
        mEmailCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEmailEdt.setText("");
            }
        });
        mIDCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIDEdt.setText("");
            }
        });

        mSaveTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setEditResult(WHRER_FROM);
            }
        });
    }

    private boolean check(int from, String value){
        boolean is = true;
        if(from == SettingsUtil.KYC_FROME_MOBILE){
            is = ValidatorUtil.isMobile(value);
            if(!is) Toast.makeText(this, "手机号不合法", Toast.LENGTH_SHORT).show();
        } else if(from == SettingsUtil.KYC_FROME_EMAIL){
            is = ValidatorUtil.isEmail(value);
            if(!is) Toast.makeText(this, "邮箱不合法", Toast.LENGTH_SHORT).show();
        } else if(from == SettingsUtil.KYC_FROME_ID){
            is = ValidatorUtil.isIDCard(value);
            if(!is) Toast.makeText(this, "生份证不合法", Toast.LENGTH_SHORT).show();
        }

        return is;
    }

    private void setEditResult(int from){
        String oldValue = null;
        String newValue = null;
        switch (from) {
            case SettingsUtil.KYC_FROME_NICKNAME:
                oldValue = BRSharedPrefs.getNickname(this);
                newValue = mNicknameEdt.getText().toString();
                if(!StringUtil.isNullOrEmpty(newValue))BRSharedPrefs.putNickname(this, newValue);
                break;
            case SettingsUtil.KYC_FROME_MOBILE:
                oldValue = BRSharedPrefs.getMobile(this);
                newValue = mMobileEdt.getText().toString();
                if(!StringUtil.isNullOrEmpty(newValue))BRSharedPrefs.putMobile(this, newValue);
                break;
            case SettingsUtil.KYC_FROME_EMAIL:
                oldValue = BRSharedPrefs.getEmail(this);
                newValue = mEmailEdt.getText().toString();
                if(!StringUtil.isNullOrEmpty(newValue))BRSharedPrefs.putEmail(this, newValue);
                break;
            case SettingsUtil.KYC_FROME_ID:
                oldValue = BRSharedPrefs.getID(this);
                newValue = mIDEdt.getText().toString();
                if(!StringUtil.isNullOrEmpty(newValue))BRSharedPrefs.putID(this, newValue);
                break;
        }

        if(StringUtil.isNullOrEmpty(newValue)) return;
        if(!check(from, newValue)) return;
        if(oldValue!=null && !oldValue.equals(newValue)) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void selectView(int from) {
        findViewById(R.id.did_nickname_layout).setVisibility(View.GONE);
        findViewById(R.id.did_email_layout).setVisibility(View.GONE);
        findViewById(R.id.did_mobile_layout).setVisibility(View.GONE);
        findViewById(R.id.did_id_layout).setVisibility(View.GONE);
        switch (from) {
            case SettingsUtil.KYC_FROME_NICKNAME:
                findViewById(R.id.did_nickname_layout).setVisibility(View.VISIBLE);
                mTitleTv.setText(R.string.My_Profile_Nickname);
                mNicknameEdt.setText(BRSharedPrefs.getNickname(this));
                break;
            case SettingsUtil.KYC_FROME_EMAIL:
                findViewById(R.id.did_email_layout).setVisibility(View.VISIBLE);
                mTitleTv.setText(R.string.My_Profile_Email);
                mEmailEdt.setText(BRSharedPrefs.getEmail(this));
                break;
            case SettingsUtil.KYC_FROME_ID:
                findViewById(R.id.did_id_layout).setVisibility(View.VISIBLE);
                mTitleTv.setText(R.string.My_Profile_ID);
                mIDEdt.setText(BRSharedPrefs.getID(this));
                break;
            case SettingsUtil.KYC_FROME_MOBILE:
                findViewById(R.id.did_mobile_layout).setVisibility(View.VISIBLE);
                mTitleTv.setText(R.string.My_Profile_Mobile);
                mMobileEdt.setText(BRSharedPrefs.getMobile(this));
                break;
                default:
                    break;
        }
    }
}
