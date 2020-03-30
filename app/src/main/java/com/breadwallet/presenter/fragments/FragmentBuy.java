package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.breadwallet.R;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.Utils;

import java.util.Date;
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
    private static final String TAG = FragmentBuy.class.getName();
    public LinearLayout backgroundLayout;
    WebView webView;
    String baseUrl;
    public static boolean appVisible = false;
    private String onCloseUrl;
    public static String URL_BUY_LTC = "https://buy.loafwallet.org";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_buy, container, false);
        backgroundLayout = (LinearLayout) rootView.findViewById(R.id.background_layout);
        webView = (WebView) rootView.findViewById(R.id.web_view);
//https://www.tanelikorri.com/tutorial/android/communication-between-application-and-webview/
        webView.setWebChromeClient(new BRWebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + request.getUrl());
                Log.d(TAG, "shouldOverrideUrlLoading: " + request.getMethod());
                if (onCloseUrl != null && request.getUrl().toString().equalsIgnoreCase(onCloseUrl)) {
                    getActivity().onBackPressed();
                    onCloseUrl = null;
                } else if (request.getUrl().toString().contains("close")) {
                    getActivity().onBackPressed();
                } else {
                    view.loadUrl(request.getUrl().toString());
                }

                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "onPageStarted: " + url);
                super.onPageStarted(view, url, favicon);

//                if (url == "https://buy.laofwallet.org/close") {
//                    Utils.hideKeyboard(getActivity());
//                    BRAnimator.buyIsShowing = false;
//                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("TEST", url);
            }
        });

        webView.addJavascriptInterface(this, "Android          ");


        //"https://buy.loafwallet.org/?address=\(currentWalletAddress)&code=\(currencyCode)&idate=\(timestamp)&uid=\(uuidString)"
        //https://buy.loafwallet.org/?address=LRG6pZZbJAd62Y8fbnBypk6Qd38oiSNCBf&code=USD&idate=1578021500&uid=3955B06D-69BD-4CAD-95D1-B151BD3FA0EA
        baseUrl = URL_BUY_LTC;
        WebSettings webSettings = webView.getSettings();
        if (0 != (getActivity().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        String currentWalletAddress = BRSharedPrefs.getReceiveAddress(getContext());
        String currencyCode = "USD";
        Long timestamp = new Date().getTime();
        String uuidString = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        String buyUrl = baseUrl + "/?address=" +
                currentWalletAddress + "&code=" + currencyCode + "&idate=" + timestamp + "&uid=" + uuidString;
        Log.d("BASEURL", "onCreate: theUrl: " + buyUrl);
        webView.loadUrl(buyUrl);
        return rootView;
    }

    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(backgroundLayout, true);
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

    @JavascriptInterface
    public void handleMessage(String message) {
        Log.e(TAG, "handle message: " + message);
        //webkit.messageHandlers.callback.postMessage('${[encoded]}')

       //version=1&partner=litecoinfoundation&payment_flow_type=wallet&return_url=https%3A%2F%2Fbuy.loafwallet.org%2Fsuccess%2F&payment_id=b19e1e8b-3422-42f8-a68f-e7cbfedd2218&user_id=B7936F17-82E8-409D-9E80-BF24DC0FF4AE&destination_wallet%5Baddress%5D=LRG6pZZbJAd62Y8fbnBypk6Qd38oiSNCBf&destination_wallet%5Bcurrency%5D=LTC&fiat_total_amount%5Bamount%5D=274.95&fiat_total_amount%5Bcurrency%5D=USD&digital_total_amount%5Bamount%5D=5.45687994864113&digital_total_amount%5Bcurrency%5D=LTC&quote_id=ad207991-f5a8-4f5c-b75f-c9c138e67517
//        guard let response = message.body as? String else { return }
//        print(response)
//        guard let url = URL(string: "https://checkout.simplexcc.com/payments/new") else { return }
//
//        var req = URLRequest(url: url)
//        req.httpBody = Data(response.utf8)
//        req.httpMethod = "POST"

    }

    @JavascriptInterface
    public void postMessage(String json) {
        Log.d("TEST", json);
    }

    @JavascriptInterface
    public void onData(String value) {
        //.. do something with the data
    }



    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
        
    }

}