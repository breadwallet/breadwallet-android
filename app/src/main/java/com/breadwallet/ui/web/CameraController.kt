package com.breadwallet.ui.web

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.ui.BaseController
import kotlinx.android.synthetic.main.controller_camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.File
import java.util.UUID

class CameraController(
    args: Bundle? = null
) : BaseController(args) {

    interface Listener {
        fun onCameraSuccess(file: File)
        fun onCameraClosed()
    }

    private val executor = Dispatchers.Main.asExecutor()

    override val layoutId = R.layout.controller_camera

    override fun onAttach(view: View) {
        super.onAttach(view)
        val activity = activity as AppCompatActivity
        cameraView.cameraLensFacing = CameraSelector.LENS_FACING_BACK
        cameraView.bindToLifecycle(activity)
        buttonShutter.setOnClickListener {
            takePicture()
        }
    }

    override fun onDetach(view: View) {
        buttonShutter.setOnClickListener(null)
        CameraX.unbindAll()
        super.onDetach(view)
    }

    override fun handleBack(): Boolean {
        getListener()?.onCameraClosed()
        return super.handleBack()
    }

    private fun takePicture() {
        layoutScrim.isVisible = true
        progressBar.isVisible = true
        buttonShutter.isClickable = false
        val file = File.createTempFile(UUID.randomUUID().toString(), ".jpg")
        cameraView.takePicture(file, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                if (!isAttached) return
                getListener()?.onCameraSuccess(file)
                router.popController(this@CameraController)
            }

            override fun onError(exception: ImageCaptureException) {
                if (!isAttached) return
                getListener()?.onCameraClosed()
                router.popController(this@CameraController)
            }
        })
    }

    private fun getListener(): Listener? =
        (targetController as? Listener)
            ?: (parentController as? Listener)
            ?: (router.backstack.dropLast(1).lastOrNull()?.controller() as? Listener)
}