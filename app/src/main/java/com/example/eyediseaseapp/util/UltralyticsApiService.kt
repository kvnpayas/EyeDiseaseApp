package com.example.eyediseaseapp.util

import android.content.Context
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
import java.io.File
import java.util.concurrent.TimeUnit
import retrofit2.Response
import java.net.MalformedURLException
import java.net.URL

interface UltralyticsApiService {
    @Multipart
    @POST("/")
    suspend fun classifyImage(
        @Part image: MultipartBody.Part,
        @Query("model") model: String,
        @Query("imgsz") imgsz: Int,
        @Query("conf") conf: Float,
        @Query("iou") iou: Float,
        @Query("api_key") apiKey: String
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
    private val modelUrl = "https://hub.ultralytics.com/models/KgnTTMy0x3ZyCCxmm70casdasdasdasdasdadasdasda"
    private val apiKey = "461adc14a1da30678afbc86355d309bff1a54ebb6ddasdasdsadasda"

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

    suspend fun classifyImage(context: Context, bitmap: Bitmap): List<Float> {
        return try {
            // URL Validation
            try {
                URL(baseUrl)
                URL(modelUrl)
            } catch (e: MalformedURLException) {
                Log.e("UltralyticsAPIHelper", "Invalid URL", e)
                return emptyList()
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            Log.d("UltralyticsAPIHelper", "Image Byte Array Size: ${byteArray.size}")

            val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)

            Log.d("UltralyticsAPIHelper", "API Request URL: $baseUrl")
            Log.d("UltralyticsAPIHelper", "API Model: $modelUrl")
            Log.d("UltralyticsAPIHelper", "Multipart file name: image.jpg")
            Log.d("UltralyticsAPIHelper", "Multipart content type: image/jpeg")
            Log.d("UltralyticsAPIHelper", "API Parameters: imgsz=640, conf=0.25, iou=0.45, apiKey=$apiKey")

            val response = apiService.classifyImage(imagePart, modelUrl, 640, 0.25f, 0.45f, apiKey)

            Log.d("UltralyticsAPIHelper", "API Response Code: ${response.code()}")
            Log.d("UltralyticsAPIHelper", "API Response Body: ${response.body()}")

            if (response.isSuccessful) {
                val apiResponse = response.body()

                if (apiResponse?.images?.isNotEmpty() == true && apiResponse.images[0].results != null) {
                    return apiResponse.images[0].results?.filter { it.classId in 0..2 }?.map { it.confidence } ?: emptyList()
                } else {
                    Log.e("UltralyticsAPIHelper", "API response or predictions is null or empty")
                    return emptyList()
                }
            } else {
                Log.e("UltralyticsAPIHelper", "API request failed with code: ${response.code()}, body: ${response.errorBody()?.string()}")
                return emptyList()
            }
        } catch (e: Exception) {
            Log.e("UltralyticsAPIHelper", "Error classifying image", e)
            return emptyList()
        }
    }
}