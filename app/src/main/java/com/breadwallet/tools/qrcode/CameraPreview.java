package com.breadwallet.tools.qrcode;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

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
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = CameraPreview.class.getName();

    private CameraSingleton cameraSingleton;
    private Handler autoFocusHandler;
    private boolean previewing = true;
    private boolean isAutoFocusOn = true;
    private boolean isSurfaceCreated = false;
    private Camera.PreviewCallback previewCallback;

    public CameraPreview(Context context, CameraSingleton cameraSingleton, Camera.PreviewCallback previewCallback) {
        super(context);
        init(cameraSingleton, previewCallback);
    }

    public void init(CameraSingleton cameraSingleton, Camera.PreviewCallback previewCallback) {
        setCamera(cameraSingleton, previewCallback);
        autoFocusHandler = new Handler();
        getHolder().addCallback(this);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(CameraSingleton cameraSingleton, Camera.PreviewCallback previewCallback) {
        this.cameraSingleton = cameraSingleton;
        this.previewCallback = previewCallback;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        isSurfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        stopCameraPreview();
        showCameraPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        isSurfaceCreated = false;
        stopCameraPreview();
    }

    public void showCameraPreview() {
        if (cameraSingleton != null) {
            try {
                getHolder().addCallback(this);
                previewing = true;
                setupCameraParameters();
                cameraSingleton.camera.setPreviewDisplay(getHolder());
                cameraSingleton.camera.setDisplayOrientation(getDisplayOrientation());
                cameraSingleton.camera.setOneShotPreviewCallback(previewCallback);
                cameraSingleton.camera.startPreview();
                if (isAutoFocusOn) {
                    if (isSurfaceCreated) {
                        tryAutoFocus();
                    } else {
                        scheduleAutoFocus();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    public void tryAutoFocus() {
        try {
            cameraSingleton.camera.autoFocus(autoFocusCallback);
        } catch (RuntimeException re) {
            scheduleAutoFocus();
        }
    }

    public void stopCameraPreview() {
        try {
            previewing = false;
            getHolder().removeCallback(CameraPreview.this);
            cameraSingleton.camera.cancelAutoFocus();
            cameraSingleton.camera.setOneShotPreviewCallback(null);
            cameraSingleton.camera.stopPreview();
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }

    }

    public void setupCameraParameters() {
        Camera.Size optimalSize = getOptimalPreviewSize();
        Camera.Parameters parameters = cameraSingleton.camera.getParameters();
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        cameraSingleton.camera.setParameters(parameters);
        adjustViewSize(optimalSize);
    }

    private void adjustViewSize(Camera.Size cameraSize) {
        Point previewSize = convertSizeToLandscapeOrientation(new Point(getWidth(), getHeight()));
        float cameraRatio = ((float) cameraSize.width) / cameraSize.height;
        float screenRatio = ((float) previewSize.x) / previewSize.y;

        if (screenRatio > cameraRatio) {
            setViewSize((int) (previewSize.y * cameraRatio), previewSize.y);
        } else {
            setViewSize(previewSize.x, (int) (previewSize.x / cameraRatio));
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Point convertSizeToLandscapeOrientation(Point size) {
        if (getDisplayOrientation() % 180 == 0) {
            return size;
        } else {
            return new Point(size.y, size.x);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setViewSize(int width, int height) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        int tmpWidth;
        int tmpHeight;
        if (getDisplayOrientation() % 180 == 0) {
            tmpWidth = width;
            tmpHeight = height;
        } else {
            tmpWidth = height;
            tmpHeight = width;
        }
        int parentWidth = ((View) getParent()).getWidth();
        int parentHeight = ((View) getParent()).getHeight();
        float ratioWidth = (float) parentWidth / (float) tmpWidth;
        float ratioHeight = (float) parentHeight / (float) tmpHeight;

        float compensation;

        if (ratioWidth > ratioHeight) {
            compensation = ratioWidth;
        } else {
            compensation = ratioHeight;
        }

        tmpWidth = Math.round(tmpWidth * compensation);
        tmpHeight = Math.round(tmpHeight * compensation);//

        layoutParams.width = tmpWidth;
        layoutParams.height = tmpHeight;
        setLayoutParams(layoutParams);
    }

    public int getDisplayOrientation() {
        if (cameraSingleton == null) {
            //If we don't have a camera set there is no orientation so return dummy value
            return 0;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        if (cameraSingleton.cameraId == -1) {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        } else {
            Camera.getCameraInfo(cameraSingleton.cameraId, info);
        }

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private Camera.Size getOptimalPreviewSize() {
        if (cameraSingleton == null) {
            return null;
        }

        List<Camera.Size> sizes = cameraSingleton.camera.getParameters().getSupportedPreviewSizes();
        int w = getWidth();
        int h = getHeight();
        int portraitWidth = h;
        h = w;
        w = portraitWidth;

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void setAutoFocus(boolean state) {
        if (cameraSingleton != null && previewing) {
            if (state == isAutoFocusOn) {
                return;
            }
            isAutoFocusOn = state;
            if (isAutoFocusOn) {
                if (isSurfaceCreated) {
                    tryAutoFocus();
                } else {
                    scheduleAutoFocus();
                }
            } else {
                cameraSingleton.camera.cancelAutoFocus();
            }
        }
    }

    private Runnable autoFocusTask = new Runnable() {
        public void run() {
            if (cameraSingleton != null && previewing && isAutoFocusOn && isSurfaceCreated) {
                tryAutoFocus();
            }
        }
    };

    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            scheduleAutoFocus();
        }
    };

    private void scheduleAutoFocus() {
        autoFocusHandler.postDelayed(autoFocusTask, 1000);
    }
}
