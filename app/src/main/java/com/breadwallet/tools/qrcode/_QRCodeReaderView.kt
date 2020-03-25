/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 11/13/19.
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
package com.breadwallet.tools.qrcode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

private const val AUTO_FOCUS_PERIOD_MS = 500L

/**
 * Emits each [String] scanned by the [QRCodeReaderView].
 * If [handleCamera] is true the camera will be started
 * and stopped automatically.  [configureView] can be used
 * to further configure the [QRCodeReaderView] before
 * starting the camera.  By default an autofocus interval
 * of [AUTO_FOCUS_PERIOD_MS] is used and the back camera
 * is selected.
 */
fun QRCodeReaderView.scannedText(
    handleCamera: Boolean = false,
    configureView: QRCodeReaderView.() -> Unit = {
        setAutofocusInterval(AUTO_FOCUS_PERIOD_MS)
        setBackCamera()
    }
): Flow<String> = callbackFlow<String> {
    setOnQRCodeReadListener { text, _ ->
        if (isActive) offer(text)
        false
    }

    configureView()

    if (handleCamera) {
        startCamera()
    }

    awaitClose {
        setOnQRCodeReadListener(null)
        if (handleCamera) {
            stopCamera()
        }
    }
}.flowOn(Dispatchers.Main)
