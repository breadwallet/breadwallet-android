package com.breadwallet.tools.qrcode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.wallet.WalletsMaster;
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
    private static final String SHARE_IMAGE_TYPE = "image/*";
    private static final String SHARE_SUBJECT = "Address";
    public static final String VIA_EMAIL = "mailto:";
    public static final String VIA_MESSAGE ="sms:";

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

//    public void saveBitmap() {
//        String filename;
//        Date date = new Date(0);
//        SimpleDateFormat sdf = new SimpleDateFormat ("yyyyMMddHHmmss");
//        filename =  sdf.format(date);
//
//        try{
//            String path = Environment.getExternalStorageDirectory().toString();
//            OutputStream fOut = null;
//            File file = new File(path, "/DCIM/Signatures/"+filename+".jpg");
//            fOut = new FileOutputStream(file);
//
//            mBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
//            fOut.flush();
//            fOut.close();
//
//            MediaStore.Images.Media.insertImage(getContentResolver()
//                    ,file.getAbsolutePath(),file.getName(),file.getName());
//
//        }catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

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
        //qrcode.setPadding(1, 1, 1, 1);
        //qrcode.setBackgroundResource(R.color.gray);
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
            Log.e(TAG, "share: app is null");
            return;
        }

        File file = saveToExternalStorage(QRUtils.encodeAsBitmap(bitcoinUri, 500), app);
        //Uri uri = Uri.fromFile(file);
        Uri uri = FileProvider.getUriForFile(app, BuildConfig.APPLICATION_ID, file);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType(SHARE_IMAGE_TYPE);
        intent.putExtra(Intent.EXTRA_SUBJECT, WalletsMaster.getInstance(app).getCurrentWallet(app).getName() + SHARE_SUBJECT);
        intent.putExtra(Intent.EXTRA_TEXT, bitcoinUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (uri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            Log.d(TAG, "Uri -> " + file.getPath());
        } else
            Log.d(TAG, "Bitmap uri is null!");
        app.startActivity(Intent.createChooser(intent, app.getString(R.string.Receive_share)));


    }

    private static File saveToExternalStorage(Bitmap bitmapImage, Activity app) {
        if (app == null) {
            Log.e(TAG, "saveToExternalStorage: app is null");
            return null;
        }


        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String fileName = "qrcode.jpg";

        bitmapImage.compress(Bitmap.CompressFormat.PNG, 0, bytes);
        File f = new File(app.getExternalCacheDir(), fileName);
        f.setReadable(true, false);
        try {
            boolean a = f.createNewFile();
            if (!a) Log.e(TAG, "saveToExternalStorage: createNewFile: failed");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "saveToExternalStorage: " + f.getAbsolutePath());
        if (f.exists()) f.delete();

        try {
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
        return f;
    }


}
