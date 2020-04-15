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
package com.breadwallet.ui.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.breadwallet.R
import com.breadwallet.logger.logInfo
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.browser.BrdNativeJs
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.platform.HTTPServer
import com.platform.PlatformTransactionBus
import com.platform.jsbridge.CameraJs
import com.platform.jsbridge.LocationJs
import com.platform.jsbridge.NativeApisJs
import com.platform.jsbridge.NativePromiseFactory
import com.platform.jsbridge.WalletJs
import kotlinx.android.synthetic.main.fragment_support.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.io.File

private const val ARG_URL = "WebController.URL"
private const val CLOSE_URL = "_close"

@Suppress("TooManyFunctions")
class WebController(
    args: Bundle
) : BaseController(args),
    CameraController.Listener {

    constructor(url: String) : this(
        bundleOf(ARG_URL to url)
    )

    companion object {
        private const val CHOOSE_IMAGE_REQUEST_CODE = 1
        private const val GET_CAMERA_PERMISSIONS_REQUEST_CODE = 2

        private val CAMERA_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    init {
        retainViewMode = RetainViewMode.RETAIN_DETACH

        registerForActivityResult(CHOOSE_IMAGE_REQUEST_CODE)
        registerForActivityResult(GET_CAMERA_PERMISSIONS_REQUEST_CODE)
    }

    override val layoutId = R.layout.fragment_support

    private var mOnCloseUrl: String? = null
    private lateinit var nativePromiseFactory: NativePromiseFactory

    private val cameraResultChannel = BroadcastChannel<String?>(BUFFERED)
    private val imageRequestFlow = cameraResultChannel.asFlow()
        .take(1)
        .onStart { pushCameraController() }
        .flowOn(Main)

    private val locationPermissionChannel = BroadcastChannel<Boolean>(BUFFERED)
    private val locationPermissionFlow = locationPermissionChannel.asFlow()
        .take(1)
        .onStart {
            if (hasPermissions(LOCATION_PERMISSIONS)) {
                emit(true)
            } else {
                requestPermissions(LOCATION_PERMISSIONS, BRConstants.GEO_REQUEST_ID)
            }
        }
        .flowOn(Main)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)

        val res = checkNotNull(resources)
        val theme = checkNotNull(activity).theme
        web_view.setBackgroundColor(res.getColor(R.color.platform_webview_bg, theme))

        signal_layout.setOnTouchListener(SlideDetector(router, view))

        if (activity!!.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        HTTPServer.setOnCloseListener {
            if (router.backstack.lastOrNull()?.controller() is WebController) {
                router.popCurrentController()
            }
            HTTPServer.setOnCloseListener(null)
        }

        web_view.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
        }

        val url: String = arg(ARG_URL)
        val isPlatformUrl =
            url.startsWith("http://127.0.0.1:" + BRSharedPrefs.getHttpServerPort(null))
        nativePromiseFactory = NativePromiseFactory(web_view)
        if (isPlatformUrl || url.startsWith("file:///")) {
            val locationManager = applicationContext!!.getSystemService<LocationManager>()
            val cameraJs = CameraJs(nativePromiseFactory, imageRequestFlow)
            val locationJs = LocationJs(nativePromiseFactory, locationPermissionFlow, locationManager)
            val walletJs = WalletJs(
                nativePromiseFactory,
                direct.instance(),
                direct.instance(),
                direct.instance(),
                direct.instance(),
                direct.instance()
            )
            val nativeApis = NativeApisJs.with(cameraJs, locationJs, walletJs)

            nativeApis.attachToWebView(web_view)

            web_view.addJavascriptInterface(BrdNativeJs, BrdNativeJs.JS_NAME)
        }

        web_view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val trimmedUrl = request.url.toString().trimEnd('/')

                if (mOnCloseUrl != null && trimmedUrl.equals(mOnCloseUrl, true)) {
                    router.popController(this@WebController)
                    mOnCloseUrl = null
                } else if (trimmedUrl.contains(CLOSE_URL)) {
                    router.popController(this@WebController)
                } else if (trimmedUrl.startsWith("file://")) {
                    view.loadUrl(trimmedUrl)
                }
                return true
            }
        }
        web_view.webChromeClient = BRWebChromeClient()
        web_view.loadUrl(url)

        handlePlatformMessages().launchIn(viewCreatedScope)
    }

    override fun handleBack() = when {
        web_view.canGoBack() -> {
            web_view.goBack()
            true
        }
        else -> super.handleBack()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        nativePromiseFactory.dispose()
    }

    private fun handlePlatformMessages() = PlatformTransactionBus.requests().onEach {
        withContext(Main) {
            val transaction = RouterTransaction.with(PlatformConfirmTransactionController(it))
            router.pushController(transaction)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        logInfo("onRequestPermissionResult: requestCode: $requestCode")
        when (requestCode) {
            GET_CAMERA_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pushCameraController()
                } else {
                    cameraResultChannel.offer(null)
                }
            }
            BRConstants.GEO_REQUEST_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logInfo("Geo permission GRANTED")
                    locationPermissionChannel.offer(true)
                } else {
                    locationPermissionChannel.offer(false)
                }
            }
        }
    }

    override fun onCameraSuccess(file: File) {
        cameraResultChannel.offer(file.absolutePath)
    }

    override fun onCameraClosed() {
        cameraResultChannel.offer(null)
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
            return false
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (checkSelfPermission(activity!!, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun pushCameraController() {
        if (hasPermissions(CAMERA_PERMISSIONS)) {
            router.pushController(
                RouterTransaction.with(CameraController())
                    .pushChangeHandler(FadeChangeHandler())
                    .popChangeHandler(FadeChangeHandler())
            )
        } else {
            requestPermissions(CAMERA_PERMISSIONS, GET_CAMERA_PERMISSIONS_REQUEST_CODE)
        }
    }
}
