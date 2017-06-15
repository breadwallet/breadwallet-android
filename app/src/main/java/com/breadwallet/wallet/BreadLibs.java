package com.breadwallet.wallet;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 6/15/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class BreadLibs {

    private static final String TAG = BreadLibs.class.getName();

    /**
     * Size of the buffer to read/write data
     */

    private static final int BUFFER_SIZE = 4096;

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     *
     * @param zipFilePath
     * @param destDirectory
     * @throws java.io.IOException
     */
    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    /**
     * Extracts a file from a zip to specified destination directory.
     * The path of the file inside the zip is discarded, the file is
     * copied directly to the destDirectory.
     *
     * @param zipFilePath   - path and file name of a zip file
     * @param inZipFilePath - path and file name inside the zip
     * @param destDirectory - directory to which the file from zip should be extracted, the path part is discarded.
     * @throws java.io.IOException
     */
    public static void extractFile(String zipFilePath, String inZipFilePath, String destDirectory) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            if (!entry.isDirectory() && inZipFilePath.equals(entry.getName())) {
                String filePath = entry.getName();
                int separatorIndex = filePath.lastIndexOf(File.separator);
                if (separatorIndex > -1)
                    filePath = filePath.substring(separatorIndex + 1, filePath.length());
                filePath = destDirectory + File.separator + filePath;
                extractFile(zipIn, filePath);
                break;
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    /**
     * Extracts a zip entry (file entry)
     *
     * @param zipIn
     * @param filePath
     * @throws java.io.IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    public static void initNativeLib(Context context, String libName) {
        try {
            // Try loading our native lib, see if it works...
            System.loadLibrary("core");
        } catch (UnsatisfiedLinkError er) {
            Log.e(TAG, "initNativeLib: ");
            ApplicationInfo appInfo = context.getApplicationInfo();
            String destPath = context.getFilesDir().toString();
            try {
                String soName = destPath + File.separator + libName;
                new File(soName).delete();
                extractFile(appInfo.sourceDir, "lib/" + Build.CPU_ABI + "/" + libName, destPath);
                System.load(soName);
            } catch (IOException e) {
                // extractFile to app files dir did not work. Not enough space? Try elsewhere...
                destPath = context.getExternalCacheDir().toString();
                // Note: location on external memory is not secure, everyone can read/write it...
                // However we extract from a "secure" place (our apk) and instantly load it,
                // on each start of the app, this should make it safer.
                String soName = destPath + File.separator + libName;
                new File(soName).delete(); // this copy could be old, or altered by an attack
                try {
                    extractFile(appInfo.sourceDir, "lib/" + Build.CPU_ABI + "/" + libName, destPath);
                    System.load(soName);
                } catch (IOException e2) {
                    Log.e(TAG, "Exception in InstallInfo.init(): " + e);
                    e.printStackTrace();

                    //try again
                    if (libName.equalsIgnoreCase("libCore.so")) {
                        initNativeLib(context, "libcore.so");
                    }
                }
            }
        }
    }
}
