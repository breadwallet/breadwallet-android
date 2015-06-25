package com.breadwallet.qrcode;

import android.graphics.Bitmap;

import com.breadwallet.R;
import com.breadwallet.activities.MainActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.jwetherell.quick_response_code.data.Contents;
import com.jwetherell.quick_response_code.qrcode.QRCodeEncoder;

/**
 * Created by Mihail on 6/23/15.
 */
public class QREncoder {
    public static final String TAG = "QREncoder";
    private static MainActivity app;

    public static Bitmap convertToQR(String message) throws WriterException {
        app = MainActivity.getApp();
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(message,
                null,
                Contents.Type.TEXT,
                BarcodeFormat.QR_CODE.toString(),
                (int) app.getResources().getDimension(R.dimen.qr_dimension));
        Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
        return bitmap;
    }
}
