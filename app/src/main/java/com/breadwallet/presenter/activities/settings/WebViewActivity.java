package com.breadwallet.presenter.activities.settings;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
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
import com.platform.HTTPServer;
import com.platform.middlewares.plugins.LinkPlugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import timber.log.Timber;

public class WebViewActivity extends BRActivity {
    public static final String URL_EXTRA = "url";
    public static final String JSON_EXTRA = "json";
    public static final String ARTICLE_ID_EXTRA = "articleId";

    WebView webView;
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

        webView = findViewById(R.id.web_view);
        webView.setBackgroundColor(0);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Timber.d("shouldOverrideUrlLoading: %s", request.getUrl());
                if ((onCloseUrl != null && request.getUrl().toString().equalsIgnoreCase(onCloseUrl)) || request.getUrl().toString().contains("_close")) {
                    onBackPressed();
                    onCloseUrl = null;
                    return true;
                }
                return false;
            }
        });

        String theUrl = getIntent().getStringExtra(URL_EXTRA);
        String json = getIntent().getStringExtra(JSON_EXTRA);
        if (!setupServerMode(theUrl)) {
            webView.loadUrl(theUrl);
            return;
        }
        String articleId = getIntent().getStringExtra(ARTICLE_ID_EXTRA);
        if (Utils.isNullOrEmpty(theUrl)) {
            throw new IllegalArgumentException("No url extra!");
        }

        WebSettings webSettings = webView.getSettings();

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        boolean hasArticle = articleId != null && !articleId.isEmpty();
        if (hasArticle) {
            theUrl = theUrl + "/" + articleId;
        }

        if (json != null) {
            request(webView, json);
        } else {
            webView.loadUrl(theUrl);
        }

        if (hasArticle) {
            navigate(articleId);
        }
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
                Timber.d("request: not enough params: %s", jsonString);
                return;
            }
            onCloseUrl = closeOn;

            JSONObject jsonHeaders = new JSONObject(headers);
            while (jsonHeaders.keys().hasNext()) {
                String key = jsonHeaders.keys().next();
                jsonHeaders.put(key, jsonHeaders.getString(key));
            }
            byte[] body = strBody.getBytes();

            if (method.equalsIgnoreCase("get")) {
                webView.loadUrl(url, new HashMap<String, String>());
            } else if (method.equalsIgnoreCase("post")) {
                webView.postUrl(url, body); // TODO: find a way to add the headers to the post request too
            } else {
                throw new NullPointerException("unexpected method: " + method);
            }
        } catch (JSONException e) {
            Timber.e(e, "request: Failed to parse json or not enough params: %s", jsonString);
        }
    }

    private void navigate(String to) {
        String js = String.format("window.location = \'%s\';", to);
        webView.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Timber.d("onReceiveValue: %s", value);
            }
        });
    }

    private boolean setupServerMode(String url) {
        if (url.equalsIgnoreCase(HTTPServer.URL_BUY)) {
            HTTPServer.mode = HTTPServer.ServerMode.BUY;
        } else if (url.equalsIgnoreCase(HTTPServer.URL_SUPPORT)) {
            HTTPServer.mode = HTTPServer.ServerMode.SUPPORT;
        } else if (url.equalsIgnoreCase(HTTPServer.URL_EA)) {
            HTTPServer.mode = HTTPServer.ServerMode.EA;
        } else {
            Timber.d("setupServerMode: unknown url: %s", url);
            return false;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public void onBackPressed() {
        if (ActivityUTILS.isLast(this)) {
            BRAnimator.startBreadActivity(this, false);
        } else {
            super.onBackPressed();
        }
        overridePendingTransition(R.anim.fade_up, R.anim.exit_to_bottom);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
        LinkPlugin.hasBrowser = false;
    }
}
