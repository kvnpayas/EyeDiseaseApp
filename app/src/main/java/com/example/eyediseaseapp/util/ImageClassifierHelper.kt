package com.example.eyediseaseapp.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ImageClassifierHelper(private val context: Context, private val modelFileName: String) {

    private var tflite: Interpreter? = null
    private var inputImageWidth: Int = 0
    private var inputImageHeight: Int = 0
    private var modelInputSize: Int = 0

    init {
        try {
            Log.d("ImageClassifierHelper", "Loading model: $modelFileName")
            val tfliteModel = loadModelFile(modelFileName)
            tflite = Interpreter(tfliteModel)
            val inputShape = tflite?.getInputTensor(0)?.shape()
            Log.d("ImageClassifierHelper", "Input Shape: ${inputShape?.contentToString()}")
            inputImageWidth = inputShape?.get(1) ?: 0
            inputImageHeight = inputShape?.get(2) ?: 0
            modelInputSize = inputImageWidth * inputImageHeight * 3 * 4
            Log.d("ImageClassifierHelper", "Model loaded successfully")
        } catch (e: IOException) {
            Log.e("ImageClassifierHelper", "Error initializing TFLite model", e)
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelFileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classifyImage(bitmap: Bitmap): List<Float> {
        if (tflite == null) {
            Log.e("ImageClassifierHelper", "TFLite model not initialized")
            return emptyList()
        }
        Log.d("ImageClassifierHelper", "Classifying image")
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)
        Log.d("ImageClassifierHelper", "Bitmap converted to ByteBuffer")
        val output = Array(1) { FloatArray(1) }
        tflite?.run(byteBuffer, output)
        Log.d("ImageClassifierHelper", "Inference completed")
        return output[0].toList()
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixel = 0
        for (i in 0 until inputImageWidth) {
            for (j in 0 until inputImageHeight) {
                val input = intValues[pixel++]

                byteBuffer.putFloat(((input shr 16 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((input shr 8 and 0xFF) / 255.0f))
                byteBuffer.putFloat(((input and 0xFF) / 255.0f))
            }
        }
        return byteBuffer
    }
}