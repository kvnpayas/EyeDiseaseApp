package com.example.eyediseaseapp.util

import android.graphics.Bitmap
import android.util.Log
import android.content.Context
import android.widget.Toast
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
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
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.http.Header
import java.io.File
import java.lang.reflect.Type

interface UltralyticsApiService {
    @Multipart
    @POST("/")
    suspend fun classifyImage(
        @Header("x-api-key") apiKey: String,
        @Part model: MultipartBody.Part,  // Removed "model"
        @Part imgsz: MultipartBody.Part,  // Removed "imgsz"
        @Part conf: MultipartBody.Part,   // Removed "conf"
        @Part iou: MultipartBody.Part,    // Removed "iou"
        @Part file: MultipartBody.Part
    ): Response<UltralyticsApiResponse>
}

data class UltralyticsApiResponse(
    @SerializedName("images") val images: List<ImageResult>?
)

data class ImageResult(
    @SerializedName("results") val results: List<DetectionResult>?, // Changed name
    @SerializedName("shape") val shape: List<Int>?,
    @SerializedName("speed") val speed: Speed?
)

data class DetectionResult( // Renamed from Prediction
    @SerializedName("class") val classId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("box") val box: Box
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

class UltralyticsAPIHelper(private val context: Context) {
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


    suspend fun classifyImage(bitmap: Bitmap): List<DetectionResult> {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            val requestFile = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData(
                "file",
                "image.jpg",
                requestFile
            ) // Use "file"
            val modelPart =
                MultipartBody.Part.createFormData(
                    "model",
                    "https://hub.ultralytics.com/models/2MI9L4PD0ozKgixXhXEp"
                )
            val imgszPart = MultipartBody.Part.createFormData("imgsz", "640")
            val confPart =
                MultipartBody.Part.createFormData("conf", "0.25") // Use 0.25 or 0.75 as needed
            val iouPart = MultipartBody.Part.createFormData("iou", "0.45")

            val apiKey = "461adc14a1da30678afbc86355d309bff1a54ebb6d"

            val response = RetrofitClient.instance.classifyImage(
                apiKey = apiKey,
                model = modelPart,
                imgsz = imgszPart,
                conf = confPart,
                iou = iouPart,
                file = imagePart // Use "file"
            )
            Log.d("UltralyticsAPIHelpers", "API Response: ${response.body()}")
            if (response.isSuccessful) {
                val apiResponse = response.body()
                Log.d("UltralyticsAPIHelpers", "API Response: ${apiResponse}")

                if (apiResponse?.images?.isNotEmpty() == true && apiResponse.images[0].results != null) {
                    return apiResponse.images[0].results!!
                } else {
                    Log.e("UltralyticsAPIHelpers", "API response or detections is null")
                    return emptyList()
                }
            } else {
                Log.e(
                    "UltralyticsAPIHelpers",
                    "API request failed with code: ${response.code()}, error: ${
                        response.errorBody()?.string()
                    }"
                )
                return emptyList()
            }

        } catch (e: Exception) {
            Log.e("UltralyticsAPIHelpers", "Error classifying image", e)
            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            return emptyList()
        }
    }
}

object RetrofitClient {
    private const val BASE_URL =  "https://predict.ultralytics.com/" // Adjust if needed

    val instance: UltralyticsApiService by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        retrofit.create(UltralyticsApiService::class.java)
    }
}