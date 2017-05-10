package com.breadwallet.tools.qrcode;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.RequestHandler;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.BRWalletManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/14/16.
 * Copyright (c) 2016 breadwallet LLC
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

@SuppressWarnings("deprecation")
public class QRScannerView extends FrameLayout implements Camera.PreviewCallback {
    private static final String TAG = QRScannerView.class.getName();

    public interface ResultHandler {
        void handleResult(Result rawResult);
    }

    public static final int CAMERA_GUIDE_MODE_GRAY = 0;
    public static final int CAMERA_GUIDE_MODE_RED = 2;

    private MultiFormatReader multiFormatReader;
    private Rect framingRectInPreview;
    private ResultHandler resultHandler;

    private ImageView cameraGuideImage;
    private TextView decoderText;
    private ImageButton flashButton;
    private MainActivity app;

    private CameraSingleton cameraSingleton;
    private CameraPreview cameraPreview;
    private CameraHandlerThread cameraHandlerThread;
    private Boolean isFlashOn;
    private boolean isAutoFocusOn = true;

    public void initViews() {
        app = MainActivity.app;
        int buttonSize = 150;
        int cameraGuideImageSize = MainActivity.screenParametersPoint.x * 3 / 5;
        int decoderTextSize = MainActivity.screenParametersPoint.x / 2;
        cameraGuideImage = new ImageView(getContext());
        cameraGuideImage.setVisibility(GONE);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (cameraGuideImage != null) {
                    cameraGuideImage.setVisibility(VISIBLE);
                    SpringAnimator.showExpandCameraGuide(cameraGuideImage);
                }
            }
        });
        ((BreadWalletApp) app.getApplication()).hideKeyboard(app);
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(cameraGuideImageSize, cameraGuideImageSize);
        imageParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        decoderText = new TextView(getContext());
        decoderText.setTextColor(getContext().getColor(R.color.white));
        decoderText.setShadowLayer(2f, -2, 2, Color.BLACK);
        decoderText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        decoderText.setGravity(View.TEXT_ALIGNMENT_CENTER);
        decoderText.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(decoderTextSize, decoderTextSize / 2);
        textParams.topMargin = MainActivity.screenParametersPoint.y / 2 - cameraGuideImageSize / 2 - cameraGuideImageSize / 3;
        textParams.gravity = Gravity.CENTER_HORIZONTAL;
        setCameraGuideMode(CAMERA_GUIDE_MODE_GRAY);
        if (isFlashAvailable()) {
            flashButton = new ImageButton(getContext());
            flashButton.setImageResource(R.drawable.flash_off);
            flashButton.setBackgroundColor(getContext().getColor(android.R.color.transparent));
            FrameLayout.LayoutParams flashParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);
            flashParams.gravity = Gravity.END | Gravity.BOTTOM;
            flashButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleFlash(flashButton);
                }
            });
            addView(flashButton, flashParams);

        }
        addView(cameraGuideImage, imageParams);
        addView(decoderText, textParams);
    }

    public QRScannerView(Context context) {
        super(context);
        initReader();

    }

    public QRScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initReader();
    }

    public void setResultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
    }

    private void initReader() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        List<BarcodeFormat> ALL_FORMATS = new ArrayList<>();
        ALL_FORMATS.add(BarcodeFormat.QR_CODE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, ALL_FORMATS);
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (resultHandler == null) {
            return;
        }

        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            int width = size.width;
            int height = size.height;

            byte[] rotatedData = new byte[data.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++)
                    rotatedData[x * height + height - y - 1] = data[x + y * width];
            }
            int tmp = width;
            width = height;
            height = tmp;
            data = rotatedData;

            Result rawResult = null;
            PlanarYUVLuminanceSource source = buildLuminanceSource(data, width, height);

            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException | NullPointerException | ArrayIndexOutOfBoundsException re) {
                } finally {
                    multiFormatReader.reset();
                }
            }

            final Result finalRawResult = rawResult;

            if (finalRawResult != null) {
                if (app == null) app = MainActivity.app;
                if (app == null) return;
                final String resultedText = finalRawResult.getText();
                boolean isPrivKey = BRWalletManager.getInstance(app).confirmSweep(app, resultedText);
                if (isPrivKey) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BRAnimator.hideDecoderFragment();
                        }
                    });
                    return;
                }
