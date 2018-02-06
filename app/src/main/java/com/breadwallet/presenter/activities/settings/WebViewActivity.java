package com.breadwallet.presenter.activities.settings;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toolbar;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.Utils;
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

    private Toolbar topToolbar;
    private Toolbar bottomToolbar;
    private BRText mCloseButton;
    private ImageButton mReloadButton;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private RelativeLayout mRootView;

    private boolean keyboardListenersAttached = false;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        webView = findViewById(R.id.web_view);
        webView.setBackgroundColor(0);
//        webView.setWebChromeClient(new BRWebChromeClient());
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.e(TAG, "onPageStarted: " + url);
                String trimmedUrl = rTrim(url, '/');
                Uri toUri = Uri.parse(trimmedUrl);
                if (closeOnMatch(toUri) || toUri.toString().contains("_close")) {
                    Log.e(TAG, "onPageStarted: close Uri found: " + toUri);
                    onBackPressed();
                    onCloseUrl = null;
                }

                if (view.canGoBack()) {
                    mBackButton.setBackground(getDrawable(R.drawable.ic_webview_left));
                } else {
                    mBackButton.setBackground(getDrawable(R.drawable.ic_webview_left_inactive));

                }
                if (view.canGoForward()) {
                    mForwardButton.setBackground(getDrawable(R.drawable.ic_webview_right));

                } else {
                    mForwardButton.setBackground(getDrawable(R.drawable.ic_webview_right_inactive));

                }

            }


        });

        topToolbar = findViewById(R.id.toolbar);
        bottomToolbar = findViewById(R.id.toolbar_bottom);
        mCloseButton = findViewById(R.id.close);
        mReloadButton = findViewById(R.id.reload);
        mForwardButton = findViewById(R.id.webview_forward_arrow);
        mBackButton = findViewById(R.id.webview_back_arrow);
        mRootView = findViewById(R.id.webview_parent);


        String articleId = getIntent().getStringExtra("articleId");

        WebSettings webSettings = webView.getSettings();

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        theUrl = getIntent().getStringExtra("url");
        String json = getIntent().getStringExtra("json");


        if (json == null) {
            if (!setupServerMode(theUrl)) {
                webView.loadUrl(theUrl);

                return;
            }


            if (articleId != null && !articleId.isEmpty())
                theUrl = theUrl + "/" + articleId;

            Log.d(TAG, "onCreate: theUrl: " + theUrl + ", articleId: " + articleId);
            if (!theUrl.contains("checkout")) {
                bottomToolbar.setVisibility(View.INVISIBLE);
            }
            webView.loadUrl(theUrl);
            if (articleId != null && !articleId.isEmpty())
                navigate(articleId);
        } else {
            request(webView, json);

        }

        //attachKeyboardListeners();


    }

    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            int heightDiff = mRootView.getRootView().getHeight() - mRootView.getHeight();
            int contentViewTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getHeight();


            if (heightDiff <= contentViewTop) {
                onHideKeyboard();
                Log.d(TAG, "Hiding keyboard");

            } else {
                int keyboardHeight = heightDiff - contentViewTop;
                onShowKeyboard(keyboardHeight);
                Log.d(TAG, "Showing keyboard");


            }
        }
    };

    protected void onShowKeyboard(int keyboardHeight) {
        bottomToolbar.setVisibility(View.INVISIBLE);
    }

    protected void onHideKeyboard() {
        bottomToolbar.setVisibility(View.VISIBLE);

    }

    protected void attachKeyboardListeners() {
        if (keyboardListenersAttached) {
            return;
        }

        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);

        keyboardListenersAttached = true;
    }


    private void request(final WebView webView, final String jsonString) {

        try {
            JSONObject json = new JSONObject(jsonString);

            String url = json.getString("url");
            Log.d(TAG, "Loading -> " + url);
            if (url != null && url.contains("checkout")) {
                attachKeyboardListeners();

                // Make the top and bottom toolbars visible for Simplex flow
                topToolbar.setVisibility(View.VISIBLE);
                bottomToolbar.setVisibility(View.VISIBLE);

                // Position the webview below the top toolbar
                RelativeLayout.LayoutParams webviewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                webviewParams.addRule(RelativeLayout.BELOW, R.id.toolbar);
                webView.setLayoutParams(webviewParams);


                // Make the reload/refresh button functional
                mReloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        request(webView, jsonString);

                    }
                });


                // Make the close button functional
                mCloseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onBackPressed();
                    }
                });

                // Make the back button functional
                mBackButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (webView.canGoBack()) {
                            webView.goBack();
                        }
                    }
                });


                // Make the forward button functional
                mForwardButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (webView.canGoForward()) {
                            webView.goForward();
                        }
                    }
                });
            }
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
//            JSONObject jsonHeaders = new JSONObject(headers);
//            while (jsonHeaders.keys().hasNext()) {
//                String key = jsonHeaders.keys().next();
//                jsonHeaders.put(key, jsonHeaders.getString(key));
//            }
            byte[] body = strBody.getBytes();

            if (method.equalsIgnoreCase("get")) {
                webView.loadUrl(url, httpHeaders);
            } else if (method.equalsIgnoreCase("post")) {
                Log.e(TAG, "request: POST:" + body.length);
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

    private boolean setupServerMode(String url) {
        if (url.equalsIgnoreCase(HTTPServer.URL_BUY)) {
            HTTPServer.mode = HTTPServer.ServerMode.BUY;
        } else if (url.equalsIgnoreCase(HTTPServer.URL_SUPPORT)) {
            HTTPServer.mode = HTTPServer.ServerMode.SUPPORT;
        } else if (url.equalsIgnoreCase(HTTPServer.URL_EA)) {
            HTTPServer.mode = HTTPServer.ServerMode.EA;
        } else {
            Log.e(TAG, "setupServerMode: " + "unknown url: " + url);
            return false;
        }
        return true;
    }

    private boolean closeOnMatch(Uri toUri) {
        if (onCloseUrl == null) {
            Log.e(TAG, "closeOnMatch: onCloseUrl is null");
            return false;
        }
        Uri savedCloseUri = Uri.parse(onCloseUrl);
        Log.e(TAG, "closeOnMatch: toUrl:" + toUri + ", savedCloseUri: " + savedCloseUri);
        return toUri.getScheme().equalsIgnoreCase(savedCloseUri.getScheme()) && toUri.getHost().equalsIgnoreCase(savedCloseUri.getHost())
                && toUri.toString().toLowerCase().contains(savedCloseUri.toString().toLowerCase());

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

//    private class BRWebChromeClient extends WebChromeClient {
//        @Override
//        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
//            Log.e(TAG, "onConsoleMessage: consoleMessage: " + consoleMessage.message());
//            return super.onConsoleMessage(consoleMessage);
//        }
//
//        @Override
//        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
//            Log.e(TAG, "onJsAlert: " + message + ", url: " + url);
//            return super.onJsAlert(view, url, message, result);
//        }
//
//        @Override
//        public void onCloseWindow(WebView window) {
//            super.onCloseWindow(window);
//            Log.e(TAG, "onCloseWindow: ");
//        }
//
//        @Override
//        public void onReceivedTitle(WebView view, String title) {
//            super.onReceivedTitle(view, title);
//            Log.e(TAG, "onReceivedTitle: view.getUrl:" + view.getUrl());
//            String trimmedUrl = rTrim(view.getUrl(), '/');
//            Log.e(TAG, "onReceivedTitle: trimmedUrl:" + trimmedUrl);
//            Uri toUri = Uri.parse(trimmedUrl);
//
////                Log.d(TAG, "onReceivedTitle: " + request.getMethod());
//            if (closeOnMatch(toUri) || toUri.toString().toLowerCase().contains("_close")) {
//                Log.e(TAG, "onReceivedTitle: close Uri found: " + toUri);
//                onBackPressed();
//                onCloseUrl = null;
//            }
//        }
//
//        on
//    }

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

    private String rTrim(String s, char c) {
        if (s == null) return null;
        String result = s;
        while (result.length() > 0 && s.charAt(s.length() - 1) == c)
            result = s.substring(s.length() - 1);
        return result;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
        LinkPlugin.hasBrowser = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (keyboardListenersAttached) {
            mRootView.getViewTreeObserver().removeGlobalOnLayoutListener(keyboardLayoutListener);
        }
    }

}
