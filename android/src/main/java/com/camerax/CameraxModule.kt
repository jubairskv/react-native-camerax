package com.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.app.Activity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import android.util.Log
import androidx.camera.view.PreviewView
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.view.ViewGroup
import android.widget.Button
import android.view.Gravity
import android.widget.TextView
import android.view.View
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class CameraxModule(reactContext: ReactApplicationContext) :ReactContextBaseJavaModule(reactContext), PermissionListener {

    private val PERMISSION_REQUEST_CODE = 10
    private var permissionPromise: Promise? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private lateinit var cameraExecutor: ExecutorService
    private var previewView: PreviewView? = null
    private lateinit var captureButton: Button
    private var isStarted = false

    init {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun getName(): String {
        return NAME
    }

   @ReactMethod
    fun getDummyText(dateString: String, promise: Promise) {
        // Log the dateString
        Log.d("CameraModule", "Received dateString: $dateString")

        // Use the dateString in your logic
        val response = "Received date: $dateString. Hello from Native Module!"
        promise.resolve(response)
    }

    @ReactMethod
    fun requestCameraPermission(promise: Promise) {
        val activity = currentActivity as? PermissionAwareActivity
            ?: return promise.reject("NO_ACTIVITY", "No activity found")

        permissionPromise = promise

        activity.requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE,
            this
        )
    }

    @ReactMethod
    fun checkCameraPermission(promise: Promise) {
        val permission = ContextCompat.checkSelfPermission(
            reactApplicationContext,
            Manifest.permission.CAMERA
        )
        promise.resolve(permission == PackageManager.PERMISSION_GRANTED)
    }

    @ReactMethod
    fun startCamera(promise: Promise) {
        if (isStarted) {
            promise.resolve(true)
            return
        }

        val activity = currentActivity ?: return promise.reject("NO_ACTIVITY", "No activity found")
        
        // Check and request permission if needed
        if (ContextCompat.checkSelfPermission(reactApplicationContext, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            // Request permission
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
                                // Permission granted, start camera
                                activity.runOnUiThread {
                                    setupUI(activity, promise)
                                    isStarted = true
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

        // Permission already granted, start camera
        activity.runOnUiThread {
            setupUI(activity, promise)
            isStarted = true
        }
    }

    private fun setupUI(activity: Activity, promise: Promise) {
        // Create a FrameLayout to hold all the UI components
        val frameLayout = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Create the PreviewView for the camera preview
        previewView = PreviewView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Instruction TextView
        val instructionTextView = TextView(activity).apply {
            text = "Take a Picture of Front side of ID Card"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                900,
                250
            ).apply {
                gravity = Gravity.TOP
                topMargin = 80
                leftMargin = 100
                rightMargin = 50
            }
            setPadding(50, 0, 50, 0)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#80000000"))
                cornerRadius = 30f
            }
        }


        // Capture Button
        captureButton = Button(activity).apply {
            text = "Capture Front ID"
            setBackgroundColor(Color.parseColor("#59d5ff"))
            setTextColor(Color.WHITE)
            textSize = 18f
            layoutParams = FrameLayout.LayoutParams(
                800,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 50
            }
            setPadding(50, 0, 50, 0)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#59d5ff"))
                cornerRadius = 30f
            }
        }

        // ID Card Border Box
        val borderBox = View(activity).apply {
            val metrics = activity.windowManager.defaultDisplay.let { display ->
                val displayMetrics = android.util.DisplayMetrics()
                display.getMetrics(displayMetrics)
                displayMetrics
            }

            val width = (metrics.widthPixels * 0.85).toInt()
            val height = (width * 0.63).toInt()

            val params = FrameLayout.LayoutParams(width, height)
            params.gravity = Gravity.CENTER
            layoutParams = params

            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(4, Color.WHITE)
                cornerRadius = 20f
            }
        }

        // Add all views to the FrameLayout
        frameLayout.addView(previewView)
        frameLayout.addView(borderBox)
        frameLayout.addView(instructionTextView)
        frameLayout.addView(captureButton)

        // Set the FrameLayout as the content view of the activity
        activity.setContentView(frameLayout)

        // Start the camera preview
        startCameraX(promise)
    }

    private fun startCameraX(promise: Promise) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(reactApplicationContext)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Build Preview use case
                preview = Preview.Builder().build()

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Create preview view surface provider
                preview?.setSurfaceProvider(previewView?.surfaceProvider)

                // Unbind any bound use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    currentActivity as LifecycleOwner,
                    cameraSelector,
                    preview
                )

                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("CAMERA_ERROR", "Failed to start camera: ${e.message}")
                Log.e(NAME, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(reactApplicationContext))
    }

    @ReactMethod
    fun stopCamera(promise: Promise) {
        if (!isStarted) {
            promise.resolve(true)
            return
        }

        try {
            val activity = currentActivity ?: return promise.reject("NO_ACTIVITY", "No activity found")
            
            activity.runOnUiThread {
                // Remove camera UI
                val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                rootView.removeAllViews()
                
                // Unbind use cases
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() && 
                         grantResults[0] == PackageManager.PERMISSION_GRANTED
            permissionPromise?.resolve(granted)
            permissionPromise = null
            return true
        }
        return false
    }

    override fun onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }

    companion object {
        const val NAME = "Camerax"
    }
}