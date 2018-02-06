package com.breadwallet.presenter.activities.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.platform.HTTPServer;
import com.platform.middlewares.plugins.LinkPlugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WebViewActivity extends BRActivity {
    private static final String TAG = WebViewActivity.class.getName();
    WebView webView;
    String theUrl;
    public static boolean appVisible = false;
    private static WebViewActivity app;
    private String onCloseUrl;

    private static final int INPUT_FILE_REQUEST_CODE = 1;
//    private static final int FILECHOOSER_RESULTCODE = 1;
//    private ValueCallback<Uri> mUploadMessage;
//    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

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
        webView.setWebChromeClient(new BRWebChromeClient());
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

            }


        });

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
            webView.loadUrl(theUrl);
            if (articleId != null && !articleId.isEmpty())
                navigate(articleId);
        } else {
            request(webView, json);
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
                webView.postUrl(url, body);

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

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            Log.e(TAG, "onCloseWindow: ");
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            Log.e(TAG, "onReceivedTitle: view.getUrl:" + view.getUrl());
            String trimmedUrl = rTrim(view.getUrl(), '/');
            Log.e(TAG, "onReceivedTitle: trimmedUrl:" + trimmedUrl);
            Uri toUri = Uri.parse(trimmedUrl);

//                Log.d(TAG, "onReceivedTitle: " + request.getMethod());
            if (closeOnMatch(toUri) || toUri.toString().toLowerCase().contains("_close")) {
                Log.e(TAG, "onReceivedTitle: close Uri found: " + toUri);
                onBackPressed();
                onCloseUrl = null;
            }
        }

        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePath, FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

            return true;
        }
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BRConstants.UPLOAD_FILE_REQUEST) {

            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] results = null;

            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)

        return super.onKeyDown(keyCode, event);
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

}
