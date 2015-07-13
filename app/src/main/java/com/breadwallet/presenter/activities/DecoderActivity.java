package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.qrcode.QRCodeReaderView;

public class DecoderActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {

    public static final String TAG = "DecoderActivity";

    private boolean accessGranted = true;
    private TextView myTextView;
    private QRCodeReaderView mydecoderview;
    private ImageView line_image;
    private Intent intent;
    private DecoderActivity decoderActivity;

    public DecoderActivity() {
        decoderActivity = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);
//
        intent = new Intent(this, ScanResultActivity.class);

        myTextView = (TextView) findViewById(R.id.exampleTextView);

        line_image = (ImageView) findViewById(R.id.red_line_image);

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

    }

    /**
     * Called when a QR is decoded
     * "text" : the text encoded in QR
     * "points" : points where QR control points are placed
     */
    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        if (accessGranted) {
            accessGranted = false;
            myTextView.setText(text);
//            Log.e(TAG, "Activity STARTED!!!!!");
            intent.putExtra("result", text);
            startActivity(intent);
            finish();
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
    protected void onResume() {
        super.onResume();
        new CameraOpenerTask().execute();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "In onPause");
        mydecoderview.getCameraManager().stopPreview();


    }

    private class CameraOpenerTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            mydecoderview = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
            mydecoderview.setOnQRCodeReadListener(decoderActivity);
            mydecoderview.getCameraManager().startPreview();
            return null;
        }
    }
}
