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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.databinding.FragmentSupportBinding
import com.breadwallet.logger.logError
import com.breadwallet.logger.logInfo
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.browser.BrdNativeJs
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.platform.LinkBus
import com.platform.LinkResultMessage
import com.platform.PlatformTransactionBus
import com.platform.jsbridge.BrdApiJs
import com.platform.jsbridge.CameraJs
import com.platform.jsbridge.KVStoreJs
import com.platform.jsbridge.LinkJs
import com.platform.jsbridge.LocationJs
import com.platform.jsbridge.NativeApisJs
import com.platform.jsbridge.NativePromiseFactory
import com.platform.jsbridge.SupportJs
import com.platform.jsbridge.WalletJs
import com.platform.middlewares.plugins.LinkPlugin
import com.platform.util.getStringOrNull
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.io.File
import java.util.Locale
import java.util.UUID

private const val ARG_URL = "WebController.URL"
private const val ARG_JSON_REQUEST = "WebController.JSON_REQUEST"
private const val CLOSE_URL = "_close"
private const val FILE_SUFFIX = ".jpg"

@Suppress("TooManyFunctions")
class WebController(
    args: Bundle
) : BaseController(args),
    CameraController.Listener {

    constructor(url: String, jsonRequest: String? = null) : this(
        bundleOf(
            ARG_URL to url,
            ARG_JSON_REQUEST to jsonRequest
        )
    )

    companion object {
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

        registerForActivityResult(BRConstants.REQUEST_IMAGE_RC)
    }

    private val binding by viewBinding(FragmentSupportBinding::inflate)

    private var mOnCloseUrl: String? = null
    private lateinit var nativePromiseFactory: NativePromiseFactory

    private val fileSelectChannel = BroadcastChannel<Uri?>(BUFFERED)
    private val cameraPermissionChannel = BroadcastChannel<Boolean>(BUFFERED)
    private val cameraPermissionFlow = cameraPermissionChannel.asFlow()
        .onStart {
            val pm = checkNotNull(applicationContext).packageManager
            when {
                !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) -> emit(false)
                hasPermissions(CAMERA_PERMISSIONS) -> emit(true)
                else -> requestPermissions(CAMERA_PERMISSIONS, BRConstants.CAMERA_PERMISSIONS_RC)
            }
        }
        .take(1)
        .flowOn(Main)
    private val cameraResultChannel = BroadcastChannel<String?>(BUFFERED)
    private val imageRequestFlow = cameraPermissionFlow
        .flatMapLatest { hasPermissions ->
            if (hasPermissions) {
                pushCameraController()
                cameraResultChannel.asFlow().take(1)
            } else flowOf(null)
        }
        .flowOn(Main)

    private val locationPermissionChannel = BroadcastChannel<Boolean>(BUFFERED)
    private val locationPermissionFlow = locationPermissionChannel.asFlow()
        .onStart {
            if (hasPermissions(LOCATION_PERMISSIONS)) {
                emit(true)
            } else {
                requestPermissions(LOCATION_PERMISSIONS, BRConstants.GEO_REQUEST_ID)
            }
        }
        .take(1)
        .flowOn(Main)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        binding.webView.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val trimmedUrl = request.url.toString().trimEnd('/')

                if (trimmedUrl.startsWith("file://")) {
                    view.loadUrl(trimmedUrl)
                } else {
                    // Simplex || Wyre links 
                    return false
                }
                return true
            }
        }

        binding.webView.webChromeClient = BRWebChromeClient()
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        if (!::nativePromiseFactory.isInitialized) {
            val url: String = arg(ARG_URL)
            nativePromiseFactory = NativePromiseFactory(binding.webView)
            val isPlatformUrl =
                url.startsWith("http://127.0.0.1:" + BRSharedPrefs.getHttpServerPort())
            if ((isPlatformUrl || url.startsWith("file:///"))) {
                binding.webView.setBackgroundResource(R.color.platform_webview_bg)
                val locationManager = applicationContext!!.getSystemService<LocationManager>()
                val brdApiJs = BrdApiJs(nativePromiseFactory, direct.instance())
                val cameraJs = CameraJs(nativePromiseFactory, imageRequestFlow)
                val locationJs =
                    LocationJs(nativePromiseFactory, locationPermissionFlow, locationManager)
                val kvStoreJs = KVStoreJs(
                    nativePromiseFactory,
                    direct.instance()
                )
                val linkJs = LinkJs(nativePromiseFactory)
                val walletJs = WalletJs(
                    nativePromiseFactory,
                    direct.instance(),
                    direct.instance(),
                    direct.instance(),
                    direct.instance(),
                    direct.instance(),
                    direct.instance()
                )
                val supportJs = SupportJs(
                    nativePromiseFactory,
                    direct.instance()
                )
                val nativeApis = if (BuildConfig.DEBUG) {
                    NativeApisJs.with(
                        cameraJs,
                        locationJs,
                        kvStoreJs,
                        linkJs,
                        walletJs,
                        brdApiJs,
                        supportJs
                    )
                } else {
                    NativeApisJs.with(walletJs, linkJs, supportJs)
                }

                nativeApis.attachToWebView(binding.webView)
                binding.webView.addJavascriptInterface(BrdNativeJs, BrdNativeJs.JS_NAME)
            }
            val jsonRequest: String? = argOptional(ARG_JSON_REQUEST)
            if (jsonRequest.isNullOrEmpty()) {
                binding.webView.loadUrl(url)
                handlePlatformMessages().launchIn(viewCreatedScope)
            } else {
                try {
                    handleJsonRequest(jsonRequest)
                    LinkBus.sendMessage(LinkResultMessage.LinkSuccess)
                } catch (e: Exception) {
                    logError("Handling json request failed", e)
                    router.popController(this@WebController)
                    LinkBus.sendMessage(LinkResultMessage.LinkFailure(e))
                }
            }
            handleLinkMessages().launchIn(viewCreatedScope)
        }
    }

    private fun handleJsonRequest(jsonRequest: String) {
        val request = JSONObject(jsonRequest)
        val url = request.getString(BRConstants.URL)

        with(binding) {
            if (url != null && url.contains(BRConstants.CHECKOUT)) {
                // TODO: attachKeyboardListeners?
                toolbar.isVisible = true
                toolbarBottom.isVisible = true

                webviewBackArrow.setOnClickListener { handleBack() }
                webviewForwardArrow.setOnClickListener { if (webView.canGoForward()) webView.goForward() }
                reload.setOnClickListener { handleJsonRequest(jsonRequest) }
            }
        }

        val method = request.getString(BRConstants.METHOD)
        val body = request.getStringOrNull(BRConstants.BODY)
        val headers = request.getStringOrNull(BRConstants.HEADERS)
        mOnCloseUrl = request.getString(BRConstants.CLOSE_ON)

        val httpHeaders = mutableMapOf<String, String>()

        if (!headers.isNullOrEmpty()) {
            val headersJSON = JSONObject(headers)
            headersJSON.keys().forEach {
                httpHeaders[it] = headersJSON.getString(it)
            }
        }

        when (method.toUpperCase(Locale.ROOT)) {
            "GET" -> {
                if (httpHeaders.isNotEmpty()) {
                    binding.webView.loadUrl(url, httpHeaders)
                } else {
                    binding.webView.loadUrl(url)
                }
            }
            "POST" -> binding.webView.postUrl(url, body?.toByteArray())
            else -> error("Unexpected method: $method")
        }
    }

    override fun handleBack() = when {
        binding.webView.canGoBack() -> {
            binding.webView.goBack()
            true
        }
        else -> {
            LinkPlugin.hasBrowser = false
            super.handleBack()
        }
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

    private fun handleLinkMessages() = LinkBus.requests().onEach { (url, jsonRequest) ->
        withContext(Main) {
            if (url.contains("/_close", true)) {
                if (router.backstack.lastOrNull()?.controller is WebController) {
                    router.popCurrentController()
                }
            } else {
                val transaction = RouterTransaction.with(WebController(url, jsonRequest))
                router.pushController(transaction)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        logInfo("onRequestPermissionResult: requestCode: $requestCode")
        when (requestCode) {
            BRConstants.CAMERA_PERMISSIONS_RC -> {
                cameraPermissionChannel.offer(grantResults.firstOrNull() == PERMISSION_GRANTED)
            }
            BRConstants.GEO_REQUEST_ID -> {
                locationPermissionChannel.offer(grantResults.firstOrNull() == PERMISSION_GRANTED)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.getParcelableExtra(MediaStore.EXTRA_OUTPUT) ?: data?.dataString?.toUri()
        fileSelectChannel.offer(uri)
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
            LinkPlugin.hasBrowser = false
            logInfo("onCloseWindow: ")
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                viewCreatedScope.launch(Main) {
                    val hasCameraPermission = cameraPermissionFlow.first()
                    if (hasCameraPermission) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    } else {
                        request.deny()
                    }
                }
            } else {
                request.deny()
            }
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePath: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            logInfo("onShowFileChooser")
            viewCreatedScope.launch(Main) {
                val context = checkNotNull(applicationContext)
                val cameraGranted = cameraPermissionFlow.first()
                val (intent, file) = createChooserIntent(context, cameraGranted)

                val selectedFile = if (intent == null) {
                    // No available apps, use internal camera if possible.
                    if (cameraGranted) {
                        imageRequestFlow.first()?.toUri()
                    } else null
                } else {
                    startActivityForResult(intent, BRConstants.REQUEST_IMAGE_RC)
                    fileSelectChannel.asFlow().first()
                }
                val result = when {
                    selectedFile != null -> arrayOf(selectedFile)
                    file != null && file.length() > 0 -> arrayOf(file.toUri())
                    else -> emptyArray()
                }
                filePath.onReceiveValue(result)
            }
            return true
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (checkSelfPermission(activity!!, permission) != PERMISSION_GRANTED) {
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
            requestPermissions(CAMERA_PERMISSIONS, BRConstants.CAMERA_PERMISSIONS_RC)
        }
    }

    private fun createTempImageFile(context: Context) = createTempFile(
        UUID.randomUUID().toString(),
        FILE_SUFFIX,
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    )

    private fun createChooserIntent(
        context: Context,
        cameraGranted: Boolean
    ): Pair<Intent?, File?> {
        val res = checkNotNull(resources)
        val pm = context.packageManager

        val intents = mutableListOf<Intent>()
        val file: File?
        if (cameraGranted) {
            file = createTempImageFile(context)
            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, file)
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                putExtra(MediaStore.EXTRA_OUTPUT, uri)
                if (resolveActivity(pm) != null) {
                    intents.add(this)
                }
            }
        } else {
            file = null
        }

        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            if (resolveActivity(pm) != null) {
                intents.add(this)
            }
        }

        return if (intents.isEmpty()) {
            null to null
        } else {
            val title = res.getString(R.string.FileChooser_selectImageSource_android)
            Intent(Intent.ACTION_CHOOSER).apply {
                putExtra(Intent.EXTRA_INTENT, intents.last())
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.dropLast(1).toTypedArray())
            } to file
        }
    }
}
