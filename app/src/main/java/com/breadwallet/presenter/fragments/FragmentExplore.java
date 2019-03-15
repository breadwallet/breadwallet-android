package com.breadwallet.presenter.fragments;

import android.content.Intent;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ExploreWebActivity;
import com.breadwallet.presenter.activities.did.KYCEditActivity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.util.BRConstants;

public class FragmentExplore extends Fragment {

    public static FragmentExplore newInstance(String text) {

        FragmentExplore f = new FragmentExplore();
        Bundle b = new Bundle();
        b.putString("text", text);

        f.setArguments(b);

        return f;
    }

    private View mBannerview1;
    private View mBannerview2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explore_layout, container, false);
        initView(rootView);
        initListener();
        return rootView;
    }


    private void initView(View rootView){
        mBannerview1 = rootView.findViewById(R.id.explore_banner1);
        mBannerview2 = rootView.findViewById(R.id.explore_banner2);
    }

    private void initListener(){
        mBannerview1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), ExploreWebActivity.class);
                intent.putExtra("explore_url", "http://aiyong.dafysz.cn/sale-m/18090500-zq.html#/insurance-source_bxfx?source=bxfx_yly");
                startActivityForResult(intent, BRConstants.PROFILE_REQUEST_MOBILE);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        mBannerview2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), ExploreWebActivity.class);
                intent.putExtra("explore_url", "https://redpacket.elastos.org");
                startActivityForResult(intent, BRConstants.PROFILE_REQUEST_MOBILE);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

//                UiUtils.openUrlByBrowser(getActivity(), url);
            }
        });
    }
}
