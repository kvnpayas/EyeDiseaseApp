package com.example.eyediseaseapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: FaceLandmarkerResult? = null
    private var boundingBoxPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    // Define the indices for the left and right iris landmarks
    private val LEFT_IRIS_LANDMARKS = listOf(474, 475, 476, 477)
    private val RIGHT_IRIS_LANDMARKS = listOf(469, 470, 471, 472)
    // Define indices for landmarks around the eyes (you might need to adjust these)
    private val LEFT_EYE_LANDMARKS = listOf(33, 133, 157, 158, 159, 160, 161, 246)
    private val RIGHT_EYE_LANDMARKS = listOf(263, 362, 384, 385, 386, 387, 388, 466)

    init {
        initPaints()
    }

    fun clear() {
        Log.d("OverlayView", "clear() called")
        results = null
        boundingBoxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        boundingBoxPaint.color = Color.GREEN
        boundingBoxPaint.strokeWidth = 5f
        boundingBoxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (results?.faceLandmarks().isNullOrEmpty()) {
            clear()
            return
        }

        results?.let { faceLandmarkerResult ->
            val scaledImageWidth = imageWidth * scaleFactor
            val scaledImageHeight = imageHeight * scaleFactor
            val offsetX = (width - scaledImageWidth) / 2f
            val offsetY = (height - scaledImageHeight) / 2f

            faceLandmarkerResult.faceLandmarks().forEach { faceLandmarks ->
                drawEyeBoundingBoxes(canvas, faceLandmarks, offsetX, offsetY)
            }
        }
    }

    private fun drawEyeBoundingBoxes(
        canvas: Canvas,
        faceLandmarks: List<NormalizedLandmark>,
        offsetX: Float,
        offsetY: Float
    ) {
        val leftEyeBox = calculateBoundingBox(faceLandmarks, LEFT_EYE_LANDMARKS, offsetX, offsetY)
        leftEyeBox?.let { canvas.drawRect(it, boundingBoxPaint) }

        val rightEyeBox = calculateBoundingBox(faceLandmarks, RIGHT_EYE_LANDMARKS, offsetX, offsetY)
        rightEyeBox?.let { canvas.drawRect(it, boundingBoxPaint) }

        // Draw Landmarks as points
        val pointPaint = Paint().apply {
            color = Color.YELLOW
            strokeWidth = 5f
            style = Paint.Style.FILL
        }

        LEFT_EYE_LANDMARKS.forEach { index ->
            if (index < faceLandmarks.size) {
                val landmark = faceLandmarks[index]
                val x = landmark.x() * imageWidth * scaleFactor + offsetX
                val y = landmark.y() * imageHeight * scaleFactor + offsetY
                canvas.drawPoint(x, y, pointPaint)
            }
        }

        RIGHT_EYE_LANDMARKS.forEach { index ->
            if (index < faceLandmarks.size) {
                val landmark = faceLandmarks[index]
                val x = landmark.x() * imageWidth * scaleFactor + offsetX
                val y = landmark.y() * imageHeight * scaleFactor + offsetY
                canvas.drawPoint(x, y, pointPaint)
            }
        }
    }

    private fun calculateBoundingBox(
        landmarks: List<NormalizedLandmark>,
        landmarkIndices: List<Int>,
        offsetX: Float,
        offsetY: Float
    ): RectF? {
        if (landmarkIndices.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (index in landmarkIndices) {
            if (index < landmarks.size) {
                val landmark = landmarks[index]
                val x = landmark.x() * imageWidth * scaleFactor + offsetX
                val y = landmark.y() * imageHeight * scaleFactor + offsetY
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)
            }
        }

        val padding = 50f
        return RectF(minX - padding, minY - padding, maxX + padding, maxY + padding)
    }

    fun setResults(
        faceLandmarkerResults: FaceLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = faceLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
        private const val TAG = "Face Landmarker Overlay"
    }
}