package com.daakknights.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File

    companion object {
        private const val TAG = "MainActivity.CameraX"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 999
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Check for camera permissions
        if (checkIfCameraPermissionGranted()) {
            startCamera()
        } else {
            // if had denied then ask for one more time
            ActivityCompat.requestPermissions(
                this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        // listener for click button
        camera_button.setOnClickListener { clickPhoto() }
        //directory to save image to
        outputDirectory = getOutputDirectory()

    }

    private fun checkIfCameraPermissionGranted() = CAMERA_PERMISSION.let {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val imageDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (imageDir != null && imageDir.exists())
            imageDir else filesDir
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }
            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbinding before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error while binding Use cases", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun clickPhoto() {
        if (camera_button.text.equals(getString(R.string.click))) {
            val imageCapture = imageCapture ?: return

            // file to save the image
            val photoFile = File(
                outputDirectory,
                SimpleDateFormat(
                    FILENAME_FORMAT, Locale.US
                ).format(System.currentTimeMillis()) + ".jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(e: ImageCaptureException) {
                        Log.e(TAG, "Failed to capture photo : ${e.message}", e)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        image_clicked.setImageURI(savedUri)
                        image_clicked_layout.visibility = View.VISIBLE
                        camera_button.text = getString(R.string.back)
                        val msg = "Photo is saved at: $savedUri"
                        image_dir_path.text = msg
                        Log.d(TAG, msg)
                    }
                })
        } else {
            image_clicked_layout.visibility = View.GONE
            camera_button.text = getString(R.string.click)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (checkIfCameraPermissionGranted()) {
                startCamera()
            } else {
                Snackbar.make(
                    main_layout,
                    "Without permission app will not work",
                    Snackbar.LENGTH_LONG
                ).show()
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 2000)

            }
        }
    }


}