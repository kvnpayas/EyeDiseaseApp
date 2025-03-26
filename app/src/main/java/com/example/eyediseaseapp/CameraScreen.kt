package com.example.eyediseaseapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.min

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    // State to track if the camera permission is granted
    var hasCamPermission by remember { mutableStateOf(false) }

    // Launcher to request camera permission
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCamPermission = granted
        }
    )

    // Check for permission and launch the request if needed
    LaunchedEffect(key1 = true) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            hasCamPermission = true
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Display the camera view if permission is granted, otherwise show a message
    if (hasCamPermission) {
        CameraView()
    } else {
        Text(text = "Camera permission denied")
    }
}

@Composable
fun CameraView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var preview by remember { mutableStateOf<Preview?>(null) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? = null
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var faceBoundingBoxes by remember { mutableStateOf<List<RectF>>(emptyList()) } // State to hold bounding boxes
    val imageAspectRatio = 4f / 3f // Default aspect ratio, adjust if needed

    val mediaPipeFaceMesh = remember { MediaPipeFaceMesh(context) }
    val coroutineScope = rememberCoroutineScope() // For launching coroutines

    var lastBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lastLeftEyeBox by remember { mutableStateOf<RectF?>(null) }
    var lastRightEyeBox by remember { mutableStateOf<RectF?>(null) }

    mediaPipeFaceMesh.listener = object : MediaPipeFaceMesh.FaceMeshListener {
        override fun onFaceMeshResult(bitmap: Bitmap, leftEyeBox: RectF?, rightEyeBox: RectF?) {
            lastBitmap = bitmap
            lastLeftEyeBox = leftEyeBox
            lastRightEyeBox = rightEyeBox
        }
    }


    val takePicture = {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis())
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture?.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri: Uri? = outputFileResults.savedUri
                    Log.d("CameraView", "Original image saved to: $savedUri")

                    // Use the stored bitmap and eye boxes
                    if (lastBitmap != null && lastLeftEyeBox != null) {
                        // Crop to the left eye
                        val croppedLeftEyeBitmap = cropBitmap(lastBitmap!!, lastLeftEyeBox!!)
                        saveCroppedBitmap(context, croppedLeftEyeBitmap, "left_eye_$name")
                    }

                    if (lastBitmap != null && lastRightEyeBox != null) {
                        // Crop to the right eye
                        val croppedRightEyeBitmap = cropBitmap(lastBitmap!!, lastRightEyeBox!!)
                        saveCroppedBitmap(context, croppedRightEyeBitmap, "right_eye_$name")
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraView", "Error saving image: ${exception.message}", exception)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.lightPrimary))
            .padding(bottom = 100.dp, top = 80.dp, start = 50.dp, end = 50.dp)
    ) {
        Column {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CATARACT AND GLAUCOMA",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Preliminary Diagnosis",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.ExtraBold),
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspectRatio) // Match camera preview aspect ratio
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val bitmap = imageProxy.toBitmap()
                                    mediaPipeFaceMesh.detect(bitmap, System.currentTimeMillis())
                                    imageProxy.close()
                                }
                            }
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()
                            imageCapture = ImageCapture.Builder().build()
                            preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("CameraView", "Error starting camera: ${e.message}", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Canvas to draw bounding boxes
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val paint = Paint().apply {
                        color = Color.Green
                        style = PaintingStyle.Stroke
                        strokeWidth = 4f
                    }
                    faceBoundingBoxes.forEach { rect ->
                        drawRect(
                            color = paint.color, // Use the color from your Paint object
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.width(), rect.height()),
                            style = Stroke(width = paint.strokeWidth) // Use the stroke width from your Paint object
                        )
                    }
                }
            }
        }

        // Capture button
        IconButton(
            onClick = { takePicture() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(80.dp)
                .border(4.dp, Color.White, CircleShape)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_camera_alt_24),
                contentDescription = "Take picture",
                modifier = Modifier.fillMaxSize(),
                tint = Color.White
            )
        }
    }
}

fun cropBitmap(bitmap: Bitmap, rect: RectF): Bitmap {
    val croppedRect = Rect(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
    // Ensure the cropping rectangle is within the bounds of the bitmap
    val safeLeft = max(0, croppedRect.left)
    val safeTop = max(0, croppedRect.top)
    val safeRight = min(bitmap.width, croppedRect.right)
    val safeBottom = min(bitmap.height, croppedRect.bottom)
    val safeWidth = safeRight - safeLeft
    val safeHeight = safeBottom - safeTop
    return if (safeWidth > 0 && safeHeight > 0) {
        Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeWidth, safeHeight)
    } else {
        // Return an empty bitmap or handle the error as needed
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}

fun saveCroppedBitmap(context: Context, bitmap: Bitmap, filename: String) {
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    }
    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
    }
}

// Placeholder function - replace with your actual logic
fun extractBoundingBoxesFromBitmap(bitmap: Bitmap): List<RectF> {
    // This function should extract the bounding box coordinates
    // from the bitmap or recalculate them based on the landmarks.
    // For demonstration, returning an empty list.
    return emptyList()

}