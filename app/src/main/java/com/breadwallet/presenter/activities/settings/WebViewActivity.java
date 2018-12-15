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

    public static final String EXTRA_JSON_PARAM = "com.breadwallet.presenter.activities.settings.WebViewActivity.EXTRA_JSON_PARAM";
    private static final String DATE_FORMAT = "yyyyMMdd_HHmmss";
    private static final String IMAGE_FILE_NAME_SUFFIX = "_kyc.jpg";
    private static final String BUY_PATH = "/buy";
    private static final String CURRENCY = "currency";
    private static final String URL_FORMAT = "%s?%s=%s";
    private static final String FILE_SCHEME = "file:";
    private static final String INTENT_TYPE_IMAGE = "image/*";

    private static final int CHOOSE_IMAGE_REQUEST_CODE = 1; // Activity request used to select an image.
    private static final int GET_CAMERA_PERMISSIONS_REQUEST_CODE = 2; // Permissions request to ask for camera permissions
    private static final String[] CAMERA_PERMISSIONS = {  // To use the camera we need the following permissions
            Manifest.permission.CAMERA, // Used to take the photo.
            Manifest.permission.WRITE_EXTERNAL_STORAGE // Used to save the taken photo to storage.
    };

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

            // Save the new call back. It will be called when the image file chooser activity returns.
            mFilePathCallback = filePath;

            if (hasPermissions(CAMERA_PERMISSIONS)) {
                startActivityForResult(getImageFileChooserIntent(true), CHOOSE_IMAGE_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(WebViewActivity.this, CAMERA_PERMISSIONS, GET_CAMERA_PERMISSIONS_REQUEST_CODE);
            }
            return true;
        }
    }

    /**
     * Checks if the specified list of permissions are currently granted.
     *
     * @param permissions The permissions to check.
     * @return True if all the specified permissions are currently granted; false otherwise.
     */
    private boolean hasPermissions(String... permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a set of intents that will be used to allow the user to select an image. These intents will be plugged
     * into the a system file chooser dialog to prompt the user to make a selection.
     *
     * @param hasCameraPermissions True if we have permission to use the camera; false otherwise.
     * @return The list of intents that can be used to select an image.
     */
    private Intent getImageFileChooserIntent(boolean hasCameraPermissions) {
        List<Intent> intents = new ArrayList();
        PackageManager packageManager = getPackageManager();

        // Get Camera intent so user can take a photo.
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) && hasCameraPermissions) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                try {
                    // Create a file to store the camera image.
                    File imageFile = File.createTempFile(new SimpleDateFormat(DATE_FORMAT).format(new Date()) + '_',
                            IMAGE_FILE_NAME_SUFFIX,
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    );

                    // Create a camera intent for use in the file chooser.
                    if (imageFile != null) {
                        mCameraPhotoPath = FILE_SCHEME + imageFile.getAbsolutePath();
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
                        intents.add(cameraIntent);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "getImageFileChooserIntent: Unable to create image file for camera intent.", e);
                }
            } else {
                Log.d(TAG, "getImageFileChooserIntent: Image capture intent not found, unable to allow camera use.");
            }
        }

        // Get all gallery intents so user can select a photo with the file chooser.
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType(INTENT_TYPE_IMAGE);
        List<ResolveInfo> galleryActivityList = packageManager.queryIntentActivities(galleryIntent, 0);
        for (ResolveInfo resolveInfo : galleryActivityList) {
            Intent intent = new Intent(galleryIntent);
            intent.setComponent(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            intent.setPackage(resolveInfo.activityInfo.packageName);
            intents.add(intent);
        }

        // Create the file chooser intent. The first intent from our list that will appear in the chooser, must be
        // removed from the intents array and passed into the chooser upon creation.
        Intent imageFileChooserIntent = Intent.createChooser(intents.get(0), getString(R.string.FileChooser_selectImageSource_android));
        intents.remove(0);

        // Add the remaining image file chooser intents to an auxiliary array.  These will also appear in the file chooser.
        imageFileChooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new Parcelable[intents.size()]));

        return imageFileChooserIntent;
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

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult: requestCode: " + requestCode + " resultCode: " + resultCode);

        if ((requestCode == CHOOSE_IMAGE_REQUEST_CODE && mFilePathCallback != null)) {
            // The user has selected an image.
            Uri[] imageFileUri = null;
            if (resultCode == Activity.RESULT_OK) {
                if (intent == null) {
                    // If there is no intent, then we may have taken a photo with the camera.
                    if (mCameraPhotoPath != null) {
                        imageFileUri = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    // Else we have the path of the selected image.
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        imageFileUri = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            // Return the image file Uris to the web.
            mFilePathCallback.onReceiveValue(imageFileUri);
            mFilePathCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(TAG, "onRequestPermissionResult: requestCode: " + requestCode);

        switch (requestCode) {
            case GET_CAMERA_PERMISSIONS_REQUEST_CODE:
                // The camera permissions have changed upon a request for the user to select an image. Show the appropriate
                // image file chooser based on the current permissions.
                startActivityForResult(getImageFileChooserIntent(permissionGranted(grantResults)), CHOOSE_IMAGE_REQUEST_CODE);
                break;
            case BRConstants.GEO_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    /**
     * Aggregates the result of a list of granted permissions.
     *
     * @param grantResults The list of permissions resuts to check.
     * @return True if *all* permissions were granted; false otherwise.
     */
    private boolean permissionGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
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
