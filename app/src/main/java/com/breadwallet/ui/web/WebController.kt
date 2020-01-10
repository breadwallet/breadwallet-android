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
