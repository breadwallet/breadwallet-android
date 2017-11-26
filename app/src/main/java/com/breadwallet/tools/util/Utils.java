package com.breadwallet.tools.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.activities.intro.IntroActivity;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static android.content.Context.FINGERPRINT_SERVICE;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/21/16.
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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class Utils {
    public static final String TAG = Utils.class.getName();

    public static boolean isUsingCustomInputMethod(Activity context) {
        if (context == null) return false;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();
        final int N = mInputMethodProperties.size();
        for (int i = 0; i < N; i++) {
            InputMethodInfo imi = mInputMethodProperties.get(i);
            if (imi.getId().equals(
                    Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.DEFAULT_INPUT_METHOD))) {
                if ((imi.getServiceInfo().applicationInfo.flags &
                        ApplicationInfo.FLAG_SYSTEM) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static void printPhoneSpecs() {
        String specsTag = "PHONE SPECS";
        Log.e(specsTag, "");
        Log.e(specsTag, "***************************PHONE SPECS***************************");
        Log.e(specsTag, "* screen X: " + IntroActivity.screenParametersPoint.x + " , screen Y: " + IntroActivity.screenParametersPoint.y);
        Log.e(specsTag, "* Build.CPU_ABI: " + Build.CPU_ABI);
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        Log.e(specsTag, "* maxMemory:" + Long.toString(maxMemory));
        Log.e(specsTag, "----------------------------PHONE SPECS----------------------------");
        Log.e(specsTag, "");
    }

    public static boolean isEmulatorOrDebug(Context app) {
        String fing = Build.FINGERPRINT;
        boolean isEmulator = false;
        if (fing != null) {
            isEmulator = fing.contains("vbox") || fing.contains("generic");
        }
        return isEmulator || (0 != (app.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }

    public static String getFormattedDateFromLong(Context app, long time) {

        SimpleDateFormat formatter = new SimpleDateFormat("M/d@ha", Locale.getDefault());
        boolean is24HoursFormat = false;
        if (app != null) {
            is24HoursFormat = android.text.format.DateFormat.is24HourFormat(app.getApplicationContext());
            if (is24HoursFormat) {
                formatter = new SimpleDateFormat("M/d H", Locale.getDefault());
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String result = formatter.format(calendar.getTime()).toLowerCase().replace("am", "a").replace("pm", "p");
        if (is24HoursFormat) result += "h";
        return result;
    }

    public static String formatTimeStamp(long time, String pattern) {
//        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(time);
        return android.text.format.DateFormat.format(
                pattern, time).toString();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNullOrEmpty(byte[] arr) {
        return arr == null || arr.length == 0;
    }

    public static boolean isNullOrEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }

    public static int getPixelsFromDps(Context context, int dps) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String createBitcoinUrl(String address, long satoshiAmount, String label, String message, String rURL) {

        Uri.Builder builder = new Uri.Builder();
        builder = builder.scheme("bitcoin");
        if (address != null && !address.isEmpty())
            builder = builder.appendPath(address);
        if (satoshiAmount != 0)
            builder = builder.appendQueryParameter("amount", new BigDecimal(satoshiAmount).divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE).toPlainString());
        if (label != null && !label.isEmpty())
            builder = builder.appendQueryParameter("label", label);
        if (message != null && !message.isEmpty())
            builder = builder.appendQueryParameter("message", message);
        if (rURL != null && !rURL.isEmpty())
            builder = builder.appendQueryParameter("r", rURL);

        return builder.build().toString().replaceFirst("/", "");

    }

    public static boolean isFingerprintEnrolled(Context app) {
        FingerprintManager fingerprintManager = (FingerprintManager) app.getSystemService(FINGERPRINT_SERVICE);
        // Device doesn't support fingerprint authentication
        return ActivityCompat.checkSelfPermission(app, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints();
    }

    public static boolean isFingerprintAvailable(Context app) {
        FingerprintManager fingerprintManager = (FingerprintManager) app.getSystemService(FINGERPRINT_SERVICE);
        // Device doesn't support fingerprint authentication
        if (ActivityCompat.checkSelfPermission(app, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(app, "Fingerprint authentication permission not enabled", Toast.LENGTH_LONG).show();
            return false;
        }
        return fingerprintManager.isHardwareDetected();
    }

    public static void hideKeyboard(Context app) {
        if (app != null) {
            View view = ((Activity) app).getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) app.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

    }

    public static String getAgentString(Context app, String cfnetwork) {

        int versionNumber = 0;
        if (app != null) {
            try {
                PackageInfo pInfo = null;
                pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                versionNumber = pInfo.versionCode;

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        String release = Build.VERSION.RELEASE;
//        return String.format("%s/%d %s %s/%s", "Bread", versionNumber, cfnetwork, "Android", release);
        return "Bread/" + String.valueOf(versionNumber) + " " + cfnetwork + " Android/" + release;
    }

    public static String reverseHex(String hex) {
        if (hex == null) return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i <= hex.length() - 2; i = i + 2) {
            result.append(new StringBuilder(hex.substring(i, i + 2)).reverse());
        }
        return result.reverse().toString();
    }

}