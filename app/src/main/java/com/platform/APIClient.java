package com.platform;

import android.app.Activity;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.util.Log;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.crypto.Base58;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.jniwrappers.BRBase58;
import com.jniwrappers.BRKey;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;

import org.apache.commons.compress.archivers.ArchiveException;
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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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

import static android.R.attr.key;
import static android.R.attr.path;
import static com.breadwallet.R.string.request;
import static com.breadwallet.R.string.rescan;
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
    //    // host is the server(s) on which the API is hosted
//    private static final String HOST = "api.breadwallet.com";
//    // convenience getter for the API endpoint
    public static final String BASE_URL = PROTO + "://" + BreadWalletApp.HOST;
    //feePerKb url
    private static final String FEE_PER_KB_URL = "/v1/fee-per-kb";
    //token
    private static final String TOKEN = "/token";
    //me
    private static final String ME = "/me";
    //singleton instance
    private static APIClient ourInstance;

    public static final String BUNDLES = "bundles";
    //    public static final String BREAD_BUY = "bread-buy-staging";
    public static String BREAD_BUY = "bread-buy-staging";

    public static String bundlesFileName = String.format("/%s", BUNDLES);
    public static String bundleFileName = String.format("/%s/%s.tar", BUNDLES, BREAD_BUY);
    public static String extractedFolder = String.format("%s-extracted", BREAD_BUY);

    public static HTTPServer server;

    private Activity ctx;

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

    public static synchronized APIClient getInstance(Activity context) {

        if (ourInstance == null) ourInstance = new APIClient(context);
        return ourInstance;
    }

    private APIClient(Activity context) {
        ctx = context;
        if (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            BREAD_BUY = "bread-buy-staging";
        }
    }

    private APIClient() {
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

    public Response buyBitcoinMe() {
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return null;
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
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return null;
        try {
            String strUtl = BASE_URL + TOKEN;

            JSONObject requestMessageJSON = new JSONObject();
            String base58PubKey = BRWalletManager.getAuthPublicKeyForAPI(KeyStoreManager.getAuthKey(ctx));
            requestMessageJSON.put("pubKey", base58PubKey);
            requestMessageJSON.put("deviceID", SharedPreferencesManager.getDeviceId(ctx));

            final MediaType JSON
                    = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(JSON, requestMessageJSON.toString());
            Request request = new Request.Builder()
                    .url(strUtl)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(requestBody).build();
            String body = null;
            String message = null;
            int code = -1;
            Response response;
            try {
                response = sendRequest(request, false, 0);
                if (response != null) {
                    body = response.body().string();
                    message = response.message();
                    code = response.code();
                }
                Log.e(TAG, "getToken: " + code + ", message: " + message + ", body: " + body);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Utils.isNullOrEmpty(body)) {
                Log.e(TAG, "getToken: retrieving token failed");
                return null;
            }
            JSONObject obj = null;
            obj = new JSONObject(body);
            String token = obj.getString("token");
            KeyStoreManager.putToken(token.getBytes(), ctx);
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
        byte[] doubleSha256 = CryptoHelper.doubleSha256(request.getBytes(StandardCharsets.UTF_8));
        BRKey key = new BRKey(KeyStoreManager.getAuthKey(ctx));
        byte[] signedBytes = key.compactSign(doubleSha256);
        return Base58.encode(signedBytes);

    }

    public Response sendRequest(Request locRequest, boolean needsAuth, int retryCount) {
        if (retryCount > 1)
            throw new RuntimeException("sendRequest: Warning retryCount is: " + retryCount);
        boolean isTestVersion = BREAD_BUY.equalsIgnoreCase("bread-buy-staging");
        boolean isTestNet = BuildConfig.BITCOIN_TESTNET;
        Request request = locRequest.newBuilder().header("X-Testflight", isTestVersion ? "1" : "0").header("X-Bitcoin-Testnet", isTestNet ? "1" : "0").build();
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
            byte[] rawToken = KeyStoreManager.getToken(ctx);
            String token = new String(rawToken == null ? new byte[0] : rawToken);
            if (token.isEmpty()) token = getToken();
            if (token == null || token.isEmpty()) {
                Log.e(TAG, "sendRequest: failed to retrieve token");
                return null;
            }
            String authValue = "bread " + token + ":" + signedRequest;
//            Log.e(TAG, "sendRequest: authValue: " + authValue);
            modifiedRequest = request.newBuilder();

            request = modifiedRequest.header("Authorization", authValue).build();

        }
        Response response = null;
        byte[] data = new byte[0];
        try {
            OkHttpClient client = new OkHttpClient.Builder().followRedirects(false)/*.addInterceptor(new LoggingInterceptor())*/.build();
//            Log.e(TAG, "sendRequest: before executing the request: " + request.headers().toString());
            request = request.newBuilder().header("User-agent", Utils.getAgentString(ctx, "OkHttp/3.4.1")).build();
//            if (!request.url().toString().contains("bch/publish-transaction"))
            response = client.newCall(request).execute();
//            else
//                Log.e(TAG, "sendRequest: Cant do client.newCall: " + request.url().toString());
            try {
                data = response.body().bytes();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!response.isSuccessful())
                Log.e(TAG, "sendRequest: " + String.format(Locale.getDefault(), "url (%s), code (%d), mess (%s), body (%s)",
                        request.url(), response.code(), response.message(), new String(data)));
            if (response.isRedirect()) {
                String newLocation = request.url().scheme() + "://" + request.url().host() + response.header("location");
                Uri newUri = Uri.parse(newLocation);
                if (newUri == null) {
                    Log.e(TAG, "sendRequest: redirect uri is null");
                } else if (!newUri.getHost().equalsIgnoreCase("breadwallet.com") || !newUri.getScheme().equalsIgnoreCase(PROTO)) {
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

            return response.newBuilder().body(postReqBody).build();
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
        File bundleFile = new File(ctx.getFilesDir().getAbsolutePath() + bundleFileName);

        if (bundleFile.exists()) {
            Log.d(TAG, "updateBundle: exists");

            byte[] bFile = new byte[0];
            try {
                bFile = IOUtils.toByteArray(new FileInputStream(bundleFile));
            } catch (IOException e) {
                e.printStackTrace();
            }

            String latestVersion = getLatestVersion();
            String currentTarVersion = null;
            byte[] hash = CryptoHelper.sha256(bFile);

            currentTarVersion = Utils.bytesToHex(hash);
            Log.e(TAG, "updateBundle: version of the current tar: " + currentTarVersion);

            if (latestVersion != null) {
                if (latestVersion.equals(currentTarVersion)) {
                    Log.d(TAG, "updateBundle: have the latest version");
                    tryExtractTar(bundleFile);
                } else {
                    Log.d(TAG, "updateBundle: don't have the most recent version, download diff");
                    downloadDiff(bundleFile, currentTarVersion);
                    tryExtractTar(bundleFile);

                }
            } else {
                Log.d(TAG, "updateBundle: latestVersion is null");
            }

        } else {
            Log.d(TAG, "updateBundle: bundle doesn't exist, downloading new copy");
            long startTime = System.currentTimeMillis();
            Request request = new Request.Builder()
                    .url(String.format("%s/assets/bundles/%s/download", BASE_URL, BREAD_BUY))
                    .get().build();
            Response response = null;
            response = sendRequest(request, false, 0);
            Log.d(TAG, "updateBundle: Downloaded, took: " + (System.currentTimeMillis() - startTime));
            writeBundleToFile(response, bundleFile);

            tryExtractTar(bundleFile);
        }

    }

    public String getLatestVersion() {
        String latestVersion = null;
        String response = null;
        try {
            response = sendRequest(new Request.Builder()
                    .get()
                    .url(String.format("%s/assets/bundles/%s/versions", BASE_URL, BREAD_BUY))
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

    public void downloadDiff(File bundleFile, String currentTarVersion) {
        Request diffRequest = new Request.Builder()
                .url(String.format("%s/assets/bundles/%s/diff/%s", BASE_URL, BREAD_BUY, currentTarVersion))
                .get().build();
        Response diffResponse = sendRequest(diffRequest, false, 0);
        File patchFile = null;
        File tempFile = null;
        byte[] patchBytes = null;
        try {
            patchFile = new File(String.format("/%s/%s.diff", BUNDLES, "patch"));
            patchBytes = diffResponse.body().bytes();
            FileUtils.writeByteArrayToFile(patchFile, patchBytes);

            String compression = System.getProperty("jbsdiff.compressor", "tar");
            compression = compression.toLowerCase();
            tempFile = new File(String.format("/%s/%s.tar", BUNDLES, "temp"));
            FileUI.diff(bundleFile, tempFile, patchFile, compression);

            byte[] updatedBundleBytes = IOUtils.toByteArray(new FileInputStream(tempFile));
            FileUtils.writeByteArrayToFile(bundleFile, updatedBundleBytes);

        } catch (IOException | InvalidHeaderException | CompressorException | NullPointerException e) {
            e.printStackTrace();
        } finally {
            if (patchFile != null)
                patchFile.delete();
            if (tempFile != null)
                tempFile.delete();
        }
    }

    public byte[] writeBundleToFile(Response response, File bundleFile) {
        byte[] bodyBytes;
        FileOutputStream fileOutputStream = null;
        assert (response != null);
        try {
            if (response == null) {
                Log.e(TAG, "writeBundleToFile: WARNING, response is null");
                return null;
            }
            bodyBytes = response.body().bytes();
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

    public boolean tryExtractTar(File inputFile) {
        String extractFolderName = MainActivity.app.getFilesDir().getAbsolutePath() + bundlesFileName + "/" + extractedFolder;
        boolean result = false;
        TarArchiveInputStream debInputStream = null;
        try {
            final InputStream is = new FileInputStream(inputFile);
            debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", is);
            TarArchiveEntry entry = null;
            while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {

                final String outPutFileName = entry.getName().replace("./", "");
                final File outputFile = new File(extractFolderName, outPutFileName);
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
        return result;

    }

    public void updateFeatureFlag() {
        String furl = "/me/features";
        Request req = new Request.Builder()
                .url(buildUrl(furl))
                .get().build();
        Response res = sendRequest(req, true, 0);
        if (res == null) {
            Log.e(TAG, "updateFeatureFlag: error fetching features");
            return;
        }

        if (!res.isSuccessful()) {
            Log.e(TAG, "updateFeatureFlag: request was unsuccessful: " + res.code() + ":" + res.message());
            return;
        }

        try {
            String j = res.body().string();
            if (j.isEmpty()) {
                Log.e(TAG, "updateFeatureFlag: JSON empty");
                return;
            }

            JSONArray arr = new JSONArray(j);
            for (int i = 0; i < arr.length(); i++) {
                try {
                    JSONObject obj = arr.getJSONObject(i);
                    String name = obj.getString("name");
                    String description = obj.getString("description");
                    boolean selected = obj.getBoolean("selected");
                    boolean enabled = obj.getBoolean("enabled");
                    SharedPreferencesManager.putFeatureEnabled(ctx, enabled, name);
                } catch (Exception e) {
                    Log.e(TAG, "malformed feature at position: " + i + ", whole json: " + j, e);
                }

            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "updateFeatureFlag: failed to pull up features");
            e.printStackTrace();
        }

    }

    public boolean isBreadChallenge(Response resp) {
        String challenge = resp.header("www-authenticate");
        return challenge != null && challenge.startsWith("bread");
    }

    public boolean isFeatureEnabled(String feature) {
        return SharedPreferencesManager.getFeatureEnabled(ctx, feature);
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
        if (BuildConfig.DEBUG) {
            final long startTime = System.currentTimeMillis();
            Log.d(TAG, "updatePlatform: updating platform...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    APIClient apiClient = APIClient.getInstance(ctx);
                    apiClient.updateBundle(); //bread-buy-staging
                    apiClient.updateFeatureFlag();
                    apiClient.syncKvStore();
                    long endTime = System.currentTimeMillis();
                    Log.e(TAG, "updatePlatform: DONE in " + (endTime - startTime) + "ms");
                }
            }).start();
        }

    }

    public void syncKvStore() {
        final APIClient client = this;
        final long startTime = System.currentTimeMillis();
        Log.d(TAG, "syncKvStore: DEBUG, syncing kv store...");
        //sync the kv stores
        RemoteKVStore remoteKVStore = RemoteKVStore.getInstance(client);
        ReplicatedKVStore kvStore = new ReplicatedKVStore(ctx, remoteKVStore);
        kvStore.syncAllKeys();
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "syncKvStore: DONE in " + (endTime - startTime) + "ms");
    }

}