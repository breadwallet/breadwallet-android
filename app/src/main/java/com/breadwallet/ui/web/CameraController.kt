package com.breadwallet.ui.web

import android.os.Bundle
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.breadwallet.databinding.ControllerCameraBinding
import com.breadwallet.ui.BaseController
import com.google.common.util.concurrent.ListenableFuture
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
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val binding by viewBinding(ControllerCameraBinding::inflate)

    override fun onAttach(view: View) {
        super.onAttach(view)
        cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext!!)
        with(binding) {
            cameraView.cameraLensFacing = CameraSelector.LENS_FACING_BACK
            cameraView.bindToLifecycle(activity as LifecycleOwner)
            buttonShutter.setOnClickListener {
                takePicture()
            }
        }
    }

    override fun onDetach(view: View) {
        binding.buttonShutter.setOnClickListener(null)
        cameraProviderFuture.addListener(Runnable {
            cameraProviderFuture.get().unbindAll()
        }, executor)
        super.onDetach(view)
    }

    override fun handleBack(): Boolean {
        findListener<Listener>()?.onCameraClosed()
        return super.handleBack()
    }

    private fun takePicture() {
        with(binding) {
            layoutScrim.isVisible = true
            progressBar.isVisible = true
            buttonShutter.isClickable = false
            val file = File.createTempFile(UUID.randomUUID().toString(), ".jpg")
            val options = ImageCapture.OutputFileOptions.Builder(file).build()
            cameraView.takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (!isAttached) return
                    findListener<Listener>()?.onCameraSuccess(file)
                    router.popController(this@CameraController)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (!isAttached) return
                    findListener<Listener>()?.onCameraClosed()
                    router.popController(this@CameraController)
                }
            })
        }
    }
}