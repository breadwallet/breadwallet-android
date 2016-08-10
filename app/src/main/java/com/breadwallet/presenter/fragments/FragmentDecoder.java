package com.breadwallet.presenter.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.AutoFitTextureView;
import com.breadwallet.tools.qrcode.PlanarYUVLuminanceSource;
import com.breadwallet.tools.security.RequestHandler;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.BRWalletManager;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
import static android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE;

public class FragmentDecoder extends Fragment
        implements FragmentCompat.OnRequestPermissionsResultCallback {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static boolean accessGranted = true;
    private static boolean showingText = false;
    private ImageView camera_guide_image;
    private TextView decoderText;
    private ImageButton flashButton;
    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;
    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = FragmentDecoder.class.getName();

    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                private void process(CaptureResult result) {
                }

                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                                @NonNull CaptureResult partialResult) {
                    process(partialResult);
                }

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    process(result);
                }

            };
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {

                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                    configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {

                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture texture) {

                }

            };
    private QRCodeReader mQrReader;
    private String mCameraId;
    private AutoFitTextureView mTextureView;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image img = null;
                    Result rawResult = null;
                    try {
                        img = reader.acquireLatestImage();
                        if (img == null) throw new NullPointerException("cannot be null");
                        ByteBuffer buffer = img.getPlanes()[0].getBuffer();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);

                        int width = img.getWidth();
                        int height = img.getHeight();
                        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                        rawResult = mQrReader.decode(bitmap);

                        if (rawResult == null) {
                            throw new NullPointerException("no QR code");
                        }
                        String decoded = rawResult.getText();

                        if (accessGranted) {
                            Log.e(TAG, "entered the accessGranted area!");
                            accessGranted = false;
                            boolean isPrivKey = BRWalletManager.getInstance(getActivity()).confirmSweep(getActivity(), decoded);
                            if (isPrivKey) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRAnimator.hideDecoderFragment();
                                    }
                                });
                                return;
                            }

                            String validationString = validateResult(decoded);
