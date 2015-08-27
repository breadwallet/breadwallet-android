
package com.breadwallet.presenter.fragments;

import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.QRCodeReaderView;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/14/15.
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

public class MainFragmentDecoder extends Fragment implements QRCodeReaderView.OnQRCodeReadListener {

    public static final String TAG = "MainFragmentDecoder";

    public static boolean accessGranted = true;
    public static QRCodeReaderView mydecoderview;
    private ImageView camera_guide_image;
    public static MainFragmentDecoder mainFragmentDecoder;
    private RelativeLayout layout;

    public MainFragmentDecoder() {
        mainFragmentDecoder = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_decoder, container, false);

        camera_guide_image = (ImageView) rootView.findViewById(R.id.decoder_camera_guide_image);
        SpringAnimator.showExpandCameraGuide(camera_guide_image);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        layout = (RelativeLayout) getView().findViewById(R.id.fragment_decoder_layout);
        Log.e(TAG, "The layout is: " + layout);

    }

    /**
     * Called when a QR is decoded
     * "text" : the text encoded in QR
     * "points" : points where QR control points are placed
     */

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        synchronized (this) {
            if (accessGranted) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        accessGranted = true;
                    }
                }, 300);
                accessGranted = false;
                if (text != null) {
                    FragmentScanResult.address = text;
                    FragmentAnimator.hideDecoderFragment();
                    FragmentAnimator.animateScanResultFragment();

                } else {
                    throw new NullPointerException();
                }
            }
        }
    }

    // Called when your device have no camera
    @Override
    public void cameraNotFound() {
        Log.d(TAG, "No Camera found!");
    }

    // Called when there's no QR codes in the camera preview image
    @Override
    public void QRCodeNotFoundOnCamImage() {
//        Log.d(TAG, "No QR Code found!");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "In the onResume()");
//        addViewAllowed = true;
        startQRScanner();

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "In the onPause()");
//        addViewAllowed = false;
        stopQRScanner();

    }

    private void startQRScanner() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MainActivity.app.activityButtonsEnable(false);
            }
        }, 300);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mydecoderview == null) {
                    mydecoderview = new QRCodeReaderView(MainActivity.app);
                }
                mydecoderview.setOnQRCodeReadListener(mainFragmentDecoder);
                mydecoderview.getCameraManager().startPreview();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mydecoderview != null)
                            layout.addView(mydecoderview, 0);
                    }
                }, 800);

                Log.e(TAG, "The camera started");
            }
        });

    }

    private void stopQRScanner() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                layout.removeView(mydecoderview);
                if (mydecoderview.getCameraManager().isOpen())
                    mydecoderview.getCameraManager().stopPreview();
            }
        });
        mydecoderview = null;
        MainActivity.app.activityButtonsEnable(true);

    }

}
