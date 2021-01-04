/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 01/17/19.
 * Copyright (c) 2019 breadwallet LLC
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

package com.breadwallet.tools.util;

import android.content.Context;
import android.os.NetworkOnMainThreadException;
import androidx.annotation.Nullable;
import android.util.Log;

import com.breadwallet.appcore.BuildConfig;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.platform.APIClient;

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
import java.io.IOException;
import java.io.InputStream;

import io.sigpipe.jbsdiff.InvalidHeaderException;
import io.sigpipe.jbsdiff.ui.FileUI;
import okhttp3.Request;


/**
 * Helper class responsible for extracting bundles from the raw resources and keep them updated.
 */
public final class ServerBundlesHelper {
    private static final String TAG = ServerBundlesHelper.class.getName();

    private static final String RAW = "raw";
    private static final String TAR = "tar";
    private static final String EXTRACTED = "-extracted";
    private static final String TAR_EXTENSION = ".tar";
    private static final String BRD_WEB = "brd-web-3";
    private static final String BRD_WEB_STAGING = "brd-web-3-staging";
    private static final String BRD_TOKEN_ASSETS = "brd-tokens";
    private static final String BRD_TOKEN_ASSETS_STAGING = "brd-tokens-staging";
    private static final String TAR_FILE_NAME_FORMAT = "/%s.tar";
    private static final String BUNDLES_FORMAT = "%s/assets/bundles/%s/diff/%s";
    private static final String BUNDLES_FOLDER = "/bundles";
    private static final String BUNDLES_URL_VERSIONS_PATH = "%s/assets/bundles/%s/versions";
    private static final String BUNDLES_URL_DOWNLOAD_PATH = "%s/assets/bundles/%s/download";

    private static final String WEB_BUNDLE_NAME = BuildConfig.DEBUG ? BRD_WEB_STAGING : BRD_WEB;
    private static final String TOKEN_ASSETS_BUNDLE_NAME = BuildConfig.DEBUG
            ? BRD_TOKEN_ASSETS_STAGING : BRD_TOKEN_ASSETS;
    private static final String[] BUNDLE_NAMES = {WEB_BUNDLE_NAME, TOKEN_ASSETS_BUNDLE_NAME};

    /**
     * Available server bundle types.
     */
    public enum Type {
        WEB, TOKEN
    }

    private ServerBundlesHelper() {
    }

    /**
     * Extract bundles from apk into the device storage if there is no bundle already available.
     *
     * @param context Execution context.
     */
    public static void extractBundlesIfNeeded(Context context) {
        for (String bundleName : BUNDLE_NAMES) {
            String fileName = bundleName + TAR_EXTENSION;
            File bundleFile = new File(getBundleResource(context, fileName));
            if (!bundleFile.exists()) {
                Log.d(TAG, "Missing files " + bundleName);
                String resourceName = bundleName.replace('-', '_');
                try {
                    // Move files from resources/raw to bundles directory and extract the resources
                    int resource = context.getResources().getIdentifier(resourceName, RAW, context.getPackageName());
                    if (resource != 0) {
                        InputStream inputStream = context.getResources().openRawResource(resource);
                        FileUtils.copyInputStreamToFile(inputStream, bundleFile);
                        boolean extracted = tryExtractTar(context, bundleName);
                        if (!extracted) {
                            Log.e(TAG, "Failed to extract tar: " + resourceName);
                        } else {
                            // Update bundle version in shared preferences, this is used to check if we have latest version.
                            String currentBundleVersion = getCurrentBundleVersion(bundleFile);
                            if (!Utils.isNullOrEmpty(currentBundleVersion)) {
                                BRSharedPrefs.putBundleHash(bundleName, currentBundleVersion);
                            }
                        }
                    }
                } catch (IOException exception) {
                    Log.e(TAG, "Failed to extract bundles from resources", exception);
                }
            } else {
                Log.d(TAG, "Bundle is already there " + bundleFile.getAbsolutePath());
            }
        }
    }

