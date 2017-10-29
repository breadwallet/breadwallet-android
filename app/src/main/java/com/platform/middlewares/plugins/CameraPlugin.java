package com.platform.middlewares.plugins;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.camera.CameraActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.util.BRConstants;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Plugin;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/2/16.
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
public class CameraPlugin implements Plugin {
    public static final String TAG = CameraPlugin.class.getName();

    private static Request globalBaseRequest;
    private static Continuation continuation;

    @Override
    public boolean handle(String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) {

        // GET /_camera/take_picture
        //
        // Optionally pass ?overlay=<id> (see overlay ids below) to show an overlay
        // in picture taking mode
        //
        // Status codes:
        //   - 200: Successful image capture
        //   - 204: User canceled image picker
        //   - 404: Camera is not available on this device
        //   - 423: Multiple concurrent take_picture requests. Only one take_picture request may be in flight at once.
        //

        if (target.startsWith("/_camera/take_picture")) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            final Activity app = BreadApp.getCurrentActivity();
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());

                return BRHTTPHelper.handleError(404, "context is null", baseRequest, response);
            }

            if (globalBaseRequest != null) {
                Log.e(TAG, "handle: already taking a picture: " + target + " " + baseRequest.getMethod());

                return BRHTTPHelper.handleError(423, null, baseRequest, response);
            }

            PackageManager pm = app.getPackageManager();

            if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                Log.e(TAG, "handle: no camera available: ");
                return BRHTTPHelper.handleError(404, null, baseRequest, response);
            }

            globalBaseRequest = baseRequest;
            continuation = ContinuationSupport.getContinuation(request);
            continuation.suspend(response);

            try {
                // Check if the camera permission is granted
                if (ContextCompat.checkSelfPermission(app,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(app,
                            Manifest.permission.CAMERA)) {
                        BRDialog.showCustomDialog(app, "Permission Required.",
                                "Allow camera access in \"Settings\" > \"Apps\" > \"loafwallet\" > \"Permissions\"",
                                "close", null, new BRDialogView.BROnClickListener() {
                                    @Override
                                    public void onClick(BRDialogView brDialogView) {
                                        brDialogView.dismiss();
                                    }
                                }, null, null, 0);
                    } else {
                        // No explanation needed, we can request the permission.
                        ActivityCompat.requestPermissions(app,
                                new String[]{Manifest.permission.CAMERA},
                                BRConstants.CAMERA_REQUEST_ID);
                    }
                } else {
                    // Permission is granted, open camera
                    Intent intent = new Intent(app, CameraActivity.class);
                    app.startActivityForResult(intent, BRConstants.REQUEST_IMAGE_CAPTURE);
                    app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        } else if (target.startsWith("/_camera/picture/")) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            final Activity app = BreadApp.getCurrentActivity();
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(404, "context is null", baseRequest, response);
            }
            String id = target.replace("/_camera/picture/", "");
            byte[] pictureBytes = readPictureForId(app, id);
            if (pictureBytes == null) {
                Log.e(TAG, "handle: WARNING pictureBytes is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "pictureBytes is null", baseRequest, response);
            }
            byte[] imgBytes = pictureBytes;
            String b64opt = request.getParameter("base64");
            String contentType = "image/jpeg";
            if (b64opt != null && !b64opt.isEmpty()) {
                contentType = "text/plain";
                String b64 = "data:image/jpeg;base64," + Base64.encodeToString(pictureBytes, Base64.NO_WRAP);
                try {
                    imgBytes = b64.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return BRHTTPHelper.handleError(500, null, baseRequest, response);
                }
            }
            return BRHTTPHelper.handleSuccess(200, imgBytes, baseRequest, response, contentType);
        } else return false;
    }

    public static void handleCameraImageTaken(final Context context, final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Bitmap img = getResizedBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), 1000);
                Log.e(TAG, "handleCameraImageTaken: w:" + img.getWidth() + "h:" + img.getHeight());
                if (globalBaseRequest == null || continuation == null) {
                    //shit should now happen
                    Log.e(TAG, "handleCameraImageTaken: WARNING: " + continuation + " " + globalBaseRequest);
                    return;
                }
                try {
                    if (img == null) {
                        //no image
                        globalBaseRequest.setHandled(true);
                        ((HttpServletResponse) continuation.getServletResponse()).setStatus(204);
                        continuation.complete();
                        return;
                    }
                    String id = writeToFile(context, img);
                    if (id != null) {
                        JSONObject respJson = new JSONObject();
                        try {
                            respJson.put("id", id);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            globalBaseRequest.setHandled(true);
                            try {
                                ((HttpServletResponse) continuation.getServletResponse()).sendError(500);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            continuation.complete();
                            return;
                        }
                        Log.i(TAG, "handleCameraImageTaken: wrote image to: " + id);
                        try {
                            continuation.getServletResponse().setContentType("application/json");
                            ((HttpServletResponse) continuation.getServletResponse()).setStatus(200);
                            continuation.getServletResponse().getWriter().write(respJson.toString());
                            globalBaseRequest.setHandled(true);
                            continuation.complete();
                            Log.d(TAG, "run: Finished taking picture");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    } else {
                        Log.e(TAG, "handleCameraImageTaken: error writing image");
                        try {
                            globalBaseRequest.setHandled(true);
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(500);
                            continuation.complete();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } finally {
                    globalBaseRequest = null;
                    continuation = null;
                }

            }
        }).start();

    }

    private static String writeToFile(Context context, Bitmap img) {
        String name = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            img.compress(Bitmap.CompressFormat.JPEG, 70, out);
            name = CryptoHelper.base58ofSha256(out.toByteArray());
            File storageDir = new File(context.getFilesDir().getAbsolutePath() + "/pictures/");
            File image = new File(storageDir, name + ".jpeg");
            FileUtils.writeByteArrayToFile(image, out.toByteArray());
            return name;
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public byte[] readPictureForId(Context context, String id) {
        Log.i(TAG, "readPictureForId: " + id);
        try {
            //create FileInputStream object
            FileInputStream fin = new FileInputStream(new File(context.getFilesDir().getAbsolutePath() + "/pictures/" + id + ".jpeg"));

            //create string from byte array
            return IOUtils.toByteArray(fin);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found " + e);
        } catch (IOException ioe) {
            Log.e(TAG, "Exception while reading the file " + ioe);
        }
        return null;
    }
}
