package com.breadwallet.presenter.fragments;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.tools.others.MyClipboardManager;
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
    private TextView mainAddressText;
    private boolean customToastAvailable = true;
    private Bitmap bitmap;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragmentqr_main, container, false);
        qrcode = (ImageView) rootView.findViewById(R.id.mainimageqrcode);
        generateQR();
        mainAddressText = (TextView) rootView.findViewById(R.id.mainaddresstext);
        mainAddressText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tmp = mainAddressText.getText().toString();
                MyClipboardManager.copyToClipboard(getActivity(), tmp);
                if (customToastAvailable) {
                    customToastAvailable = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            customToastAvailable = true;
                        }
                    }, 2000);
                    ((BreadWalletApp) getActivity().getApplicationContext()).
                            showCustomToast(getActivity(), "Address copied to clipboard", 360, Toast.LENGTH_SHORT);
                }
            }
        });
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
            bitmap = qrCodeEncoder.encodeAsBitmap();
            qrcode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
