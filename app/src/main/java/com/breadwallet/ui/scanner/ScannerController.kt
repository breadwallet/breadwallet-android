package com.breadwallet.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.breadwallet.R
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.tools.qrcode.scannedText
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.Link
import com.breadwallet.tools.util.asLink
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.MainActivity
import kotlinx.android.synthetic.main.controller_scanner.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest

private const val CAMERA_UI_UPDATE_MS = 100L

@UseExperimental(ExperimentalCoroutinesApi::class)
class ScannerController(
    args: Bundle? = null
) : BaseController(args) {

    interface Listener {
        fun onLinkScanned(link: Link)
    }

    override val layoutId = R.layout.controller_scanner

    override fun onAttach(view: View) {
        super.onAttach(view)
        val context = checkNotNull(applicationContext)

        val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
            startScanner()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                BRConstants.CAMERA_REQUEST_ID
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BRConstants.CAMERA_REQUEST_ID) {
            when (grantResults.single()) {
                PackageManager.PERMISSION_GRANTED -> startScanner()
                PackageManager.PERMISSION_DENIED -> router.popController(this)
            }
        }
    }

    private fun startScanner() {
        qrdecoderview
            .scannedText(true)
            .mapLatest { text -> text to text.asLink() }
            .flowOn(Default)
            .transformLatest { (text, link) ->
                if (link == null) {
                    logError("Found incompatible QR code")
                    showGuideError()
                } else {
                    logDebug("Found compatible QR code")
                    scan_guide.setImageResource(R.drawable.cameraguide)
                    emit(text to link)
                }
            }
            .take(1)
            .onEach { (text, link) ->
                handleValidLink(text, link)
            }
            .flowOn(Main)
            .launchIn(viewAttachScope)
    }

    private fun handleValidLink(text: String, link: Link) {
        // Try calling the targetController to handle the link,
        // if no listener handles it, dispatch to MainActivity.
        val consumed: Unit? = (targetController as? Listener)?.onLinkScanned(link)
        if (consumed == null) {
            Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_DATA, text)
                .run(this::startActivity)
        }
        router.popController(this@ScannerController)
    }

    /** Display an error state for [CAMERA_UI_UPDATE_MS] then reset. */
    private suspend fun showGuideError() {
        scan_guide.setImageResource(R.drawable.cameraguide_red)
        delay(CAMERA_UI_UPDATE_MS)
        scan_guide.setImageResource(R.drawable.cameraguide)
    }
}