//                boolean isBitIdUri = RequestHandler.tryBitIdUri(app, resultedText, null);
//                if (isBitIdUri) {
//                    app.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            BRAnimator.hideDecoderFragment();
//                        }
//                    });
//                    return;
//                }

                final String decoderText = validateResult(resultedText);

                if (!Objects.equals(decoderText, BRConstants.TEXT_EMPTY)) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setErrorText(decoderText);
                            setCameraGuideMode(QRScannerView.CAMERA_GUIDE_MODE_RED);
                        }
                    });
                    camera.setOneShotPreviewCallback(this);
                    return;
                }

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ResultHandler tmpResultHandler = resultHandler;
                        resultHandler = null;

                        stopCameraPreview();
                        if (tmpResultHandler != null) {
                            tmpResultHandler.handleResult(finalRawResult);
                        }
                    }
                });
            } else {
                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setErrorText("");
                        setCameraGuideMode(CAMERA_GUIDE_MODE_GRAY);
                    }
                });
                camera.setOneShotPreviewCallback(this);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, e.toString(), e);
        }
    }


    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        if (framingRectInPreview == null) {
            framingRectInPreview = new Rect(0, 0, width, height);
        }
        PlanarYUVLuminanceSource source = null;

        try {
            source = new PlanarYUVLuminanceSource(data, width, height, framingRectInPreview.left, framingRectInPreview.top,
                    framingRectInPreview.width(), framingRectInPreview.height(), false);
        } catch (Exception e) {

        }

        return source;
    }

    public void setErrorText(String errorText) {
        if (decoderText != null)
            decoderText.setText(errorText);
    }

    public void setCameraGuideMode(int cameraGuideMode) {
        if (cameraGuideImage == null) return;
        switch (cameraGuideMode) {
            case CAMERA_GUIDE_MODE_GRAY:
                cameraGuideImage.setImageResource(R.drawable.cameraguide);
                break;
            case CAMERA_GUIDE_MODE_RED:
                cameraGuideImage.setImageResource(R.drawable.cameraguide_red);
                break;
        }
    }

    /**
     * Validate the qr string and return the text to be shown to the user if the addresses is invalid
     * or an empty text TEXT_EMPTY
     */
    private String validateResult(String str) {
        RequestObject obj = null;
        obj = RequestHandler.getRequestFromString(str);

        if (obj == null) {
            return getContext().getResources().getString(R.string.fragmentdecoder_not_a_bitcoin_qr_code);
        }
        if (obj.r != null) {
            return BRConstants.TEXT_EMPTY;
        }
        if (obj.address != null) {
            if (BRWalletManager.validateAddress(obj.address)) {
                return BRConstants.TEXT_EMPTY;
            } else {
                return getContext().getResources().getString(R.string.fragmentdecoder_not_valid_bitcoin_address);
            }
        }
        return getContext().getResources().getString(R.string.fragmentdecoder_not_a_bitcoin_qr_code);
    }

    public final void setupLayout(CameraSingleton cs) {
        removeAllViews();

        cameraPreview = new CameraPreview(getContext(), cs, this);
        addView(cameraPreview);

    }

    public void startCamera(int cameraId) {
        if (cameraHandlerThread == null) {
            cameraHandlerThread = new CameraHandlerThread(this);
        }
        cameraHandlerThread.startCamera(cameraId);
    }

    public void setupCameraPreview(CameraSingleton cs) {
        cameraSingleton = cs;
        if (cameraSingleton != null) {
            setupLayout(cameraSingleton);
            if (isFlashOn != null) {
                setFlash(isFlashOn);
            }
            setAutoFocus(isAutoFocusOn);
        }
    }

    public void startCamera() {
        startCamera(CameraSingleton.getDefaultCameraId());
    }

    public void stopCamera() {
        if (cameraSingleton != null) {
            cameraPreview.stopCameraPreview();
            cameraPreview.setCamera(null, null);
            cameraSingleton.camera.release();
            cameraSingleton = null;
        }
        if (cameraHandlerThread != null) {
            cameraHandlerThread.quit();
            cameraHandlerThread = null;
        }
    }

    public void stopCameraPreview() {
        if (cameraPreview != null) {
            cameraPreview.stopCameraPreview();
        }
    }

    public void setFlash(boolean flag) {
        isFlashOn = flag;
        if (cameraSingleton != null && CameraSingleton.isFlashSupported(cameraSingleton.camera)) {

            Camera.Parameters parameters = cameraSingleton.camera.getParameters();
            if (flag) {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            cameraSingleton.camera.setParameters(parameters);
        }
    }

    public boolean isFlashAvailable() {
        return cameraSingleton != null && CameraSingleton.isFlashSupported(cameraSingleton.camera);

    }

    public void toggleFlash(ImageButton button) {
        if (cameraSingleton != null && CameraSingleton.isFlashSupported(cameraSingleton.camera)) {
            Camera.Parameters parameters = cameraSingleton.camera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                button.setImageResource(R.drawable.flash_off);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                button.setImageResource(R.drawable.flash_on);
            }
            cameraSingleton.camera.setParameters(parameters);
        }
    }

    public void setAutoFocus(boolean state) {
        isAutoFocusOn = state;
        if (cameraPreview != null) {
            cameraPreview.setAutoFocus(state);
        }
    }
}
