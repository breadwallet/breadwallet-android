package com.breadwallet.tools.qrcode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@UseExperimental(ExperimentalCoroutinesApi::class)
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
        QRCodeReaderView.startCamera()
    }

    awaitClose {
        setOnQRCodeReadListener(null)
        if (handleCamera) {
            QRCodeReaderView.stopCamera()
        }
    }
}.flowOn(Dispatchers.Main)
