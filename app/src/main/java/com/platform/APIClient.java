package com.platform;


import android.accounts.AuthenticatorException;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.crypto.Base58;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRCompressor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.ServerBundlesHelper;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;
import com.platform.tools.TokenHolder;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.sigpipe.jbsdiff.InvalidHeaderException;
import io.sigpipe.jbsdiff.ui.FileUI;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;

import static com.breadwallet.tools.util.BRConstants.*;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/29/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class APIClient {

    public static final String TAG = APIClient.class.getName();

    // proto is the transport protocol to use for talking to the API (either http or https)
    private static final String PROTO = "https";
    private static final String HTTPS_SCHEME = "https://";
    private static final String GMT = "GMT";
    private static final String BREAD = "bread";
    private static final int NETWORK_ERROR_CODE = 599;
    private static final int SYNC_ITEMS_COUNT = 4;
    private static final String FEATURE_FLAG_PATH = "/me/features";
    private static final String TAR = "tar";
    private static final String BUNDLES_FORMAT = "%s/assets/bundles/%s/diff/%s";
    private static final String PUBKEY = "pubKey";
    private static final String DEVICE_ID = "deviceID";

    // convenience getter for the API endpoint
    private static final String BASE_URL = HTTPS_SCHEME + BreadApp.getHost();
    //Fee per kb url
    private static final String FEE_PER_KB_URL = "/v1/fee-per-kb";
    //token path
    private static final String TOKEN_PATH = "/token";
    private static final String TOKEN = "token";
    //me path
    private static final String ME = "/me";

    // Http Header constants
    private static final String HEADER_WALLET_ID = "X-Wallet-Id";
    private static final String HEADER_IS_INTERNAL = "X-Is-Internal";
    private static final String HEADER_TESTFLIGHT = "X-Testflight";
    private static final String HEADER_TESTNET = "X-Bitcoin-Testnet";
    private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    private static final String HEADER_USER_AGENT = "User-agent";

    // User Agent constants
    public static final String SYSTEM_PROPERTY_USER_AGENT = "http.agent";
    private static final String USER_AGENT_APP_NAME = "breadwallet/";
    private static final String USER_AGENT_PLATFORM_NAME = "android/";

    private static APIClient ourInstance;

    private byte[] mCachedAuthKey;

    private boolean mIsFetchingToken;

    private OkHttpClient mHTTPClient;
    private static final Map<String, String> mHttpHeaders = new HashMap<>();


    private static final String TAR_FILE_NAME_FORMAT = "/%s.tar";
    private static final String BUY_NOTIFICATION_KEY = "buy-notification";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    private static final boolean PRINT_FILES = false;
    private static final int MAX_RETRY = 3;
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;

    private boolean mIsPlatformUpdating = false;
    private AtomicInteger mItemsLeftToUpdate = new AtomicInteger(0);

    private Context mContext;

    public enum FeatureFlags {
        BUY_NOTIFICATION(BUY_NOTIFICATION_KEY);

        private final String mText;

        FeatureFlags(final String text) {
            mText = text;
        }

        @Override
        public String toString() {
            return mText;
        }
    }

    public static synchronized APIClient getInstance(Context context) {

        if (ourInstance == null) {
            ourInstance = new APIClient(context);
        }
        return ourInstance;
    }

    private APIClient(Context context) {
        mContext = context;

        // Split the default device user agent string by spaces and take the first string.
        // Example user agent string: "Dalvik/1.6.0 (Linux; U;Android 5.1; LG-F320SBuild/KOT49I.F320S22g) Android/9"
        // We only want: "Dalvik/1.6.0"
        String deviceUserAgent = System.getProperty(SYSTEM_PROPERTY_USER_AGENT).split(BRConstants.SPACE_REGEX)[0];

        // The BRD server expects the following user agent: appName/appVersion engine/engineVersion plaform/plaformVersion
        String brdUserAgent = (new StringBuffer()).append(USER_AGENT_APP_NAME).append(BuildConfig.VERSION_CODE).append(' ')
                .append(deviceUserAgent).append(' ')
                .append(USER_AGENT_PLATFORM_NAME).append(Build.VERSION.RELEASE).toString();

        mHttpHeaders.put(HEADER_IS_INTERNAL, BuildConfig.IS_INTERNAL_BUILD ? TRUE : FALSE);
        mHttpHeaders.put(HEADER_TESTFLIGHT, BuildConfig.DEBUG ? TRUE : FALSE);
        mHttpHeaders.put(HEADER_TESTNET, BuildConfig.BITCOIN_TESTNET ? TRUE : FALSE);
        mHttpHeaders.put(HEADER_ACCEPT_LANGUAGE, getCurrentLanguageCode(context));
        mHttpHeaders.put(HEADER_USER_AGENT, brdUserAgent);
    }

    /**
     * Return the current language code i.e. "en_US" for US English.
     *
     * @return The current language code.
     */
    @TargetApi(Build.VERSION_CODES.N)
    private String getCurrentLanguageCode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0).toString();
        } else {
            // No inspection deprecation.
            return context.getResources().getConfiguration().locale.toString();
        }
    }

    //returns the fee per kb or 0 if something went wrong
    public long feePerKb() {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        try {
            String strUtl = getBaseURL() + FEE_PER_KB_URL;
            Request request = new Request.Builder().url(strUtl).get().build();
            BRResponse response = sendRequest(request, false);
            JSONObject object = new JSONObject(response.getBodyText());
            return (long) object.getInt(FEE_PER_KB);
        } catch (JSONException e) {
            Log.e(TAG, "feePerKb: ", e);
        }
        return 0;
    }

    //only for testing
    public String buyBitcoinMe() {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        if (mContext == null) {
            mContext = BreadApp.getBreadContext();
        }
        if (mContext == null) {
            return null;
        }
        String strUtl = getBaseURL() + ME;
        Request request = new Request.Builder()
                .url(strUtl)
                .get()
                .build();
        BRResponse response = sendRequest(request, true);

        return response.getBodyText();
    }

    public String getToken() {
        if (mIsFetchingToken) {
            return null;
        }
        mIsFetchingToken = true;

        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        if (mContext == null) {
            mContext = BreadApp.getBreadContext();
        }
        if (mContext == null) {
            return null;
        }
        try {
            String strUtl = getBaseURL() + TOKEN_PATH;

            JSONObject requestMessageJSON = new JSONObject();
            String base58PubKey = BRCoreKey.getAuthPublicKeyForAPI(getCachedAuthKey());
            requestMessageJSON.put(PUBKEY, base58PubKey);
            requestMessageJSON.put(DEVICE_ID, BRSharedPrefs.getDeviceId(mContext));

            final MediaType JSON = MediaType.parse(CONTENT_TYPE_JSON_CHARSET_UTF8);
            RequestBody requestBody = RequestBody.create(JSON, requestMessageJSON.toString());
            Request request = new Request.Builder()
                    .url(strUtl)
                    .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON_CHARSET_UTF8)
                    .header(HEADER_ACCEPT, CONTENT_TYPE_JSON_CHARSET_UTF8)
                    .post(requestBody).build();
            BRResponse response = sendRequest(request, false);
            if (Utils.isNullOrEmpty(response.getBodyText())) {
                Log.e(TAG, "getToken: retrieving token failed");
                return null;
            }
            JSONObject obj = null;
            obj = new JSONObject(response.getBodyText());

            return obj.getString(TOKEN);
        } catch (JSONException e) {
            Log.e(TAG, "getToken: ", e);

        } finally {
            mIsFetchingToken = false;
        }
        return null;

    }

    private String createRequest(String reqMethod, String base58Body, String contentType, String dateHeader, String url) {
        return (reqMethod == null ? "" : reqMethod) + "\n"
                + (base58Body == null ? "" : base58Body) + "\n"
                + (contentType == null ? "" : contentType) + "\n"
                + (dateHeader == null ? "" : dateHeader) + "\n"
                + (url == null ? "" : url);
    }

    public String signRequest(String request) {
        byte[] doubleSha256 = CryptoHelper.doubleSha256(request.getBytes(StandardCharsets.UTF_8));
        BRCoreKey key;
        try {
            byte[] authKey = getCachedAuthKey();
            if (Utils.isNullOrEmpty(authKey)) {
                Log.e(TAG, "signRequest: authkey is null");
                return null;
            }
            key = new BRCoreKey(authKey);
        } catch (IllegalArgumentException ex) {
            key = null;
            Log.e(TAG, "signRequest: " + request, ex);
        }
        if (key == null) {
            Log.e(TAG, "signRequest: key is null, failed to create BRKey");
            return null;
        }
        byte[] signedBytes = key.compactSign(doubleSha256);
        return Base58.encode(signedBytes);

    }

    private Response sendHttpRequest(Request locRequest, boolean withAuth) {
        if (UiUtils.isMainThread()) {
            Log.e(TAG, "urlGET: network on main thread");
            throw new RuntimeException("network on main thread");
        }

        Request.Builder newBuilder = locRequest.newBuilder();
        for (String key : mHttpHeaders.keySet()) {
            String value = mHttpHeaders.get(key);
            newBuilder.header(key, value);
        }

        //Add wallet rewards Id for signed requests
        if (withAuth) {
            String walletId = BRSharedPrefs.getWalletRewardId(BreadApp.getBreadContext());
            if (!Utils.isNullOrEmpty(walletId)) {
                try {
                    newBuilder.addHeader(HEADER_WALLET_ID, walletId);
                } catch (IllegalArgumentException ex) {
                    BRReportsManager.reportBug(ex);
                    Log.e(TAG, "sendHttpRequest: ", ex);
                }
            }
        }

        Request request = newBuilder.build();
        if (withAuth) {
            AuthenticatedRequest authenticatedRequest = authenticateRequest(request);
            if (authenticatedRequest == null) {
                return null;
            }
            request = authenticatedRequest.getRequest();
            if (request == null) {
                return null;
            }
        }

        Response rawResponse;
        try {
            if (mHTTPClient == null) {
                mHTTPClient = new OkHttpClient.Builder().followRedirects(false)
                        .connectTimeout(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        /*.addInterceptor(new LoggingInterceptor())*/.build();
            }

            rawResponse = mHTTPClient.newCall(request).execute();
        } catch (IOException e) {
            Log.e(TAG, "sendRequest: ", e);
            String message = e.getMessage() == null ? "" : e.getMessage();
            return new Response.Builder().code(NETWORK_ERROR_CODE).request(request)
                    .body(ResponseBody.create(null, message)).message(message).protocol(Protocol.HTTP_1_1).build();
        }
        byte[] bytesBody = new byte[0];
        try {
            bytesBody = rawResponse.body().bytes();
        } catch (IOException e) {
            Log.e(TAG, "sendHttpRequest: ", e);
            BRReportsManager.reportBug(e);
        }

        if (Utils.isNullOrEmpty(bytesBody)) {
            return createNewResponseWithBody(rawResponse, bytesBody);
        }

        if (rawResponse.header(BRConstants.CONTENT_ENCODING) != null && rawResponse.header(BRConstants.CONTENT_ENCODING).equalsIgnoreCase(BRConstants.GZIP)) {
            Log.d(TAG, "sendRequest: the content is gzip, unzipping");

            byte[] decompressed = BRCompressor.gZipExtract(bytesBody);
            if (decompressed == null) {
                BRReportsManager.reportBug(new IllegalArgumentException("failed to decrypt data!"));
                return createNewResponseWithBody(rawResponse, null);
            }
            return createNewResponseWithBody(rawResponse, decompressed);
        } else {
            return createNewResponseWithBody(rawResponse, bytesBody);
        }

    }

    private Response createNewResponseWithBody(Response response, byte[] body) {
        if (body == null) {
            body = new byte[0];
        }
        ResponseBody postReqBody = ResponseBody.create(null, body);
        return response.newBuilder().body(postReqBody).build();
    }

    @NonNull
    public BRResponse sendRequest(Request request, boolean withAuth) {
        try (Response response = sendHttpRequest(request, withAuth)) {
            if (response == null) {
                BRReportsManager.reportBug(new AuthenticatorException("Request: " + request.url() + " response is null"));
                return new BRResponse();
            }
            if (response.code() == 401) {
                BRReportsManager.reportBug(new AuthenticatorException("Request: " + request.url() + " returned 401!"));
            }
            if (!response.isSuccessful()) {
                logRequestAndResponse(request, response);
            }
            if (response.isRedirect()) {
                String newLocation = request.url().scheme() + "://" + request.url().host() + response.header("location");
                Uri newUri = Uri.parse(newLocation);
                if (newUri == null) {
                    Log.e(TAG, "sendRequest: redirect uri is null");
                    return createBrResponse(response);
                } else if (!BuildConfig.DEBUG && (!newUri.getHost().equalsIgnoreCase(BreadApp.getHost())
                        || !newUri.getScheme().equalsIgnoreCase(PROTO))) {
                    Log.e(TAG, "sendRequest: WARNING: redirect is NOT safe: " + newLocation);
                    return createBrResponse(new Response.Builder().code(HttpStatus.INTERNAL_SERVER_ERROR_500).request(request)
                            .body(ResponseBody.create(null, new byte[0])).message("").protocol(Protocol.HTTP_1_1).build());
                } else {
                    Log.w(TAG, "redirecting: " + request.url() + " >>> " + newLocation);
                    return createBrResponse(sendHttpRequest(new Request.Builder().url(newLocation).get().build(), withAuth));
                }

            } else if (withAuth && isBreadChallenge(response)) {
                Log.d(TAG, "sendRequest: got authentication challenge from API - will attempt to get token, url -> " + request.url().toString());
                int i = 0;
                Response newResponse;
                do {
                    i++;
                    String tokenUsed = TokenHolder.retrieveToken(mContext);
                    TokenHolder.updateToken(mContext, tokenUsed);
                    newResponse = sendHttpRequest(request, true);
                } while (isBreadChallenge(response) && i < MAX_RETRY);
                return createBrResponse(newResponse);
            }
            return createBrResponse(response);
        }

    }

    private BRResponse createBrResponse(Response res) {
        BRResponse brRsp = new BRResponse();
        try {
            if (res != null) {
                int code = res.code();
                Map<String, String> headers = new HashMap<>();
                for (String name : res.headers().names()) {
                    headers.put(name, res.header(name));
                }

                byte[] bytesBody = null;
                String contentType = null;
                try {
                    ResponseBody body = res.body();
                    contentType = body.contentType() == null ? "" : body.contentType().type();
                    bytesBody = body.bytes();
                } catch (IOException ex) {
                    Log.e(TAG, "createBrResponse: ", ex);
                } finally {
                    res.close();
                }
                brRsp = new BRResponse(bytesBody, code, headers, res.request().url().toString(), contentType);
            }

        } finally {
            if (!brRsp.isSuccessful()) {
                brRsp.print();
            }
        }
        return brRsp;
    }

    private AuthenticatedRequest authenticateRequest(Request request) {
        Request.Builder modifiedRequest = request.newBuilder();
        String base58Body = "";
        RequestBody body = request.body();

        try {
            if (body != null && body.contentLength() != 0) {
                BufferedSink sink = new Buffer();
                try {
                    body.writeTo(sink);
                } catch (IOException e) {
                    Log.e(TAG, "authenticateRequest: ", e);
                }
                byte[] bytes = sink.buffer().readByteArray();
                base58Body = CryptoHelper.base58ofSha256(bytes);
            }
        } catch (IOException e) {
            Log.e(TAG, "authenticateRequest: ", e);
        }


        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(GMT));
        String httpDate = DATE_FORMAT.format(new Date());

        request = modifiedRequest.header(BRConstants.DATE, httpDate.substring(0, httpDate.indexOf(GMT) + GMT.length())).build();

        String queryString = request.url().encodedQuery();

        String requestString = createRequest(request.method(), base58Body, request.header(BRConstants.HEADER_CONTENT_TYPE),
                request.header(BRConstants.DATE), request.url().encodedPath()
                        + ((queryString != null && !queryString.isEmpty()) ? ("?" + queryString) : ""));
        String signedRequest = signRequest(requestString);
        if (signedRequest == null) {
            return null;
        }
        String token = TokenHolder.retrieveToken(mContext);
        String authValue = BREAD + " " + token + ":" + signedRequest;
        modifiedRequest = request.newBuilder();

        try {
            request = modifiedRequest.header(BRConstants.AUTHORIZATION, authValue).build();
        } catch (Exception e) {
            BRReportsManager.reportBug(e);
            return null;
        }
        return new AuthenticatedRequest(request, token);
    }

    /**
     * Gets the bundle version of the specified bundle that is currently on the device.
     *
     * @param bundleFile the file the bundle resides in.
     * @return The bundle version of the specified bundle that is currently on the device.
     */
    // TODO: Move to ServerBundlesHelper DROID-1133
    private String getCurrentBundleVersion(File bundleFile) {
        byte[] bundleBytes;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(bundleFile);
            bundleBytes = IOUtils.toByteArray(fileInputStream);
            byte[] hash = CryptoHelper.sha256(bundleBytes);
            return Utils.isNullOrEmpty(hash) ? null : Utils.bytesToHex(hash);
        } catch (IOException e) {
            Log.e(TAG, "getCurrentBundleVersion: ", e);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "getCurrentBundleVersion: ", e);
            }
        }
        return null;
    }

    // TODO: Move to ServerBundlesHelper DROID-1133
    public synchronized void updateBundle() {
        for (String bundleName : ServerBundlesHelper.getBundleNames(mContext)) {
            File bundleFile = new File(getBundleResource(mContext, String.format(TAR_FILE_NAME_FORMAT, bundleName)));
            String currentTarVersion = BRSharedPrefs.getBundleHash(mContext, bundleName);
            if (bundleFile.exists()) {
                Log.d(TAG, bundleFile + ": updateBundle: exists");
                String latestVersion = fetchBundleVersion(bundleName);

                Log.d(TAG, bundleFile + ": updateBundle: version of the current tar: " + currentTarVersion);
                if (latestVersion != null) {
                    if (latestVersion.equals(currentTarVersion)) {
                        Log.d(TAG, bundleFile + ": updateBundle: have the latest version");
                        tryExtractTar(bundleName);
                    } else {
                        Log.d(TAG, bundleFile + ": updateBundle: don't have the most recent version, download diff");
                        downloadDiff(bundleName, currentTarVersion);
                        tryExtractTar(bundleName);
                        BRSharedPrefs.putBundleHash(mContext, bundleName, latestVersion);
                    }
                } else {
                    Log.d(TAG, bundleFile + ": updateBundle: latestVersion is null");
                }

            } else {
                Log.d(TAG, bundleFile + ": updateBundle: bundle doesn't exist, downloading new copy");
                long startTime = System.currentTimeMillis();
                Request request = new Request.Builder()
                        .url(String.format("%s/assets/bundles/%s/download", getBaseURL(), bundleName))
                        .get().build();
                byte[] body;
                BRResponse response = sendRequest(request, false);
                Log.d(TAG, bundleFile + ": updateBundle: Downloaded, took: " + (System.currentTimeMillis() - startTime));
                body = writeBundleToFile(bundleName, response.getBody());
                if (Utils.isNullOrEmpty(body)) {
                    Log.e(TAG, "updateBundle: body is null, returning.");
                    return;
                }

                boolean extracted = tryExtractTar(bundleName);
                if (!extracted) {
                    Log.e(TAG, "updateBundle: Failed to extract tar");
                } else {
                    String currentBundleVersion = getCurrentBundleVersion(bundleFile);
                    if (!Utils.isNullOrEmpty(currentBundleVersion)) {
                        BRSharedPrefs.putBundleHash(mContext, bundleName, currentBundleVersion);
                    }
                }

            }

            logFiles("updateBundle after", bundleName, mContext);
        }
    }

    /**
     * Gets the bundle version of the specified bundle from the server.  This can be used to determine if a new version is available.
     *
     * @param bundleName
     * @return
     */
    public String fetchBundleVersion(String bundleName) {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        String latestVersion = null;
        Request request = new Request.Builder()
                .get()
                .url(String.format("%s/assets/bundles/%s/versions", getBaseURL(), bundleName))
                .build();

        BRResponse response = sendRequest(request, false);

        try {
            JSONObject versionsJson = new JSONObject(response.getBodyText());
            JSONArray jsonArray = versionsJson.getJSONArray("versions");
            if (jsonArray.length() == 0) {
                return null;
            }
            latestVersion = (String) jsonArray.get(jsonArray.length() - 1);

        } catch (JSONException e) {
            Log.e(TAG, "fetchBundleVersion: ", e);
        }
        return latestVersion;
    }

    // TODO: Move to ServerBundlesHelper DROID-1133, only keep the request to the API here.
    public void downloadDiff(String bundleName, String currentTarVersion) {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        Request diffRequest = new Request.Builder().url(String.format(BUNDLES_FORMAT, getBaseURL(), bundleName, currentTarVersion)).get().build();
        BRResponse resp = sendRequest(diffRequest, false);
        if (Utils.isNullOrEmpty(resp.getBodyText())) {
            Log.e(TAG, "downloadDiff: no response");
            return;
        }
        File patchFile = null;
        File tempFile = null;
        byte[] patchBytes = null;
        try {
            patchFile = new File(getBundleResource(mContext, bundleName + "-patch.diff"));
            patchBytes = resp.getBody();
            Log.e(TAG, "downloadDiff: trying to write to file");
            FileUtils.writeByteArrayToFile(patchFile, patchBytes);
            tempFile = new File(getBundleResource(mContext, bundleName + "-2temp.tar"));
            tempFile.createNewFile();
            File bundleFile = new File(getBundleResource(mContext, bundleName + "." + TAR));
            FileUI.patch(bundleFile, tempFile, patchFile);
            byte[] updatedBundleBytes = IOUtils.toByteArray(new FileInputStream(tempFile));
            if (Utils.isNullOrEmpty(updatedBundleBytes)) {
                Log.e(TAG, "downloadDiff: failed to get bytes from the updatedBundle: " + tempFile.getAbsolutePath());
            }
            FileUtils.writeByteArrayToFile(bundleFile, updatedBundleBytes);

        } catch (IOException | InvalidHeaderException | CompressorException | NullPointerException e) {
            Log.e(TAG, "downloadDiff: ", e);
            new File(getBundleResource(mContext, bundleName + "." + TAR)).delete();
        } finally {
            if (patchFile != null) {
                patchFile.delete();
            }
            if (tempFile != null) {
                tempFile.delete();
            }
        }

        logFiles("downloadDiff", bundleName, mContext);
    }

    // TODO: Move to ServerBundlesHelper DROID-1133
    public byte[] writeBundleToFile(String bundleName, byte[] response) {
        try {
            if (response == null) {
                Log.e(TAG, "writeBundleToFile: WARNING, response is null");
                return null;
            }
            File bundleFile = new File(getBundleResource(mContext, bundleName + ".tar"));
            FileUtils.writeByteArrayToFile(bundleFile, response);
            return response;
        } catch (IOException e) {
            Log.e(TAG, "writeBundleToFile: ", e);
        }

        return null;
    }

    // TODO: Move to ServerBundlesHelper DROID-1133
    public boolean tryExtractTar(String bundleName) {
        Context app = BreadApp.getBreadContext();
        if (app == null) {
            Log.e(TAG, "tryExtractTar: failed to extract, app is null");
            return false;
        }
        File bundleFile = new File(getBundleResource(mContext, bundleName + "." + TAR));
        Log.e(TAG, "tryExtractTar: " + bundleFile.getAbsolutePath());
        boolean result = false;
        TarArchiveInputStream debInputStream = null;
        try {
            final InputStream is = new FileInputStream(bundleFile);
            debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream(TAR, is);
            TarArchiveEntry entry = null;
            while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {

                final String outPutFileName = entry.getName().replace("./", "");
                final File outputFile = new File(getExtractedPath(mContext, bundleName, null), outPutFileName);
                if (!entry.isDirectory()) {
                    FileUtils.writeByteArrayToFile(outputFile, org.apache.commons.compress.utils.IOUtils.toByteArray(debInputStream));
                }
            }

            result = true;
        } catch (Exception e) {
            Log.e(TAG, "tryExtractTar: ", e);
        } finally {
            try {
                if (debInputStream != null) {
                    debInputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "tryExtractTar: ", e);
            }
        }
        logFiles("tryExtractTar", bundleName, mContext);
        return result;

    }

    private void updateFeatureFlag() {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }

        Request req = new Request.Builder()
                .url(buildUrl(FEATURE_FLAG_PATH))
                .get().build();
        BRResponse res = sendRequest(req, true);

        try {
            if (res.getBodyText().isEmpty()) {
                Log.e(TAG, "updateFeatureFlag: JSON empty");
                return;
            }

            JSONArray arr = new JSONArray(res.getBodyText());
            for (int i = 0; i < arr.length(); i++) {
                try {
                    JSONObject obj = arr.getJSONObject(i);
                    String name = obj.getString(NAME);
                    String description = obj.getString(DESCRIPTION);
                    boolean selected = obj.getBoolean(SELECTED);
                    boolean enabled = obj.getBoolean(ENABLED);
                    boolean isPrivate = obj.getBoolean(PRIVATE);
                    BRSharedPrefs.putFeatureEnabled(mContext, enabled, name);
                } catch (Exception e) {
                    Log.e(TAG, "malformed feature at position: " + i + ", whole json: " + res, e);
                }

            }
        } catch (JSONException e) {
            Log.e(TAG, "updateFeatureFlag: failed to pull up features", e);
        }

    }

    private boolean isBreadChallenge(Response resp) {
        String challenge = resp.header(BRConstants.HEADER_WWW_AUTHENTICATE);
        return challenge != null && challenge.startsWith(BREAD);
    }

    public String buildUrl(String path) {
        return getBaseURL() + path;
    }

    private class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request request = chain.request();

            long t1 = System.nanoTime();
            Log.d(TAG, String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()));

            Response response = chain.proceed(request);

            long t2 = System.nanoTime();
            Log.d(TAG, String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));

            return response;
        }
    }

    // TODO: Move to ServerBundlesHelper DROID-1133
    public void updatePlatform(final Context app) {
        if (mIsPlatformUpdating) {
            Log.e(TAG, "updatePlatform: platform already Updating!");
            return;
        }
        mIsPlatformUpdating = true;

        //update Bundle
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance(mContext);
                apiClient.updateBundle();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "updateBundle " + ServerBundlesHelper.getBundle(mContext, ServerBundlesHelper.Type.WEB) + ": DONE in " + (endTime - startTime) + "ms");
                itemFinished();
            }
        });

        //update feature flags
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance(mContext);
                apiClient.updateFeatureFlag();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "updateFeatureFlag: DONE in " + (endTime - startTime) + "ms");
                itemFinished();
            }
        });

        //update kvStore
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("updatePlatform");
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance(mContext);
                apiClient.syncKvStore();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "syncKvStore: DONE in " + (endTime - startTime) + "ms");
                itemFinished();
            }
        });

        //update fee
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                List<BaseWalletManager> wallets = new ArrayList<>(WalletsMaster.getInstance().getAllWallets(app));
                for (BaseWalletManager w : wallets) {
                    w.updateFee(app);
                }
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "update fee: DONE in " + (endTime - startTime) + "ms");
                itemFinished();
            }
        });

    }

    private void itemFinished() {
        int items = mItemsLeftToUpdate.incrementAndGet();
        if (items >= SYNC_ITEMS_COUNT) {
            Log.d(TAG, "PLATFORM ALL UPDATED: " + items);
            mIsPlatformUpdating = false;
            mItemsLeftToUpdate.set(0);
        }
    }

    private void syncKvStore() {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        final APIClient client = this;
        //sync the kv stores
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(client);
        ReplicatedKVStore kvStore = ReplicatedKVStore.getInstance(mContext, remoteKVStore);
        kvStore.syncAllKeys();
    }

    //returns the resource at bundles/path, if path is null then the bundle folder
    // TODO: Move to ServerBundlesHelper DROID-1133
    private String getBundleResource(Context app, String path) {
        String bundle = app.getFilesDir().getAbsolutePath() + ServerBundlesHelper.BUNDLES_FOLDER;
        if (Utils.isNullOrEmpty(path)) {
            return bundle;
        } else {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return bundle + path;
        }
    }

    //returns the extracted folder or the path in it
    // TODO: Move to ServerBundlesHelper DROID-1133
    public String getExtractedPath(Context app, String bundleName, String path) {
        String extractedBundleFolder = String.format("%s-extracted", bundleName);
        String extracted = app.getFilesDir().getAbsolutePath() + "/" + extractedBundleFolder;
        if (Utils.isNullOrEmpty(path)) {
            return extracted;
        } else {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return extracted + path;
        }
    }

    //too many requests will call too many BRKeyStore _getData, causing ui elements to freeze
    private synchronized byte[] getCachedAuthKey() {
        if (Utils.isNullOrEmpty(mCachedAuthKey)) {
            mCachedAuthKey = BRKeyStore.getAuthKey(mContext);
        }
        return mCachedAuthKey;
    }

    // TODO: Move to ServerBundlesHelper DROID-1133
    private void logFiles(String tag, String bundleName, Context ctx) {
        if (PRINT_FILES) {
            Log.e(TAG, "logFiles " + tag + " : START LOGGING");
            String path = getExtractedPath(ctx, bundleName, null);

            File directory = new File(path);
            File[] files = directory.listFiles();
            Log.e("Files", "Path: " + path + ", size: " + (files == null ? 0 : files.length));
            for (int i = 0; files != null && i < files.length; i++) {
                Log.e("Files", "FileName:" + files[i].getName());
            }
            Log.e(TAG, "logFiles " + tag + " : START LOGGING");
        }
    }

    public static class AuthenticatedRequest {
        private Request mRequest;
        private String mTokenUsed;

        public AuthenticatedRequest(Request request, String tokenUsed) {
            this.mRequest = request;
            this.mTokenUsed = tokenUsed;
        }

        public Request getRequest() {
            return mRequest;
        }

        public String getTokenUsed() {
            return mTokenUsed;
        }
    }


    public static class BRResponse {
        private Map<String, String> mHeaders;
        private int mCode;
        private byte[] mBody = new byte[0];
        private String mUrl = "";
        private String mContentType = "";

        public BRResponse(byte[] body, int code, Map<String, String> headers, String url, String contentType) {
            mHeaders = headers;
            mCode = code;
            mBody = body;
            mUrl = url;
            if (Utils.isNullOrEmpty(contentType)) {
                if (headers != null && headers.containsKey(BRConstants.HEADER_CONTENT_TYPE)) {
                    contentType = headers.get(BRConstants.HEADER_CONTENT_TYPE);
                    if (Utils.isNullOrEmpty(contentType)) {
                        contentType = BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8;
                    }
                }
            }

            mContentType = contentType;

        }

        public BRResponse(byte[] body, int code, String contentType) {
            this(body, code, null, null, contentType);

        }

        public BRResponse() {
            this(null, 0, null, null, null);
        }

        public BRResponse(String contentType, int code) {
            this(null, code, null, null, contentType);
        }

        public Map<String, String> getHeaders() {
            return mHeaders == null ? new HashMap<String, String>() : mHeaders;
        }

        public int getCode() {
            if (mCode == 0) {
                throw new RuntimeException("code can't be 0");
            }
            return mCode;
        }

        public byte[] getBody() {
            return mBody;
        }

        public String getBodyText() {
            if (!Utils.isNullOrEmpty(mBody)) {
                return new String(mBody);
            } else {
                return "";
            }
        }

        public String getUrl() {
            return mUrl;
        }

        public String getContentType() {
            return mContentType;
        }

        public void setContentType(String contentType) {
            mContentType = contentType;
        }

        public boolean isSuccessful() {
            return mCode >= HttpStatus.OK_200 && mCode < HttpStatus.MULTIPLE_CHOICES_300;
        }

        public void print() {
            String logText = String.format(Locale.getDefault(), "%s (%d)|%s|", mUrl, mCode, getBodyText());
            if (isSuccessful()) {
                Log.d(TAG, "BRResponse: " + logText);
            } else {
                Log.e(TAG, "BRResponse: " + logText);
            }
        }

        public void setBody(byte[] body) {
            mBody = body;
        }

        public void setCode(int code) {
            mCode = code;
        }
    }

    public static String getBaseURL() {
        if (BuildConfig.DEBUG) {
            // In the debug case, the user may have changed the host.
            return HTTPS_SCHEME + BreadApp.getHost();
        }
        return BASE_URL;
    }

    private void logRequestAndResponse(Request request, Response response) {
        StringBuffer reportStringBuffer = new StringBuffer();
        reportStringBuffer.append("Request:\n");
        reportStringBuffer.append(request.url());
        reportStringBuffer.append("\n");
        reportStringBuffer.append(request.headers().toString());
        reportStringBuffer.append(bodyToString(request));
        reportStringBuffer.append("\n\n");
        reportStringBuffer.append("Response:\n");
        reportStringBuffer.append(response.code());
        reportStringBuffer.append(response.message());
        reportStringBuffer.append("\n");
        reportStringBuffer.append(response.headers().toString());
        reportStringBuffer.append("\n");
        Log.e(TAG, "sendRequest: Not successful: \n" + reportStringBuffer.toString());
    }

    /**
     * Convert {@link Request} to a {@link String}.
     *
     * Reference: <a href="https://stackoverflow.com/a/29033727/3211679">stackoverflow</a>
     *
     * @param request The request to convert to a {@link String}.
     * @return The {@link String} version of the specified {@link Request}.
     */

    private static String bodyToString(final Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            RequestBody body = copy.body();
            if (body != null) {
                body.writeTo(buffer);
            }
            return buffer.readUtf8();
        } catch (final IOException e) {
            return null;
        }
    }

}
