package com.breadwallet.presenter.fragments;

import android.content.Intent;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ScanResultActivity;
import com.breadwallet.tools.qrcode.QRCodeReaderView;

public class MainFragmentDecoder extends Fragment implements QRCodeReaderView.OnQRCodeReadListener {

    public static final String TAG = "DecoderActivity";

    private boolean accessGranted = true;
    private TextView myTextView;
    private QRCodeReaderView mydecoderview;
    private ImageView line_image;
    private Intent intent;
    private MainFragmentDecoder decoderFragment;
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
        line_image = (ImageView) rootView.findViewById(R.id.red_line_image);

        TranslateAnimation mAnimation = new TranslateAnimation(
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.RELATIVE_TO_PARENT, 0f,
                TranslateAnimation.RELATIVE_TO_PARENT, 0.5f);
        mAnimation.setDuration(1000);
        mAnimation.setRepeatCount(-1);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(new LinearInterpolator());
        line_image.setAnimation(mAnimation);
        // Inflate the layout for this fragment
        return rootView;
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
                myTextView.setText(text);
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
        new ViewRemoverTask().execute();

    }

    private class CameraOpenerTask extends AsyncTask {


        @Override
        protected Object doInBackground(Object[] params) {
            layout = (RelativeLayout) getActivity().findViewById(R.id.fragment_decoder_layout);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            mydecoderview = new QRCodeReaderView(getActivity().getApplicationContext());
            mydecoderview.setOnQRCodeReadListener(decoderFragment);
            mydecoderview.getCameraManager().startPreview();
            layout.addView(mydecoderview);
        }
    }

    private class ViewRemoverTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            layout.removeView(mydecoderview);
            mydecoderview = null;
            Log.e(TAG, "Removed everithing good!");
        }
    }
}
