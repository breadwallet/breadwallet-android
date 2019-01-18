/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 01/17/19.
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
import android.util.Log;

import com.breadwallet.BuildConfig;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.manager.BRSharedPrefs;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


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
    private static final String BRD_TOKEN_ASSETS = "brd-tokens-prod";
    private static final String BRD_TOKEN_ASSETS_STAGING = "brd-tokens-staging";
    public static final String BUNDLES_FOLDER = "/bundles";

    private static final boolean PRINT_FILES = false;

    public static final String WEB_BUNDLE_NAME = BuildConfig.DEBUG ? BRD_WEB_STAGING : BRD_WEB;
    public static final String TOKEN_ASSETS_BUNDLE_NAME = BuildConfig.DEBUG
            ? BRD_TOKEN_ASSETS_STAGING : BRD_TOKEN_ASSETS;
    public static final String[] BUNDLE_NAMES = {WEB_BUNDLE_NAME, TOKEN_ASSETS_BUNDLE_NAME};

    private ServerBundlesHelper() {
    }

    /**
     * Extract bundles from apk into the device storage if there is no bundle already available.
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
                    InputStream inputStream = context.getResources().openRawResource(resource);
                    FileUtils.copyInputStreamToFile(inputStream, bundleFile);
                    boolean extracted = tryExtractTar(context, bundleName);
                    if (!extracted) {
                        Log.e(TAG, "Failed to extract tar: " + resourceName);
                    } else {
                        // Update bundle version in shared preferences, this is used to check if we have latest version.
                        String currentBundleVersion = getCurrentBundleVersion(bundleFile);
                        if (!Utils.isNullOrEmpty(currentBundleVersion)) {
                            BRSharedPrefs.putBundleHash(context, bundleName, currentBundleVersion);
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
     *  Methods copied from APIClient, duplicate code must be removed from APIClient after refactor 01/17/19 DROID-1133
     */

    /**
     * Returns the resource at bundles/path, if path is null then the bundle folder.
     *
     * @param context Execution context.
     * @param path    Bundle's path.
     * @return Resource path or the bundle folder if the path is null.
     */
    private static String getBundleResource(Context context, String path) {
        String bundle = context.getFilesDir().getAbsolutePath() + BUNDLES_FOLDER;
        if (Utils.isNullOrEmpty(path)) {
            return bundle;
        } else {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return bundle + path;
        }
    }

    private static boolean tryExtractTar(Context context, String bundleName) {
        File bundleFile = new File(getBundleResource(context, bundleName + TAR_EXTENSION));
        Log.d(TAG, "tryExtractTar: " + bundleFile.getAbsolutePath());
        boolean result = false;
        try (InputStream inputStream = new FileInputStream(bundleFile)) {
            try (TarArchiveInputStream debInputStream
                         = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream(TAR, inputStream)) {
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

    //returns the extracted folder or the path in it
    private static String getExtractedPath(Context context, String bundleName, String path) {
        String extracted = new StringBuffer().append(context.getFilesDir().getAbsolutePath())
                .append("/")
                .append(bundleName)
                .append(EXTRACTED)
                .toString();
        if (Utils.isNullOrEmpty(path)) {
            return extracted;
        } else {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            return extracted + path;
        }
    }

    private static void logFiles(String tag, String bundleName, Context context) {
        if (PRINT_FILES) {
            Log.e(TAG, "logFiles " + tag + " : START LOGGING");
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
}
