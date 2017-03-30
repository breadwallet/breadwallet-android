package com.breadwallet.tools.qrcode;

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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public class QRReader {
    private final String LOG = getClass().getSimpleName();
    private CameraSource cameraSource = null;
    private BarcodeDetector barcodeDetector = null;

    private static BarcodeDetector detector;

    public static final int FRONT_CAM = CameraSource.CAMERA_FACING_FRONT;
    public static final int BACK_CAM = CameraSource.CAMERA_FACING_BACK;

    private final int width;
    private final int height;
    private final int facing;
    private final OnQrFoundListener qrDataListener;
    private final Context context;
    private final SurfaceView surfaceView;
    private boolean autoFocusEnabled;

    private boolean cameraRunning = false;

    private boolean surfaceCreated = false;

    public void initAndStart(final SurfaceView surfaceView) {

        surfaceView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        init();
                        start();
                        surfaceView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
    }

    private final SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            surfaceCreated = true;
            startCameraView(context, cameraSource, surfaceView);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            surfaceCreated = false;
            stop();
            surfaceHolder.removeCallback(this);
        }
    };

    private QRReader(final Builder builder) {
        this.autoFocusEnabled = builder.autofocusEnabled;
        this.width = builder.width;
        this.height = builder.height;
        this.facing = builder.facing;
        this.qrDataListener = builder.qrDataListener;
        this.context = builder.context;
        this.surfaceView = builder.surfaceView;
        if (builder.barcodeDetector == null) {
            this.barcodeDetector = getBarcodeDetector(context);
        }
        else {
            this.barcodeDetector = builder.barcodeDetector;
        }
    }

    public boolean isCameraRunning() {
        return cameraRunning;
    }

    private void init() {
        if (!hasAutofocus(context)) {
            Log.e(LOG, "NO autofocus");
            autoFocusEnabled = false;
        }

        if (!hasCameraHardware(context)) {
            Log.e(LOG, "No camera hardware");
            return;
        }
        if (!checkCameraPermission(context)) {
            Log.e(LOG, "NO camera permission");
            return;
        }

        if (barcodeDetector.isOperational()) {
            barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
                @Override
                public void release() {
                }

                @Override
                public void receiveDetections(Detector.Detections<Barcode> detections) {
                    final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                    if (barcodes.size() != 0 && qrDataListener != null) {
                        qrDataListener.onDetected(barcodes.valueAt(0).displayValue);
                    }
                }
            });

            cameraSource = new CameraSource.Builder(context, barcodeDetector).setAutoFocusEnabled(autoFocusEnabled)
                            .setFacing(facing)
                            .setRequestedPreviewSize(width, height)
                            .build();
        } else {
            Log.e(LOG, "Barcode detector is not operational");
        }
    }

    public void start() {
        if (surfaceView != null && surfaceHolderCallback != null) {
            if (surfaceCreated) {
                startCameraView(context, cameraSource, surfaceView);
            } else {
                surfaceView.getHolder().addCallback(surfaceHolderCallback);
            }
        }
    }

    private void startCameraView(Context context, CameraSource cameraSource,
                                 SurfaceView surfaceView) {
        if (cameraRunning) {
            throw new IllegalStateException("Camera already started!");
        }
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(LOG, "Permission not granted!");
            }
            else if (!cameraRunning && cameraSource != null && surfaceView != null) {
                cameraSource.start(surfaceView.getHolder());
                cameraRunning = true;
            }
        } catch (IOException ie) {
            Log.e(LOG, ie.getMessage());
            ie.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (cameraRunning && cameraSource != null) {
                cameraSource.stop();
                cameraRunning = false;
            }
        } catch (Exception ie) {
            Log.e(LOG, ie.getMessage());
            ie.printStackTrace();
        }
    }

    public void release() {
        stop();
        if (cameraSource != null) {
            cameraSource.release();
            cameraSource = null;
        }
    }

    private boolean checkCameraPermission(Context context) {
        String permission = Manifest.permission.CAMERA;
        int res = context.checkCallingOrSelfPermission(permission);
        return res == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private boolean hasAutofocus(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);
    }

    public static class Builder {
        private boolean autofocusEnabled;
        private int width;
        private int height;
        private int facing;
        private final OnQrFoundListener qrDataListener;
        private final Context context;
        private final SurfaceView surfaceView;
        private BarcodeDetector barcodeDetector;

        public Builder(Context context, SurfaceView surfaceView, OnQrFoundListener qrDataListener) {
            this.autofocusEnabled = true;
            this.width = 800;
            this.height = 800;
            this.facing = BACK_CAM;
            this.qrDataListener = qrDataListener;
            this.context = context;
            this.surfaceView = surfaceView;
        }

        public Builder enableAutofocus(boolean autofocusEnabled) {
            this.autofocusEnabled = autofocusEnabled;
            return this;
        }

        public Builder width(int width) {
            if (width != 0) {
                this.width = width;
            }
            return this;
        }

        public Builder height(int height) {
            if (height != 0) {
                this.height = height;
            }
            return this;
        }

        public Builder facing(int facing) {
            this.facing = facing;
            return this;
        }
        public QRReader build() {
            return new QRReader(this);
        }

        public void barcodeDetector(BarcodeDetector barcodeDetector) {
            this.barcodeDetector = barcodeDetector;
        }
    }

    static BarcodeDetector getBarcodeDetector(Context context) {
        if (detector == null) {
            detector = new BarcodeDetector.Builder(context.getApplicationContext()).setBarcodeFormats(
                    Barcode.QR_CODE).build();
        }
        return detector;
    }

    public interface OnQrFoundListener {

        void onDetected(final String data);
    }
}