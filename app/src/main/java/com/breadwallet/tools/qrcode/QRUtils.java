package com.breadwallet.tools.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.tools.util.Utils;
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

import static android.R.attr.path;
import static android.R.attr.width;
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
    private static final String TAG = QRUtils.class.getName();

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
            e.printStackTrace();
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
            Log.e(TAG, "share: app is null");
            return;
        }

        String path = saveToExternalStorage(QRUtils.encodeAsBitmap(bitcoinUri, 500), app);
        Uri uri;
        if (path == null) {
            uri = null;
        } else {
            File qrFile = new File(path);
            uri = Uri.fromFile(qrFile);
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse(via));

        if (via.equalsIgnoreCase("sms:")) {
            intent.putExtra("sms_body", bitcoinUri);
            intent.putExtra("exit_on_sent", true);
        } else {
            intent.putExtra(Intent.EXTRA_SUBJECT, "Litecoin Address");
            intent.putExtra(Intent.EXTRA_TEXT, bitcoinUri);
        }
        if (uri != null)
            intent.putExtra(Intent.EXTRA_STREAM, uri);
        app.startActivity(Intent.createChooser(intent, "Litecoin Address"));
    }

    private static String saveToExternalStorage(Bitmap bitmapImage, Activity app) {
        if (app == null) {
            Log.e(TAG, "saveToExternalStorage: app is null");
            return null;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String fileName = "qrcode.jpg";

        bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        File f = new File(Environment.getExternalStorageDirectory() + File.separator + fileName);
        try {
            boolean result = f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bytes.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return f.getAbsolutePath();
    }
}
