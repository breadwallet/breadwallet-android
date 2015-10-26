package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v13.app.FragmentCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.AutoFitTextureView;
import com.breadwallet.tools.qrcode.PlanarYUVLuminanceSource;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.hardware.camera2.CameraCharacteristics.LENS_FACING;
import static android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP;
import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;
import static android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_CONVERGED;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED;
import static android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_PRECAPTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_TRIGGER_CANCEL;
import static android.hardware.camera2.CameraMetadata.CONTROL_AF_TRIGGER_START;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE;
import static android.hardware.camera2.CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER;
import static android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE;
import static android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER;
import static android.hardware.camera2.CaptureResult.CONTROL_AE_STATE;
import static android.hardware.camera2.CaptureResult.CONTROL_AF_STATE;

public class FragmentDecoder extends Fragment
        implements FragmentCompat.OnRequestPermissionsResultCallback {
    private static final int sImageFormat = ImageFormat.YUV_420_888;

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    public static boolean accessGranted = true;
    private static Object mockObjectForLocking = new Object();
    private ImageView camera_guide_image;
//    private Timer timer;
//    private TimerTask timerTask;

    //we are going to use a handler to be able to run in our TimerTask
//    final Handler handler = new Handler();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

//    public void startTimer() {
//        //set a new Timer
//        timer = new Timer();
//
//        //initialize the TimerTask's job
//        initializeTimerTask();
//
//        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
//        timer.schedule(timerTask, 5000, 400); //
//    }
//
//    public void stoptimertask() {
//        //stop the timer, if it's not already null
//        if (timer != null) {
//            timer.cancel();
//        }
//        timer = null;
//        handler = null;
//    }

//    public void initializeTimerTask() {
//
//        timerTask = new TimerTask() {
//            public void run() {
//
//                //use a handler to run a toast that shows the current timestamp
//                handler.post(new Runnable() {
//                    public void run() {
//                        //get the current timeStamp
//                        takePicture();
//                        int duration = Toast.LENGTH_SHORT;
//                        Toast toast = Toast.makeText(getActivity(), "timer executed", duration);
//                        toast.show();
//                    }
//                });
//            }
//        };
//    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = FragmentDecoder.class.getName();

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;
    private int mState = STATE_PREVIEW;
    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;
    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;
    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

                private void process(CaptureResult result) {
                    switch (mState) {
                        case STATE_PREVIEW: {
                            captureStillPicture();
                            //checkForTheResult();
                            // We have nothing to do when the camera preview is working normally.
                            break;
                        }
                        case STATE_WAITING_LOCK: {
                            int afState = result.get(CONTROL_AF_STATE);
                            if (afState == CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                    afState == CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                // CONTROL_AE_STATE can be null on some devices
                                Integer aeState = result.get(CONTROL_AE_STATE);
                                if (aeState == null || aeState == CONTROL_AE_STATE_CONVERGED) {
                                    mState = STATE_WAITING_NON_PRECAPTURE;
                                    captureStillPicture();
                                } else {
                                    runPrecaptureSequence();
                                }
                            }
                            break;
                        }
                        case STATE_WAITING_PRECAPTURE: {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CONTROL_AE_STATE);
                            if (aeState == null || aeState == CONTROL_AE_STATE_PRECAPTURE ||
                                    aeState == CONTROL_AE_STATE_FLASH_REQUIRED) {
                                mState = STATE_WAITING_NON_PRECAPTURE;
                            }
                            break;
                        }
                        case STATE_WAITING_NON_PRECAPTURE: {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CONTROL_AE_STATE);
                            if (aeState == null || aeState != CONTROL_AE_STATE_PRECAPTURE) {
                                mState = STATE_PICTURE_TAKEN;
                                captureStillPicture();
                            }
                            break;
                        }
                    }
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                                CaptureResult partialResult) {
                    process(partialResult);
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
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
    private ImageView mCapturedView;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {

                @Override
                public void onImageAvailable(ImageReader reader) {
                    Log.e(TAG, "Posted a new message with the Runnable on the queue!!!!!!!!!!");
//                  mBackgroundHandler.post(new ImageProcessor(mQrReader, reader));
//                  mTextureView.onPreviewFrame(reader.acquireNextImage().getPlanes([0].getBuffer().array());
                    Log.e(TAG, "in the run!");
                    Image mImage = null;
                    if (reader != null) {
                        mImage = reader.acquireLatestImage();
                    }
                    if (mImage == null) return;
                    ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    int width = mImage.getWidth();
                    int height = mImage.getHeight();
                    Result rawResult = null;
                    Log.e("C2", data.length + " (" + width + "x" + height + ")");
                    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                    try {
                        rawResult = mQrReader.decode(bitmap);
                        onQRCodeRead(rawResult.getText());
                        mCameraDevice = null;
                    } catch (ReaderException ignored) {
                        Log.e(TAG, "Reader shows an exception! ", ignored);
                        /* Ignored */
                    } finally {
                        mQrReader.reset();
                        mImage.close();
                    }
                    if (rawResult != null) {
                        Log.e("C2", "Decoding successful!");
                    } else {
                        Log.d("C2", "No QR code found…");
                    }
                    mImage.close();
                }

            };
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
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
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private int pictureTaken = 0;

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Bitmap renderBitmap(LuminanceSource source) {
        int width = source.getWidth();
        int height = source.getHeight();
        byte[] data = source.getMatrix();

        /*
        if (cameraManager.isFrontFaced()) {
            if (cameraManager.isPortraitPreview()) {
                data = mirrorMatrixVertically(data, width, height);
            } else {
                data = mirrorMatrixHorizontally(data, width, height);
            }
        }
        */

        final int[] pixels = new int[width * height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = 0xFF000000 | ((data[x * height + y] & 0xff) * 0x00010101);
                pixels[x * height + y] = pixel;
            }
        }

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_decoder, container, false);
        mQrReader = new QRCodeReader();
        mTextureView = (AutoFitTextureView) rootView.findViewById(R.id.texture);
        camera_guide_image = (ImageView) rootView.findViewById(R.id.decoder_camera_guide_image);
        SpringAnimator.showExpandCameraGuide(camera_guide_image);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
