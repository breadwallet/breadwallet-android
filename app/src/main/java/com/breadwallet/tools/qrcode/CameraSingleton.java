package com.breadwallet.tools.qrcode;

import android.hardware.Camera;

import java.util.List;

@SuppressWarnings("deprecation")
public class CameraSingleton {
    public final Camera camera;
    public final int cameraId;

    private CameraSingleton(Camera camera, int cameraId) {
        if (camera == null) {
            throw new NullPointerException("Camera cannot be null");
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
