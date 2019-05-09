package com.breadwallet.presenter.activities.sign;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.util.StringUtil;

public class SignaureEditActivity extends BRActivity {

    private View mLimitLayout;
    private View mContentLayout;
    private BREdit mLimitEdt;
    private BaseTextView mContentTv;
    private BaseTextView mCloseTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_edit_layout);

        findView();
        initListener();
        initData();
    }

    private void findView(){
        mLimitLayout = findViewById(R.id.add_limitation_layout);
        mContentLayout = findViewById(R.id.content_detail);
        mLimitEdt = findViewById(R.id.add_limitation_edt);
        mContentTv = findViewById(R.id.content_detail);
        mCloseTv = findViewById(R.id.close_button);
    }

    private void initListener(){
        mCloseTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("purpose", mLimitEdt.getText());
                setResult(RESULT_OK, intent);
            }
        });
    }

    private void initData(){
        Intent intent = getIntent();
        if(null == intent) return;
        String from = intent.getStringExtra("from");
        String value = intent.getStringExtra("value");
        if(!StringUtil.isNullOrEmpty(from)) {
            if(from.equalsIgnoreCase("limit")){
                mLimitLayout.setVisibility(View.VISIBLE);
                mContentLayout.setVisibility(View.GONE);
                mCloseTv.setVisibility(View.VISIBLE);
                if(null != value) mLimitEdt.setText(value);
            } else if(from.equalsIgnoreCase("viewAll")){
                mLimitLayout.setVisibility(View.GONE);
                mContentLayout.setVisibility(View.VISIBLE);
                mCloseTv.setVisibility(View.GONE);
                if(null != value) mContentTv.setText(value);
            }
        }
    }

}
