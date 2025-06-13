package com.febrivio.sumbercomplang

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.febrivio.sumbercomplang.databinding.ActivityScanBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.appcompat.app.AlertDialog
import com.febrivio.sumbercomplang.model.TiketValidationResponse
import com.febrivio.sumbercomplang.model.TransaksiTiketResponse
import com.febrivio.sumbercomplang.network.ApiClient
import com.febrivio.sumbercomplang.network.ApiClient.ApiServiceAuth
import com.febrivio.sumbercomplang.services.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ScanTiketActivity : AppCompatActivity() {
    private lateinit var b: ActivityScanBinding
    private lateinit var cameraExecutor: ExecutorService
    private var lastToastTime = 0L
    private var flashEnabled = false
    private var camera: Camera? = null
    private var scanAreaRect = RectF()
    private var isProcessingQR = false // Flag to prevent multiple detection events

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityScanBinding.inflate(layoutInflater)
        setContentView(b.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up flash button
        b.btnFlash.setOnClickListener {
            toggleFlash()
        }

        // Set up back button
        b.btnBack.setOnClickListener {
            finish()
        }

        // Get scan area bounds once layout is ready
        b.previewView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateScanAreaRect()
        }
    }

    private fun updateScanAreaRect() {
        // Get scan area coordinates relative to the preview
        val location = IntArray(2)
        b.scanArea.getLocationInWindow(location)

        val previewLocation = IntArray(2)
        b.previewView.getLocationInWindow(previewLocation)

        // Calculate the scan area rectangle relative to the preview
        scanAreaRect = RectF(
            location[0].toFloat() - previewLocation[0],
            location[1].toFloat() - previewLocation[1],
            (location[0] + b.scanArea.width).toFloat() - previewLocation[0],
            (location[1] + b.scanArea.height).toFloat() - previewLocation[1]
        )

        Log.d(TAG, "Scan Area updated: $scanAreaRect")

        // Start scan line animation after we have scan area bounds
        startScanLineAnimation()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(b.previewView.surfaceProvider)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeAnalyzer { qrResult, boundingBox ->
                    if (qrResult.isNotEmpty() && !isProcessingQR) {
                        if (isQrCodeInScanArea(boundingBox)) {
                            // Set flag to prevent multiple scans
                            isProcessingQR = true

                            // Handle successful scan
                            runOnUiThread {
                                // Visual feedback
                                showScanSuccess()
                                
                                // Send API request to validate the ticket
                                validateTicket(qrResult)
                            }
                        } else {
                            // Provide feedback that QR code is detected but not in scan area
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastToastTime > 5000) {  // Increased from 2000ms to 5000ms (5 seconds)
                                lastToastTime = currentTime
                                runOnUiThread {
                                    // Vibrate instead of Toast
                                    vibratePhone()
                                }
                            }
                        }
                    }
                })

                // Select back camera as default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showScanSuccess() {
        // Animate the scan area or show success indicator
        b.scanSuccessOverlay?.visibility = View.VISIBLE
        b.scanSuccessOverlay?.alpha = 0f
        b.scanSuccessOverlay?.animate()
            ?.alpha(1f)
            ?.setDuration(300)
            ?.start()
    }

    private fun isQrCodeInScanArea(boundingBox: Rect?): Boolean {
        if (boundingBox == null) return false

        // Simplest approach - always return true to make scanning work anywhere
        // This completely bypasses the coordinate mapping logic that's causing issues
        return true
    }

    private fun toggleFlash() {
        camera?.let {
            if (it.cameraInfo.hasFlashUnit()) {
                flashEnabled = !flashEnabled
                it.cameraControl.enableTorch(flashEnabled)

                // Update flash icon based on status
                if (flashEnabled) {
                    b.btnFlash.setImageResource(R.drawable.ic_flashlightoff_24)
                } else {
                    b.btnFlash.setImageResource(R.drawable.ic_flashlighton_24)
                }
            }
        }
    }

    private fun startScanLineAnimation() {
        // Wait for layout to be ready
        b.scanArea.post {
            // Get actual scan area height
            val scanAreaHeight = b.scanArea.height

            // Limit scan line movement to 90% of scan area height
            val maxDistance = (scanAreaHeight * 0.9).toFloat()

            val animator = ObjectAnimator.ofFloat(
                b.scanLine,
                "translationY",
                0f,
                maxDistance
            )

            animator.duration = 2000 // Faster animation (2 seconds)
            animator.repeatMode = ValueAnimator.REVERSE
            animator.repeatCount = ValueAnimator.INFINITE
            animator.start()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
 
    private fun validateTicket(orderId: String) {
        try {
            // Show loading indicator
            runOnUiThread {
                b.progressIndicator?.visibility = View.VISIBLE
                // If you don't have a progress indicator, add one to your layout
            }
            
            // Get the user token
            val token = SessionManager(this).getToken()
            val apiService = ApiServiceAuth(this, token)
            
            // First, fetch transaction details
            apiService.getTransaksiDetail(orderId).enqueue(object : Callback<TransaksiTiketResponse> {
                override fun onResponse(call: Call<TransaksiTiketResponse>, response: Response<TransaksiTiketResponse>) {
                    b.progressIndicator?.visibility = View.GONE
                    
                    if (response.isSuccessful && response.body() != null) {
                        val transactionResponse = response.body()!!
                        
                        // Check if we got valid data
                        if (transactionResponse.success && transactionResponse.data != null) {
                            // Vibrate for success feedback
                            vibratePhone()
                            
                            // Get transaction data
                            val transaksiData = transactionResponse.data
                            
                            // Navigate to ValidasiTiketActivity with the transaction data
                            val intent = Intent(this@ScanTiketActivity, ValidasiTiketActivity::class.java)
                            intent.putExtra("transaksi", transaksiData)
                            startActivity(intent)
                            
                            // Optionally finish this activity if you don't want to return to scanner
                            // finish()
                            
                            // Reset flag after delay
                            b.root.postDelayed({
                                isProcessingQR = false
                            }, 3000)
                        } else {
                            // Transaction data not valid
                            showErrorDialog("Invalid Transaction", transactionResponse.message)
                            isProcessingQR = false
                        }
                    } else {
                        // Handle error response
                        val errorMessage = try {
                            response.errorBody()?.string() ?: "Unknown error"
                        } catch (e: Exception) {
                            "Error reading response: ${e.message}"
                        }
                        
                        if (response.code() == 404) {
                            showErrorDialog("Invalid QR Code", "Tiket tidak ditemukan")
                        } else {
                            showErrorDialog("Server Error", "Error ${response.code()}: $errorMessage")
                        }
                        
                        isProcessingQR = false
                    }
                }
                
                override fun onFailure(call: Call<TransaksiTiketResponse>, t: Throwable) {
                    b.progressIndicator?.visibility = View.GONE
                    
                    // Error handling for network failures
                    Log.e(TAG, "Error fetching transaction details", t)
                    showErrorDialog("Network Error", "Gagal mengambil data transaksi: ${t.message}")
                    
                    // Reset processing flag
                    isProcessingQR = false
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in validateTicket", e)
            showErrorDialog("Application Error", "Terjadi kesalahan: ${e.message}")
            isProcessingQR = false
        }
    }
    
    private fun showErrorDialog(title: String, message: String) {
        try {
            val builder = AlertDialog.Builder(this)
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.dialog_error, null)
            
            val dialog = builder.setView(dialogView).create()
            
            // Set dialog properties
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            
            // Set title and message
            dialogView.findViewById<android.widget.TextView>(R.id.dialogTitle).text = title
            dialogView.findViewById<android.widget.TextView>(R.id.dialogMessage).text = message
            
            // Set OK button action
            dialogView.findViewById<android.widget.Button>(R.id.dialogButtonOk).setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error dialog", e)
            // If custom dialog fails, fall back to simple Toast
            try {
                Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Log.e(TAG, "Even Toast failed", e2)
            }
        }
    }

    private fun showSuccessDialog(title: String, message: String) {
        try {
            val builder = AlertDialog.Builder(this)
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.dialog_success, null)

            val dialog = builder.setView(dialogView).create()

            // Set dialog properties
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

            // Set title and message - menggunakan ID yang benar dari dialog_success.xml
            dialogView.findViewById<android.widget.TextView>(R.id.dialogTitleSuccess).text = title
            dialogView.findViewById<android.widget.TextView>(R.id.dialogMessageSuccess).text = message

            // Set OK button action - menggunakan ID yang benar dari dialog_success.xml
            dialogView.findViewById<android.widget.Button>(R.id.dialogButtonOkSuccess).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing success dialog", e)
            // If custom dialog fails, fall back to simple Toast
            try {
                Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Log.e(TAG, "Even Toast failed", e2)
            }
        }
    }
    
    private fun showTicketAlreadyValidatedDialog() {
        try {
            val builder = AlertDialog.Builder(this)
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.dialog_ticket_validated, null)
            
            val dialog = builder.setView(dialogView).create()
            
            // Set dialog properties
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            
            // Set OK button action
            dialogView.findViewById<android.widget.Button>(R.id.dialogButtonOk).setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing ticket already validated dialog", e)
            // Fallback tanpa Toast, langsung log saja
        }
    }
    
    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // For API 26 and above
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            // For API 25 and below
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    class QRCodeAnalyzer(private val onQRCodeFound: (String, Rect?) -> Unit) : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { value ->
                                onQRCodeFound(value, barcode.boundingBox)
                                Log.d("QRCodeAnalyzer", "Image size: ${image.width}x${image.height}")
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    companion object {
        private const val TAG = "ScanTiketActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}