//        startTimer();

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
//        stoptimertask();
        super.onPause();
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getActivity().getSystemService(getActivity().CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                if (characteristics.get(LENS_FACING) == LENS_FACING_FRONT) continue;

                StreamConfigurationMap map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP);

                // For still image captures, we use the largest available size.
                List<Size> outputSizes = Arrays.asList(map.getOutputSizes(sImageFormat));
                Size largest = Collections.max(outputSizes, new CompareSizesByArea());

                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), sImageFormat, 2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                // Danger, W.R.! Attempting to use too large a preview size could exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);
                Log.e(TAG, "WIDTH: " + mPreviewSize.getWidth() + " HEIGHT: " + mPreviewSize.getHeight());
                // We fit the aspect ratio of TextureView to the size of preview we picked.

//                int orientation = getResources().getConfiguration().orientation;
//                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//                } else {
                mTextureView.setAspectRatio(MainActivity.screenParametersPoint.x,
                        MainActivity.screenParametersPoint.y - getStatusBarHeight());
//                }

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
        CameraManager manager = (CameraManager) getActivity().getSystemService(getActivity().CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
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
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
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

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (mCameraDevice == null) return;

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
//                                mPreviewRequestBuilder.set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON_AUTO_FLASH); // no need for flash now

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                                        mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Toast.makeText(getActivity(), "Failed", Toast.LENGTH_SHORT).show();
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

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        if (mCaptureSession != null) {
            synchronized (this) {
                Log.e(TAG, "in the takePicture() ");
                lockFocus();
            }
        }
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CONTROL_AF_TRIGGER, CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when we
     * get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CONTROL_AE_PRECAPTURE_TRIGGER, CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            if (mCameraDevice == null) return;

            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            captureBuilder.set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON_AUTO_FLASH);

            // FIXME: Orientation
//            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();  //let's use only the portrait one
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0 /*ORIENTATIONS.get(rotation)*/);

            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    // FIXME: Capture request was successful
                    Toast.makeText(getActivity(), "Picture taken" + ++pictureTaken, Toast.LENGTH_LONG).show();
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is finished.
     */
    private void unlockFocus() {

        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CONTROL_AF_TRIGGER, CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON_AUTO_FLASH);

            if (mCaptureSession != null) {
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
                // After this, the camera will go back to the normal state of preview.
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
            }
            mState = STATE_PREVIEW;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static class ImageProcessor implements Runnable {

        private final QRCodeReader mQrReader;
        ImageReader mReader;

        public ImageProcessor(QRCodeReader qrReader, ImageReader reader) {
            mQrReader = qrReader;
            mReader = reader;
        }

        @Override
        public void run() {
            Log.e(TAG, "in the run!");
            Image mImage = mReader.acquireLatestImage();
            if (mImage == null) return;
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            int width = mImage.getWidth();
            int height = mImage.getHeight();
            Result rawResult = null;
            Log.e("C2", data.length + " (" + width + "x" + height + ")");
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            try {
                rawResult = mQrReader.decode(bitmap);
                onQRCodeRead(rawResult.getText());
            } catch (ReaderException ignored) {
                Log.e(TAG, "Reader shows an exception! ", ignored);
                /* Ignored */
            } finally {
                mQrReader.reset();
                mImage.close();
            }
            if (rawResult != null) {
                Log.e("C2", "Decoding successful!");
            } else {
                Log.d("C2", "No QR code found…");
            }
            mImage.close();
        }

    }

    public static void onQRCodeRead(final String text) {

        synchronized (mockObjectForLocking) {
            if (accessGranted) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        accessGranted = true;
                    }
                }, 300);
                accessGranted = false;
                if (text != null) {
                    MainActivity.app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FragmentScanResult.address = text;
                            FragmentAnimator.hideDecoderFragment();
                            FragmentAnimator.animateScanResultFragment();
                        }
                    });

                } else {

                }
            }
        }
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

    public int getStatusBarHeight() {
        Rect rectangle = new Rect();
        Window window = getActivity().getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        int statusBarHeight = rectangle.top;
        int contentViewTop =
                window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight = contentViewTop - statusBarHeight;

        Log.e(TAG, "StatusBar Height= " + statusBarHeight + " , TitleBar Height = " + titleBarHeight);
        return statusBarHeight + titleBarHeight;
    }

}

