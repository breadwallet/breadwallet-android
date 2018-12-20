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

public class FragmentWebModal extends ModalDialogFragment {
    private static final String TAG = FragmentWebModal.class.getName();
    private String mOnCloseUrl;
    public static final String EXTRA_URL = "com.breadwallet.presenter.fragments.FragmentWebModal.URL";
    public static final String CLOSE = "_close";

    @SuppressLint("SetJavaScriptEnabled")
    @Override

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup rootView = assignRootView((ViewGroup) inflater.inflate(R.layout.fragment_support, container, false));
        ViewGroup signalLayout = assignSignalLayout((ViewGroup) rootView.findViewById(R.id.signal_layout));
        assignBackgroundLayout((ViewGroup) rootView.findViewById(R.id.background_layout));

        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        signalLayout.setLayoutTransition(UiUtils.getDefaultTransition());

        WebView webView = rootView.findViewById(R.id.web_view);
        webView.setWebChromeClient(new BRWebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + request.getUrl() + " | " + request.getMethod());
                if (mOnCloseUrl != null && request.getUrl().toString().equalsIgnoreCase(mOnCloseUrl)) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    mOnCloseUrl = null;
                } else if (request.getUrl().toString().contains(CLOSE)) {
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

        String url = getArguments() == null ? null : getArguments().getString(EXTRA_URL);
        if (Utils.isNullOrEmpty(url)) {
            throw new IllegalArgumentException("No url provided!");
        }

        WebSettings webSettings = webView.getSettings();

        if ((getActivity().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        HTTPServer.setOnCloseListener(new HTTPServer.OnCloseListener() {
            @Override
            public void onClose() {
                closeWithAnimation();
                HTTPServer.setOnCloseListener(null);
            }
        });


        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl(url);

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