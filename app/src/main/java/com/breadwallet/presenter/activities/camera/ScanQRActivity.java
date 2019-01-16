package com.breadwallet.presenter.activities.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.AppEntryPointHandler;
import com.breadwallet.tools.qrcode.QRCodeReaderView;
import com.breadwallet.tools.util.BRConstants;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;


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
public class ScanQRActivity extends BRActivity implements ActivityCompat.OnRequestPermissionsResultCallback, QRCodeReaderView.OnQRCodeReadListener {
    private static final String TAG = ScanQRActivity.class.getName();
    private static final int MY_PERMISSION_REQUEST_CAMERA = 56432;
    private static final int CAMERA_EXPANSION_DELAY_MILLISECONDS = 400;
    private static final long AUTO_FOCUS_PERIOD_TIME = 500;
    private static final long CAMERA_UI_UPDATE_MILLISECONDS = 100;
    public static final String EXTRA_RESULT = "com.breadwallet.presenter.activities.camera.ScanQRActivity.EXTRA_RESULT";

    private ImageView mCameraGuide;
    private long mLastUpdated;
    private boolean mIsQrProcessing;
    private Timer mCameraGuideTimer;
    private TimerTask mCameraGuideTask;
    private Handler mCameraGuideHandler;
    private QRCodeReaderView mQrCodeReaderView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        mCameraGuide = findViewById(R.id.scan_guide);

        mCameraGuideHandler = new Handler();

        mCameraGuide.setImageResource(R.drawable.cameraguide);
        mCameraGuide.setVisibility(View.GONE);

        if (android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initQRCodeReaderView();
        } else {
            Log.e(TAG, "onCreate: Permissions should have been granted by now, Can't happen!");
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mCameraGuide.setVisibility(View.VISIBLE);
                SpringAnimator.showExpandCameraGuide(mCameraGuide);
            }
        }, CAMERA_EXPANSION_DELAY_MILLISECONDS);

    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mQrCodeReaderView != null) {
            mQrCodeReaderView.startCamera();
        }
        startTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mQrCodeReaderView != null) {
            mQrCodeReaderView.stopCamera();
        }
        stopTimerTask();
    }

    @Override
    public void onBackPressed() {
        overridePendingTransition(R.anim.fade_down, 0);
        super.onBackPressed();
    }

    public void startTimer() {
        //set a new Timer
        if (mCameraGuideTimer != null) return;
        mCameraGuideTimer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();

        mCameraGuideTimer.schedule(mCameraGuideTask, 0, CAMERA_UI_UPDATE_MILLISECONDS);
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (mCameraGuideTimer != null) {
            mCameraGuideTimer.cancel();
            mCameraGuideTimer = null;
        }
    }

    private void initializeTimerTask() {
        mCameraGuideTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                mCameraGuideHandler.post(new Runnable() {
                    public void run() {
                        if (System.currentTimeMillis() - mLastUpdated > CAMERA_UI_UPDATE_MILLISECONDS) {
                            mCameraGuide.setImageResource(R.drawable.cameraguide);
                        }
                    }
                });
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != MY_PERMISSION_REQUEST_CAMERA) {
            return;
        }

        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initQRCodeReaderView();
        }
    }

    @Override
    public synchronized boolean onQRCodeRead(final String url, PointF[] points) {
        mLastUpdated = System.currentTimeMillis();
        if (!mIsQrProcessing) {
            if (AppEntryPointHandler.isSupportedQRCode(this, url)) {
                mIsQrProcessing = true;
                Log.d(TAG, "onQRCodeRead: crypto url: " + url);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraGuide.setImageResource(R.drawable.cameraguide);
                        Intent intent = new Intent(ScanQRActivity.this, HomeActivity.class);
                        intent.putExtra(HomeActivity.EXTRA_DATA, url);
                        intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }
                });
                return true;
            } else {
                Log.d(TAG, "onQRCodeRead: not a crypto url: " + url);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraGuide.setImageResource(R.drawable.cameraguide_red);
                        mLastUpdated = System.currentTimeMillis();
                    }
                });
                return false;
            }
        }
        return false;
    }

    private void initQRCodeReaderView() {
        mQrCodeReaderView = findViewById(R.id.qrdecoderview);
        mQrCodeReaderView.setAutofocusInterval(AUTO_FOCUS_PERIOD_TIME);
        mQrCodeReaderView.setOnQRCodeReadListener(this);
        mQrCodeReaderView.setBackCamera();
        mQrCodeReaderView.startCamera();
    }

}