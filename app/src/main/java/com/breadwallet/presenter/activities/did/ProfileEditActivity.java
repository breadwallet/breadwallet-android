package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

public class ProfileEditActivity extends BaseSettingsActivity {
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

    private EditText mAreaEdt;
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
        Log.i("ProfileFunction", "WHRER_FROM:"+WHRER_FROM);
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
        mAreaEdt = findViewById(R.id.did_mobile_area);

        mEmailEdt = findViewById(R.id.did_email_edt);
        mEmailCleanTv = findViewById(R.id.did_email_clean);

        mRealNameEdt = findViewById(R.id.did_realname_edt);
        mRealCleanTv = findViewById(R.id.did_realname_clean);

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
                mAreaEdt.setText("");
            }
        });
        mEmailCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEmailEdt.setText("");
            }
        });
        mRealCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRealNameEdt.setText("");
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
                if(!check(WHRER_FROM)) return;
                setEditResult(WHRER_FROM);
            }
        });

        mNicknameEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String input = mNicknameEdt.getText().toString();
                mNickCleanTv.setVisibility(StringUtil.isNullOrEmpty(input)?View.GONE:View.VISIBLE);
            }
        });

        mEmailEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String input = mEmailEdt.getText().toString();
                mEmailCleanTv.setVisibility(StringUtil.isNullOrEmpty(input)?View.GONE:View.VISIBLE);
            }
        });

        mMobileEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String input = mMobileEdt.getText().toString();
                mMobileCleanTv.setVisibility(StringUtil.isNullOrEmpty(input)?View.GONE:View.VISIBLE);
            }
        });

        mRealNameEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String input = mRealNameEdt.getText().toString();
                mRealCleanTv.setVisibility(StringUtil.isNullOrEmpty(input)?View.GONE:View.VISIBLE);
            }
        });

        mIDEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String input = mIDEdt.getText().toString();
                mIDCleanTv.setVisibility(StringUtil.isNullOrEmpty(input)?View.GONE:View.VISIBLE);
            }
        });

    }

    private boolean check(int from){
        boolean is = true;
        if(from == SettingsUtil.KYC_FROME_EMAIL){
            is = ValidatorUtil.isEmail(mEmailEdt.getText().toString());
            if(!is) Toast.makeText(this, getResources().getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
        } else if(from == SettingsUtil.KYC_FROME_MOBILE){
            is = ValidatorUtil.isMobile(mMobileEdt.getText().toString());
            if(!is) Toast.makeText(this, getResources().getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
        }

//        Log.i("ProfileFunction", "check validate");
//        boolean is = true;
//        if(from == SettingsUtil.KYC_FROME_MOBILE){
//            is = ValidatorUtil.isMobile(mMobileEdt.getText().toString());
//            if(!is) Toast.makeText(this, getResources().getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
//        } else if(from == SettingsUtil.KYC_FROME_EMAIL){
//            is = ValidatorUtil.isEmail(mEmailEdt.getText().toString());
//            if(!is) Toast.makeText(this, getResources().getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
//        }
//        else if(from == SettingsUtil.KYC_FROME_ID){
//            is = ValidatorUtil.isIDCard(mIDEdt.getText().toString());
//            if(!is) Toast.makeText(this, getResources().getString(R.string.invalid_id), Toast.LENGTH_SHORT).show();
//        }
//
//        return is;
        return is;
    }

    private void setEditResult(int from){
        Log.i("ProfileFunction", "setEditResult");
        Intent intent = null;
        switch (from) {
            case SettingsUtil.KYC_FROME_NICKNAME:
                String oNickname = BRSharedPrefs.getNickname(this);
                String nickname = mNicknameEdt.getText().toString();
                if(!nickname.equals(oNickname)){
                    intent = new Intent();
                    intent.putExtra("nickname", nickname);
                }
                break;
            case SettingsUtil.KYC_FROME_MOBILE:
                String oMobile = BRSharedPrefs.getMobile(this);
                String oArea = BRSharedPrefs.getArea(this);
                String mobile = mMobileEdt.getText().toString();
                String area = mAreaEdt.getText().toString();
                if(mobile.equals(oMobile) && area.equals(oArea)) break;
                intent = new Intent();
                intent.putExtra("mobile", mobile);
                intent.putExtra("area", area);
                break;
            case SettingsUtil.KYC_FROME_EMAIL:
                String oEmial = BRSharedPrefs.getEmail(this);
                String email = mEmailEdt.getText().toString();
                if(!email.equals(oEmial)){
                    intent = new Intent();
                    intent.putExtra("email", email);
                }
                break;
            case SettingsUtil.KYC_FROME_ID:
                String oRealname = BRSharedPrefs.getRealname(this);
                String realname = mRealNameEdt.getText().toString();
                String oIdcard = BRSharedPrefs.getID(this);
                String idcard = mIDEdt.getText().toString();
                if(realname.equals(oRealname) && idcard.equals(oIdcard)) break;
                intent = new Intent();
                intent.putExtra("realname", realname);
                intent.putExtra("idcard", idcard);
                break;
        }

        if(null != intent) {
            setResult(RESULT_OK, intent);
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
        String mobile  = mMobileEdt.getText().toString();
        mMobileCleanTv.setVisibility(StringUtil.isNullOrEmpty(mobile)?View.GONE:View.VISIBLE);
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
                mRealNameEdt.setText(BRSharedPrefs.getRealname(this));
                mIDEdt.setText(BRSharedPrefs.getID(this));
                break;
            case SettingsUtil.KYC_FROME_MOBILE:
                findViewById(R.id.did_mobile_layout).setVisibility(View.VISIBLE);
                mTitleTv.setText(R.string.My_Profile_Mobile);
                mAreaEdt.setText(BRSharedPrefs.getArea(this));
                mMobileEdt.setText(BRSharedPrefs.getMobile(this));
                break;
                default:
                    break;
        }
    }
}
