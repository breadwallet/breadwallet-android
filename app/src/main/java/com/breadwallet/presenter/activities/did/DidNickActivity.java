package com.breadwallet.presenter.activities.did;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.tools.manager.BRSharedPrefs;

public class DidNickActivity extends BaseSettingsActivity {
    @Override
    public int getLayoutId() {
        return R.layout.activity_did_nick_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    private EditText mNicknameEdt;
    private TextView mCleanTv;
    private TextView mSaveTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initListener();

    }

    private void initView(){
        mNicknameEdt = findViewById(R.id.did_nickname_edt);
        mCleanTv = findViewById(R.id.did_nickname_clean);
        mSaveTv = findViewById(R.id.close_button);
        mNicknameEdt.setText(BRSharedPrefs.getNickname(this));
    }

    private void initListener(){
        mCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNicknameEdt.setText("");
            }
        });

        mSaveTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRSharedPrefs.putNickname(DidNickActivity.this, mNicknameEdt.getText().toString());
                finish();
            }
        });
    }

}
