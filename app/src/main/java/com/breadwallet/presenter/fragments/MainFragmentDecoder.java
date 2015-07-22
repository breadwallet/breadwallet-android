package com.breadwallet.presenter.fragments;

import android.content.Intent;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ScanResultActivity;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.QRCodeReaderView;

public class MainFragmentDecoder extends Fragment implements QRCodeReaderView.OnQRCodeReadListener {

    public static final String TAG = "MainFragmentDecoder";

    private boolean accessGranted = true;
    private TextView myTextView;
    private static QRCodeReaderView mydecoderview;
    private ImageView camera_guide_image;
    private Intent intent;
    private static MainFragmentDecoder decoderFragment;
    private RelativeLayout layout;

    public MainFragmentDecoder() {
        decoderFragment = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_decoder, container, false);

        intent = new Intent(getActivity(), ScanResultActivity.class);
        myTextView = (TextView) rootView.findViewById(R.id.exampleTextView);
        camera_guide_image = (ImageView) rootView.findViewById(R.id.camera_guide_image);
        SpringAnimator.showExpandCameraGuide(camera_guide_image);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        layout = (RelativeLayout) getView().findViewById(R.id.fragment_decoder_layout);
        mydecoderview = new QRCodeReaderView(getActivity().getApplicationContext());
        mydecoderview.setOnQRCodeReadListener(decoderFragment);
        if (mydecoderview != null)
            mydecoderview.getCameraManager().startPreview();
        mydecoderview.setVisibility(View.GONE);

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
                accessGranted = false;
//            Log.e(TAG, "Activity STARTED!!!!!");
                intent.putExtra("result", text);
                startActivity(intent);
            }
        }
    }

    // Called when your device have no camera
    @Override
    public void cameraNotFound() {

    }

    // Called when there's no QR codes in the camera preview image
    @Override
    public void QRCodeNotFoundOnCamImage() {

    }

    @Override
    public void onResume() {
        super.onResume();
        new CameraOpenerTask().execute();

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "In onPause");
        mydecoderview.getCameraManager().stopPreview();
        layout.removeView(mydecoderview);

    }

    private class CameraOpenerTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    layout.addView(mydecoderview, 0);
                    mydecoderview.setVisibility(View.VISIBLE);
                }
            }, 1300);

            Log.e(TAG, "The camera started");
        }
    }

    public void stopCamera() {
        if (mydecoderview != null) {
            mydecoderview.getCameraManager().stopPreview();
            mydecoderview.getCameraManager().closeDriver();
        }
        mydecoderview = null;

    }

    public static MainFragmentDecoder getMainFragmentDecoder() {
        return decoderFragment;
    }

    public static QRCodeReaderView getMydecoderview() {
        return mydecoderview;
    }

}
