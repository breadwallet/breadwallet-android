package com.breadwallet.presenter.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ExploreWebActivity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.StringUtil;

import java.util.Locale;

public class FragmentExplore extends Fragment {

    public static FragmentExplore newInstance(String text) {

        FragmentExplore f = new FragmentExplore();
        Bundle b = new Bundle();
        b.putString("text", text);

        f.setArguments(b);

        return f;
    }

    private View mDisclaimLayout;
    private View mBannerview1;
    private View mBannerview2;
    private View mBannerview3;
    private View mOkBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explore_layout, container, false);
        initView(rootView);
        initListener();
        if(BRSharedPrefs.getDisclaimShow(getContext())) mDisclaimLayout.setVisibility(View.VISIBLE);
        return rootView;
    }


    private void initView(View rootView){
        mDisclaimLayout = rootView.findViewById(R.id.disclaim_layout);
        mBannerview1 = rootView.findViewById(R.id.explore_banner1);
        mBannerview2 = rootView.findViewById(R.id.explore_banner2);
        mBannerview3 = rootView.findViewById(R.id.explore_banner3);
        mOkBtn = rootView.findViewById(R.id.disclaim_ok_btn);
    }

    @Override
    public void onResume() {
        super.onResume();
        String languageCode = Locale.getDefault().getLanguage();
        if(!StringUtil.isNullOrEmpty(languageCode)){
            mBannerview1.setBackgroundResource(languageCode.contains("en")? (R.drawable.explore_banner1_en): (R.drawable.explore_banner1_zh));
            mBannerview2.setBackgroundResource(languageCode.contains("en")? (R.drawable.explore_banner2_en): (R.drawable.explore_banner2_zh));
            mBannerview3.setBackgroundResource(languageCode.contains("en")? (R.drawable.explore_banner3_en): (R.drawable.explore_banner3_zh));
        } else {
            mBannerview1.setBackgroundResource(R.drawable.explore_banner1_en);
            mBannerview2.setBackgroundResource(R.drawable.explore_banner2_en);
            mBannerview3.setBackgroundResource(R.drawable.explore_banner3_en);
        }
    }

    private void initListener(){
        mBannerview1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiUtils.startWebviewActivity(getContext(), "http://aiyong.dafysz.cn/sale-m/18090500-zq.html#/insurance-source_bxfx?source=bxfx_yly");
            }
        });

        mBannerview2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiUtils.startWebviewActivity(getContext(), "https://redpacket.elastos.org");
            }
        });
        mOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDisclaimLayout.setVisibility(View.GONE);
                BRSharedPrefs.setDisclaimshow(getContext(), false);
            }
        });
        mDisclaimLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
    }
}
