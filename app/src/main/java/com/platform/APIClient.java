package com.platform;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.tools.crypto.Base58;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;

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
import java.io.UnsupportedEncodingException;
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

    // convenience getter for the API endpoint
    public static String BASE_URL = PROTO + "://" + BreadApp.HOST;
    //feePerKb url
    private static final String FEE_PER_KB_URL = "/v1/fee-per-kb";
    //token
    private static final String TOKEN = "/token";
    //me
    private static final String ME = "/me";
    //singleton instance
    private static APIClient ourInstance;

    private byte[] mCachedToken;
    private byte[] mCachedAuthKey;

    private OkHttpClient mHTTPClient;

    public static final String BUNDLES = "bundles";
    public static String BREAD_POINT = "brd-web";

    private static final String BUNDLES_FOLDER = String.format("/%s", BUNDLES);

    private static String BREAD_FILE;
    private static String BREAD_EXTRACTED;
    private static final boolean PRINT_FILES = false;

    private SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    private boolean platformUpdating = false;
    private AtomicInteger itemsLeftToUpdate = new AtomicInteger(0);

    public static HTTPServer server;

    private Context ctx;

    public enum FeatureFlags {
        BUY_BITCOIN("buy-bitcoin"),
        EARLY_ACCESS("early-access");

        private final String text;

        /**
         * @param text
         */
        private FeatureFlags(final String text) {
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

        if (ourInstance == null) ourInstance = new APIClient(context);
        return ourInstance;
    }

    private APIClient(Context context) {
        ctx = context;
        itemsLeftToUpdate = new AtomicInteger(0);
        if (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            BREAD_POINT = "brd-web-staging";
        }
        BREAD_FILE = String.format("/%s.tar", BREAD_POINT);
        BREAD_EXTRACTED = String.format("%s-extracted", BREAD_POINT);
    }

    //returns the fee per kb or 0 if something went wrong
    public long feePerKb() {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        try {
            String strUtl = BASE_URL + FEE_PER_KB_URL;
            Request request = new Request.Builder().url(strUtl).get().build();
            BRResponse response = sendRequest(request, false, 0);
            JSONObject object = null;
            object = new JSONObject(response.getBodyText());
            return (long) object.getInt("fee_per_kb");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //only for testing
    public String buyBitcoinMe() {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        if (ctx == null) ctx = BreadApp.getBreadContext();
        if (ctx == null) return null;
        String strUtl = BASE_URL + ME;
        Request request = new Request.Builder()
                .url(strUtl)
                .get()
                .build();
        BRResponse response = sendRequest(request, true, 0);

        if (response == null) throw new NullPointerException();

        return response.getBodyText();
    }

    public synchronized String getToken() {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        if (ctx == null) ctx = BreadApp.getBreadContext();
        if (ctx == null) return null;
        try {
            String strUtl = BASE_URL + TOKEN;

            JSONObject requestMessageJSON = new JSONObject();
            String base58PubKey = BRCoreKey.getAuthPublicKeyForAPI(getCachedAuthKey());
            requestMessageJSON.put("pubKey", base58PubKey);
            requestMessageJSON.put("deviceID", BRSharedPrefs.getDeviceId(ctx));

            final MediaType JSON
                    = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, requestMessageJSON.toString());
            Request request = new Request.Builder()
                    .url(strUtl)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(requestBody).build();
            BRResponse response = sendRequest(request, false, 0);
            if (Utils.isNullOrEmpty(response.getBodyText())) {
                Log.e(TAG, "getToken: retrieving token failed");
                return null;
            }
            JSONObject obj = null;
            obj = new JSONObject(response.getBodyText());
            String token = obj.getString("token");
            if (!Utils.isNullOrEmpty(token))
                BRKeyStore.putToken(token.getBytes(), ctx);
            return token;
        } catch (JSONException e) {
            e.printStackTrace();

        }
        return null;

    }

    private String createRequest(String reqMethod, String base58Body, String contentType, String dateHeader, String url) {
        return (reqMethod == null ? "" : reqMethod) + "\n" +
                (base58Body == null ? "" : base58Body) + "\n" +
                (contentType == null ? "" : contentType) + "\n" +
                (dateHeader == null ? "" : dateHeader) + "\n" +
                (url == null ? "" : url);
    }

    public String signRequest(String request) {
//        Log.d(TAG, "signRequest: " + request);
        byte[] doubleSha256 = CryptoHelper.doubleSha256(request.getBytes(StandardCharsets.UTF_8));
        BRCoreKey key;
        try {
            byte[] authKey = getCachedAuthKey();
            if (Utils.isNullOrEmpty(authKey)) {
                BRReportsManager.reportBug(new IllegalArgumentException("Auth key is null!"));
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

    @NonNull
    public BRResponse sendRequest(Request locRequest, boolean needsAuth, int retryCount) {
//        Log.d(TAG, "sendRequest, url -> " + locRequest.url().toString());
        if (retryCount > 1)
            throw new RuntimeException("sendRequest: Warning retryCount is: " + retryCount);
        if (ActivityUTILS.isMainThread()) {
            Log.e(TAG, "urlGET: network on main thread");
            throw new RuntimeException("network on main thread");
        }

        Map<String, String> headers = BreadApp.getBreadHeaders();

        Request.Builder newBuilder = locRequest.newBuilder();
        for (String key : headers.keySet()) {
            String value = headers.get(key);
            newBuilder.header(key, value);
        }

        Request request = newBuilder.build();
        if (needsAuth) {
            request = authenticateRequest(request);
            if (request == null) return resToBRResponse(null);
        }

        Response response = null;
        BRResponse mainBrResponse = null;
        try {
            if (mHTTPClient == null)
                mHTTPClient = new OkHttpClient.Builder().followRedirects(false).connectTimeout(20, TimeUnit.SECONDS)
                        /*.addInterceptor(new LoggingInterceptor())*/.build();
            request = request.newBuilder().header("User-agent", Utils.getAgentString(ctx, "OkHttp/3.4.1")).build();
            response = mHTTPClient.newCall(request).execute();
            mainBrResponse = resToBRResponse(response);
            if (response.isRedirect()) {
                String newLocation = request.url().scheme() + "://" + request.url().host() + response.header("location");
                Uri newUri = Uri.parse(newLocation);
                if (newUri == null) {
                    Log.e(TAG, "sendRequest: redirect uri is null");
                } else if (!Utils.isEmulatorOrDebug(ctx) && (!newUri.getHost().equalsIgnoreCase(BreadApp.HOST) || !newUri.getScheme().equalsIgnoreCase(PROTO))) {
                    Log.e(TAG, "sendRequest: WARNING: redirect is NOT safe: " + newLocation);
                } else {
                    Log.w(TAG, "redirecting: " + request.url() + " >>> " + newLocation);
                    response.close();
                    return sendRequest(new Request.Builder().url(newLocation).get().build(), needsAuth, 0);
                }
                return resToBRResponse(new Response.Builder().code(500).request(request)
                        .body(ResponseBody.create(null, new byte[0])).protocol(Protocol.HTTP_1_1).build());
            }
        } catch (IOException e) {
            Log.e(TAG, "sendRequest: ", e);
            return resToBRResponse(new Response.Builder().code(599).request(request)
                    .body(ResponseBody.create(null, e.getMessage())).protocol(Protocol.HTTP_1_1).build());
        }
        ResponseBody postReqBody = null;
        try {
            if (response.header("content-encoding") != null && response.header("content-encoding").equalsIgnoreCase("gzip")) {
                Log.d(TAG, "sendRequest: the content is gzip, unzipping");
                if (Utils.isNullOrEmpty(mainBrResponse.getBody())) {
                    BRReportsManager.reportBug(new NullPointerException("string response is null for: " + request.url()));
                    return resToBRResponse(response);
                }
                byte[] decompressed = gZipExtract(mainBrResponse.getBody());
                if (decompressed == null) {
                    BRReportsManager.reportBug(new IllegalArgumentException("failed to decrypt data!"));
                    return resToBRResponse(response);
                }
                postReqBody = ResponseBody.create(null, decompressed);
                return resToBRResponse(response.newBuilder().body(postReqBody).build());
            } else {
                Log.d(TAG, "sendRequest: " + String.format(Locale.getDefault(), "(%s)%s, code (%d), mess (%s), body (%s)", request.method(),
                        request.url(), response.code(), response.message(), mainBrResponse.getBodyText()));
            }

            byte[] data = mainBrResponse.getBody();
            if (Utils.isNullOrEmpty(data)) {
                Log.e(TAG, "sendRequest: no data!");
            }
            if (data != null)
                postReqBody = ResponseBody.create(null, data);
            if (needsAuth && isBreadChallenge(response)) {
                Log.d(TAG, "sendRequest: got authentication challenge from API - will attempt to get token, url -> " + locRequest.url().toString());
                byte[] bytesToken = getCachedToken();
                String token = bytesToken == null ? "" : new String(bytesToken);
                //Double check if we have the token
                if (Utils.isNullOrEmpty(token))
                    getToken();

                if (retryCount < 1) {
                    response.close();
                    sendRequest(request, true, retryCount + 1);
                } else {
                    getToken(); //update token if it failed the second time.
                }
            }
        } finally {
            if (postReqBody != null) postReqBody.close();
        }
        if (postReqBody == null) return resToBRResponse(response);

        return resToBRResponse(response.newBuilder().body(postReqBody).build());
    }

    private void cleanRespones(Response res) {
        if (res != null) res.close();
    }

    private BRResponse resToBRResponse(Response res) {
        BRResponse brRsp = new BRResponse();
        try {
            if (res != null) {
                int code = res.code();
                Map<String, String> headers = new HashMap<>();
                for (String name : res.headers().names()) {
                    headers.put(name, res.header(name));
                }

                byte[] s = null;
                String contentType = null;
                try {
                    ResponseBody body = res.body();
                    contentType = body.contentType() == null ? "" : body.contentType().type();
                    s = body.bytes();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    res.close();
                }
                brRsp = new BRResponse(s, code, headers, res.request().url().toString(), contentType);
            }

        } finally {
            brRsp.print();
        }
        return brRsp;
    }

    public Request authenticateRequest(Request request) {
        Request.Builder modifiedRequest = request.newBuilder();
        String base58Body = "";
        RequestBody body = request.body();

        try {
            if (body != null && body.contentLength() != 0) {
                BufferedSink sink = new Buffer();
                try {
                    body.writeTo(sink);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] bytes = sink.buffer().readByteArray();
                base58Body = CryptoHelper.base58ofSha256(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String httpDate = sdf.format(new Date());

        request = modifiedRequest.header("Date", httpDate.substring(0, httpDate.length() - 6)).build();

        String queryString = request.url().encodedQuery();

        String requestString = createRequest(request.method(), base58Body, request.header("Content-Type"),
                request.header("Date"), request.url().encodedPath()
                        + ((queryString != null && !queryString.isEmpty()) ? ("?" + queryString) : ""));
        String signedRequest = signRequest(requestString);
        if (signedRequest == null) return null;
        byte[] tokenBytes = getCachedToken();
        String token = tokenBytes == null ? "" : new String(tokenBytes);
        if (token.isEmpty()) token = getToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "sendRequest: failed to retrieve token");
            return null;
        }
        String authValue = "bread " + token + ":" + signedRequest;
//            Log.e(TAG, "sendRequest: authValue: " + authValue);
        modifiedRequest = request.newBuilder();

        try {
            request = modifiedRequest.header("Authorization", authValue).build();
        } catch (Exception e) {
            BRReportsManager.reportBug(e);
            return null;
        }
        return request;
    }

    public synchronized void updateBundle() {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        File bundleFile = new File(getBundleResource(ctx, BREAD_FILE));
        if (bundleFile.exists()) {
            Log.d(TAG, bundleFile + ": updateBundle: exists");

            byte[] bFile = new byte[0];
            try {
                FileInputStream in = new FileInputStream(bundleFile);
                bFile = IOUtils.toByteArray(in);
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String latestVersion = getLatestVersion();
            String currentTarVersion = null;
            byte[] hash = CryptoHelper.sha256(bFile);

            currentTarVersion = Utils.bytesToHex(hash);
            Log.d(TAG, bundleFile + ": updateBundle: version of the current tar: " + currentTarVersion);
//            FileHelper.printDirectoryTree(new File(getExtractedPath(ctx, null)));
            if (latestVersion != null) {
                if (latestVersion.equals(currentTarVersion)) {
                    Log.d(TAG, bundleFile + ": updateBundle: have the latest version");
                    tryExtractTar();
                } else {
                    Log.d(TAG, bundleFile + ": updateBundle: don't have the most recent version, download diff");
                    downloadDiff(currentTarVersion);
                    tryExtractTar();
                }
            } else {
                Log.d(TAG, bundleFile + ": updateBundle: latestVersion is null");
            }
//            FileHelper.printDirectoryTree(new File(getExtractedPath(ctx, null)));

        } else {
            Log.d(TAG, bundleFile + ": updateBundle: bundle doesn't exist, downloading new copy");
            long startTime = System.currentTimeMillis();
            Request request = new Request.Builder()
                    .url(String.format("%s/assets/bundles/%s/download", BASE_URL, BREAD_POINT))
                    .get().build();
            byte[] body;
            BRResponse response = sendRequest(request, false, 0);
            Log.d(TAG, bundleFile + ": updateBundle: Downloaded, took: " + (System.currentTimeMillis() - startTime));
            body = writeBundleToFile(response.getBody());
            if (Utils.isNullOrEmpty(body)) {
                Log.e(TAG, "updateBundle: body is null, returning.");
                return;
            }

            boolean b = tryExtractTar();
            if (!b) {
                Log.e(TAG, "updateBundle: Failed to extract tar");
            }
        }

        logFiles("updateBundle after", ctx);
    }

    public String getLatestVersion() {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        String latestVersion = null;

        BRResponse response = sendRequest(new Request.Builder()
                .get()
                .url(String.format("%s/assets/bundles/%s/versions", BASE_URL, BREAD_POINT))
                .build(), false, 0);

        if (response == null) return null;
        try {
            JSONObject versionsJson = new JSONObject(response.getBodyText());
            JSONArray jsonArray = versionsJson.getJSONArray("versions");
            if (jsonArray.length() == 0) return null;
            latestVersion = (String) jsonArray.get(jsonArray.length() - 1);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return latestVersion;
    }

    public void downloadDiff(String currentTarVersion) {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        Request diffRequest = new Request.Builder()
                .url(String.format("%s/assets/bundles/%s/diff/%s", BASE_URL, BREAD_POINT, currentTarVersion))
                .get().build();
        BRResponse resp = sendRequest(diffRequest, false, 0);
        if (Utils.isNullOrEmpty(resp.getBodyText())) {
            Log.e(TAG, "downloadDiff: no response");
            return;
        }
        File patchFile = null;
        File tempFile = null;
        byte[] patchBytes = null;
        try {
            patchFile = new File(getBundleResource(ctx, BREAD_POINT + "-patch.diff"));
            patchBytes = resp.getBody();
            Log.e(TAG, "downloadDiff: trying to write to file");
            FileUtils.writeByteArrayToFile(patchFile, patchBytes);
            tempFile = new File(getBundleResource(ctx, BREAD_POINT + "-2temp.tar"));
            boolean a = tempFile.createNewFile();
            File bundleFile = new File(getBundleResource(ctx, BREAD_POINT + ".tar"));
            FileUI.patch(bundleFile, tempFile, patchFile);
            byte[] updatedBundleBytes = IOUtils.toByteArray(new FileInputStream(tempFile));
            if (Utils.isNullOrEmpty(updatedBundleBytes))
                Log.e(TAG, "downloadDiff: failed to get bytes from the updatedBundle: " + tempFile.getAbsolutePath());
            FileUtils.writeByteArrayToFile(bundleFile, updatedBundleBytes);

        } catch (IOException | InvalidHeaderException | CompressorException | NullPointerException e) {
            Log.e(TAG, "downloadDiff: ", e);
            new File(getBundleResource(ctx, BREAD_POINT + ".tar")).delete();
        } finally {
            if (patchFile != null)
                patchFile.delete();
            if (tempFile != null)
                tempFile.delete();
        }

        logFiles("downloadDiff", ctx);
    }

    public byte[] writeBundleToFile(byte[] response) {
        try {
            if (response == null) {
                Log.e(TAG, "writeBundleToFile: WARNING, response is null");
                return null;
            }
            File bundleFile = new File(getBundleResource(ctx, BREAD_POINT + ".tar"));
            FileUtils.writeByteArrayToFile(bundleFile, response);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean tryExtractTar() {
        Context app = BreadApp.getBreadContext();
        if (app == null) {
            Log.e(TAG, "tryExtractTar: failed to extract, app is null");
            return false;
        }
        File bundleFile = new File(getBundleResource(ctx, BREAD_POINT + ".tar"));
        Log.e(TAG, "tryExtractTar: " + bundleFile.getAbsolutePath());
        boolean result = false;
        TarArchiveInputStream debInputStream = null;
        try {
            final InputStream is = new FileInputStream(bundleFile);
            debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
            TarArchiveEntry entry = null;
            while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {

                final String outPutFileName = entry.getName().replace("./", "");
                final File outputFile = new File(getExtractedPath(ctx, null), outPutFileName);
                if (!entry.isDirectory()) {
                    FileUtils.writeByteArrayToFile(outputFile, org.apache.commons.compress.utils.IOUtils.toByteArray(debInputStream));
                }
            }

            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (debInputStream != null)
                    debInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logFiles("tryExtractTar", ctx);
        return result;

    }

    public void updateFeatureFlag() {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        String furl = "/me/features";
        Request req = new Request.Builder()
                .url(buildUrl(furl))
                .get().build();
        BRResponse res = sendRequest(req, true, 0);

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
                    BRSharedPrefs.putFeatureEnabled(ctx, enabled, name);
                } catch (Exception e) {
                    Log.e(TAG, "malformed feature at position: " + i + ", whole json: " + res, e);
                }

            }
        } catch (JSONException e) {
            Log.e(TAG, "updateFeatureFlag: failed to pull up features");
            e.printStackTrace();
        }

    }

    public boolean isBreadChallenge(Response resp) {
        String challenge = resp.header("www-authenticate");
        return challenge != null && challenge.startsWith("bread");
    }

    public boolean isFeatureEnabled(String feature) {
        boolean b = BRSharedPrefs.getFeatureEnabled(ctx, feature);
//        Log.e(TAG, "isFeatureEnabled: " + feature + " - " + b);
        return b;
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
        if (platformUpdating) {
            Log.e(TAG, "updatePlatform: platform already Updating!");
            return;
        }
        platformUpdating = true;

        //update Bundle
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("UpdateBundle");
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance(ctx);
                apiClient.updateBundle();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "updateBundle " + BREAD_POINT + ": DONE in " + (endTime - startTime) + "ms");
                itemFinished();
            }
        });

        //update feature flags
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Thread.currentThread().setName("updateFeatureFlag");
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance(ctx);
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
                APIClient apiClient = APIClient.getInstance(ctx);
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
        int items = itemsLeftToUpdate.incrementAndGet();
        if (items >= 4) {
            Log.d(TAG, "PLATFORM ALL UPDATED: " + items);
            platformUpdating = false;
            itemsLeftToUpdate.set(0);
        }
    }

    public void syncKvStore() {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        final APIClient client = this;
        //sync the kv stores
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(client);
        ReplicatedKVStore kvStore = ReplicatedKVStore.getInstance(ctx, remoteKVStore);
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
    public String getExtractedPath(Context app, String path) {
        String extracted = app.getFilesDir().getAbsolutePath() + "/" + BREAD_EXTRACTED;
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
    private byte[] getCachedToken() {
        if (Utils.isNullOrEmpty(mCachedToken)) {
            mCachedToken = BRKeyStore.getToken(ctx);
            BreadApp.addOnBackgroundedListener(new BreadApp.OnAppBackgrounded() {
                @Override
                public void onBackgrounded() {
                    mCachedToken = null;
                }
            });
        }
        return mCachedToken;
    }

    //too many requests will call too many BRKeyStore _getData, causing ui elements to freeze
    private byte[] getCachedAuthKey() {
        if (Utils.isNullOrEmpty(mCachedAuthKey)) {
            mCachedAuthKey = BRKeyStore.getAuthKey(ctx);
            BreadApp.addOnBackgroundedListener(new BreadApp.OnAppBackgrounded() {
                @Override
                public void onBackgrounded() {
                    mCachedAuthKey = null;
                }
            });
        }
        return mCachedAuthKey;
    }

    public void logFiles(String tag, Context ctx) {
        if (PRINT_FILES) {
            Log.e(TAG, "logFiles " + tag + " : START LOGGING");
            String path = getExtractedPath(ctx, null);

            File directory = new File(path);
            File[] files = directory.listFiles();
            Log.e("Files", "Path: " + path + ", size: " + (files == null ? 0 : files.length));
            for (int i = 0; files != null && i < files.length; i++) {
                Log.e("Files", "FileName:" + files[i].getName());
            }
            Log.e(TAG, "logFiles " + tag + " : START LOGGING");
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
                    if (Utils.isNullOrEmpty(contentType)) contentType = "application/json";
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
            if (code == 0) throw new RuntimeException("code can't be 0");
            return code;
        }

        public byte[] getBody() {
            return body;
        }

        public String getBodyText() {
            if (!Utils.isNullOrEmpty(body)) return new String(body);
            else return "";
        }

        public String getUrl() {
            return url;
        }

        public String getContentType() {
            return contentType;
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
