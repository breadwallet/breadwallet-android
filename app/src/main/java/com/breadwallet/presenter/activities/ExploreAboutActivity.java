package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.StringUtil;

public class ExploreAboutActivity extends BaseSettingsActivity {

    private TextView mDes1;
    private TextView mDes2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
    }

    private void initView(){
        mDes1 = findViewById(R.id.explore_about_desc1);
        mDes2 = findViewById(R.id.explore_about_desc2);
    }

    private void initData(){
        String from = BRSharedPrefs.getExploreFrom(this);
        if(StringUtil.isNullOrEmpty(from)) return;
        if(from.equalsIgnoreCase("vote")){
            mDes1.setText(getString(R.string.explore_vote_about_desc1));
            mDes2.setText(getString(R.string.explore_vote_about_desc2));
        } else if(from.equalsIgnoreCase("redpacket")){
            mDes1.setText(getString(R.string.redpackage_hint1));
            mDes2.setText(getString(R.string.redpackage_hint2));
        } else if(from.equalsIgnoreCase("exchange")){
            mDes1.setText(getString(R.string.exchange_hint1));
            mDes2.setText(getString(R.string.exchange_hint2));
        } else {
            mDes1.setText("");
            mDes2.setText("");
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_explore_about_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }
}
