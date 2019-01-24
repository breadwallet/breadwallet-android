package com.breadwallet.tools.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.tools.manager.BRSharedPrefs;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/5/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class LogsUtils {
    private static final String DEFAULT_LOG_ATTACHMENT_BODY = "No logs.";
    // Filters out our apps events at log level = verbose
    private static final String LOGCAT_COMMAND = String.format("logcat -d %s:V", BuildConfig.APPLICATION_ID);
    private static final String FAILED_ERROR_MESSAGE = "Failed to get logs.";
    private static final String DEFAULT_LOGS_EMAIL = "android@brd.com";
    private static final String NO_EMAIL_APP_ERROR_MESSAGE = "No email app found.";
    private static final String LOGS_EMAIL_SUBJECT = "BRD Android App Feedback [ID:%s]"; // Placeholder is for a unique id.
    private static final String LOGS_FILE_NAME = "Logs.txt";
    private static final String MIME_TYPE = "text/plain";

    private LogsUtils() {
    }

    private static String getLogs(Context context) {
        try {
            Process process = Runtime.getRuntime().exec(LOGCAT_COMMAND);
            return IOUtils.toString(process.getInputStream());
        } catch (IOException ex) {
            BRToast.showCustomToast(context, FAILED_ERROR_MESSAGE,
                    BRSharedPrefs.getScreenHeight(context) / 2, Toast.LENGTH_LONG, 0);
        }
        return DEFAULT_LOG_ATTACHMENT_BODY;
    }

    private static String getDeviceInfo(Context context) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Feedback\n");
        stringBuilder.append("------------\n");
        stringBuilder.append("[Please add your feedback.]\n\n");
        stringBuilder.append("Device Info\n");
        stringBuilder.append("------------\n");
        stringBuilder.append("Wallet id: " + BRSharedPrefs.getWalletRewardId(context));
        stringBuilder.append("\nDevice id: " + BRSharedPrefs.getDeviceId(context));
        stringBuilder.append("\nDebuggable: " + BuildConfig.DEBUG);
        stringBuilder.append("\nApplication id: " + BuildConfig.APPLICATION_ID);
        stringBuilder.append("\nBuild Type: " + BuildConfig.BUILD_TYPE);
        stringBuilder.append("\nBuild Flavor: " + BuildConfig.FLAVOR);
        stringBuilder.append("\nApp Version: " + (BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_VERSION));
        for (String bundleName : ServerBundlesHelper.getBundleNames(context)) {
            stringBuilder.append(String.format("\n Bundle %s - Version: %s", bundleName, BRSharedPrefs.getBundleHash(context, bundleName)));
        }
        stringBuilder.append("\nNetwork: " + (BuildConfig.BITCOIN_TESTNET ? "Testnet" : "Mainnet"));
        stringBuilder.append("\nOS Version: " + Build.VERSION.RELEASE);
        stringBuilder.append("\nDevice Type: " + (Build.MANUFACTURER + " " + Build.MODEL + "\n"));

        return stringBuilder.toString();
    }

    public static void shareLogs(Context context) {
        File file = FileHelper.saveToExternalStorage(context, LOGS_FILE_NAME, getLogs(context));
        Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, file);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType(MIME_TYPE);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{DEFAULT_LOGS_EMAIL});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(LOGS_EMAIL_SUBJECT, BRSharedPrefs.getDeviceId(context)));
        emailIntent.putExtra(Intent.EXTRA_TEXT, getDeviceInfo(context));

        try {
            context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.Receive_share)));
        } catch (ActivityNotFoundException e) {
            BRToast.showCustomToast(context, NO_EMAIL_APP_ERROR_MESSAGE,
                    BRSharedPrefs.getScreenHeight(context) / 2, Toast.LENGTH_LONG, 0);
        }
    }
}
