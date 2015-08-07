
package com.breadwallet.presenter.fragments;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.qrcode.Contents;
import com.breadwallet.tools.qrcode.QRCodeEncoder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

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
    public static final String TAG = "MainFragmentQR";
    private ImageView qrcode;
    private static final String TEST_ADDRESS = "mhBmRiqosSHR9YnPTKc3xXcvhEcKtjet2p";
    private TextView mainAddressText;
    private RelativeLayout addressLayout;
    private Bitmap bitmap;
    public SharingFragment sharingFragment;
    public FragmentManager fm;
    private int count;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragmentqr_main, container, false);
        qrcode = (ImageView) rootView.findViewById(R.id.mainimageqrcode);
        sharingFragment = new SharingFragment();
        generateQR();
        mainAddressText = (TextView) rootView.findViewById(R.id.mainaddresstext);
        addressLayout = (RelativeLayout) rootView.findViewById(R.id.theAddressLayout);
        fm = getActivity().getSupportFragmentManager();
        addressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FragmentAnimator.multiplePressingAvailable) {
                    FragmentAnimator.pauseTheAnimationAvailabilityFor(300);
                    sharingFragment.setTheAddress(mainAddressText.getText().toString());
                    sharingFragment.show(fm, "sharingFragment");
                }
            }
        });
        qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count == 0) {
                    ((BreadWalletApp) getActivity().getApplication()).showCustomToast(MainActivity.app,
                            getResources().getString(R.string.toast_qr_tip), 460, Toast.LENGTH_LONG);
                    Log.e(TAG, "Toast show nr: " + count);
                    count++;
                } else if (count == 1) {
                    ((BreadWalletApp) getActivity().getApplication()).showCustomToast(MainActivity.app,
                            getResources().getString(R.string.toast_address_tip), 140, Toast.LENGTH_LONG);
                    Log.e(TAG, "Toast show nr: " + count);
                    count--;
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
