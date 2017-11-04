package com.platform;

import android.annotation.TargetApi;
import android.app.Activity;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.tools.crypto.Base58;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;
import com.jniwrappers.BRKey;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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


    private static final String BUNDLES = "bundles";
    public static String BREAD_POINT = "bread-frontend-staging"; //todo make this production

    private static final String BUNDLES_FOLDER = String.format("/%s", BUNDLES);

    private static String BREAD_FILE;
    private static String BREAD_EXTRACTED;
    private static final boolean PRINT_FILES = false;


    private boolean platformUpdating = false;
    private AtomicInteger itemsLeftToUpdate = new AtomicInteger(0);

    public static HTTPServer server;

    private Context ctx;

    public enum FeatureFlags {
        BUY_BITCOIN("buy-litecoin"),
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

    public static synchronized APIClient getInstance() {

        if (ourInstance == null) ourInstance = new APIClient(BreadApp.getInstance());
        return ourInstance;
    }

    private APIClient(Context context) {
        ctx = context;
        itemsLeftToUpdate = new AtomicInteger(0);
        if (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            BREAD_POINT = "bread-frontend-staging";
            BREAD_FILE = String.format("/%s.tar", BREAD_POINT);
            BREAD_EXTRACTED = String.format("%s-extracted", BREAD_POINT);
        }
    }

    //returns the fee per kb or 0 if something went wrong
    public long feePerKb() {

        try {
            String strUtl = BASE_URL + FEE_PER_KB_URL;
            Request request = new Request.Builder().url(strUtl).get().build();
            String body = null;
            try {
                Response response = sendRequest(request, false, 0);
                body = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            JSONObject object = null;
            object = new JSONObject(body);
            return (long) object.getInt("fee_per_kb");
        } catch (JSONException e) {
            e.printStackTrace();

        }
        return 0;
    }

    //only for testing
    public Response buyBitcoinMe() {
        if (ctx == null) ctx = BreadApp.getInstance();
        String strUtl = BASE_URL + ME;
        Request request = new Request.Builder()
                .url(strUtl)
                .get()
                .build();
        String response = null;
        Response res = null;
        try {
            res = sendRequest(request, true, 0);
            response = res.body().string();
            if (response.isEmpty()) {
                res = sendRequest(request, true, 0);
                response = res.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response == null) throw new NullPointerException();

        return res;
    }

    public String getToken() {
        if (ctx == null) ctx = BreadApp.getInstance();
        try {
            String strUtl = BASE_URL + TOKEN;

            JSONObject requestMessageJSON = new JSONObject();
            String base58PubKey = BRWalletManager.getAuthPublicKeyForAPI(BRKeyStore.getAuthKey(ctx));
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
            String strResponse = null;
            Response response;
            try {
                response = sendRequest(request, false, 0);
                if (response != null)
                    strResponse = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Utils.isNullOrEmpty(strResponse)) {
                Log.e(TAG, "getToken: retrieving token failed");
                return null;
            }
            JSONObject obj = null;
            obj = new JSONObject(strResponse);
            String token = obj.getString("token");
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
        Log.d(TAG, "signRequest: " + request);
        byte[] doubleSha256 = CryptoHelper.doubleSha256(request.getBytes(StandardCharsets.UTF_8));
        BRKey key;
        try {
            byte[] authKey = BRKeyStore.getAuthKey(ctx);
            if (Utils.isNullOrEmpty(authKey)) {
                Log.e(TAG, "signRequest: authkey is null");
                return null;
            }
            key = new BRKey(authKey);
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

    public Response sendRequest(Request locRequest, boolean needsAuth, int retryCount) {
        if (retryCount > 1)
            throw new RuntimeException("sendRequest: Warning retryCount is: " + retryCount);
        boolean isTestVersion = BREAD_POINT.contains("staging");
        boolean isTestNet = BuildConfig.BITCOIN_TESTNET;
        String lang = getCurrentLocale(ctx);
        Request request = locRequest.newBuilder().header("X-Testflight", isTestVersion ? "true" : "false").header("X-Litecoin-Testnet", isTestNet ? "true" : "false").header("Accept-Language", lang).build();
        if (needsAuth) {
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
            SimpleDateFormat sdf =
                    new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String httpDate = sdf.format(new Date());

            request = modifiedRequest.header("Date", httpDate.substring(0, httpDate.length() - 6)).build();

            String queryString = request.url().encodedQuery();

            String requestString = createRequest(request.method(), base58Body,
                    request.header("Content-Type"), request.header("Date"), request.url().encodedPath()
                            + ((queryString != null && !queryString.isEmpty()) ? ("?" + queryString) : ""));
            String signedRequest = signRequest(requestString);
            if (signedRequest == null) return null;
            byte[] tokenBytes = BRKeyStore.getToken(ctx);
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
                FirebaseCrash.report(e);

            }

        }

        Response response = null;
        byte[] data = new byte[0];
        try {
            OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).connectTimeout(60, TimeUnit.SECONDS)/*.addInterceptor(new LoggingInterceptor())*/.build();
//            Log.e(TAG, "sendRequest: before executing the request: " + request.headers().toString());
            Log.d(TAG, "sendRequest: headers for : " + request.url() + "\n" + request.headers());
            String agent = Utils.getAgentString(ctx, "OkHttp/3.4.1");
//            Log.e(TAG, "sendRequest: agent: " + agent);
            request = request.newBuilder().header("User-agent", agent).build();
            response = client.newCall(request).execute();
            String s = null;
            try {
                data = response.body().bytes();
                s = new String(data);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (response.isRedirect()) {
                String newLocation = request.url().scheme() + "://" + request.url().host() + response.header("location");
                Uri newUri = Uri.parse(newLocation);
                if (newUri == null) {
                    Log.e(TAG, "sendRequest: redirect uri is null");
                } else if (!newUri.getHost().equalsIgnoreCase(BreadApp.HOST) || !newUri.getScheme().equalsIgnoreCase(PROTO)) {
                    Log.e(TAG, "sendRequest: WARNING: redirect is NOT safe: " + newLocation);
                } else {
                    Log.w(TAG, "redirecting: " + request.url() + " >>> " + newLocation);
                    return sendRequest(new Request.Builder().url(newLocation).get().build(), needsAuth, 0);
                }
                return new Response.Builder().code(500).request(request).body(ResponseBody.create(null, new byte[0])).protocol(Protocol.HTTP_1_1).build();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new Response.Builder().code(599).request(request).body(ResponseBody.create(null, new byte[0])).protocol(Protocol.HTTP_1_1).build();
        }

        if (response.header("content-encoding") != null && response.header("content-encoding").equalsIgnoreCase("gzip")) {
            Log.d(TAG, "sendRequest: the content is gzip, unzipping");
            byte[] decompressed = gZipExtract(data);
            ResponseBody postReqBody = ResponseBody.create(null, decompressed);
            try {
                Log.e(TAG, "sendRequest: " + String.format(Locale.getDefault(), "(%s)%s, code (%d), mess (%s), body (%s)", request.method(),
                        request.url(), response.code(), response.message(), new String(decompressed, "utf-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return response.newBuilder().body(postReqBody).build();
        } else {
            try {
                Log.d(TAG, "sendRequest: " + String.format(Locale.getDefault(), "(%s)%s, code (%d), mess (%s), body (%s)", request.method(),
                        request.url(), response.code(), response.message(), new String(data, "utf-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        ResponseBody postReqBody = ResponseBody.create(null, data);
        if (needsAuth && isBreadChallenge(response)) {
            Log.e(TAG, "sendRequest: got authentication challenge from API - will attempt to get token");
            getToken();
            if (retryCount < 1) {
                sendRequest(request, true, retryCount + 1);
            }
        }

        return response.newBuilder().body(postReqBody).build();
    }

    public void updateBundle() {
        // Disable support bundle

//        File bundleFile = new File(getBundleResource(ctx, BREAD_FILE));
//        Log.d(TAG, "updateBundle: " + bundleFile);
//        if (bundleFile.exists()) {
//            Log.d(TAG, bundleFile + ": updateBundle: exists");
//
//            byte[] bFile = new byte[0];
//            try {
//                bFile = IOUtils.toByteArray(new FileInputStream(bundleFile));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            String latestVersion = getLatestVersion();
//            String currentTarVersion = null;
//            byte[] hash = CryptoHelper.sha256(bFile);
//
//            currentTarVersion = Utils.bytesToHex(hash);
//            Log.d(TAG, bundleFile + ": updateBundle: version of the current tar: " + currentTarVersion);
//
//            if (latestVersion != null) {
//                if (latestVersion.equals(currentTarVersion)) {
//                    Log.d(TAG, bundleFile + ": updateBundle: have the latest version");
//                    tryExtractTar();
//                } else {
//                    Log.d(TAG, bundleFile + ": updateBundle: don't have the most recent version, download diff");
//                    downloadDiff(currentTarVersion);
//                    tryExtractTar();
//
//                }
//            } else {
//                Log.d(TAG, bundleFile + ": updateBundle: latestVersion is null");
//            }
//
//        } else {
//            Log.d(TAG, bundleFile + ": updateBundle: bundle doesn't exist, downloading new copy");
//            long startTime = System.currentTimeMillis();
//            Request request = new Request.Builder()
//                    .url(String.format("%s/assets/bundles/%s/download", BASE_URL, BREAD_POINT))
//                    .get().build();
//            Response response = null;
//            response = sendRequest(request, false, 0);
//            Log.d(TAG, bundleFile + ": updateBundle: Downloaded, took: " + (System.currentTimeMillis() - startTime));
//            byte[] body = writeBundleToFile(response);
//            if (Utils.isNullOrEmpty(body))
//                throw new NullPointerException("failed to write bundle to file");
//
//            boolean b = tryExtractTar();
//            if (!b) throw new NullPointerException("failed to extract the bundle tar");
//        }
//
//        logFiles("updateBundle after", ctx);
    }

    public String getLatestVersion() {
        String latestVersion = null;
        String response = null;
        try {
            response = sendRequest(new Request.Builder()
                    .get()
                    .url(String.format("%s/assets/bundles/%s/versions", BASE_URL, BREAD_POINT))
                    .build(), false, 0).body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String respBody;
        respBody = response;
        try {
            JSONObject versionsJson = new JSONObject(respBody);
            JSONArray jsonArray = versionsJson.getJSONArray("versions");
            if (jsonArray.length() == 0) return null;
            latestVersion = (String) jsonArray.get(jsonArray.length() - 1);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return latestVersion;
    }

    public void downloadDiff(String currentTarVersion) {
        Request diffRequest = new Request.Builder()
                .url(String.format("%s/assets/bundles/%s/diff/%s", BASE_URL, BREAD_POINT, currentTarVersion))
                .get().build();
        Response diffResponse = sendRequest(diffRequest, false, 0);
        File patchFile = null;
        File tempFile = null;
        byte[] patchBytes = null;
        try {
            patchFile = new File(getBundleResource(ctx, BREAD_POINT + "-patch.diff"));
            patchBytes = diffResponse.body().bytes();
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
        } finally {
            if (patchFile != null)
                patchFile.delete();
            if (tempFile != null)
                tempFile.delete();
        }

        logFiles("downloadDiff", ctx);
    }

    public byte[] writeBundleToFile(Response response) {
        byte[] bodyBytes;
        FileOutputStream fileOutputStream = null;
        assert (response != null);
        try {
            if (response == null) {
                Log.e(TAG, "writeBundleToFile: WARNING, response is null");
                return null;
            }
            bodyBytes = response.body().bytes();
            File bundleFile = new File(getBundleResource(ctx, BREAD_POINT + ".tar"));
            FileUtils.writeByteArrayToFile(bundleFile, bodyBytes);
            return bodyBytes;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public boolean tryExtractTar() {
        File bundleFile = new File(getBundleResource(ctx, BREAD_POINT + ".tar"));
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
        // Disable remote feature flags (including buy support)

//        String furl = "/me/features";
//        Request req = new Request.Builder()
//                .url(buildUrl(furl))
//                .get().build();
//        Response res = sendRequest(req, true, 0);
//        if (res == null) {
//            Log.e(TAG, "updateFeatureFlag: error fetching features");
//            return;
//        }
//
//        if (!res.isSuccessful()) {
//            Log.e(TAG, "updateFeatureFlag: request was unsuccessful: " + res.code() + ":" + res.message());
//            return;
//        }
//
//        try {
//            String j = res.body().string();
//            if (j.isEmpty()) {
//                Log.e(TAG, "updateFeatureFlag: JSON empty");
//                return;
//            }
//
//            JSONArray arr = new JSONArray(j);
//            for (int i = 0; i < arr.length(); i++) {
//                try {
//                    JSONObject obj = arr.getJSONObject(i);
//                    String name = obj.getString("name");
//                    String description = obj.getString("description");
//                    boolean selected = obj.getBoolean("selected");
//                    boolean enabled = obj.getBoolean("enabled");
//                    BRSharedPrefs.putFeatureEnabled(ctx, enabled, name);
//                } catch (Exception e) {
//                    Log.e(TAG, "malformed feature at position: " + i + ", whole json: " + j, e);
//                }
//
//            }
//        } catch (IOException | JSONException e) {
//            Log.e(TAG, "updateFeatureFlag: failed to pull up features");
//            e.printStackTrace();
//        }

    }

    public boolean isBreadChallenge(Response resp) {
        String challenge = resp.header("www-authenticate");
        return challenge != null && challenge.startsWith("bread");
    }

    public boolean isFeatureEnabled(String feature) {
        return BRSharedPrefs.getFeatureEnabled(ctx, feature);
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

    public void updatePlatform() {

        if (platformUpdating) {
            Log.e(TAG, "updatePlatform: platform already Updating!");
            return;
        }
        platformUpdating = true;

        //update Bundle
        new Thread(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance();
                apiClient.updateBundle();
                long endTime = System.currentTimeMillis();
                Log.e(TAG, "updateBundle " + BREAD_POINT + ": DONE in " + (endTime - startTime) + "ms");
                itemFinished();
            }
        }).start();

        //update feature flags
        new Thread(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance();
                apiClient.updateFeatureFlag();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "updateFeatureFlag: DONE in " + (endTime - startTime) + "ms");
                itemFinished();
            }
        }).start();

        //update kvStore
        new Thread(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance();
                apiClient.syncKvStore();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "updatePlatform: DONE in " + (endTime - startTime) + "ms");
                itemFinished();
            }
        }).start();

    }

    private void itemFinished() {
        int items = itemsLeftToUpdate.incrementAndGet();
        if (items >= 3) {
            Log.d(TAG, "PLATFORM ALL UPDATED: " + items);
            platformUpdating = false;
            itemsLeftToUpdate.set(0);
        }
    }

    public void syncKvStore() {
        final APIClient client = this;
        //sync the kv stores
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(client);
        ReplicatedKVStore kvStore = new ReplicatedKVStore(remoteKVStore);
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


    @TargetApi(Build.VERSION_CODES.N)
    public String getCurrentLocale(Context ctx){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            return ctx.getResources().getConfiguration().getLocales().get(0).getLanguage();
        } else{
            //noinspection deprecation
            return ctx.getResources().getConfiguration().locale.getLanguage();
        }
    }

}