    /**
     * Gets the names of the server bundles to be used.
     *
     * @return Server bundles to be used.
     */
    public static String[] getBundleNames() {
        if (BuildConfig.DEBUG) {
            String[] debugBundles = {
                    getBundle(Type.TOKEN),
                    getBundle(Type.WEB)
            };
            return debugBundles;
        } else {
            return BUNDLE_NAMES;
        }
    }

    /**
     * Sets the debug bundle into the shared preferences, only if the build is DEBUG.
     *
     * @param context Execution context.
     * @param type    Bundle type.
     * @param bundle  Bundle to be used.
     */
    public static void setDebugBundle(final Context context, Type type, String bundle) {
        if (BuildConfig.DEBUG) {
            BRSharedPrefs.putDebugBundle(type, bundle);
            BRExecutor.getInstance().forLightWeightBackgroundTasks()
                    .execute(() -> updateBundles(context));
        }
    }

    /**
     * Get server bundle to use or the stored debug bundle if available and if build is DEBUG.
     *
     * @param type    Bundle type.
     * @return The bundle to be used.
     */
    public static String getBundle(Type type) {
        String debugBundle = BuildConfig.DEBUG ? BRSharedPrefs.getDebugBundle(type) : null;
        String defaultBundle;
        switch (type) {
            case TOKEN:
                defaultBundle = TOKEN_ASSETS_BUNDLE_NAME;
                break;
            case WEB:
                defaultBundle = WEB_BUNDLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unexpected bundle: " + type.name());
        }
        return !Utils.isNullOrEmpty(debugBundle) ? debugBundle : defaultBundle;
    }

    /**
     * Saves the given web platform debug URL into the shared preferences, if build is DEBUG.
     *
     * @param webPlatformDebugURL The web platform debug URL.
     */
    public static void setWebPlatformDebugURL(String webPlatformDebugURL) {
        if (BuildConfig.DEBUG) {
            BRSharedPrefs.putWebPlatformDebugURL(webPlatformDebugURL);
        }
    }

    /**
     * Returns the web platform debug URL from the shared preferences, if build is DEBUG.
     *
     * @return The web platform debug URL or empty.
     */
    public static String getWebPlatformDebugURL() {
        return BuildConfig.DEBUG ? BRSharedPrefs.getWebPlatformDebugURL() : "";
    }

    /**
     * Update bundles if there is a newer version.
     * @param context Execution context.
     */
    public static synchronized void updateBundles(Context context) {
        for (String bundleName : ServerBundlesHelper.getBundleNames()) {
            File bundleFile = new File(getBundleResource(context, String.format(TAR_FILE_NAME_FORMAT, bundleName)));
            String currentTarVersion = BRSharedPrefs.getBundleHash(bundleName);
            if (bundleFile.exists()) {
                Log.d(TAG, bundleFile + ": updateBundles: exists");
                String latestVersion = fetchBundleVersion(context, bundleName);

                Log.d(TAG, bundleFile + ": updateBundles: version of the current tar: " + currentTarVersion);
                if (latestVersion != null) {
                    if (latestVersion.equals(currentTarVersion)) {
                        Log.d(TAG, bundleFile + ": updateBundles: have the latest version");
                        tryExtractTar(context, bundleName);
                    } else {
                        Log.d(TAG, bundleFile + ": updateBundles: don't have the most recent version, download diff");
                        downloadDiff(context, bundleName, currentTarVersion);
                        tryExtractTar(context, bundleName);
                        BRSharedPrefs.putBundleHash(bundleName, latestVersion);
                    }
                } else {
                    Log.d(TAG, bundleFile + ": updateBundles: latestVersion is null");
                }

            } else {
                Log.d(TAG, bundleFile + ": updateBundles: bundle doesn't exist, downloading new copy");
                long startTime = System.currentTimeMillis();
                Request request = new Request.Builder()
                        .url(String.format(BUNDLES_URL_DOWNLOAD_PATH, APIClient.getBaseURL(), bundleName))
                        .get().build();
                byte[] body;
                APIClient.BRResponse response = APIClient.getInstance(context).sendRequest(request, false);
                Log.d(TAG, bundleFile + ": updateBundles: Downloaded, took: " + (System.currentTimeMillis() - startTime));
                body = writeBundleToFile(context, bundleName, response.getBody());
                if (Utils.isNullOrEmpty(body)) {
                    Log.e(TAG, "updateBundles: body is null, returning.");
                    return;
                }

                boolean extracted = tryExtractTar(context, bundleName);
                if (!extracted) {
                    Log.e(TAG, "updateBundles: Failed to extract tar");
                } else {
                    String currentBundleVersion = getCurrentBundleVersion(bundleFile);
                    if (!Utils.isNullOrEmpty(currentBundleVersion)) {
                        BRSharedPrefs.putBundleHash(bundleName, currentBundleVersion);
                    }
                }

            }

            logFiles("updateBundles after", bundleName, context);
        }
    }

