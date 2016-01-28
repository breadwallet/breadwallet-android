
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.qrcode.QRCodeEncoder;
import com.breadwallet.wallet.BRWalletManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 6/23/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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

public class MainFragmentQR extends Fragment {
    private static final String TAG = MainFragmentQR.class.getName();
    private ImageView qrcode;
    private TextView mainAddressText;
    private Bitmap bitmap;
    private SharingFragment sharingFragment;
    private FragmentManager fm;
    private int count;
    private int firstToastY = -1;
    private int secondToastY = -1;
    public static File qrCodeImageFile;
    public static String receiveAddress;
    public static final String RECEIVE_ADDRESS_PREFS = "ReceiveAddress";
    public static final String RECEIVE_ADDRESS = "address";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragment_qr_main, container, false);

        SharedPreferences prefs = getActivity().getSharedPreferences(RECEIVE_ADDRESS_PREFS, Context.MODE_PRIVATE);
        receiveAddress = prefs.getString(RECEIVE_ADDRESS, null);
        Log.e(TAG,"FROM PREFS receiveAddress: " + receiveAddress);
        if (receiveAddress == null) {
            BRWalletManager.getInstance(getActivity()).refreshAddress();
        }
        //TODO refresh the address once used
        qrcode = (ImageView) rootView.findViewById(R.id.main_image_qr_code);
        sharingFragment = new SharingFragment();
        RelativeLayout main_fragment_qr = (RelativeLayout) rootView.findViewById(R.id.main_fragment_qr);
        mainAddressText = (TextView) rootView.findViewById(R.id.main_address_text);
        RelativeLayout addressLayout = (RelativeLayout) rootView.findViewById(R.id.theAddressLayout);
        generateQR();
        fm = getActivity().getFragmentManager();
        main_fragment_qr.setPadding(0, MainActivity.screenParametersPoint.y / 5, 0, 0);
        final BreadWalletApp breadWalletApp = (BreadWalletApp) MainActivity.app.getApplication();
        final String finalReceiveAddress = receiveAddress;
        addressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                breadWalletApp.cancelToast();
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    sharingFragment.setTheAddress(finalReceiveAddress);
//                    saveBitmapToFile();
                    sharingFragment.show(fm, SharingFragment.class.getName());
                }
            }
        });
        qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    if (count == 0) {
                        if (firstToastY == -1)
                            firstToastY = BreadWalletApp.DISPLAY_HEIGHT_PX - breadWalletApp.getRelativeTop(mainAddressText) + 400;
                        breadWalletApp.showCustomToast(MainActivity.app,
                                getResources().getString(R.string.toast_qr_tip), firstToastY, Toast.LENGTH_LONG);
                        Log.e(TAG, "Toast show nr: " + count);
                        count++;
                    } else if (count == 1) {
                        if (secondToastY == -1)
                            secondToastY = BreadWalletApp.DISPLAY_HEIGHT_PX - breadWalletApp.getRelativeTop(mainAddressText);
                        breadWalletApp.showCustomToast(MainActivity.app,
                                getResources().getString(R.string.toast_address_tip),
                                secondToastY, Toast.LENGTH_LONG);
                        Log.e(TAG, "Toast show nr: " + count);
                        count--;
                    }
                }

            }
        });
//        //TODO testing only
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                receiveAddress = BRWalletManager.getInstance(getActivity()).getReceiveAddress();
//                mainAddressText.setText(receiveAddress + " ----");
//            }
//        }, 10000);
        return rootView;
    }

    private void generateQR() {
        WindowManager manager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 3 / 4;

        Log.e(TAG, "@@@@@@@@@@@receiveAddress: " + receiveAddress);
        if (receiveAddress.length() < 5)
            throw new NullPointerException("receiveAddress cannot be null or it's corrupted!");
        mainAddressText.setText(receiveAddress);
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder("bitcoin:" + receiveAddress,
                BarcodeFormat.QR_CODE.toString(),
                smallerDimension);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
            qrcode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void saveBitmapToFile() {
        FileOutputStream out = null;
        String path = Environment.getExternalStorageDirectory().toString();
        qrCodeImageFile = new File(path, "qrImage" + ".jpeg");
        if (qrCodeImageFile.exists()) {
            Log.d(TAG, "File exists! deleting");
            qrCodeImageFile.delete();
        } else {
            Log.d(TAG, "File did not exist, creating a new one");
        }
        try {
            out = new FileOutputStream(qrCodeImageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(null);
    }

}
