package com.breadwallet.presenter.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.Utils;

import java.util.Date;

import timber.log.Timber;

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

public class FragmentBuy extends Fragment {
    private static final int FILE_CHOOSER_REQUEST_CODE = 15423;
    public LinearLayout backgroundLayout;
    private ProgressBar progress;
    private WebView webView;
    private String onCloseUrl;
    private static final String URL_BUY_LTC = BuildConfig.DEBUG ? "https://api-stage.lite-wallet.org" : "https://api-prod.lite-wallet.org";
    static final String CURRENCY_KEY = "currency_code_key";
    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;

    public static Fragment newInstance(String currency) {
        Bundle bundle = new Bundle();
        bundle.putString(CURRENCY_KEY, currency);
        Fragment fragment = new FragmentBuy();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    closePayment();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_buy, container, false);
        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> closePayment());
        backgroundLayout = rootView.findViewById(R.id.background_layout);
        progress = rootView.findViewById(R.id.progress);
        webView = rootView.findViewById(R.id.web_view);
        webView.setWebChromeClient(mWebChromeClient);
        webView.setWebViewClient(mWebViewClient);

        WebSettings webSettings = webView.getSettings();
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        String walletAddress = BRSharedPrefs.getReceiveAddress(getContext());
        String currency = getArguments().getString(CURRENCY_KEY);
        Long timestamp = new Date().getTime();
        String uuid = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String buyUrl = url(walletAddress, currency, timestamp, uuid);
        Timber.d("URL %s", buyUrl);
        webView.loadUrl(buyUrl);
        return rootView;
    }

    private String url(Object... args) {
        return String.format(URL_BUY_LTC + "?address=%s&code=%s&idate=%s&uid=%s", args);
    }

    private void closePayment() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(backgroundLayout, true);
    }

    private WebChromeClient mWebChromeClient = new WebChromeClient() {
        // For Android API < 11 (3.0 OS)
        public void openFileChooser(ValueCallback<Uri> valueCallback) {
            uploadMessage = valueCallback;
            openImageChooserActivity();
        }

        // For Android API >= 11 (3.0 OS)
        public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
            uploadMessage = valueCallback;
            openImageChooserActivity();
        }

        // For Android API >= 21 (5.0 OS)
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            uploadMessageAboveL = filePathCallback;
            openImageChooserActivity();
            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progress.setProgress(newProgress, true);
            } else progress.setProgress(newProgress);
        }
    };

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Timber.d("shouldOverrideUrlLoading: URL=%s\nMethod=%s", url, request.getMethod());
            if (url.equalsIgnoreCase(onCloseUrl)) {
                closePayment();
                onCloseUrl = null;
            } else if (url.contains("close")) {
                closePayment();
            } else {
                view.loadUrl(url);
            }

            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Timber.d("onPageStarted: %s", url);
            super.onPageStarted(view, url, favicon);
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Timber.d("onPageFinished %s", url);
            progress.setVisibility(View.GONE);
        }
    };

    private void openImageChooserActivity() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Image Chooser"), FILE_CHOOSER_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            if (uploadMessageAboveL != null) {
                Uri[] results = getResultAboveL(resultCode, data);
                uploadMessageAboveL.onReceiveValue(results);
            } else if (uploadMessage != null) {
                Uri result = data != null && resultCode == Activity.RESULT_OK ? data.getData() : null;
                uploadMessage.onReceiveValue(result);
            }
            uploadMessageAboveL = null;
            uploadMessage = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Uri[] getResultAboveL(int resultCode, Intent intent) {
        Uri[] results = null;
        if (intent != null && resultCode == Activity.RESULT_OK) {
            String dataString = intent.getDataString();
            ClipData clipData = intent.getClipData();
            if (clipData != null) {
                results = new Uri[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    results[i] = item.getUri();
                }
            } else if (dataString != null) {
                results = new Uri[]{Uri.parse(dataString)};
            }
        }
        return results;
    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
    }
}