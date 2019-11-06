package com.breadwallet.ui.web

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.os.bundleOf
import com.breadwallet.R
import com.breadwallet.logger.logDebug
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.platform.HTTPServer
import kotlinx.android.synthetic.main.fragment_support.*

private const val ARG_URL = "WebController.URL"
private const val CLOSE_URL = "_close"

class WebController(
    args: Bundle
) : BaseController(args) {

    constructor(url: String) : this(
        bundleOf(ARG_URL to url)
    )

    init {
        overridePopHandler(BottomSheetChangeHandler())
        overridePushHandler(BottomSheetChangeHandler())
    }

    override val layoutId = R.layout.fragment_support

    private var mOnCloseUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        signal_layout.setOnTouchListener(SlideDetector(router, view))

        if (activity!!.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        HTTPServer.setOnCloseListener {
            router.popCurrentController()
            HTTPServer.setOnCloseListener(null)
        }

        web_view.settings.apply {
            domStorageEnabled = true
            javaScriptEnabled = true
        }

        web_view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                logDebug("shouldOverrideUrlLoading: ${request.url} | ${request.method}")
                if (mOnCloseUrl != null && request.url.toString().equals(mOnCloseUrl, ignoreCase = true)) {
                    router.popController(this@WebController)
                    mOnCloseUrl = null
                } else if (request.url.toString().contains(CLOSE_URL)) {
                    router.popController(this@WebController)
                } else {
                    view.loadUrl(request.url.toString())
                }

                return true
            }
        }

        web_view.webChromeClient = WebChromeClient()
        web_view.loadUrl(arg(ARG_URL))
    }
}
