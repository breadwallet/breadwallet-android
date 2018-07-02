package com.breadwallet.presenter.fragments;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.breadwallet.R;
import com.breadwallet.presenter.fragments.utils.ModalDialogFragment;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.util.Utils;
import com.platform.HTTPServer;

import static com.platform.HTTPServer.URL_SUPPORT;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentSupport extends ModalDialogFragment {
    private static final String TAG = FragmentSupport.class.getName();
    private WebView mWebView;
    private String mUrl;
    private String mOnCloseUrl;
    private ViewGroup mBackgroundLayout;
    private ViewGroup mSignalLayout;

    @SuppressLint("SetJavaScriptEnabled")
    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup rootView = assignRootView((ViewGroup) inflater.inflate(R.layout.fragment_support, container, false));
        mBackgroundLayout = assignBackgroundLayout((ViewGroup) rootView.findViewById(R.id.background_layout));
        mSignalLayout = assignSignalLayout((ViewGroup) rootView.findViewById(R.id.signal_layout));

        mSignalLayout.setOnTouchListener(new SlideDetector(getContext(), mSignalLayout));

        mSignalLayout.setLayoutTransition(UiUtils.getDefaultTransition());

        mWebView = rootView.findViewById(R.id.web_view);
        mWebView.setWebChromeClient(new BRWebChromeClient());
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + request.getUrl() + " | " + request.getMethod());
                if (mOnCloseUrl != null && request.getUrl().toString().equalsIgnoreCase(mOnCloseUrl)) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    mOnCloseUrl = null;
                } else if (request.getUrl().toString().contains("_close")) {
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    view.loadUrl(request.getUrl().toString());
                }

                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "onPageStarted: " + url);
                super.onPageStarted(view, url, favicon);
            }
        });

        mUrl = URL_SUPPORT;
        HTTPServer.setOnCloseListener(new HTTPServer.OnCloseListener() {
            @Override
            public void onClose() {
                closeWithAnimation();
                HTTPServer.setOnCloseListener(null);
            }
        });
        String articleId = getArguments() == null ? null : getArguments().getString("articleId");
        String walletIso = getArguments() == null ? null : getArguments().getString("walletIso");
        if (Utils.isNullOrEmpty(mUrl) || Utils.isNullOrEmpty(walletIso)) {
            throw new IllegalArgumentException("No articleId or walletIso extra! " + walletIso);
        }

        WebSettings webSettings = mWebView.getSettings();

        if (0 != (getActivity().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        if (!Utils.isNullOrEmpty(articleId)) {
            mUrl = mUrl + "/article?slug=" + articleId + "&currency=" + walletIso.toLowerCase();
        }

        Log.d(TAG, "onCreate: mUrl: " + mUrl + ", articleId: " + articleId);
        mWebView.loadUrl(mUrl);

        return rootView;
    }

    private class BRWebChromeClient extends WebChromeClient {
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.e(TAG, "onConsoleMessage: consoleMessage: " + consoleMessage.message());
            return super.onConsoleMessage(consoleMessage);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.e(TAG, "onJsAlert: " + message + ", url: " + url);
            return super.onJsAlert(view, url, message, result);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
        UiUtils.setIsSupportFragmentShown(false);
    }

}