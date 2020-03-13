/*
 * Copyright (C) 2010 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.breadwallet.tools.qrcode.xzing;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.breadwallet.tools.qrcode.xzing.open.CameraFacing;
import com.breadwallet.tools.qrcode.xzing.open.OpenCamera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
final class CameraConfigurationManager {

    // This is bigger than the size of a small screen, which is still supported. The routine
    // below will still select the default (presumably 320x240) size for these. This prevents
    // accidental selection of very low resolution on some devices.
    private static final int MIN_PREVIEW_PIXELS = 470 * 320; // normal screen
    private static final int MAX_PREVIEW_PIXELS = 1280 * 720;
    private static final float MAX_EXPOSURE_COMPENSATION = 1.5f;
    private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;
    private final Context context;

    private Point resolution;
    private Point cameraResolution;
    private Point bestPreviewSize;
    private Point previewSizeOnScreen;
    private int cwRotationFromDisplayToCamera;
    private int cwNeededRotation;

    CameraConfigurationManager(Context context) {
        this.context = context;
    }

    void initFromCameraParameters(OpenCamera camera, int width, int height) {
        Camera.Parameters parameters = camera.getCamera().getParameters();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();

        int displayRotation = display.getRotation();
        int cwRotationFromNaturalToDisplay;
        switch (displayRotation) {
            case Surface.ROTATION_0:
                cwRotationFromNaturalToDisplay = 0;
                break;
            case Surface.ROTATION_90:
                cwRotationFromNaturalToDisplay = 90;
                break;
            case Surface.ROTATION_180:
                cwRotationFromNaturalToDisplay = 180;
                break;
            case Surface.ROTATION_270:
                cwRotationFromNaturalToDisplay = 270;
                break;
            default:
                // Have seen this return incorrect values like -90
                if (displayRotation % 90 == 0) {
                    cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
                } else {
                    throw new IllegalArgumentException("Bad rotation: " + displayRotation);
                }
        }
        Timber.d("Display at: %s", cwRotationFromNaturalToDisplay);

        int cwRotationFromNaturalToCamera = camera.getOrientation();
        Timber.d("Camera at: %s", cwRotationFromNaturalToCamera);

        // Still not 100% sure about this. But acts like we need to flip this:
        if (camera.getFacing() == CameraFacing.FRONT) {
            cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360;
            Timber.d("Front camera overriden to: %s", cwRotationFromNaturalToCamera);
        }

        cwRotationFromDisplayToCamera =
                (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360;
        Timber.d("Final display orientation: %s", cwRotationFromDisplayToCamera);
        if (camera.getFacing() == CameraFacing.FRONT) {
            Timber.d("Compensating rotation for front camera");
            cwNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360;
        } else {
            cwNeededRotation = cwRotationFromDisplayToCamera;
        }
        Timber.d("Clockwise rotation from display to camera: %s", cwNeededRotation);

        resolution = new Point(width, height);
        Timber.d("Screen resolution in current orientation: %s", resolution);
        cameraResolution = findBestPreviewSizeValue(parameters, resolution);
        Timber.d("Camera resolution: %s", cameraResolution);
        bestPreviewSize = findBestPreviewSizeValue(parameters, resolution);
        Timber.d("Best available preview size: %s", bestPreviewSize);

        boolean isScreenPortrait = resolution.x < resolution.y;
        boolean isPreviewSizePortrait = bestPreviewSize.x < bestPreviewSize.y;

        if (isScreenPortrait == isPreviewSizePortrait) {
            previewSizeOnScreen = bestPreviewSize;
        } else {
            previewSizeOnScreen = new Point(bestPreviewSize.y, bestPreviewSize.x);
        }
        Timber.d("Preview size on screen: %s", previewSizeOnScreen);
    }

    void setDesiredCameraParameters(OpenCamera camera, boolean safeMode) {

        Camera theCamera = camera.getCamera();
        Camera.Parameters parameters = theCamera.getParameters();

        if (parameters == null) {
            Timber.d("Device error: no camera parameters are available. Proceeding without configuration.");
            return;
        }

        Timber.d("Initial camera parameters: %s", parameters.flatten());

        if (safeMode) {
            Timber.d("In camera config safe mode -- most settings will not be honored");
        }

        // Maybe selected auto-focus but not available, so fall through here:
        String focusMode = null;
        if (!safeMode) {
            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
            focusMode =
                    findSettableValue("focus mode", supportedFocusModes, Camera.Parameters.FOCUS_MODE_AUTO);
        }
        if (focusMode != null) {
            parameters.setFocusMode(focusMode);
        }

        parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);

        theCamera.setParameters(parameters);

        theCamera.setDisplayOrientation(cwRotationFromDisplayToCamera);

        Camera.Parameters afterParameters = theCamera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (bestPreviewSize.x != afterSize.width
                || bestPreviewSize.y != afterSize.height)) {
            Timber.d(
                    "Camera said it supported preview size "
                            + bestPreviewSize.x
                            + 'x'
                            + bestPreviewSize.y
                            + ", but after setting it, preview size is "
                            + afterSize.width
                            + 'x'
                            + afterSize.height);
            bestPreviewSize.x = afterSize.width;
            bestPreviewSize.y = afterSize.height;
        }
    }

    Point getCameraResolution() {
        return cameraResolution;
    }

    Point getScreenResolution() {
        return resolution;
    }

    // All references to Torch are removed from here, methods, variables...

    public Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {

        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Timber.d("Device returned no supported preview sizes; using default");
            Camera.Size defaultSize = parameters.getPreviewSize();
            return new Point(defaultSize.width, defaultSize.height);
        }

        // Sort by size, descending
        List<Camera.Size> supportedPreviewSizes = new ArrayList<Camera.Size>(rawSupportedSizes);
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        StringBuilder previewSizesString = new StringBuilder();
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            previewSizesString.append(supportedPreviewSize.width)
                    .append('x')
                    .append(supportedPreviewSize.height)
                    .append(' ');
        }
        Timber.d("Supported preview sizes: %s", previewSizesString);

        Point bestSize = null;
        float screenAspectRatio = (float) screenResolution.x / (float) screenResolution.y;

        float diff = Float.POSITIVE_INFINITY;
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            int pixels = realWidth * realHeight;
            if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
                continue;
            }

            // This code is modified since We're using portrait mode
            boolean isCandidateLandscape = realWidth > realHeight;
            int maybeFlippedWidth = isCandidateLandscape ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidateLandscape ? realWidth : realHeight;

            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Timber.d("Found preview size exactly matching screen size: %s", exactPoint);
                return exactPoint;
            }
            float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
            float newDiff = Math.abs(aspectRatio - screenAspectRatio);
            if (newDiff < diff) {
                bestSize = new Point(realWidth, realHeight);
                diff = newDiff;
            }
        }

        if (bestSize == null) {
            Camera.Size defaultSize = parameters.getPreviewSize();
            bestSize = new Point(defaultSize.width, defaultSize.height);
            Timber.d("No suitable preview sizes, using default: %s", bestSize);
        }

        Timber.d("Found best approximate preview size: %s", bestSize);
        return bestSize;
    }

    private static String findSettableValue(String name, Collection<String> supportedValues,
                                            String... desiredValues) {
        Timber.d("Requesting " + name + " value from among: " + Arrays.toString(desiredValues));
        Timber.d("Supported " + name + " values: " + supportedValues);
        if (supportedValues != null) {
            for (String desiredValue : desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    Timber.d("Can set " + name + " to: " + desiredValue);
                    return desiredValue;
                }
            }
        }
        Timber.d("No supported values match");
        return null;
    }

    boolean getTorchState(Camera camera) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters != null) {
                String flashMode = camera.getParameters().getFlashMode();
                return flashMode != null && (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)
                        || Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode));
            }
        }
        return false;
    }

    void setTorchEnabled(Camera camera, boolean enabled) {
        Camera.Parameters parameters = camera.getParameters();
        setTorchEnabled(parameters, enabled, false);
        camera.setParameters(parameters);
    }

    void setTorchEnabled(Camera.Parameters parameters, boolean enabled, boolean safeMode) {
        setTorchEnabled(parameters, enabled);

        if (!safeMode) {
            setBestExposure(parameters, enabled);
        }
    }

    public static void setTorchEnabled(Camera.Parameters parameters, boolean enabled) {
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        String flashMode;
        if (enabled) {
            flashMode =
                    findSettableValue("flash mode", supportedFlashModes, Camera.Parameters.FLASH_MODE_TORCH,
                            Camera.Parameters.FLASH_MODE_ON);
        } else {
            flashMode =
                    findSettableValue("flash mode", supportedFlashModes, Camera.Parameters.FLASH_MODE_OFF);
        }
        if (flashMode != null) {
            if (flashMode.equals(parameters.getFlashMode())) {
                Timber.d("Flash mode already set to %s", flashMode);
            } else {
                Timber.d("Setting flash mode to %s", flashMode);
                parameters.setFlashMode(flashMode);
            }
        }
    }

    public static void setBestExposure(Camera.Parameters parameters, boolean lightOn) {

        int minExposure = parameters.getMinExposureCompensation();
        int maxExposure = parameters.getMaxExposureCompensation();
        float step = parameters.getExposureCompensationStep();
        if ((minExposure != 0 || maxExposure != 0) && step > 0.0f) {
            // Set low when light is on
            float targetCompensation = lightOn ? MIN_EXPOSURE_COMPENSATION : MAX_EXPOSURE_COMPENSATION;
            int compensationSteps = Math.round(targetCompensation / step);
            float actualCompensation = step * compensationSteps;
            // Clamp value:
            compensationSteps = Math.max(Math.min(compensationSteps, maxExposure), minExposure);
            if (parameters.getExposureCompensation() == compensationSteps) {
                Timber.d("Exposure compensation already set to " + compensationSteps + " / "
                        + actualCompensation);
            } else {
                Timber.d("Setting exposure compensation to " + compensationSteps + " / " + actualCompensation);
                parameters.setExposureCompensation(compensationSteps);
            }
        } else {
            Timber.d("Camera does not support exposure compensation");
        }
    }
}
