package com.breadwallet.presenter.activities.did;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;

public class DidAuthListActivity extends BaseSettingsActivity {

    private ListView mAuthorList;


    @Override
    public int getLayoutId() {
        return R.layout.activity_did_auth_list_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initListener();
    }

    private void initView(){
        mAuthorList = findViewById(R.id.author_app_list);
        mAuthorList.setAdapter(new AuthorAdapter(this));
    }

    private void initListener(){
        mAuthorList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(DidAuthListActivity.this, DidDetailActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });
    }
}