//                        Log.e(TAG, "validationString: " + validationString);
                            if (Objects.equals(validationString, BRConstants.TEXT_EMPTY)) {
                                onQRCodeRead((MainActivity) getActivity(), rawResult.getText());
                            } else {
                                if (!showingText) {
                                    showingText = true;
                                    setCameraGuide(BRConstants.CAMERA_GUIDE_RED);
                                    setGuideText(validationString);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            showingText = false;
                                        }
                                    }, 1000);
                                }
                                accessGranted = true;
                            }
                        }
                    } catch (ReaderException | NullPointerException | IllegalStateException ignored) {
                        if (!showingText)
                            setCameraGuide(BRConstants.CAMERA_GUIDE);
//                        Log.e(TAG, "Reader shows an exception! ", ignored);
                        /* Ignored */
                    } finally {
                        mQrReader.reset();
                        if (img != null)
                            img.close();

                    }
                    if (rawResult == null) {
                        if (!showingText)
                            setCameraGuide(BRConstants.CAMERA_GUIDE);
                    }
                }

            };
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            getActivity().onBackPressed();
        }

    };

    private Size mPreviewSize;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private int flashButtonCount;

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_decoder, container, false);
        mQrReader = new QRCodeReader();

        mTextureView = new AutoFitTextureView(getActivity());
        RelativeLayout layout = (RelativeLayout) rootView.findViewById(R.id.fragment_decoder_layout);
        camera_guide_image = (ImageView) rootView.findViewById(R.id.decoder_camera_guide_image);
        decoderText = (TextView) rootView.findViewById(R.id.decoder_text);
        flashButton = (ImageButton) rootView.findViewById(R.id.button_flash);
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    if (mPreviewRequestBuilder == null || mCaptureSession == null)
                        return;

                    if (++flashButtonCount % 2 != 0) {
                        mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                        mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
                        flashButton.setBackgroundResource(R.drawable.flash_on);
                        SpringAnimator.showAnimation(flashButton);
                    } else {
                        mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                        mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
                        flashButton.setBackgroundResource(R.drawable.flash_off);
                        SpringAnimator.showAnimation(flashButton);
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        layout.addView(mTextureView, 0);
        startBackgroundThread();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        accessGranted = true;
        showingText = false;
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (camera_guide_image != null)

                    SpringAnimator.showExpandCameraGuide(camera_guide_image);
            }
        });
        ((BreadWalletApp) getActivity().getApplication()).hideKeyboard(getActivity());

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth()/2, largest.getHeight()/2,
                        ImageFormat.YUV_420_888, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                mTextureView.setAspectRatio(MainActivity.screenParametersPoint.x,
                        MainActivity.screenParametersPoint.y - getStatusBarHeight()); //portrait onl

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        flashButton.setVisibility(mFlashSupported? View.VISIBLE : View.GONE);
                    }
                });

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the camera specified by {@link FragmentDecoder#mCameraId}.
     */
    private void openCamera(int width, int height) {
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (mCameraId != null)
                manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        try {
            mBackgroundThread.quitSafely();
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException | NullPointerException e) {
            e.printStackTrace();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//            Log.e(TAG, "mPreviewSize.getWidth(): " + mPreviewSize.getWidth() + ", mPreviewSize.getHeight(): "
//                    + mPreviewSize.getHeight());

            Surface surface = new Surface(texture);
            Surface mImageSurface = mImageReader.getSurface();
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mImageSurface);
            mPreviewRequestBuilder.addTarget(surface);
//            mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 800);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(mImageSurface, surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
//                            Log.e(TAG, "onConfigured");
                            if (mCameraDevice == null) return;

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                /**no need for flash_on now*/
                                //mPreviewRequestBuilder.set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                                        mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.failed),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */

    private void configureTransform(int viewWidth, int viewHeight) {

        if (mTextureView == null || mPreviewSize == null) return;

        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private static synchronized void onQRCodeRead(final MainActivity app, final String text) {
        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (text != null) {
                    BRAnimator.hideDecoderFragment();
                    Log.e(TAG, "BEFORE processRequest");
                    RequestHandler.processRequest(app, text);

                }
            }
        });

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    private int getStatusBarHeight() {
        Rect rectangle = new Rect();
        Window window = getActivity().getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        int contentViewTop =
                window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight = contentViewTop - statusBarHeight;

//        Log.e(TAG, "StatusBar Height= " + statusBarHeight + " , TitleBar Height = " + titleBarHeight);
        return statusBarHeight + titleBarHeight;
    }


    /**
     * Validate the qr string and return the text to be shown to the user if the addresses is invalid
     * or an empty text TEXT_EMPTY
     */
    private String validateResult(String str) {
        RequestObject obj = null;
        try {
            obj = RequestHandler.getRequestFromString(str);
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        if (obj == null) {
            return getActivity().getResources().getString(R.string.fragmentdecoder_not_a_bitcoin_qr_code);
        }
        if (obj.r != null) {
            return BRConstants.TEXT_EMPTY;
        }
        if (obj.address != null) {
            if (BRWalletManager.validateAddress(obj.address)) {
                return BRConstants.TEXT_EMPTY;
            } else {
                return getActivity().getResources().getString(R.string.fragmentdecoder_not_valid_bitcoin_address);
            }
        }
        return getActivity().getResources().getString(R.string.fragmentdecoder_not_a_bitcoin_qr_code);
    }

    private void setCameraGuide(final String str) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (str.equalsIgnoreCase(BRConstants.CAMERA_GUIDE)) {
                    camera_guide_image.setImageResource(R.drawable.cameraguide);
                    setGuideText(BRConstants.TEXT_EMPTY);
//                    Log.e(TAG, "camera_guide set: cameraguide");
                } else if (str.equalsIgnoreCase(BRConstants.CAMERA_GUIDE_RED)) {
                    camera_guide_image.setImageResource(R.drawable.cameraguide_red);
//                    Log.e(TAG, "camera_guide set: cameraguide_red ");
                } else {
                    throw new IllegalArgumentException("CAMERA_GUIDE and CAMERA_GUIDE_RED only");
                }
            }
        });
    }

    private void setGuideText(final String str) {
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    decoderText.setText(str);
                }
            });

    }

}
