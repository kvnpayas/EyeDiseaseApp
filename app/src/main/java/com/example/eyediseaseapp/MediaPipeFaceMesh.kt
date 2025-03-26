package com.example.eyediseaseapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import com.google.mediapipe.formats.proto.LandmarkProto.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class MediaPipeFaceMesh(private val context: Context) {
    private val TAG = "MediaPipeFaceMesh"
    private var faceLandmarker: FaceLandmarker? = null
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()
    var listener: FaceMeshListener? = null

    // Define the indices for the left and right iris landmarks
    private val LEFT_IRIS_LANDMARKS = listOf(474, 475, 476, 477)
    private val RIGHT_IRIS_LANDMARKS = listOf(469, 470, 471, 472)
    // Define indices for landmarks around the eyes (you might need to adjust these)
    private val LEFT_EYE_LANDMARKS = listOf(33, 133, 157, 158, 159, 160, 161, 246)
    private val RIGHT_EYE_LANDMARKS = listOf(263, 362, 384, 385, 386, 387, 388, 466)
    // Define indices for landmarks around the entire face (example)
    private val FACE_BOUNDING_BOX_LANDMARKS = listOf(10, 152, 234, 454) // Example: top, bottom, left, right

    init {
        setupFaceLandmarker()
    }

    private fun setupFaceLandmarker() {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .setDelegate(Delegate.GPU)
            val optionsBuilder = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMinFacePresenceConfidence(0.5f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener(this::returnLivestreamResult)
                .setErrorListener(this::returnLivestreamError)
            faceLandmarker = FaceLandmarker.createFromOptions(context, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            Log.e(TAG, "MediaPipe failed to load a model asset: $e")
        }
    }

    private fun calculateBoundingBox(landmarks: List<NormalizedLandmark>, landmarkIndices: List<Int>, canvasWidth: Int, canvasHeight: Int): RectF? {
        if (landmarkIndices.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (index in landmarkIndices) {
            if (index < landmarks.size) {
                val landmark = landmarks[index]
                val x = landmark.x() * canvasWidth // Access as function (likely available for NormalizedLandmark)
                val y = landmark.y() * canvasHeight // Access as function (likely available for NormalizedLandmark)
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)
            }
        }

        val rect = RectF(minX, minY, maxX, maxY)
        Log.d(TAG, "Calculated Bounding Box: $rect")
        return rect
    }

    private fun returnLivestreamResult(result: FaceLandmarkerResult, input: MPImage) {
        Log.d(TAG, "Face landmarker result: $result")
        val bitmap = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f

        var leftEyeBox: RectF? = null
        var rightEyeBox: RectF? = null

        if (result.faceLandmarks().isNotEmpty()) {
            val landmarks = result.faceLandmarks()[0] // Get the first (and likely only) face

            // Draw iris landmarks (existing code)
            paint.color = Color.GREEN
            for (index in LEFT_IRIS_LANDMARKS) {
                if (index < landmarks.size) {
                    val landmark = landmarks[index]
                    val x = landmark.x() * canvas.width
                    val y = landmark.y() * canvas.height
                    canvas.drawCircle(x, y, 10f, paint)
                }
            }
            for (index in RIGHT_IRIS_LANDMARKS) {
                if (index < landmarks.size) {
                    val landmark = landmarks[index]
                    val x = landmark.x() * canvas.width
                    val y = landmark.y() * canvas.height
                    canvas.drawCircle(x, y, 10f, paint)
                }
            }

            // Calculate bounding box around eyes
            paint.color = Color.BLUE
            leftEyeBox = calculateBoundingBox(landmarks, LEFT_EYE_LANDMARKS, canvas.width, canvas.height)
            leftEyeBox?.let { canvas.drawRect(it, paint) }
            rightEyeBox = calculateBoundingBox(landmarks, RIGHT_EYE_LANDMARKS, canvas.width, canvas.height)
            rightEyeBox?.let { canvas.drawRect(it, paint) }

            // Calculate bounding box around the entire face (optional)
            paint.color = Color.YELLOW
            val faceBox = calculateBoundingBox(landmarks, FACE_BOUNDING_BOX_LANDMARKS, canvas.width, canvas.height)
            faceBox?.let { canvas.drawRect(it, paint) }
        }
        listener?.onFaceMeshResult(bitmap, leftEyeBox, rightEyeBox)
    }

    private fun returnLivestreamError(error: RuntimeException) {
        Log.e(TAG, "Face landmarker error: $error")
    }

    fun detect(bitmap: Bitmap, time: Long) {
        if (faceLandmarker == null) {
            Log.e(TAG, "Face landmarker has not been initialized yet.")
            return
        }
        val mpImage = BitmapImageBuilder(bitmap).build()
        faceLandmarker?.detectAsync(mpImage, time)
    }

    interface FaceMeshListener {
        fun onFaceMeshResult(bitmap: Bitmap, leftEyeBox: RectF?, rightEyeBox: RectF?)
    }
}