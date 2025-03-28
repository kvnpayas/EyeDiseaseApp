package com.example.eyediseaseapp.util

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import retrofit2.Response
import okhttp3.ResponseBody
import retrofit2.Converter
import java.io.File
import java.lang.reflect.Type

interface UltralyticsApiService {
    @Multipart
    @POST("/")
    suspend fun classifyImage(
        @Part image: MultipartBody.Part,
        @Query("model") model: String = "https://hub.ultralytics.com/models/KgnTTMy0x3ZyCCxmm70c",
        @Query("imgsz") imgsz: Int = 640,
        @Query("conf") conf: Float = 0.25f,
        @Query("iou") iou: Float = 0.45f,
        @Query("api_key") apiKey: String = "461adc14a1da30678afbc86355d309bff1a54ebb6d"
    ): Response<UltralyticsApiResponse>
}

data class UltralyticsApiResponse(
    @SerializedName("images") val images: List<ImageResult>?
)

data class ImageResult(
    @SerializedName("results") val results: List<Prediction>?,
    @SerializedName("shape") val shape: List<Int>?,
    @SerializedName("speed") val speed: Speed?
)

data class Prediction(
    @SerializedName("class") val classId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("box") val box: Box
)

data class Box(
    @SerializedName("x1") val x1: Float,
    @SerializedName("x2") val x2: Float,
    @SerializedName("y1") val y1: Float,
    @SerializedName("y2") val y2: Float
)

data class Speed(
    @SerializedName("inference") val inference: Double,
    @SerializedName("postprocess") val postprocess: Double,
    @SerializedName("preprocess") val preprocess: Double
)

class UltralyticsAPIHelper {
    private val baseUrl = "https://predict.ultralytics.com/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(UltralyticsApiService::class.java)


    suspend fun classifyImage(bitmap: Bitmap): List<Float> {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            Log.d("UltralyticsAPIHelper", "Image Byte Array Size: ${byteArray.size}")
            Log.d("UltralyticsAPIHelper", "Image Content Type: image/jpeg")
            val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)


            Log.d("UltralyticsAPIHelper", "API Request URL: ${baseUrl}")
            Log.d("UltralyticsAPIHelper", "API Request Model: https://hub.ultralytics.com/models/KgnTTMy0x3ZyCCxmm70c")


            val response = apiService.classifyImage(imagePart)
            Log.d("UltralyticsAPIHelper", "API Response Code: ${response.code()}")
            if (response.isSuccessful) {
                val apiResponse = response.body()
                Log.d("UltralyticsAPIHelper", "API Response: ${apiResponse}")

                if (apiResponse != null && apiResponse.images != null && apiResponse.images.isNotEmpty() && apiResponse.images[0].results != null) {
                    val predictions = apiResponse.images[0].results

                    val results = FloatArray(3) { 0f }.toMutableList()

                    predictions?.forEach { prediction ->
                        if (prediction.classId in 0..2) {
                            results[prediction.classId] = prediction.confidence
                        }
                    }
                    return results
                } else {
                    Log.e("UltralyticsAPIHelper", "API response or predictions is null")
                    return emptyList()
                }
            } else {
                Log.e("UltralyticsAPIHelper", "API request failed: ${response.errorBody()?.string()}")
                return emptyList()
            }
        } catch (e: Exception) {
            Log.e("UltralyticsAPIHelper", "Error classifying image", e)
            return emptyList()
        }
    }
}