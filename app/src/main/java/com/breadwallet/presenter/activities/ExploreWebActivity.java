package com.breadwallet.presenter.activities;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.tools.util.StringUtil;

import org.wallet.library.AuthorizeManager;

public class ExploreWebActivity extends BRActivity {

    private WebView webView;
    private LoadingDialog mLoadingDialog;
    private BaseTextView mTitleTv;
    private BaseTextView mBackTv;
    private View mMenuLayout;
    private BaseTextView mAboutTv;
    private BaseTextView mCancelTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expolre_web_layout);

        initView();
        initListener();
    }

    private void initView(){
        webView = findViewById(R.id.web_view);
        mTitleTv = findViewById(R.id.explore_web_title);
        mBackTv = findViewById(R.id.explore_web_back);
        mMenuLayout = findViewById(R.id.explore_web_about_layout);
        mAboutTv = findViewById(R.id.explore_web_about);
        mCancelTv = findViewById(R.id.explore_web_cancle);
        webviewSetting();

        mLoadingDialog = new LoadingDialog(this, R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);
    }

    private void initListener(){
        findViewById(R.id.explore_web_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuLayout.setVisibility(View.VISIBLE);
            }
        });


        findViewById(R.id.explore_web_finish).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mBackTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(webView.canGoBack()) webView.goBack();
            }
        });

        mMenuLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        mCancelTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuLayout.setVisibility(View.GONE);
            }
        });
        mAboutTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuLayout.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        String url = getIntent().getStringExtra("explore_url");
        loadUrl(url);
    }

    private void webviewSetting() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadsImagesAutomatically(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(StringUtil.isNullOrEmpty(url)) return true;
                loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!isFinishing() && !mLoadingDialog.isShowing()){
                            mLoadingDialog.show();
                        }
                    }
                });
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                String title = view.getTitle();
                mTitleTv.setText(title);

                WebBackForwardList mWebBackForwardList = webView.copyBackForwardList();
                int account = mWebBackForwardList.getCurrentIndex();
                if(url.contains("redpacket")){
                    mBackTv.setVisibility((account>1)?View.VISIBLE:View.GONE);
                } else {
                    mBackTv.setVisibility((account>0)?View.VISIBLE:View.GONE);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!isFinishing() && mLoadingDialog.isShowing()){
                            mLoadingDialog.dismiss();
                        }
                    }
                });
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        });
    }

    private void loadUrl(String url){
        if(StringUtil.isNullOrEmpty(url)) return;
        if(url.startsWith("elaphant") && url.contains("identity")) {
            AuthorizeManager.startWalletActivity(ExploreWebActivity.this, url, "com.breadwallet.presenter.activities.did.DidAuthorizeActivity");
            finish();
        } else if(url.startsWith("elaphant") && url.contains("elapay")) {
            AuthorizeManager.startWalletActivity(ExploreWebActivity.this, url, "com.breadwallet.presenter.activities.WalletActivity");
            finish();
        } else {
            webView.loadUrl(url);
        }
    }
}