    /**
     * Returns the resource at bundles/path, if path is null then the bundle folder.
     *
     * @param context Execution context.
     * @param path    Bundle's path.
     * @return Resource path or the bundle folder if the path is null.
     */
    private static String getBundleResource(Context context, String path) {
        String bundle = context.getFilesDir().getAbsolutePath() + BUNDLES_FOLDER;
        return concatPaths(path, bundle);
    }

    private static boolean tryExtractTar(Context context, String bundleName) {
        File bundleFile = new File(getBundleResource(context, bundleName + TAR_EXTENSION));
        Log.d(TAG, "tryExtractTar: " + bundleFile.getAbsolutePath());
        boolean result = false;
        try (InputStream inputStream = new FileInputStream(bundleFile)) {
            ArchiveStreamFactory streamFactory = new ArchiveStreamFactory();
            try (TarArchiveInputStream debInputStream
                         = (TarArchiveInputStream) streamFactory.createArchiveInputStream(TAR, inputStream)) {
                TarArchiveEntry entry;
                while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {
                    final String outputFileName = entry.getName().replace("./", "");
                    final File outputFile = new File(getExtractedPath(context, bundleName, null), outputFileName);
                    if (!entry.isDirectory()) {
                        FileUtils.writeByteArrayToFile(outputFile, IOUtils.toByteArray(debInputStream));
                    }
                }

                result = true;
            } catch (IOException | ArchiveException exception) {
                Log.e(TAG, "tryExtractTar: ", exception);
            }
        } catch (IOException exception) {
            Log.e(TAG, "tryExtractTar: ", exception);
        }
        logFiles("tryExtractTar", bundleName, context);
        return result;
    }

    /**
     * Returns the extracted folder or the path in it
     *
     * @param context    Execution context.
     * @param bundleName Name of the bundle from which we want to get the path.
     * @param path       Path into the extracted folder.
     * @return Path of the extracted bundle.
     */
    public static String getExtractedPath(Context context, String bundleName, @Nullable String path) {
        String extractedPath = new StringBuffer().append(context.getFilesDir().getAbsolutePath())
                .append("/")
                .append(bundleName)
                .append(EXTRACTED)
                .toString();
        return concatPaths(path, extractedPath);
    }

    private static String concatPaths(String rootPath, String endPath) {
        if (Utils.isNullOrEmpty(rootPath)) {
            return endPath;
        } else {
            if (!rootPath.startsWith("/")) {
                rootPath = "/" + rootPath;
            }
            return endPath + rootPath;
        }
    }

    private static void logFiles(String tag, String bundleName, Context context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "logFiles " + tag + " : START LOGGING");
            String path = getExtractedPath(context, bundleName, null);

