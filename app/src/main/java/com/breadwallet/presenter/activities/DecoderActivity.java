
package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.qrcode.QRCodeReaderView;

/**
 * BreadWallet
 *
 * Created by Mihail on 8/4/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class DecoderActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {

    public static final String TAG = "DecoderActivity";

    private boolean accessGranted = true;
    private QRCodeReaderView mydecoderview;
    private DecoderActivity decoderActivity;

    public DecoderActivity() {
        decoderActivity = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);
//
    }

    /**
     * Called when a QR is decoded
     * "text" : the text encoded in QR
     * "points" : points where QR control points are placed
     */
    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        Log.e(TAG, "QRCode was read!");
        if (accessGranted) {
            accessGranted = false;
            if(text!= null){
                FragmentScanResult.address = text;
                FragmentAnimator.animateScanResultFragment();
            } else {
                throw new NullPointerException("The scanned text is null!");
            }
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
