package com.example.eyediseaseapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.graphics.Bitmap
import android.util.Log

class FaceMeshOverlayView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var bitmap: Bitmap? = null
    private var leftEyeBox: RectF? = null
    private var rightEyeBox: RectF? = null
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    init {
        holder.addCallback(this)
    }

    fun updateResults(bitmap: Bitmap?, leftEyeBox: RectF?, rightEyeBox: RectF?) {
        this.bitmap = bitmap
        this.leftEyeBox = leftEyeBox
        this.rightEyeBox = rightEyeBox
        holder.lockCanvas()?.let { canvas ->
            drawOnCanvas(canvas)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawOnCanvas(canvas: Canvas) {
        Log.d("FaceMeshOverlay", "Drawing on canvas.")
        canvas.drawColor(Color.TRANSPARENT)
        bitmap?.let {
            Log.d("FaceMeshOverlay", "Drawing bitmap.")
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        leftEyeBox?.let {
            Log.d("FaceMeshOverlay", "Drawing left eye box: $it")
            canvas.drawRect(it, paint)
        }
        rightEyeBox?.let {
            Log.d("FaceMeshOverlay", "Drawing right eye box: $it")
            canvas.drawRect(it, paint)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {}
}