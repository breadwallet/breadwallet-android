package com.breadwallet.presenter.fragments;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.tools.qrcode.Contents;
import com.breadwallet.tools.qrcode.QRCodeEncoder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

/**
 * Created by Mihail on 6/23/15.
 */
public class MainFragmentQR extends Fragment {
    public static final String TAG = "MainFragmentQR";
    private ImageView qrcode;
    private static final String TEST_ADDRESS = "mhBmRiqosSHR9YnPTKc3xXcvhEcKtjet2p";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragmentqr_main, container, false);
        qrcode = (ImageView) rootView.findViewById(R.id.mainimageqrcode);
        generateQR();
        return rootView;
    }

    private void generateQR() {
        WindowManager manager = (WindowManager) getActivity().getSystemService(getActivity().WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 3 / 4;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(TEST_ADDRESS,
                null,
                Contents.Type.TEXT,
                BarcodeFormat.QR_CODE.toString(),
                smallerDimension);
        try {
            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
            qrcode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
