package com.breadwallet.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.qrcode.QREncoder;
import com.google.zxing.WriterException;

/**
 * Created by Mihail on 6/23/15.
 */
public class MainFragmentQR extends Fragment {
    public static final String TAG = "MainFragmentQR";
    private ImageView qrcode;


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragmentqr_main, container, false);
        qrcode = (ImageView) rootView.findViewById(R.id.mainimageqrcode);
        try {
            qrcode.setImageBitmap(QREncoder.convertToQR("mhBmRiqosSHR9YnPTKc3xXcvhEcKtjet2p"));
        } catch (WriterException e) {
            Log.e(TAG, "Failed to set the QR Image BitMap! ", e);
        }

        return rootView;
    }
}
