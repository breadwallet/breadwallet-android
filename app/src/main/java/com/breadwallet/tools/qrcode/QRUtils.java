package com.breadwallet.tools.qrcode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;

import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import timber.log.Timber;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/10/17.
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
public class QRUtils {

    public static Bitmap encodeAsBitmap(String content, int dimension) {

        if (content == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(content);
        hints = new EnumMap<>(EncodeHintType.class);
        if (encoding != null) {
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix result = null;
        try {
            result = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, dimension, dimension, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        } catch (WriterException e) {
            Timber.e(e);
        }
        if (result == null) return null;
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static boolean generateQR(Context ctx, String bitcoinURL, ImageView qrcode) {
        if (qrcode == null || bitcoinURL == null || bitcoinURL.isEmpty()) return false;
        WindowManager manager = (WindowManager) ctx.getSystemService(Activity.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = (int) (smallerDimension * 0.45f);
        Bitmap bitmap = null;
        bitmap = QRUtils.encodeAsBitmap(bitcoinURL, smallerDimension);
        if (bitmap == null) return false;
        qrcode.setImageBitmap(bitmap);
        return true;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

    public static void share(String via, Activity app, String bitcoinUri) {
        if (app == null) {
            Timber.d("share: app is null");
            return;
        }

        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Litecoin Address");
        intent.putExtra(Intent.EXTRA_TEXT, bitcoinUri);
        app.startActivity(Intent.createChooser(intent, "Open mail app"));
    }

    private static File saveToExternalStorage(Bitmap bitmapImage, Activity app) {
        if (app == null) {
            Timber.d("saveToExternalStorage: app is null");
            return null;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String fileName = "qrcode.jpg";

        bitmapImage.compress(Bitmap.CompressFormat.PNG, 0, bytes);
        File f = new File(app.getCacheDir(), fileName);
        f.setReadable(true, false);
        try {
            boolean a = f.createNewFile();
            if(!a) Timber.d("saveToExternalStorage: createNewFile: failed");
        } catch (IOException e) {
            Timber.e(e);
        }
        Timber.d("saveToExternalStorage: " + f.getAbsolutePath());
        if (f.exists()) f.delete();

        try {
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            try {
                bytes.close();
            } catch (IOException e) {
                Timber.e(e);
            }
        }
        return f;
    }
}
