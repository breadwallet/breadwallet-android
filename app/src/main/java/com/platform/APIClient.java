package com.platform;


import android.accounts.AuthenticatorException;
import android.content.Context;
import android.net.Uri;
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
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.util.JsonRpcHelper;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;
import com.platform.tools.TokenHolder;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

import static com.breadwallet.tools.manager.BRApiManager.HEADER_WALLET_ID;

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

import static com.breadwallet.tools.util.BRCompressor.gZipExtract;


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

    private static final String GMT = "GMT";
    private static final String BREAD = "bread";

    // convenience getter for the API endpoint
    public static final String BASE_URL = PROTO + "://" + BreadApp.HOST;
    //Fee per kb url
    private static final String FEE_PER_KB_URL = "/v1/fee-per-kb";
    //token path
    private static final String TOKEN = "/token";
    //me path
    private static final String ME = "/me";
    //singleton instance
    private static APIClient ourInstance;

    private byte[] mCachedAuthKey;

    private boolean mIsFetchingToken;

    private OkHttpClient mHTTPClient;

    private static final String BUNDLES_FOLDER = "/bundles";
    private static final String BRD_WEB = "brd-web-3";
    private static final String BRD_WEB_STAGING = "brd-web-3-staging";
    public static final String BRD_TOKEN_ASSETS = "brd-tokens-prod";
    public static final String BRD_TOKEN_ASSETS_STAGING = "brd-tokens-staging";
    private static final String TAR_FILE_NAME_FORMAT = "/%s.tar";

    public static final String WEB_BUNDLE_NAME = BuildConfig.DEBUG ? BRD_WEB_STAGING : BRD_WEB;
    public static final String TOKEN_ASSETS_BUNDLE_NAME = BuildConfig.DEBUG ? BRD_TOKEN_ASSETS_STAGING : BRD_TOKEN_ASSETS;
    public static final String[] BUNDLE_NAMES = {WEB_BUNDLE_NAME, TOKEN_ASSETS_BUNDLE_NAME};

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    private static final boolean PRINT_FILES = false;
    private static final int MAX_RETRY = 3;
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;

    private boolean mIsPlatformUpdating = false;
    private AtomicInteger mItemsLeftToUpdate = new AtomicInteger(0);

    private Context mContext;

    public enum FeatureFlags {
        BUY_BITCOIN("buy-bitcoin"),
        EARLY_ACCESS("early-access");

        private final String text;

        /**
         * @param text
         */
        FeatureFlags(final String text) {
            this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
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
    }

    //returns the fee per kb or 0 if something went wrong
    public long feePerKb() {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        try {
            String strUtl = BASE_URL + FEE_PER_KB_URL;
            Request request = new Request.Builder().url(strUtl).get().build();
            BRResponse response = sendRequest(request, false);
            JSONObject object = new JSONObject(response.getBodyText());
            return (long) object.getInt("fee_per_kb");
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
        String strUtl = BASE_URL + ME;
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
            String strUtl = BASE_URL + TOKEN;

            JSONObject requestMessageJSON = new JSONObject();
            String base58PubKey = BRCoreKey.getAuthPublicKeyForAPI(getCachedAuthKey());
            requestMessageJSON.put("pubKey", base58PubKey);
            requestMessageJSON.put("deviceID", BRSharedPrefs.getDeviceId(mContext));

            final MediaType JSON
                    = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, requestMessageJSON.toString());
            Request request = new Request.Builder()
                    .url(strUtl)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(requestBody).build();
            BRResponse response = sendRequest(request, false);
            if (Utils.isNullOrEmpty(response.getBodyText())) {
                Log.e(TAG, "getToken: retrieving token failed");
                return null;
            }
            JSONObject obj = null;
            obj = new JSONObject(response.getBodyText());

            return obj.getString("token");
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
        Map<String, String> headers = new HashMap<>(BreadApp.getBreadHeaders());

        Request.Builder newBuilder = locRequest.newBuilder();
        for (String key : headers.keySet()) {
            String value = headers.get(key);
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
            request = request.newBuilder().header("User-agent", Utils.getAgentString(mContext, "OkHttp/3.4.1")).build();
            rawResponse = mHTTPClient.newCall(request).execute();
        } catch (IOException e) {
            Log.e(TAG, "sendRequest: ", e);
            BRReportsManager.reportBug(e);
            return new Response.Builder().code(599).request(request)
                    .body(ResponseBody.create(null, e.getMessage())).message(e.getMessage()).protocol(Protocol.HTTP_1_1).build();
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

        if (rawResponse.header("content-encoding") != null && rawResponse.header("content-encoding").equalsIgnoreCase("gzip")) {
            Log.d(TAG, "sendRequest: the content is gzip, unzipping");

            byte[] decompressed = gZipExtract(bytesBody);
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
            if (response.isRedirect()) {
                String newLocation = request.url().scheme() + "://" + request.url().host() + response.header("location");
                Uri newUri = Uri.parse(newLocation);
                if (newUri == null) {
                    Log.e(TAG, "sendRequest: redirect uri is null");
                    return createBrResponse(response);
                } else if (!Utils.isEmulatorOrDebug(mContext) && (!newUri.getHost().equalsIgnoreCase(BreadApp.HOST)
                        || !newUri.getScheme().equalsIgnoreCase(PROTO))) {
                    Log.e(TAG, "sendRequest: WARNING: redirect is NOT safe: " + newLocation);
                    BRReportsManager.reportBug(new IllegalAccessException("redirect is NOT safe: " + newLocation));
                    return createBrResponse(new Response.Builder().code(500).request(request)
                            .body(ResponseBody.create(null, new byte[0])).protocol(Protocol.HTTP_1_1).build());
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

    public AuthenticatedRequest authenticateRequest(Request request) {
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

        request = modifiedRequest.header(JsonRpcHelper.DATE, httpDate.substring(0, httpDate.indexOf(GMT) + GMT.length())).build();

        String queryString = request.url().encodedQuery();

        String requestString = createRequest(request.method(), base58Body, request.header(BRConstants.HEADER_CONTENT_TYPE),
                request.header(JsonRpcHelper.DATE), request.url().encodedPath()
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

    public synchronized void updateBundle() {
        for (String bundleName : BUNDLE_NAMES) {
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
                        .url(String.format("%s/assets/bundles/%s/download", BASE_URL, bundleName))
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
                .url(String.format("%s/assets/bundles/%s/versions", BASE_URL, bundleName))
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

    public void downloadDiff(String bundleName, String currentTarVersion) {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        Request diffRequest = new Request.Builder()
                .url(String.format("%s/assets/bundles/%s/diff/%s", BASE_URL, bundleName, currentTarVersion))
                .get().build();
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
            boolean a = tempFile.createNewFile();
            File bundleFile = new File(getBundleResource(mContext, bundleName + ".tar"));
            FileUI.patch(bundleFile, tempFile, patchFile);
            byte[] updatedBundleBytes = IOUtils.toByteArray(new FileInputStream(tempFile));
            if (Utils.isNullOrEmpty(updatedBundleBytes))
                Log.e(TAG, "downloadDiff: failed to get bytes from the updatedBundle: " + tempFile.getAbsolutePath());
            FileUtils.writeByteArrayToFile(bundleFile, updatedBundleBytes);

        } catch (IOException | InvalidHeaderException | CompressorException | NullPointerException e) {
            Log.e(TAG, "downloadDiff: ", e);
            new File(getBundleResource(mContext, bundleName + ".tar")).delete();
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

    public boolean tryExtractTar(String bundleName) {
        Context app = BreadApp.getBreadContext();
        if (app == null) {
            Log.e(TAG, "tryExtractTar: failed to extract, app is null");
            return false;
        }
        File bundleFile = new File(getBundleResource(mContext, bundleName + ".tar"));
        Log.d(TAG, "tryExtractTar: " + bundleFile.getAbsolutePath());
        boolean result = false;
        TarArchiveInputStream debInputStream = null;
        try {
            final InputStream is = new FileInputStream(bundleFile);
            debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
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
        String furl = "/me/features";
        Request req = new Request.Builder()
                .url(buildUrl(furl))
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
                    String name = obj.getString("name");
                    String description = obj.getString("description");
                    boolean selected = obj.getBoolean("selected");
                    boolean enabled = obj.getBoolean("enabled");
                    boolean isPrivate = obj.getBoolean("private");
                    BRSharedPrefs.putFeatureEnabled(mContext, enabled, name);
                } catch (Exception e) {
                    Log.e(TAG, "malformed feature at position: " + i + ", whole json: " + res, e);
                }

            }
        } catch (JSONException e) {
            Log.e(TAG, "updateFeatureFlag: failed to pull up features", e);
        }

    }

    public boolean isBreadChallenge(Response resp) {
        String challenge = resp.header("www-authenticate");
        return challenge != null && challenge.startsWith("bread");
    }

    public boolean isFeatureEnabled(String feature) {
        return BRSharedPrefs.getFeatureEnabled(mContext, feature);
    }

    public String buildUrl(String path) {
        return BASE_URL + path;
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
                Thread.currentThread().setName("UpdateBundle");
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance(mContext);
                apiClient.updateBundle();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "updateBundle " + WEB_BUNDLE_NAME + ": DONE in " + (endTime - startTime) + "ms");
                itemFinished();
            }
        });

        //update feature flags
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(2));
                } catch (InterruptedException e) {
                    Log.e(TAG, "updateFeatureFlag: ", e);
                }
                Thread.currentThread().setName("updateFeatureFlag");
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
                List<BaseWalletManager> wallets = new ArrayList<>(WalletsMaster.getInstance(app).getAllWallets(app));
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
        if (items >= 4) {
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
    public String getBundleResource(Context app, String path) {
        String bundle = app.getFilesDir().getAbsolutePath() + BUNDLES_FOLDER;
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

        public AuthenticatedRequest(Request mRequest, String tokenUsed) {
            this.mRequest = mRequest;
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
        private Map<String, String> headers;
        public int code;
        public byte[] body = new byte[0];
        private String url = "";
        private String contentType = "";

        public BRResponse(byte[] body, int code, Map<String, String> headers, String url, String contentType) {
            this.headers = headers;
            this.code = code;
            this.body = body;
            this.url = url;
            if (Utils.isNullOrEmpty(contentType)) {
                if (headers != null && headers.containsKey("Content-Type")) {
                    contentType = headers.get("Content-Type");
                    if (Utils.isNullOrEmpty(contentType)) {
                        contentType = "application/json";
                    }
                }
            }

            this.contentType = contentType;

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
            return headers == null ? new HashMap<String, String>() : headers;
        }

        public int getCode() {
            if (code == 0) {
                throw new RuntimeException("code can't be 0");
            }
            return code;
        }

        public byte[] getBody() {
            return body;
        }

        public String getBodyText() {
            if (!Utils.isNullOrEmpty(body)) {
                return new String(body);
            } else {
                return "";
            }
        }

        public String getUrl() {
            return url;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public boolean isSuccessful() {
            return code >= 200 && code < 300;
        }

        public void print() {
            String logText = String.format(Locale.getDefault(), "%s (%d)|%s|", url, code, getBodyText());
            if (isSuccessful())
                Log.d(TAG, "BRResponse: " + logText);
            else Log.e(TAG, "BRResponse: " + logText);
        }
    }

}
