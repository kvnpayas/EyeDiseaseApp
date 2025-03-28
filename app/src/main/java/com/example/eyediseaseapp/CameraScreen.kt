package com.example.eyediseaseapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.LineBreak.Companion.Paragraph
import androidx.compose.ui.zIndex
import com.example.eyediseaseapp.ui.theme.EyeDiseaseAppTheme
import com.example.eyediseaseapp.util.ImageClassifierHelper
import com.example.eyediseaseapp.util.generatePdf
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.source.ByteArrayOutputStream
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName.Document
import com.itextpdf.kernel.pdf.PdfWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph


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
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val coroutineScope = rememberCoroutineScope()

    var capturedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var results by remember { mutableStateOf<List<Float>>(emptyList()) }
    var bestConfidence by remember { mutableStateOf(0f) }
    var bestClassIndex by remember { mutableStateOf(-1) }
    var isLoading by remember { mutableStateOf(false) }

    var showDetailsCard by remember { mutableStateOf(false) }
    var initState by remember { mutableStateOf(true) }
    var blurredBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showRecaptureButton by remember { mutableStateOf(false) }

    val classLabels = listOf("Normal", "Cataract", "Glaucoma")

    var faceBoundingBoxes by remember { mutableStateOf<List<RectF>>(emptyList()) } // State to hold bounding boxes
    val imageAspectRatio = 4f / 3f // Default aspect ratio, adjust if needed

    val mediaPipeFaceMesh = remember { MediaPipeFaceMesh(context) }

    var lastBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lastLeftEyeBox by remember { mutableStateOf<RectF?>(null) }
    var lastRightEyeBox by remember { mutableStateOf<RectF?>(null) }

    val imageClassifierHelper = remember {
        try {
            ImageClassifierHelper(
                context,
                "model_unquant.tflite",
                3
            )
        } catch (e: Exception) {
            Log.e("ImageClassifierHelper", "Error initializing TFLite model", e)
            null
        }
    }
    val localContext = LocalContext.current // Use a different name for the context variable
    val overlayView = remember {
        OverlayView(localContext, null).apply { // Use the new variable name
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }

    mediaPipeFaceMesh.listener = object : MediaPipeFaceMesh.FaceMeshListener {
        override fun onFaceMeshResult(
            bitmap: Bitmap,
            leftEyeBox: RectF?,
            rightEyeBox: RectF?,
            faceLandmarkerResult: FaceLandmarkerResult
        ) {
            lastBitmap = bitmap
            lastLeftEyeBox = leftEyeBox
            lastRightEyeBox = rightEyeBox

            if (faceLandmarkerResult.faceLandmarks().isNotEmpty()) {
                // Face detected, update results with a dummy value (e.g., 1.0f)
                results = listOf(1.0f) // Use a dummy float value to indicate face detected
            } else {
                // No face detected, clear results
                results = emptyList()
            }

            faceBoundingBoxes = listOfNotNull(leftEyeBox, rightEyeBox)
            overlayView.setResults(
                faceLandmarkerResult,
                bitmap.height,
                bitmap.width,
                RunningMode.LIVE_STREAM
            )
        }
    }

    val takePicture = {
        Log.d("CameraView", "imageCapture: $imageCapture")
        if (imageCapture != null) {
            imageCapture?.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = image.toBitmap() // Convert ImageProxy to Bitmap
                        val rotatedBitmap =
                            rotateBitmap(context, bitmap) // Rotate the bitmap if needed
                        image.close() // Close the ImageProxy

                        isLoading = true
                        bestClassIndex = -1
                        bestConfidence = 0f
                        results = emptyList()

                        coroutineScope.launch {
                            try {
                                capturedImageBitmap = rotatedBitmap
                                Log.d("result", "Resukls: $results")
                                delay(5000) // Simulate processing delay

                                if (imageClassifierHelper != null) {
                                    val classificationResults =
                                        imageClassifierHelper.classifyImage(rotatedBitmap)
                                    results = classificationResults
                                    bestClassIndex = classificationResults.indexOf(
                                        classificationResults.maxOrNull() ?: 0f
                                    )
                                    bestConfidence = classificationResults.maxOrNull() ?: 0f
                                } else {
                                    Log.e("CameraView", "ImageClassifierHelper is null")
                                }

                            } catch (e: Exception) {
                                Log.e("CameraView", "Error processing image: ${e.message}", e)
                            } finally {
                                isLoading = false
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(
                            "CameraView",
                            "Error capturing image: ${exception.message}",
                            exception
                        )
                    }
                }
            )
        } else {
            Log.e("CameraView", "imageCapture is null, cannot take picture.")
        }
    }
    Box(modifier = Modifier.fillMaxSize()) { // Use Box to overlay content
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.lightPrimary))
                    .padding(bottom = 100.dp, top = 80.dp, start = 50.dp, end = 50.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
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
                    val aspectRatio = 1f
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(colorResource(id = R.color.white))
                            .aspectRatio(aspectRatio)
                    ) {
                        if (initState) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        imageCapture = ImageCapture.Builder().build()

                                        preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                            Log.d("CameraView", "ImageAnalysis.Analyzer called")
                                            val bitmap = imageProxy.toBitmap()
                                            Log.d(
                                                "CameraView",
                                                "Calling mediaPipeFaceMesh.detect()"
                                            )
                                            mediaPipeFaceMesh.detect(
                                                bitmap,
                                                System.currentTimeMillis()
                                            )
                                            imageProxy.close()
                                        }

                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                imageCapture!!,
                                                imageAnalysis
                                            )
                                            Log.d(
                                                "CameraView",
                                                "captureImage1st: $imageCapture"
                                            )
                                        } catch (e: Exception) {
                                            Log.e(
                                                "CameraView",
                                                "Error starting camera: ${e.message}",
                                                e
                                            )
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            AndroidView(
                                { overlayView },
                                modifier = Modifier.fillMaxSize()
                            ) // Add OverlayView

                        }
                        if (capturedImageBitmap != null){
                            Column(modifier = Modifier.fillMaxSize()) {
                                AnimatedVisibility(visible = !isLoading) {
                                    capturedImageBitmap?.let { bitmap ->
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Captured Image",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize() // Assuming you want to fill the parent
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (initState) {
                        if (results.isEmpty()) { // Check if no face is detected
                            Text(
                                text = "No Eye Detected",
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 5.dp),
                                color = Color.Red,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else { // Face detected, show capture button
                            IconButton(

                                onClick = {
                                    Log.d("CameraView", "IconButton clicked")
                                    Log.d("CameraView", "captureImage: $imageCapture")
                                    takePicture()
                                    initState = false
                                          },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 5.dp)
                                    .size(80.dp)
                                    .zIndex(1f)
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
                    if (capturedImageBitmap != null) {
                        if(!isLoading) {
                            Button(
                                modifier = Modifier.padding(5.dp)
                                    .align(Alignment.CenterHorizontally),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(id = R.color.darkPrimary)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                onClick = {
                                    // Reset state variables
                                    capturedImageBitmap = null
                                    initState = true
                                    results = emptyList()
                                    bestClassIndex = -1
                                    bestConfidence = 0f
                                },
                            ) {
                                Text(
                                    text = "Capture again",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(fontWeight = FontWeight.ExtraBold),
                                )
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val className =
                                        classLabels.getOrNull(bestClassIndex) ?: "Unknown"
                                    val formattedConfidence =
                                        String.format("%.2f", bestConfidence * 100)

                                    Text(
                                        text = "Diagnosis: $className",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(id = R.color.darkPrimary)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Confidence: $formattedConfidence%",
                                        fontSize = 16.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        modifier = Modifier.padding(5.dp)
                                            .align(Alignment.CenterHorizontally),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorResource(id = R.color.darkPrimary)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        onClick = {
                                            showDetailsCard = true
                                            captureAndBlurScreenshot(context) { blurredBitmap = it }
                                        },
                                    ) {
                                        Text(
                                            text = "Show more details",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center,
                                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showDetailsCard,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)) + scaleIn(
                animationSpec = tween(
                    durationMillis = 500
                )
            ),
            exit = fadeOut(animationSpec = tween(durationMillis = 500)) + scaleOut(
                animationSpec = tween(
                    durationMillis = 500
                )
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),  // Add padding to make it "almost" full screen
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val className = classLabels.getOrNull(bestClassIndex) ?: "Unknown"
                    val formattedConfidence = String.format("%.2f", bestConfidence * 100)

                    Text(
                        text = "Preliminary Diagnosis: $className",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black // Or any color you prefer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Confidence: $formattedConfidence%",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )

                    if (className == "Cataract" || className == "Glaucoma") {
                        Text(
                            text = "For further evaluation, consult an ophthalmologist. Here are suggested clinics:",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.padding(6.dp))
                        Text(
                            text = "Gueco Optical",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = "Rizal St, Poblacion, Gerona, 2302",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = "Tarlac",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = "(045) 925 0303",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.padding(6.dp))
                        Text(
                            text = "Chu Eye Center, ENT & Optical Clinic",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = "M.H Del Pilar St, Paniqui, 2307",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = "Tarlac",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = "(045) 470 08260",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.padding(6.dp))
                        Text(
                            text = "Uycoco Optical Clinic",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = "127 Burgos St., Paniqui, 2307",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = "Tarlac",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = "0933 856 1668",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 16.sp,
                        )
                    } else if (className == "Normal") {
                        Text(
                            text = "If you are experiencing any eye discomfort, vision changes, or have any concerns about your eye health, it is always recommended to consult with a qualified ophthalmologist for a comprehensive eye examination.",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 14.sp,
                        )
                    } else {
                        Text(
                            text = "The uploaded image could not be clearly classified. This may be due to image quality, variations in eye appearance, or limitations in the analysis model. It is essential to consult an ophthalmologist for a thorough eye examination and diagnosis.",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 14.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            generatePdf(
                                context,
                                capturedImageBitmap,
                                classLabels.getOrNull(bestClassIndex) ?: "Unknown",
                                bestConfidence
                            )
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Download PDF")
                    }

                    Spacer(modifier = Modifier.height(32.dp)) // Add space before button

                    Button(
                        onClick = { showDetailsCard = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

fun rotateBitmap(context: Context, bitmap: Bitmap): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(90f) // Or any other rotation you need

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun captureAndBlurScreenshot(context: Context, onBitmapReady: (Bitmap?) -> Unit) {
    // Capture the current composable as a Bitmap
    // Apply blur using RenderScript or other algorithm
    // Return the blurred Bitmap using onBitmapReady
}

//fun generatePdf(context: Context, imageBitmap: Bitmap?, className: String, confidence: Float) {
//    val fileName = "diagnosis_report_${System.currentTimeMillis()}.pdf"
//    val filePath = File(
//        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
//        fileName
//    )
//
//    try {
//        val writer = PdfWriter(FileOutputStream(filePath))
//        val pdf = PdfDocument(writer)
//        val document = Document(pdf)
//
//        // Add image to PDF
//        if (imageBitmap != null) {
//            val stream = ByteArrayOutputStream()
//            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
//            val imageData = ImageDataFactory.create(stream.toByteArray())
//            val image = Image(imageData) // Correct Image creation
//            image.scaleToFit(500f, 500f)
//            document.add(image) // Correct usage
//        }
//
//        // Add text to PDF
//        document.add(Paragraph("Diagnosis: $className"))
//        document.add(Paragraph("Confidence: ${String.format("%.2f", confidence * 100)}%"))
//
//        document.close()
//
//        // Optionally, show a toast message to indicate the PDF has been saved
//        Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_SHORT).show()
//    } catch (e: IOException) {
//        e.printStackTrace()
//        Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
//    }
//}


@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    val hasCamPermission = true

    EyeDiseaseAppTheme {
        if (hasCamPermission) {
            CameraView()
        } else {
            Text(text = "Camera permission denied")
        }
    }
}

