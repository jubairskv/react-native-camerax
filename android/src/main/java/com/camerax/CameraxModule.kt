package com.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraxModule(reactContext: ReactApplicationContext) :cdReactContextBaseJavaModule(reactContext) {

    private var isStarted = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewView: PreviewView? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private lateinit var cameraExecutor: ExecutorService

    init {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    fun getDummyText(dateString: String, promise: Promise) {
        promise.resolve("Camera time: $dateString")
    }

    @ReactMethod
    fun toggleCamera(promise: Promise) {
        try {
            if (isStarted) {
                stopCamera(promise)
            } else {
                startCamera(promise)
            }
        } catch (e: Exception) {
            promise.reject("CAMERA_ERROR", "Failed to toggle camera: ${e.message}")
        }
    }

    private fun startCamera(promise: Promise) {
        if (isStarted) {
            promise.resolve(true)
            return
        }

        val activity = currentActivity ?: return promise.reject("NO_ACTIVITY", "No activity found")
        
        if (ContextCompat.checkSelfPermission(reactApplicationContext, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            
            val permissionAwareActivity = activity as? PermissionAwareActivity
                ?: return promise.reject("NO_ACTIVITY", "No activity found")

            permissionAwareActivity.requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CODE,
                object : PermissionListener {
                    override fun onRequestPermissionsResult(
                        requestCode: Int,
                        permissions: Array<String>,
                        grantResults: IntArray
                    ): Boolean {
                        if (requestCode == PERMISSION_REQUEST_CODE) {
                            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                activity.runOnUiThread {
                                    startCameraPreview(activity, promise)
                                }
                                return true
                            } else {
                                promise.reject("PERMISSION_DENIED", "Camera permission not granted")
                                return true
                            }
                        }
                        return false
                    }
                }
            )
            return
        }

        activity.runOnUiThread {
            startCameraPreview(activity, promise)
        }
    }

    private fun startCameraPreview(activity: android.app.Activity, promise: Promise) {
        try {
            previewView = PreviewView(activity)
            val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
            rootView.addView(previewView)
            
            val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                preview = Preview.Builder().build()
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider?.unbindAll()
                    camera = cameraProvider?.bindToLifecycle(
                        activity as androidx.lifecycle.LifecycleOwner,
                        cameraSelector,
                        preview
                    )
                    preview?.setSurfaceProvider(previewView?.surfaceProvider)
                    isStarted = true
                    promise.resolve(true)
                } catch (e: Exception) {
                    promise.reject("CAMERA_ERROR", "Use case binding failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(activity))
        } catch (e: Exception) {
            promise.reject("CAMERA_ERROR", "Failed to start camera: ${e.message}")
        }
    }

    private fun stopCamera(promise: Promise) {
        if (!isStarted) {
            promise.resolve(true)
            return
        }

        try {
            val activity = currentActivity ?: return promise.reject("NO_ACTIVITY", "No activity found")
            
            activity.runOnUiThread {
                val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                previewView?.let { rootView.removeView(it) }
                
                cameraProvider?.unbindAll()
                previewView = null
                camera = null
                preview = null
                isStarted = false
            }
            
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CAMERA_ERROR", "Failed to stop camera: ${e.message}")
        }
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val NAME = "Camerax"
        private const val PERMISSION_REQUEST_CODE = 10
    }
}
