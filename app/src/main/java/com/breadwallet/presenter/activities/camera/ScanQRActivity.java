package com.breadwallet.presenter.activities.camera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.QRReader;
import com.breadwallet.tools.security.BitcoinUrlHandler;
import com.platform.tools.BRBitId;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/29/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class ScanQRActivity extends BRActivity {
    private SurfaceView mySurfaceView;
    private QRReader qrEader;
    private static final String TAG = ScanQRActivity.class.getName();
    //    private boolean dataProcessing = false;
    private ImageView cameraGuide;
    private TextView descriptionText;
    private long lastUpdated;
    private UIUpdateTask task;
    private boolean handlingCode;
    public static boolean appVisible = false;
    private static ScanQRActivity app;

    public static ScanQRActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);


        mySurfaceView = (SurfaceView) findViewById(R.id.camera_view);
        cameraGuide = (ImageView) findViewById(R.id.scan_guide);
        descriptionText = (TextView) findViewById(R.id.description_text);

        task = new UIUpdateTask();
        task.start();

        cameraGuide.setImageResource(R.drawable.cameraguide);
        cameraGuide.setVisibility(View.GONE);

        qrEader = new QRReader.Builder(this, mySurfaceView, new QRReader.OnQrFoundListener() {
            @Override
            public void onDetected(final String data) {
                if (handlingCode) return;
                if (BitcoinUrlHandler.isBitcoinUrl(data) || BRBitId.isBitId(data)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lastUpdated = System.currentTimeMillis();
                            cameraGuide.setImageResource(R.drawable.cameraguide);
                            descriptionText.setText("");
                            handlingCode = true;
                            handleDetected(data);

                        }
                    });
                }  else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lastUpdated = System.currentTimeMillis();
                            cameraGuide.setImageResource(R.drawable.cameraguide_red);
                            descriptionText.setText(getString(R.string.Send_invalidAddressTitle));
                        }
                    });
                }

            }
        }).facing(QRReader.BACK_CAM)
                .enableAutofocus(true)
                .height(mySurfaceView.getHeight())
                .width(mySurfaceView.getWidth())
                .build();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cameraGuide.setVisibility(View.VISIBLE);
                SpringAnimator.showExpandCameraGuide(cameraGuide);
            }
        }, 400);

    }


    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        qrEader.initAndStart(mySurfaceView);
        app = this;
        ActivityUTILS.init(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
        qrEader.release();
    }

    @Override
    public void onBackPressed() {
        overridePendingTransition(R.anim.fade_down, 0);
        super.onBackPressed();
    }

    private void handleDetected(String data) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", data);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    private class UIUpdateTask extends Thread {
        public boolean running = true;

        @Override
        public void run() {
            super.run();
            while (running) {
                if (System.currentTimeMillis() - lastUpdated > 300) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cameraGuide.setImageResource(R.drawable.cameraguide);
                            descriptionText.setText("");
                        }
                    });
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }
}