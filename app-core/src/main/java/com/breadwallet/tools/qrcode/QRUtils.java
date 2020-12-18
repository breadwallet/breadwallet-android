package com.breadwallet.tools.qrcode;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.breadwallet.appcore.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.io.OutputStream;
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
    private static final String SHARE_IMAGE_TYPE = "image/jpeg";
    private static final String INTENT_TYPE = "image/*";
    private static final String SHARE_TITLE = "QrCodeTitle";
    public static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_ID = 1133;
    private static final int BITMAP_SIZE = 500;
    private static final int BITMAP_QUALITY = 100;
    private static String mQrDataToShare; // TODO to fix this later in the code.
    private static String mTextToShare; // TODO same as above

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

    /**
     * Send a share intent with a QR code and a text.
     * @param context       The context in which we are operating.
     * @param qrData        Uri used to generate the QR code.
     * @param text          Uri to be include in the message.
     */
    @Nullable
    public static Intent sendShareIntent(Context context, String qrData, String text, String walletName) {
        mQrDataToShare = qrData;
        mTextToShare = text;
        return share(context, walletName);
    }

    @Nullable
    public static Intent share(Context context, String walletName) {
        Bitmap qrImage = QRUtils.encodeAsBitmap(mQrDataToShare, BITMAP_SIZE);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(SHARE_IMAGE_TYPE);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, SHARE_TITLE);
        values.put(MediaStore.Images.Media.MIME_TYPE, SHARE_IMAGE_TYPE);
        Uri fileUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (fileUri != null) {
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(fileUri)) {
                qrImage.compress(Bitmap.CompressFormat.JPEG, BITMAP_QUALITY, outputStream);
            } catch (IOException e) {
                Log.e(TAG, "share: ", e);
            }
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.setType(INTENT_TYPE);
            shareIntent.putExtra(Intent.EXTRA_TEXT, mTextToShare);
            String emailSubject = context.getString(R.string.Email_address_subject, walletName);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return Intent.createChooser(shareIntent, context.getString(R.string.Receive_share));
        }
        return null;
    }

}
