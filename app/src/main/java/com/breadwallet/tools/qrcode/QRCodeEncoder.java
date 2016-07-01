package com.breadwallet.tools.qrcode;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

public final class QRCodeEncoder {
    private static final String TAG = QRCodeEncoder.class.getName();

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private int dimension = Integer.MIN_VALUE;
    private String contents = null;
    private String displayContents = null;
    private String title = null;
    private BarcodeFormat format = null;
    private boolean encoded = false;

    public QRCodeEncoder(String data, String format, int dimension) {
        this.dimension = dimension;
        encoded = encodeContents(data, null, Contents.Type.TEXT, format);
    }

    public String getContents() {
        return contents;
    }

    public String getDisplayContents() {
        return displayContents;
    }

    public String getTitle() {
        return title;
    }

    private boolean encodeContents(String data, Bundle bundle, String type, String formatString) {
        // Default to QR_CODE if no format given.
        format = null;
        if (formatString != null) {
            try {
                format = BarcodeFormat.valueOf(formatString);
            } catch (IllegalArgumentException iae) {
                // Ignore it then
            }
        }
        if (format == null || format == BarcodeFormat.QR_CODE) {
            this.format = BarcodeFormat.QR_CODE;
            encodeQRCodeContents(data, bundle, type);
        } else if (data != null && data.length() > 0) {
            contents = data;
            displayContents = data;
            title = "Text";
        }
        return contents != null && contents.length() > 0;
    }

    private void encodeQRCodeContents(String data, Bundle bundle, String type) {
        switch (type) {
            case Contents.Type.TEXT:
                if (data != null && data.length() > 0) {
                    contents = data;
                    displayContents = data;
                    title = "Text";
                }
                break;
        }
    }

    public Bitmap encodeAsBitmap() throws WriterException {
        if (!encoded) return null;

        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(contents);
        if (encoding != null) {
            hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result = writer.encode(contents, format, dimension, dimension, hints);
        int width = result.getWidth();
        int height = result.getHeight();
        Log.e(TAG, "width: " + width);
        Log.e(TAG, "height: " + height);
        int[] pixels = new int[width * height];
        // All are 0, or black, by default
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        Log.e(TAG, "The Bitmap pixels: " + bitmap.getWidth() + "  " + bitmap.getHeight());
        Bitmap croppedResult = Bitmap.createBitmap(bitmap, 80, 80, bitmap.getWidth() - 160, bitmap.getHeight() - 160);
        bitmap = null;
        return croppedResult;
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

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String result = s.trim();
        return result.length() == 0 ? null : result;
    }

    private static String escapeMECARD(String input) {
        if (input == null || (input.indexOf(':') < 0 && input.indexOf(';') < 0)) {
            return input;
        }
        int length = input.length();
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (c == ':' || c == ';') {
                result.append('\\');
            }
            result.append(c);
        }
        return result.toString();
    }
}