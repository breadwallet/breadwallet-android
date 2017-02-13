
package com.breadwallet.presenter.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
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
import com.platform.HTTPServer;

import static com.breadwallet.R.color.red;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
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

public class FragmentWebView extends Fragment {
    public static final String TAG = FragmentWebView.class.getName();

    private int mode = 0; // default 0 - EA, 1 - buy bitcoin

    WebView webView;
    String theUrl = HTTPServer.URL_EA;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(
                R.layout.fragment_early_access, container, false);
        if (webView != null) webView.destroy();
        webView = (WebView) rootView.findViewById(R.id.early_access_web_view);
        webView.setWebChromeClient(new BRWebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.e(TAG, "shouldOverrideUrlLoading: " + request.getUrl());
                Log.e(TAG, "shouldOverrideUrlLoading: " + request.getMethod());
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.e(TAG, "onPageStarted: " + url);
                super.onPageStarted(view, url, favicon);
            }
        });

        WebSettings webSettings = webView.getSettings();
        if (mode == 1) theUrl = HTTPServer.URL_BUY_BITCOIN;
        if (0 != (getActivity().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl(theUrl);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
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

//    private class BRWebViewClient extends WebViewClient {
//
//        @Override
//        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//            return super.shouldInterceptRequest(view, request);
//        }
//
//        @Override
//        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
//            super.onReceivedError(view, request, error);
//            Log.e(TAG, "onReceivedError: error:" + error.toString());
//        }
//
//        @Override
//        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
//            super.onReceivedHttpAuthRequest(view, handler, host, realm);
//            Log.e(TAG, "onReceivedHttpAuthRequest: ");
//        }
//
//        @Override
//        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
//            super.onReceivedError(view, errorCode, description, failingUrl);
//            Log.e(TAG, "onReceivedError: failingUrl: " + failingUrl);
//        }
//
//        @Override
//        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//            super.onReceivedSslError(view, handler, error);
//            Log.e(TAG, "onReceivedSslError: error: " + error);
//        }
//
//        @Override
//        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
//            super.onReceivedHttpError(view, request, errorResponse);
//
//            Log.e(TAG, String.format("ERROR: onReceivedHttpError: (%s) %s %s %d", request.getMethod(), request.getUrl(),
//                    errorResponse.getReasonPhrase(), errorResponse.getStatusCode()));
//        }
//
//
//        @Override
//        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
//            return super.shouldInterceptRequest(view, url);
//        }
//
//    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
