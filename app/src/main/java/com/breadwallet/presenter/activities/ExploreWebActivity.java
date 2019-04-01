package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.tools.util.StringUtil;

import org.wallet.library.AuthorizeManager;

public class ExploreWebActivity extends BRActivity {

    private WebView webView;
    private LoadingDialog mLoadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expolre_web_layout);

        webView = findViewById(R.id.web_view);
        webviewSetting();

        mLoadingDialog = new LoadingDialog(this, R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        String url = getIntent().getStringExtra("explore_url");
        Log.i("loadUrl", "onResume url:"+url);
        loadUrl(url);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String url = getIntent().getStringExtra("explore_url");
        Log.i("loadUrl", "onNewIntent url:"+url);
//        loadUrl(url);
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
                Log.i("loadUrl", "shouldOverrideUrl url:"+url);
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
        Log.i("loadUrl", "url:"+url);
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