            File directory = new File(path);
            File[] files = directory.listFiles();
            Log.d("Files", "Path: " + path + ", size: " + (files == null ? 0 : files.length));
            for (int i = 0; files != null && i < files.length; i++) {
                Log.d("Files", "FileName:" + files[i].getName());
            }
            Log.d(TAG, "logFiles " + tag + " : START LOGGING");
        }
    }

    /**
     * Gets the bundle version of the specified bundle that is currently on the device.
     *
     * @param bundleFile the file the bundle resides in.
     * @return The bundle version of the specified bundle that is currently on the device.
     */
    private static String getCurrentBundleVersion(File bundleFile) {
        byte[] bundleBytes;
        try (FileInputStream fileInputStream = new FileInputStream(bundleFile)) {
            bundleBytes = IOUtils.toByteArray(fileInputStream);
            byte[] hash = CryptoHelper.sha256(bundleBytes);
            return Utils.isNullOrEmpty(hash) ? null : Utils.bytesToHex(hash);
        } catch (IOException exception) {
            Log.e(TAG, "getCurrentBundleVersion: ", exception);
        }
        return null;
    }

    /**
     * Gets the bundle version of the specified bundle from the server.  This can be used to determine if a new version is available.
     *
     * @param bundleName
     * @return
     */
    private static String fetchBundleVersion(Context context, String bundleName) {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        String latestVersion = null;
        Request request = new Request.Builder()
                .get()
                .url(String.format(BUNDLES_URL_VERSIONS_PATH, APIClient.getBaseURL(), bundleName))
                .build();

        APIClient.BRResponse response = APIClient.getInstance(context).sendRequest(request, false);

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


    private static void downloadDiff(Context context, String bundleName, String currentTarVersion) {
        Request diffRequest = new Request.Builder().url(String.format(BUNDLES_FORMAT, APIClient.getBaseURL(), bundleName, currentTarVersion)).get().build();
        APIClient.BRResponse resp = APIClient.getInstance(context).sendRequest(diffRequest, false);
        if (Utils.isNullOrEmpty(resp.getBodyText())) {
            Log.e(TAG, "downloadDiff: no response");
            return;
        }
        File patchFile = null;
        File tempFile = null;
        try {
            patchFile = new File(getBundleResource(context, bundleName + "-patch.diff"));
            byte[] patchBytes = resp.getBody();
            Log.i(TAG, "downloadDiff: trying to write to file");
            FileUtils.writeByteArrayToFile(patchFile, patchBytes);
            tempFile = new File(getBundleResource(context, bundleName + "-2temp.tar"));
            tempFile.createNewFile();
            File bundleFile = new File(getBundleResource(context, bundleName + "." + TAR));
            FileUI.patch(bundleFile, tempFile, patchFile);
            byte[] updatedBundleBytes = IOUtils.toByteArray(new FileInputStream(tempFile));
            if (Utils.isNullOrEmpty(updatedBundleBytes)) {
                Log.e(TAG, "downloadDiff: failed to get bytes from the updatedBundle: " + tempFile.getAbsolutePath());
            }
            FileUtils.writeByteArrayToFile(bundleFile, updatedBundleBytes);

        } catch (IOException | InvalidHeaderException | CompressorException | NullPointerException e) {
            Log.e(TAG, "downloadDiff: ", e);
            new File(getBundleResource(context, bundleName + "." + TAR)).delete();
        } finally {
            if (patchFile != null) {
                patchFile.delete();
            }
            if (tempFile != null) {
                tempFile.delete();
            }
        }

        logFiles("downloadDiff", bundleName, context);
    }

    private static byte[] writeBundleToFile(Context context, String bundleName, byte[] response) {
        try {
            if (response == null) {
                Log.e(TAG, "writeBundleToFile: WARNING, response is null");
                return null;
            }
            File bundleFile = new File(getBundleResource(context, bundleName + ".tar"));
            FileUtils.writeByteArrayToFile(bundleFile, response);
            return response;
        } catch (IOException e) {
            Log.e(TAG, "writeBundleToFile: ", e);
        }

        return null;
    }
}
