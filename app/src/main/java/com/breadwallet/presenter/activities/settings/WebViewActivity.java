package com.breadwallet.presenter.activities.settings;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.platform.middlewares.plugins.GeoLocationPlugin;
import com.platform.middlewares.plugins.LinkPlugin;

import org.eclipse.jetty.http.HttpMethod;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WebViewActivity extends BRActivity {
    private static final String TAG = WebViewActivity.class.getName();

    private static final int REQUEST_CHOOSE_IMAGE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 29;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    public static final String EXTRA_JSON_PARAM = "com.breadwallet.presenter.activities.settings.WebViewActivity.EXTRA_JSON_PARAM";
    private static final String DATE_FORMAT = "yyyyMMdd_HHmmss";
    private static final String DOCUMENTS_ACTIVITY_CLASS_NAME = "com.android.documentsui.DocumentsActivity";
    private static final String KYC_SUFFIX = "_kyc.jpg";
    private static final String BUY_PATH = "/buy";
    private static final String CURRENCY = "currency";
    private static final String URL_FORMAT = "%s?%s=%s";
    private static final String SELECT_IMAGE_TITLE = "Select Image Source";
    private static final String INTENT_TYPE_IMAGE = "image/*";

    private WebView mWebView;
    private RelativeLayout mRootView;
    private Toolbar mTopToolbar;
    private Toolbar mBottomToolbar;
    private BaseTextView mCloseButton;
    private ImageButton mReloadButton;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private String mUrl;
    private String mOnCloseUrl;
    private boolean mKeyboardListenersAttached = false;

    private ViewTreeObserver.OnGlobalLayoutListener mKeyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            int heightDiff = mRootView.getRootView().getHeight() - mRootView.getHeight();
            int contentViewTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getHeight();

            if (heightDiff <= contentViewTop) {
                onHideKeyboard();
                Log.d(TAG, "Hiding keyboard");

            } else {
                onShowKeyboard();
                Log.d(TAG, "Showing keyboard");

            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        mWebView = findViewById(R.id.web_view);
        mWebView.setBackgroundColor(0);
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "onPageStarted: url:" + url);
                String trimmedUrl = rTrim(url, '/');
                Uri toUri = Uri.parse(trimmedUrl);
                if (closeOnMatch(toUri) || toUri.toString().contains(BRConstants.CLOSE)) {
                    Log.d(TAG, "onPageStarted: close Uri found: " + toUri);
                    onBackPressed();
                    mOnCloseUrl = null;
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

        mTopToolbar = findViewById(R.id.toolbar);
        mBottomToolbar = findViewById(R.id.toolbar_bottom);
        mCloseButton = findViewById(R.id.close);
        mReloadButton = findViewById(R.id.reload);
        mForwardButton = findViewById(R.id.webview_forward_arrow);
        mBackButton = findViewById(R.id.webview_back_arrow);
        mRootView = findViewById(R.id.webview_parent);

        String articleId = getIntent().getStringExtra(BRConstants.ARTICLE_ID);

        WebSettings webSettings = mWebView.getSettings();

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);

        mUrl = getIntent().getStringExtra(BRConstants.EXTRA_URL);
        String json = getIntent().getStringExtra(EXTRA_JSON_PARAM);

        if (json == null) {
            if (articleId != null && !articleId.isEmpty()) {
                mUrl = mUrl + "/" + articleId;
            }

            Log.d(TAG, "onCreate: theUrl: " + mUrl + ", articleId: " + articleId);
            if (!mUrl.contains(BRConstants.CHECKOUT)) {
                mBottomToolbar.setVisibility(View.INVISIBLE);
            }
            if (mUrl.endsWith(BUY_PATH)) {
                mUrl = String.format(URL_FORMAT, mUrl, CURRENCY, WalletsMaster.getInstance(this).getCurrentWallet(this).getCurrencyCode().toLowerCase());

            }
            mWebView.loadUrl(mUrl);
            if (articleId != null && !articleId.isEmpty()) {
                navigate(articleId);
            }
        } else {
            request(mWebView, json);
        }

        mWebView.setWebChromeClient(new BRWebChromeClient());

    }

    protected void onShowKeyboard() {
        mBottomToolbar.setVisibility(View.INVISIBLE);
    }

    protected void onHideKeyboard() {
        mBottomToolbar.setVisibility(View.VISIBLE);
    }

    protected void attachKeyboardListeners() {
        if (mKeyboardListenersAttached) {
            return;
        }
        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(mKeyboardLayoutListener);
        mKeyboardListenersAttached = true;
    }

    private void request(final WebView webView, final String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);

            String url = json.getString(BRConstants.URL);
            Log.d(TAG, "Loading -> " + url);
            if (url != null && url.contains(BRConstants.CHECKOUT)) {
                attachKeyboardListeners();

                // Make the top and bottom toolbars visible for Simplex flow
                mTopToolbar.setVisibility(View.VISIBLE);
                mBottomToolbar.setVisibility(View.VISIBLE);

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
            String method = json.getString(BRConstants.METHOD);
            String strBody = json.has(BRConstants.BODY) ? json.getString(BRConstants.BODY) : null;
            String headers = json.has(BRConstants.HEADERS) ? json.getString(BRConstants.HEADERS) : null;
            mOnCloseUrl = json.getString(BRConstants.CLOSE_ON);
            Map<String, String> httpHeaders = null;
            if (!Utils.isNullOrEmpty(headers)) {
                httpHeaders = new HashMap<>();
                JSONObject jsonHeaders = new JSONObject(headers);
                Iterator<String> headerIterator = jsonHeaders.keys();
                while (headerIterator.hasNext()) {
                    String key = headerIterator.next();
                    httpHeaders.put(key, jsonHeaders.getString(key));
                }
            }
            byte[] body = strBody == null ? null : strBody.getBytes();

            if (method.equalsIgnoreCase(HttpMethod.GET.asString())) {
                if (httpHeaders != null) {
                    webView.loadUrl(url, httpHeaders);
                } else {
                    webView.loadUrl(url);
                }
            } else if (method.equalsIgnoreCase(HttpMethod.POST.asString())) {
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
        mWebView.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.e(TAG, "onReceiveValue: " + value);
            }
        });
    }

    private boolean closeOnMatch(Uri toUri) {
        if (mOnCloseUrl == null) {
            Log.e(TAG, "closeOnMatch: onCloseUrl is null");
            return false;
        }
        Uri savedCloseUri = Uri.parse(rTrim(mOnCloseUrl, '/'));
        Log.e(TAG, "closeOnMatch: toUrl:" + toUri + ", savedCloseUri: " + savedCloseUri);
        return toUri.getScheme() != null && toUri.getHost() != null && toUri.getScheme().equalsIgnoreCase(savedCloseUri.getScheme()) && toUri.getHost().equalsIgnoreCase(savedCloseUri.getHost())
                && toUri.toString().toLowerCase().contains(savedCloseUri.toString().toLowerCase());

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
            if (closeOnMatch(toUri) || toUri.toString().toLowerCase().contains(BRConstants.CLOSE)) {
                Log.e(TAG, "onReceivedTitle: close Uri found: " + toUri);
                onBackPressed();
                mOnCloseUrl = null;
            }
        }

        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePath, FileChooserParams fileChooserParams) {
            Log.d(TAG, "onShowFileChooser");
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                Log.d(TAG, "Image Capture Activity FOUND");
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
                    Log.d(TAG, "Image File NOT null");
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    Log.d(TAG, "Image File IS NULL");

                    takePictureIntent = null;
                }
            } else {
                Log.d(TAG, "Image Capture Activity NOT FOUND");
            }

            requestImageFilePermission();
            requestCameraPermission();

            return true;
        }
    }

    // Get URI to image received from capture by camera.
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), KYC_SUFFIX));
        }
        return outputFileUri;
    }


    public Intent getPickImageChooserIntent() {
        List<Intent> allIntents = new ArrayList();
        PackageManager packageManager = getPackageManager();

        // collect all camera intents
        Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);

        for (ResolveInfo res : listCam) {
            Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // Collect all gallery intents
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType(INTENT_TYPE_IMAGE);
        List<ResolveInfo> listGallery = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo res : listGallery) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            allIntents.add(intent);
        }

        // The main intent is the last in the list so pickup the last one
        Intent mainIntent = allIntents.get(allIntents.size() - 1);
        for (Intent intent : allIntents) {
            if (intent.getComponent().getClassName().equals(DOCUMENTS_ACTIVITY_CLASS_NAME)) {
                mainIntent = intent;
                break;
            }
        }
        allIntents.remove(mainIntent);

        // Create a chooser from the main intent
        Intent chooserIntent = Intent.createChooser(mainIntent, SELECT_IMAGE_TITLE);

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toArray(new Parcelable[allIntents.size()]));

        return chooserIntent;
    }


    @Override
    public void onBackPressed() {
        if (UiUtils.isLast(this)) {
            UiUtils.startBreadActivity(this, false);
        } else {
            super.onBackPressed();
        }
        overridePendingTransition(R.anim.fade_up, R.anim.exit_to_bottom);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        Log.d(TAG, "requestCode -> " + requestCode);
        Log.d(TAG, "resultCode -> " + resultCode);

        if (requestCode != REQUEST_CHOOSE_IMAGE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        } else if (requestCode == REQUEST_CHOOSE_IMAGE) {
            //todo why is this empty?
        }

        Uri[] results = null;

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Photo Path -> " + mCameraPhotoPath);

            if (data != null && data.getDataString() != null && !data.getDataString().isEmpty()) {
                Log.d(TAG, "Data string -> " + data.getDataString());
                results = new Uri[]{Uri.parse(data.getDataString())};


            } else if (mCameraPhotoPath != null) {
                results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                Toast.makeText(WebViewActivity.this, "Error getting selected image!", Toast.LENGTH_SHORT).show();
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        }

    }

    private void requestCameraPermission() {
        Log.d(TAG, "requestCameraPermission");

        // Camera permission is NOT granted, request it
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "CAMERA permission NOT granted");
            android.support.v4.app.ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);

        } else {
            // Camera permission already granted, start the camera
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA_PERMISSION);
            overridePendingTransition(R.anim.fade_up, R.anim.fade_down);

        }
    }

    private void requestImageFilePermission() {
        Log.d(TAG, "requestImageFilePermission");

        // Image File permission is not granted, request it now
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            startActivityForResult(getPickImageChooserIntent(), REQUEST_CHOOSE_IMAGE);
        }
    }

    private File createImageFile() throws IOException {

        // ActivityCompat.requestPermissions(WebViewActivity.this,
        //new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        // Create an image file name
        String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                KYC_SUFFIX,         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        Log.d(TAG, "onRequestPermissionResult");
        Log.d(TAG, "Request Code -> " + requestCode);

        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted, open camera
                    Log.d(TAG, "Camera permission GRANTED");
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CAMERA_PERMISSION);
                    overridePendingTransition(R.anim.fade_up, R.anim.fade_down);

                } else {
                    Toast.makeText(WebViewActivity.this, getString(R.string.Send_cameraUnavailabeMessage_android), Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Storage permission GRANTED");

                    startActivityForResult(getPickImageChooserIntent(), REQUEST_CHOOSE_IMAGE);
                }
                break;
            }

            case BRConstants.GEO_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted
                    Log.d(TAG, "Geo permission GRANTED");
                    GeoLocationPlugin.handleGeoPermission(true);
                } else {
                    GeoLocationPlugin.handleGeoPermission(false);
                }
                break;
            }
        }

    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)

        return super.onKeyDown(keyCode, event);
    }

    private String rTrim(String s, char c) {
//        if (s == null) return null;
        String result = s;
        if (result.length() > 0 && s.charAt(s.length() - 1) == c)
            result = s.substring(0, s.length() - 1);
        return result;
    }

    @Override
    protected void onPause() {
        super.onPause();
        LinkPlugin.hasBrowser = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mKeyboardListenersAttached) {
            mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(mKeyboardLayoutListener);
        }
    }

}
