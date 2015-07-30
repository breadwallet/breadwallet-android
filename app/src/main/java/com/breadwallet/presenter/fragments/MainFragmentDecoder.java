package com.breadwallet.presenter.fragments;

import android.content.Intent;
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
import com.breadwallet.presenter.activities.ScanResultActivity;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.QRCodeReaderView;

public class MainFragmentDecoder extends Fragment implements QRCodeReaderView.OnQRCodeReadListener {

    public static final String TAG = "MainFragmentDecoder";

    private boolean accessGranted = true;
    public static QRCodeReaderView mydecoderview;
    private ImageView camera_guide_image;
    private Intent intent;
    private static boolean addViewAllowed;
    public static MainFragmentDecoder mainFragmentDecoder;
    private RelativeLayout layout;

    public MainFragmentDecoder() {
        mainFragmentDecoder = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_decoder, container, false);

        intent = new Intent(getActivity(), ScanResultActivity.class);
        camera_guide_image = (ImageView) rootView.findViewById(R.id.camera_guide_image);
        SpringAnimator.showExpandCameraGuide(camera_guide_image);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        layout = (RelativeLayout) getView().findViewById(R.id.fragment_decoder_layout);
//        mydecoderview = new QRCodeReaderView(getActivity().getApplicationContext());
//        mydecoderview.setOnQRCodeReadListener(mainFragmentDecoder);

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
//            Log.e(TAG, "Activity STARTED!!!!!");
                intent.putExtra("result", text);
                startActivity(intent);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity.app.hideDecoderFragment();
                }
            }, 1000);
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
        addViewAllowed = true;
        startQRScanner();
        MainActivity.app.activityButtonsEnable(false);

    }

    @Override
    public void onPause() {
        super.onPause();
        addViewAllowed = false;
        stopQRScanner();

    }

    private void startQRScanner() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mydecoderview == null) {
                    mydecoderview = new QRCodeReaderView(MainActivity.app);
                    mydecoderview.setOnQRCodeReadListener(mainFragmentDecoder);
                    mydecoderview.getCameraManager().startPreview();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (addViewAllowed) {
                            addViewAllowed = false;
                            layout.addView(mydecoderview, 0);
                        }
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
                mydecoderview.getCameraManager().stopPreview();
            }
        });
        mydecoderview = null;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MainActivity.app.activityButtonsEnable(true);
            }
        },300);

    }

}
