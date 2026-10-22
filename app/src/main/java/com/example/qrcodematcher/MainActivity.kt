package com.example.qrcodematcher

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val isProcessingFrame = AtomicBoolean(false)
    private lateinit var scanner: BarcodeScanner
    private lateinit var previewView: PreviewView
    private lateinit var stepText: TextView
    private lateinit var statusText: TextView
    private lateinit var resetButton: Button
    private var firstValue: String? = null
    private var firstCodeRemoved = false
    private var comparisonFinished = false

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                statusText.text = "カメラの使用を許可してください。"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
        setContentView(createContentView())
        resetButton.setOnClickListener { resetComparison() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        scanner.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private fun createContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        root.addView(
            previewView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(16), dp(20), dp(20))
        }
        stepText = TextView(this).apply {
            text = "1つ目のQRコードを読み取ってください"
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        statusText = TextView(this).apply {
            text = "「KKNI」で始まる19文字または20文字のQRコードが対象です。"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(12))
        }
        resetButton = Button(this).apply {
            text = "最初からやり直す"
        }
        controls.addView(stepText)
        controls.addView(statusText)
        controls.addView(resetButton)
        root.addView(
            controls,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        return root
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { useCase ->
                            useCase.setAnalyzer(cameraExecutor, ::analyzeFrame)
                        }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (exception: Exception) {
                    statusText.text =
                        "カメラを起動できませんでした: ${exception.localizedMessage ?: exception.javaClass.simpleName}"
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !isProcessingFrame.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        scanner.process(
            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees),
        )
            .addOnSuccessListener { barcodes ->
                if (barcodes.isEmpty()) {
                    runOnUiThread {
                        if (firstValue != null && !comparisonFinished) {
                            firstCodeRemoved = true
                            statusText.text = "2つ目のQRコードを読み取ってください。"
                        }
                    }
                    return@addOnSuccessListener
                }
                val value = barcodes.firstNotNullOfOrNull { it.rawValue }
                if (value != null) {
                    runOnUiThread { handleScannedValue(value) }
                }
            }
            .addOnFailureListener {
                runOnUiThread { statusText.text = "QRコードを読み取れませんでした。もう一度お試しください。" }
            }
            .addOnCompleteListener {
                isProcessingFrame.set(false)
                imageProxy.close()
            }
    }

    private fun handleScannedValue(value: String) {
        if (comparisonFinished) return
        if (firstValue != null && !firstCodeRemoved) return
        if (!QrCodeValidator.isAccepted(value)) {
            statusText.text = "対象外です。「KKNI」で始まる19文字または20文字を読み取ってください。"
            return
        }

        val first = firstValue
        if (first == null) {
            firstValue = value
            firstCodeRemoved = false
            stepText.text = "2つ目のQRコードを読み取ってください"
            statusText.text = "1つ目を読み取りました。カメラを一度QRコードから外してください。"
            return
        }

        comparisonFinished = true
        stepText.text = if (first == value) "一致しました" else "一致しません"
        statusText.text = if (first == value) {
            "2つのQRコードのデータは完全に同じです。"
        } else {
            "2つのQRコードのデータは異なります。"
        }
    }

    private fun resetComparison() {
        firstValue = null
        firstCodeRemoved = false
        comparisonFinished = false
        stepText.text = "1つ目のQRコードを読み取ってください"
        statusText.text = "「KKNI」で始まる19文字または20文字のQRコードが対象です。"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
