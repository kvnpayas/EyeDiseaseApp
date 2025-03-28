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
import kotlin.math.atan2
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
                drawIrisLandmarks(canvas, faceLandmarks, offsetX, offsetY)
                drawIrisBoundingBoxes (canvas, faceLandmarks, offsetX, offsetY)// Call the iris drawing function
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

    private fun drawIrisLandmarks(
        canvas: Canvas,
        faceLandmarks: List<NormalizedLandmark>,
        offsetX: Float,
        offsetY: Float
    ) {
        val irisPaint = Paint().apply {
            color = Color.BLUE
            strokeWidth = 10f
            style = Paint.Style.FILL
        }

        // Draw left iris landmarks
        LEFT_IRIS_LANDMARKS.forEach { index ->
            if (index < faceLandmarks.size) {
                val landmark = faceLandmarks[index]
                val x = landmark.x() * imageWidth * scaleFactor + offsetX
                val y = landmark.y() * imageHeight * scaleFactor + offsetY
                canvas.drawCircle(x, y, 8f, irisPaint) // Draw a circle for each iris landmark
            }
        }

        // Draw right iris landmarks
        RIGHT_IRIS_LANDMARKS.forEach { index ->
            if (index < faceLandmarks.size) {
                val landmark = faceLandmarks[index]
                val x = landmark.x() * imageWidth * scaleFactor + offsetX
                val y = landmark.y() * imageHeight * scaleFactor + offsetY
                canvas.drawCircle(x, y, 8f, irisPaint) // Draw a circle for each iris landmark
            }
        }
    }

    private fun drawIrisBoundingBoxes(
        canvas: Canvas,
        faceLandmarks: List<NormalizedLandmark>,
        offsetX: Float,
        offsetY: Float
    ) {
        val irisBoundingBoxPaint = Paint().apply {
            color = Color.CYAN
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        val leftIrisBox = calculateIrisBoundingBox(faceLandmarks, LEFT_IRIS_LANDMARKS, offsetX, offsetY)
        leftIrisBox?.let { canvas.drawRect(it, irisBoundingBoxPaint) }

        val rightIrisBox = calculateIrisBoundingBox(faceLandmarks, RIGHT_IRIS_LANDMARKS, offsetX, offsetY)
        rightIrisBox?.let { canvas.drawRect(it, irisBoundingBoxPaint) }
    }

    private fun calculateIrisBoundingBox(
        landmarks: List<NormalizedLandmark>,
        landmarkIndices: List<Int>,
        offsetX: Float,
        offsetY: Float
    ): RectF? {
        if (landmarkIndices.isEmpty() || landmarkIndices.size < 2) return null // Need at least 2 points

        val points = landmarkIndices.mapNotNull { index ->
            landmarks.getOrNull(index)?.let { landmark ->
                landmark.x() * imageWidth * scaleFactor + offsetX to landmark.y() * imageHeight * scaleFactor + offsetY
            }
        }

        if (points.size < 2) return null

        // Calculate center
        val centerX = points.map { it.first }.average().toFloat()
        val centerY = points.map { it.second }.average().toFloat()

        // Calculate bounding box dimensions
        var minX = points.minOf { it.first }
        var minY = points.minOf { it.second }
        var maxX = points.maxOf { it.first }
        var maxY = points.maxOf { it.second }

        // Calculate rotation angle (simplified)
        val leftPoint = points.minByOrNull { it.first } ?: return RectF(minX, minY, maxX, maxY) // Default if error
        val rightPoint = points.maxByOrNull { it.first } ?: return RectF(minX, minY, maxX, maxY)

        val deltaY = rightPoint.second - leftPoint.second
        val deltaX = rightPoint.first - leftPoint.first
        val angle = atan2(deltaY, deltaX) * 180 / Math.PI.toFloat()

        // Apply rotation (simplified - adjust as needed)
        val padding = 10f
        val width = (maxX - minX) + 2 * padding
        val height = (maxY - minY) + 2 * padding

        // Adjust for elliptical shape (more robust)
        val aspectRatio = 1.4f // Adjust as needed
        val adjustedWidth = maxOf(width, height * aspectRatio)
        val adjustedHeight = minOf(height, width / aspectRatio)

        val rotatedRect = RectF(
            centerX - adjustedWidth / 2,
            centerY - adjustedHeight / 2,
            centerX + adjustedWidth / 2,
            centerY + adjustedHeight / 2
        )

        // Note: A true rotation would require matrix transformations, which is more complex
        return rotatedRect
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
        private const val TAG = "Face Landmarker Overlay"
    }
}