//package com.breadwallet.presenter.fragments;
//
//import android.app.Fragment;
//import android.graphics.PointF;
//import android.os.Bundle;
//import android.os.Handler;
//import android.support.v13.app.FragmentCompat;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.RelativeLayout;
//
//import com.breadwallet.R;
//import com.breadwallet.presenter.activities.MainActivity;
//import com.breadwallet.tools.animation.FragmentAnimator;
//import com.breadwallet.tools.animation.SpringAnimator;
//import com.breadwallet.tools.qrcode.QRCodeReaderView;
//
///**
// * BreadWallet
// * <p/>
// * Created by Mihail on 7/14/15.
// * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
// * <p/>
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// * <p/>
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// * <p/>
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// */
//
//public class FragmentDecoder extends Fragment implements
//        FragmentCompat.OnRequestPermissionsResultCallback, QRCodeReaderView.OnQRCodeReadListener {
//
//    public static final String TAG = FragmentDecoder.class.getName();
//    public static boolean accessGranted = true;
//    public static QRCodeReaderView mydecoderview;
//    private ImageView camera_guide_image;
//    public static FragmentDecoder mainFragmentDecoder;
//    private RelativeLayout layout;
//
//    public FragmentDecoder() {
//        mainFragmentDecoder = this;
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//
//        View rootView = inflater.inflate(R.layout.fragment_decoder, container, false);
//        camera_guide_image = (ImageView) rootView.findViewById(R.id.decoder_camera_guide_image);
//        SpringAnimator.showExpandCameraGuide(camera_guide_image);
//
//        // Inflate the layout for this fragment
//        return rootView;
//    }
//
//    @Override
//    public void onActivityCreated(Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        layout = (RelativeLayout) getView().findViewById(R.id.fragment_decoder_layout);
//        Log.e(TAG, "The layout is: " + layout);
//
//    }
//
//    /**
//     * Called when a QR is decoded
//     * "text" : the text encoded in QR
//     * "points" : points where QR control points are placed
//     */
//
//    public void onQRCodeRead(String text, PointF[] points) {
//        synchronized (this) {
//            if (accessGranted) {
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        accessGranted = true;
//                    }
//                }, 300);
//                accessGranted = false;
//                if (text != null) {
//                    FragmentScanResult.address = text;
//                    FragmentAnimator.hideDecoderFragment();
//                    FragmentAnimator.animateScanResultFragment();
//
//                } else {
//
//                }
//            }
//        }
//    }
//
//    // Called when your device have no camera
//    @Override
//    public void cameraNotFound() {
//        Log.d(TAG, "No Camera found!");
//    }
//
//    // Called when there's no QR codes in the camera preview image
//    @Override
//    public void QRCodeNotFoundOnCamImage() {
////        Log.d(TAG, "No QR Code found!");
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        Log.e(TAG, "In the onResume()");
////        addViewAllowed = true;
//        startQRScanner();
//
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        Log.e(TAG, "In the onPause()");
////        addViewAllowed = false;
//        stopQRScanner();
//
//    }
//
//    private void startQRScanner() {
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                MainActivity.app.activityButtonsEnable(false);
//            }
//        }, 300);
//
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (mydecoderview == null) {
//                    mydecoderview = new QRCodeReaderView(MainActivity.app);
//                }
//                mydecoderview.setOnQRCodeReadListener(mainFragmentDecoder);
//                mydecoderview.getCameraManager().startPreview();
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mydecoderview != null)
//                            layout.addView(mydecoderview, 0);
//                    }
//                }, 800);
//                Log.e(TAG, "The camera started");
//            }
//        });
//
//    }
//
//    private void stopQRScanner() {
//        MainActivity.app.activityButtonsEnable(true);
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                layout.removeView(mydecoderview);
//                if (mydecoderview.getCameraManager().isOpen())
//                    mydecoderview.getCameraManager().getCamera().release();
//            }
//        });
//        mydecoderview = null;
//
//    }
//
//}
