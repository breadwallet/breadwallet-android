package com.breadwallet.tools.qrcode;


import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class CameraHandlerThread extends HandlerThread {
    private static final String TAG = CameraHandlerThread.class.getName();

    private QRScannerView qrScannerView;

    public CameraHandlerThread(QRScannerView scannerView) {
        super(TAG);
        qrScannerView = scannerView;
        start();
    }

    public void startCamera(final int cameraId) {
        Handler localHandler = new Handler(getLooper());
        localHandler.post(new Runnable() {
            @Override
            public void run() {
                final Camera camera = CameraSingleton.getCameraInstance(cameraId);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        qrScannerView.setupCameraPreview(CameraSingleton.getSingleton(camera, cameraId));
                    }
                });
            }
        });
    }
}
