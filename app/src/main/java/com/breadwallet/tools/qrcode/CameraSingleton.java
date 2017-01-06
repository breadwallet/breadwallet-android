package com.breadwallet.tools.qrcode;

import android.hardware.Camera;

import com.google.firebase.crash.FirebaseCrash;

import java.util.List;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/14/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

@SuppressWarnings("deprecation")
public class CameraSingleton {
    public final Camera camera;
    public final int cameraId;

    private CameraSingleton(Camera camera, int cameraId) {
        if (camera == null) {
            RuntimeException ex = new NullPointerException("Camera cannot be null");
            FirebaseCrash.report(ex);
            throw ex;
        }
        this.camera = camera;
        this.cameraId = cameraId;
    }

    public static CameraSingleton getSingleton(Camera camera, int cameraId) {
        if (camera == null) {
            return null;
        } else {
            return new CameraSingleton(camera, cameraId);
        }
    }

    public static int getDefaultCameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int defaultCameraId = -1;
        for (int i = 0; i < numberOfCameras; i++) {
            defaultCameraId = i;
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return defaultCameraId;
    }

    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            if(cameraId == -1) {
                c = Camera.open();
            } else {
                c = Camera.open(cameraId);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    public static boolean isFlashSupported(Camera camera) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();

            if (parameters.getFlashMode() == null) {
                return false;
            }

            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if (supportedFlashModes == null || supportedFlashModes.isEmpty() || supportedFlashModes.size() == 1
                    && supportedFlashModes.get(0).equals(Camera.Parameters.FLASH_MODE_OFF)) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }
}
