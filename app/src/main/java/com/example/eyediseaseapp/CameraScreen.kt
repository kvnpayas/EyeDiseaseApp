package com.example.eyediseaseapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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

    // State to hold the camera preview
    var preview by remember { mutableStateOf<Preview?>(null) }

    // Get the camera provider
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // State to hold the image capture use case
    var imageCapture: ImageCapture? = null

    // Executor for camera operations
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Function to take a picture
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
                    Log.d("CameraView", "Image saved to: $savedUri")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraView", "Error saving image: ${exception.message}", exception)
                }
            }
        )
    }

    // Main layout for the camera screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.lightPrimary))
            .padding(bottom = 100.dp, top = 80 .dp, start = 50.dp, end = 50.dp)
    ) {
        // Camera preview
        Column(
//            modifier = Modifier.padding(100.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
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
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
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
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraView", "Error starting camera: ${e.message}", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
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