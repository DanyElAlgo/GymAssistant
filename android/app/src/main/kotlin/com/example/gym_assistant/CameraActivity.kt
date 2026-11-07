package com.example.gym_assistant

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONObject

/**
 * Native camera screen.
 * Pipeline: CameraX (RGBA frames) -> MediaPipe PoseLandmarker (Kotlin, LIVE_STREAM)
 *           -> backend.py via Chaquopy (angles / reps) -> native overlay + Flutter EventChannel.
 */
class CameraActivity : ComponentActivity() {
    companion object {
        private const val TAG = "GymPose"
        private const val CAMERA_PERMISSION_REQUEST = 10
    }

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val analysisBusy = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private var analyzedFrames = 0
    private var poseResults = 0
    private var lastTimestamp = 0L

    private lateinit var landmarkOverlay: LandmarkOverlay
    private lateinit var feedbackView: TextView
    private var poseLandmarker: PoseLandmarker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "CameraActivity created")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST
            )
        } else {
            startCamera()
        }
    }

    override fun onDestroy() {
        closed.set(true)
        cameraExecutor.shutdown()
        analysisExecutor.shutdown()
        poseLandmarker?.close()
        poseLandmarker = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            Log.e(TAG, "Camera permission denied")
            finish()
        }
    }

    private fun startCamera() {
        PoseAnalyzer.init(this)
        PoseAnalyzer.reset()

        poseLandmarker = PoseLandmarker.createFromOptions(
            this,
            PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath("pose_landmarker_lite.task")
                        .build()
                )
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener(::onPoseResult)
                .setErrorListener { error -> Log.e(TAG, "PoseLandmarker error", error) }
                .build()
        )

        val previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
        landmarkOverlay = LandmarkOverlay(this)
        feedbackView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(140, 0, 0, 0))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(32, 24, 32, 24)
            text = "Looking for a person…"
        }
        val container = FrameLayout(this).apply {
            addView(previewView, FrameLayout.LayoutParams(-1, -1))
            addView(landmarkOverlay, FrameLayout.LayoutParams(-1, -1))
            addView(
                feedbackView,
                FrameLayout.LayoutParams(-1, -2, Gravity.TOP)
            )
        }
        setContentView(container)

        // Preview and analysis must share an aspect ratio so the overlay's
        // FIT_CENTER mapping lines up with what PreviewView shows.
        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()

        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(cameraExecutor, ::analyzeFrame) }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            Log.d(TAG, "Front camera bound (RGBA_8888 analysis, 4:3)")
        }, ContextCompat.getMainExecutor(this))
    }

    /** Runs on cameraExecutor. Converts the frame to an upright bitmap and sends it to MediaPipe. */
    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            val landmarker = poseLandmarker ?: return
            if (closed.get()) return
            analyzedFrames++
            val rotation = imageProxy.imageInfo.rotationDegrees
            val upright = rotate(imageProxy.toBitmap(), rotation)
            if (analyzedFrames == 1 || analyzedFrames % 30 == 0) {
                Log.d(
                    TAG,
                    "Frame #$analyzedFrames: ${imageProxy.width}x${imageProxy.height} " +
                        "rotation=$rotation -> ${upright.width}x${upright.height}"
                )
            }
            // LIVE_STREAM requires strictly increasing timestamps.
            val timestamp = maxOf(SystemClock.uptimeMillis(), lastTimestamp + 1)
            lastTimestamp = timestamp
            landmarker.detectAsync(BitmapImageBuilder(upright).build(), timestamp)
        } catch (exception: Exception) {
            Log.e(TAG, "Frame processing failed", exception)
        } finally {
            imageProxy.close()
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Runs on MediaPipe's result thread. */
    private fun onPoseResult(result: PoseLandmarkerResult, input: MPImage) {
        if (closed.get()) return
        val landmarks = JSONArray()
        result.landmarks().firstOrNull().orEmpty().forEach { landmark ->
            landmarks.put(
                JSONObject()
                    .put("x", landmark.x().toDouble())
                    .put("y", landmark.y().toDouble())
                    .put("z", landmark.z().toDouble())
                    .put("visibility", landmark.visibility().orElse(1f).toDouble())
            )
        }
        val width = input.width
        val height = input.height

        // Draw immediately: landmarks are already in upright image coordinates.
        // Front camera frames are not mirrored, but the preview is, so mirror the overlay.
        landmarkOverlay.update(landmarks.toString(), width, height, 0, mirror = true)

        poseResults++
        if (poseResults == 1 || poseResults % 30 == 0) {
            Log.i(TAG, "Pose result #$poseResults: ${landmarks.length()} landmarks, image=${width}x$height")
        }

        // Python analysis runs off the MediaPipe thread. Drop results while Python is busy.
        if (!analysisBusy.compareAndSet(false, true)) return
        try {
            analysisExecutor.execute {
                try {
                    val analysis = PoseAnalyzer.analyze(landmarks.toString())
                    val payload = JSONObject()
                        .put("landmarks", landmarks)
                        .put("analysis", analysis)
                        .put("imageWidth", width)
                        .put("imageHeight", height)
                    runOnUiThread { showFeedback(analysis) }
                    PoseEventStream.emit(payload.toString())
                } catch (exception: Exception) {
                    Log.e(TAG, "Analysis failed", exception)
                } finally {
                    analysisBusy.set(false)
                }
            }
        } catch (rejected: java.util.concurrent.RejectedExecutionException) {
            analysisBusy.set(false) // Activity is closing.
        }
    }

    private fun showFeedback(analysis: JSONObject) {
        if (closed.get()) return
        if (analysis.has("error")) {
            feedbackView.text = "Analysis error: ${analysis.optString("error")}"
            return
        }
        val reps = analysis.optInt("reps", 0)
        val stage = analysis.optString("stage").takeUnless { it.isEmpty() || it == "null" } ?: "-"
        val feedback = analysis.optString("feedback")
        feedbackView.text = "Reps: $reps  ·  Stage: $stage\n$feedback"
    }
}
