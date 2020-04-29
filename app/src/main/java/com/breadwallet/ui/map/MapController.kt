/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 11/6/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.logger.logError
import com.breadwallet.logger.logInfo
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.platform.HTTPServer
import com.platform.PlatformTransactionBus
import com.platform.jsbridge.NativePromiseFactory
import com.platform.middlewares.plugins.GeoLocationPlugin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

private const val ARG_URL = "WebController.URL"
private const val CLOSE_URL = "_close"

@Suppress("TooManyFunctions")
class MapController(
    args: Bundle
) : BaseController(args) {

    constructor(url: String) : this(
        bundleOf(ARG_URL to url)
    )

    companion object {
        private const val CHOOSE_IMAGE_REQUEST_CODE = 1
        private const val GET_CAMERA_PERMISSIONS_REQUEST_CODE = 2
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        private const val IMAGE_FILE_NAME_SUFFIX = "_kyc.jpg"
        private const val INTENT_TYPE_IMAGE = "image/*"

        private val CAMERA_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    init {
        retainViewMode = RetainViewMode.RETAIN_DETACH

        registerForActivityResult(CHOOSE_IMAGE_REQUEST_CODE)
        registerForActivityResult(GET_CAMERA_PERMISSIONS_REQUEST_CODE)
    }

    override val layoutId = R.layout.fragment_map

    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraImageFileUri: Uri? = null
    private lateinit var nativePromiseFactory: NativePromiseFactory

    val SYDNEY = LatLng(-33.862, 151.21)
    val ZOOM_LEVEL = 13f

    @Nullable
    fun getActivityFromContext(@NonNull context: Context): AppCompatActivity? {
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) return context
            return context.baseContext as AppCompatActivity
        }
        return null //we failed miserably
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)

        HTTPServer.setOnCloseListener {
            router.popCurrentController()
            HTTPServer.setOnCloseListener(null)
        }

        val fragmentManager = getActivityFromContext(view.context)!!.supportFragmentManager
        val fragment : SupportMapFragment = fragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        fragment.getMapAsync(object: OnMapReadyCallback {

            /**
             * This is where we can add markers or lines, add listeners or move the camera. In this case,
             * we just move the camera to Sydney and add a marker in Sydney.
             */
            override fun onMapReady(googleMap: GoogleMap?) {
                googleMap ?: return
                with(googleMap) {
                    moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, ZOOM_LEVEL))
                    addMarker(MarkerOptions().position(SYDNEY))
                }
            }
        })

        handlePlatformMessages().launchIn(viewCreatedScope)
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        nativePromiseFactory.dispose()
    }

    private fun handlePlatformMessages() = PlatformTransactionBus.requests().onEach {
        withContext(Dispatchers.Main) {
            val transaction = RouterTransaction.with(PlatformConfirmTransactionController(it))
            router.pushController(transaction)
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        if (permissions != null) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        activity!!,
                        permission
                    ) !== PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    private fun getImageFileChooserIntent(hasCameraPermissions: Boolean): Intent {
        val intents = mutableListOf<Intent>()
        val packageManager = activity!!.packageManager
        val res = checkNotNull(resources)

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) && hasCameraPermissions) {
            createCameraIntent()?.run(intents::add)
        }

        intents.addAll(getGalleryIntents())

        // Create the file chooser intent. The first intent from our list that will
        // appear in the chooser, must be removed from the intents array and passed
        // into the chooser upon creation.
        val imageFileChooserIntent = Intent.createChooser(
            intents[0],
            res.getString(R.string.FileChooser_selectImageSource_android)
        )
        intents.removeAt(0)

        // Add the remaining image file chooser intents to an auxiliary array.
        // These will also appear in the file chooser.
        imageFileChooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())

        return imageFileChooserIntent
    }

    private fun createCameraIntent(): Intent? {
        val packageManager = activity!!.packageManager
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            try {
                // Create a file to store the camera image.
                val imageFile = File.createTempFile(
                    SimpleDateFormat(DATE_FORMAT).format(Date()) + '_',
                    IMAGE_FILE_NAME_SUFFIX,
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                )

                // Create a camera intent for use in the file chooser.
                if (imageFile != null) {
                    mCameraImageFileUri =
                        FileProvider.getUriForFile(
                            activity!!,
                            BuildConfig.APPLICATION_ID,
                            imageFile
                        )
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraImageFileUri)
                    return cameraIntent
                }
            } catch (e: IOException) {
                logError("Unable to create image file for camera intent.", e)
            }
        } else {
            logError("Image capture intent not found, unable to allow camera use.")
        }
        return null
    }

    private fun getGalleryIntents(): List<Intent> {
        val packageManager = activity!!.packageManager
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = INTENT_TYPE_IMAGE
        }
        val galleryActivityList = packageManager.queryIntentActivities(galleryIntent, 0)
        val intents = mutableListOf<Intent>()
        for (resolveInfo in galleryActivityList) {
            intents.add(Intent(galleryIntent).apply {
                component =
                    ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name
                    )
                setPackage(resolveInfo.activityInfo.packageName)
            })
        }
        return intents
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        logInfo("onActivityResult: requestCode: $requestCode resultCode: $resultCode")

        if (requestCode == CHOOSE_IMAGE_REQUEST_CODE && mFilePathCallback != null) {
            var imageFileUri: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK) {
                when (intent?.dataString) {
                    null -> {
                        // Camera Intent Result:
                        // If the resulting intent is null or empty the user has taken a photo
                        // with the camera.  On some versions of Android, the camera intent returns
                        // a null intent (i.e. Android 8.1), on other versions (i.e. Android 9),
                        // the intent is empty (all fields are null). We only check getDataString()
                        // because in the else case the data string will not be null.
                        if (mCameraImageFileUri != null) {
                            imageFileUri = arrayOf(mCameraImageFileUri!!)
                        }
                    }
                    else -> {
                        // Gallery Intent Result:
                        // If the resulting intent has the dataString, the user has selected
                        // an existing image and the data string the URI of the selected image.
                        val dataString = intent.dataString
                        if (dataString != null) {
                            imageFileUri = arrayOf(Uri.parse(dataString))
                        }
                    }
                }
            }

            // Return the image file Uris to the web.
            mFilePathCallback!!.onReceiveValue(imageFileUri)
            mFilePathCallback = null
        } else {
            // We have returned from some other intent not handled by this Activity.
            super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        logInfo("onRequestPermissionResult: requestCode: $requestCode")
        when (requestCode) {
            GET_CAMERA_PERMISSIONS_REQUEST_CODE ->
                // The camera permissions have changed upon a request for the user to select an
                // image. Show the appropriate image file chooser based on the current permissions.
                startActivityForResult(
                    getImageFileChooserIntent(permissionGranted(grantResults)),
                    CHOOSE_IMAGE_REQUEST_CODE
                )
            BRConstants.GEO_REQUEST_ID -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted
                    logInfo("Geo permission GRANTED")
                    GeoLocationPlugin.handleGeoPermission(true)
                } else {
                    GeoLocationPlugin.handleGeoPermission(false)
                }
            }
        }
    }

    private fun permissionGranted(grantResults: IntArray): Boolean {
        for (grantResult in grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    private inner class BRWebChromeClient : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            logInfo("onConsoleMessage: consoleMessage: " + consoleMessage.message())
            return super.onConsoleMessage(consoleMessage)
        }

        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            logInfo("onJsAlert: $message, url: $url")
            return super.onJsAlert(view, url, message, result)
        }

        override fun onCloseWindow(window: WebView) {
            super.onCloseWindow(window)
            logInfo("onCloseWindow: ")
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePath: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            logInfo("onShowFileChooser")

            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback!!.onReceiveValue(null)
            }

            // Save the new call back. It will be called when the image file chooser activity returns.
            mFilePathCallback = filePath

            if (hasPermissions(CAMERA_PERMISSIONS)) {
                startActivityForResult(getImageFileChooserIntent(true), CHOOSE_IMAGE_REQUEST_CODE)
            } else {
                requestPermissions(
                    CAMERA_PERMISSIONS,
                    GET_CAMERA_PERMISSIONS_REQUEST_CODE
                )
            }
            return true
        }
    }
}
