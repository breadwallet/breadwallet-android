package com.breadwallet.presenter.activities.sign;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;

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
    private ImageButton mBackBtn;
    private BaseTextView mTitleTv;
    private BaseTextView mCleanTv;

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
        mBackBtn = findViewById(R.id.sign_back_button);
        mTitleTv = findViewById(R.id.title);
        mCleanTv = findViewById(R.id.add_limitation_clean);
    }

    private void initListener(){
        mCloseTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("purpose", mLimitEdt.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mCleanTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLimitEdt.setText("");
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
                mTitleTv.setText("Add limitation");
                mLimitLayout.setVisibility(View.VISIBLE);
                mContentLayout.setVisibility(View.GONE);
                mCloseTv.setVisibility(View.VISIBLE);
                if(null != value) mLimitEdt.setText(value);
            } else if(from.equalsIgnoreCase("viewAll")){
                mTitleTv.setText("Content");
                mLimitLayout.setVisibility(View.GONE);
                mCloseTv.setVisibility(View.GONE);
                mContentLayout.setVisibility(View.VISIBLE);
                if(null != value) mContentTv.setText(value);
            }
        }
    }

}
