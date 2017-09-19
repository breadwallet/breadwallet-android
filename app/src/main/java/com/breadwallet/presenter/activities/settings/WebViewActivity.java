package com.breadwallet.presenter.activities.settings;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.Utils;
import com.platform.BRHTTPHelper;
import com.platform.HTTPServer;
import com.platform.middlewares.plugins.LinkPlugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WebViewActivity extends BRActivity {
    private static final String TAG = WebViewActivity.class.getName();
    WebView webView;
    String theUrl;
    public static boolean appVisible = false;
    private static WebViewActivity app;
    private String onCloseUrl;

    public static WebViewActivity getApp() {
        return app;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

//        if (webView != null) webView.destroy();
        webView = (WebView) findViewById(R.id.web_view);
        webView.setBackgroundColor(0);
        webView.setWebChromeClient(new BRWebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + request.getUrl());
//                Log.d(TAG, "shouldOverrideUrlLoading: " + request.getMethod());
                if (onCloseUrl != null && request.getUrl().toString().equalsIgnoreCase(onCloseUrl)) {
                    onBackPressed();
                    onCloseUrl = null;
                }
                if (request.getUrl().toString().contains("_close")) {
                    onBackPressed();
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
//                Log.d(TAG, "onPageStarted: " + url);
                super.onPageStarted(view, url, favicon);
            }
        });

        theUrl = getIntent().getStringExtra("url");
        String json = getIntent().getStringExtra("json");
        setupServerMode(theUrl);
        String articleId = getIntent().getStringExtra("articleId");
        if (Utils.isNullOrEmpty(theUrl)) throw new IllegalArgumentException("No url extra!");

        WebSettings webSettings = webView.getSettings();

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        if (articleId != null && !articleId.isEmpty())
            theUrl = theUrl + "/" + articleId;

        Log.d(TAG, "onCreate: theUrl: " + theUrl + ", articleId: " + articleId);
        if (json != null) {
            request(webView, json);
        } else {
            webView.loadUrl(theUrl);
        }

        if (articleId != null && !articleId.isEmpty())
            navigate(articleId);
    }

    private void request(WebView webView, String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);

            String url = json.getString("url");
            String method = json.getString("method");
            String strBody = json.getString("body");
            String headers = json.getString("headers");
            String closeOn = json.getString("closeOn");
            if (Utils.isNullOrEmpty(url) || Utils.isNullOrEmpty(method) ||
                    Utils.isNullOrEmpty(strBody) || Utils.isNullOrEmpty(headers) || Utils.isNullOrEmpty(closeOn)) {
                Log.e(TAG, "request: not enough params: " + jsonString);
                return;
            }
            onCloseUrl = closeOn;

            Map<String, String> httpHeaders = new HashMap<>();
            JSONObject jsonHeaders = new JSONObject(headers);
            while (jsonHeaders.keys().hasNext()) {
                String key = jsonHeaders.keys().next();
                jsonHeaders.put(key, jsonHeaders.getString(key));
            }
            byte[] body = strBody.getBytes();

            if (method.equalsIgnoreCase("get")) {
                webView.loadUrl(url, httpHeaders);
            } else if (method.equalsIgnoreCase("post")) {
                webView.postUrl(url, body);//todo find a way to add the headers to the post request too
            } else {
                throw new NullPointerException("unexpected method: " + method);
            }
        } catch (JSONException e) {
            Log.e(TAG, "request: Failed to parse json or not enough params: " + jsonString);
            e.printStackTrace();
        }

    }

    private void navigate(String to) {
        String js = String.format("window.location = \'%s\';", to);
        webView.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.e(TAG, "onReceiveValue: " + value);
            }
        });
    }

    private void setupServerMode(String url) {
        if (url.equalsIgnoreCase(HTTPServer.URL_BUY)) {
            HTTPServer.mode = HTTPServer.ServerMode.BUY;
        } else if (url.equalsIgnoreCase(HTTPServer.URL_SUPPORT)) {
            HTTPServer.mode = HTTPServer.ServerMode.SUPPORT;
        } else if (url.equalsIgnoreCase(HTTPServer.URL_EA)) {
            HTTPServer.mode = HTTPServer.ServerMode.EA;
        } else {
            throw new RuntimeException("unknown url: " + url);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_up, R.anim.exit_to_bottom);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
        LinkPlugin.hasBrowser = false;
    }

}
