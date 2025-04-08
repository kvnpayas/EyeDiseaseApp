package com.example.eyediseaseapp

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.eyediseaseapp.ui.theme.EyeDiseaseAppTheme
import com.example.eyediseaseapp.util.DetectionResult
import com.example.eyediseaseapp.util.ImageClassifierHelper
import com.example.eyediseaseapp.util.UltralyticsAPIHelper
import com.example.eyediseaseapp.util.generatePdf
import kotlinx.coroutines.delay
import java.io.IOException

@Composable
fun ImageClassificationScreen(navController: NavController) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var results by remember { mutableStateOf<List<Float>>(emptyList()) }
    var bestConfidence by remember { mutableStateOf(0f) }
    var bestClassIndex by remember { mutableStateOf(-1) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageClassifierHelper = remember {
        try {
            ImageClassifierHelper(
                context,
                "eye_disease.tflite",
                3
            ) // Replace with your model name
        } catch (e: IOException) {
            Log.e("ImageClassifierHelper", "Error initializing TFLite model", e)
            null
        }
    }

    val ultralyticsAPIHelper = remember {
        UltralyticsAPIHelper(context)
    }
    var detections by remember { mutableStateOf<List<DetectionResult>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        imageUri = uri
    }

    fun getResultMessage(resultIndex: Int, confidence: Float): String {
        Log.d("detections", "Detections resultIndex: $resultIndex")
        Log.d("detections", "Detections Confidence: $confidence")
        val formattedConfidence = String.format("%.2f", confidence * 100)
        val classLabels = listOf("Cataract", "Glaucoma", "Normal")
        val className: String

        if (confidence * 100 < 60) {
            className = "Unknown"
        } else {
            className = classLabels.getOrNull(resultIndex) ?: "Unknown"
        }

        return when (className) {
            "Cataract" -> "Cataract"
            "Glaucoma" -> "Glaucoma"
            "Normal" -> "Normal"

            else -> className
        }
    }

    // LaunchedEffect to handle image processing
    LaunchedEffect(imageUri) {
        val currentImageUri = imageUri // Create a local immutable copy
        if (currentImageUri != null) {
            isLoading = true
            bestClassIndex = -1
            bestConfidence = 0f
            results = emptyList()

            var tempBitmap: Bitmap? = null
            try {
                if (Build.VERSION.SDK_INT < 28) {
                    tempBitmap =
                        MediaStore.Images.Media.getBitmap(
                            context.contentResolver,
                            currentImageUri
                        ) // Use the local copy
                } else {
                    val source = ImageDecoder.createSource(
                        context.contentResolver,
                        currentImageUri
                    ) // Use the local copy
                    tempBitmap = ImageDecoder.decodeBitmap(source)
                }
                bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    tempBitmap?.copy(Bitmap.Config.RGBA_F16, true)
                } else {
                    tempBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                }
                bitmap = tempBitmap
            } catch (e: IOException) {
                Log.e("ImageClassificationScreen", "Error decoding bitmap", e)
            }

            // Simulate processing delay (you can adjust or remove this)
            delay(5000)

            if (tempBitmap != null) {
                val ultralyticsResults = ultralyticsAPIHelper.classifyImage(bitmap!!)
                Log.d("ultralyticsResults", "ultralyticsResults: $ultralyticsResults")
                detections = ultralyticsResults
//                results = imageClassifierHelper?.classifyImage(bitmap!!) ?: emptyList()
                if (detections.isNotEmpty()) {
                    val mutableBitmap = tempBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = android.graphics.Canvas(mutableBitmap)
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.RED // Choose your desired color
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 5f // Adjust the stroke width as needed
                    }

                    detections.forEach { detection ->
                        val box = detection.box
                        val rect = android.graphics.RectF(box.x1, box.y1, box.x2, box.y2)
                        canvas.drawRect(rect, paint)
                    }
                    bitmap = mutableBitmap

                    var bestConf = 0f
                    var bestIndex = -1
                    detections.forEachIndexed { index, detection ->
                        if (detection.confidence > bestConf) {
                            val detect = detection.confidence
                            Log.d("detections", "Detections Confidence: $detect")
                            bestConf = detection.confidence
                            bestIndex = index
                        }
                    }
                    bestConfidence = bestConf
                    bestClassIndex = detections.getOrNull(bestIndex)?.classId ?: -1
                } else {
                    bestConfidence = 0f
                    bestClassIndex = -1
                }
//                Log.d("imageClassifierHelper", "imageClassifierHelper: $results")
//                if (results.isNotEmpty()) {
//                    bestClassIndex = results.indexOf(results.maxOrNull() ?: 0f)
//                    bestConfidence = results.maxOrNull() ?: 0f
//                }
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp), // Adjust the height as needed
            contentAlignment = Alignment.Center // Center the text inside the Box
        ) {
            Image(
                painter = painterResource(id = R.drawable.header_bg), // Use header_bg here
                contentDescription = "Header Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth // Changed to FillWidth
            )
            Text(
                text = "Upload Image",
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(top = 100.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.warning_icon),
                        contentDescription = "Warning Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "This app provides preliminary assessments and is not a substitute for professional medical diagnosis. Results may not be 100% accurate. Consult with a qualified ophthalmologist for accurate diagnosis and treatment.",
                        color = colorResource(id = R.color.darkPrimary),
                        fontSize = 12.sp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .border(
                            width = 1.dp,
                            color = colorResource(id = R.color.darkPrimary),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .padding(16.dp), // Add padding inside the box
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ophthalmology_icon), // Replace with your upload icon
                            contentDescription = "Upload Icon",
                            modifier = Modifier.size(24.dp) // Adjust size as needed
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = "Upload Image",
                            color = colorResource(id = R.color.darkPrimary),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                imageUri?.let {
                    AnimatedVisibility(visible = !isLoading) {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Upload Image",
                                modifier = Modifier
                                    .size(250.dp)
                                    .border(
                                        width = 1.dp,
                                        color = colorResource(id = R.color.darkPrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            )
                        }
                    }
                    AnimatedVisibility(visible = isLoading) {
                        LoadingAnimation()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (bestClassIndex != -1) {
                        val resultMessage = getResultMessage(bestClassIndex, bestConfidence)
                        val confidence = String.format("%.2f", bestConfidence * 100)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = colorResource(id = R.color.extraLightPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Preliminary Assessment",
                                    color = colorResource(id = R.color.darkPrimary),
                                    fontSize = 14.sp,
                                )
                                Spacer(modifier = Modifier.padding(2.dp))
                                Text(
                                    text = resultMessage,
                                    color = colorResource(id = R.color.darkPrimary),
                                    fontSize = 24.sp,
                                    style = TextStyle(fontWeight = FontWeight.ExtraBold),
                                )

                                if (bestConfidence * 100 > 60) {
                                    Spacer(modifier = Modifier.padding(2.dp))
                                    Text(
                                        text = "Confidence: $confidence%",
                                        color = colorResource(id = R.color.darkPrimary),
                                        fontSize = 14.sp,
                                    )
                                }
                                Spacer(modifier = Modifier.padding(10.dp))
                                if (resultMessage == "Cataract" || resultMessage == "Glaucoma") {
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
                                } else if (resultMessage == "Normal") {
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
                                if (resultMessage == "Cataract" || resultMessage == "Glaucoma") {
                                    if (bestConfidence * 100 > 60) {

                                        // Add mild, moderate, or severe selection here
//                                        var severity by remember { mutableStateOf("Mild") } // Default to mild
//
//                                        Spacer(modifier = Modifier.height(16.dp))
//
//                                        Row(
//                                            modifier = Modifier.fillMaxWidth(),
//                                            horizontalArrangement = Arrangement.SpaceEvenly
//                                        ) {
//                                            OutlinedButton(
//                                                onClick = { severity = "Mild" },
//                                                colors = ButtonDefaults.outlinedButtonColors(
//                                                    containerColor = if (severity == "Mild") colorResource(
//                                                        id = R.color.darkPrimary
//                                                    ) else Color.Transparent,
//                                                    contentColor = if (severity == "Mild") Color.White else Color.Black
//                                                ),
//                                                border = BorderStroke(1.dp, Color.Gray)
//                                            ) {
//                                                Text("Mild")
//                                            }
//
//                                            OutlinedButton(
//                                                onClick = { severity = "Moderate" },
//                                                colors = ButtonDefaults.outlinedButtonColors(
//                                                    containerColor = if (severity == "Moderate") colorResource(
//                                                        id = R.color.darkPrimary
//                                                    ) else Color.Transparent,
//                                                    contentColor = if (severity == "Moderate") Color.White else Color.Black
//                                                ),
//                                                border = BorderStroke(1.dp, Color.Gray)
//                                            ) {
//                                                Text("Moderate")
//                                            }
//
//                                            OutlinedButton(
//                                                onClick = { severity = "Severe" },
//                                                colors = ButtonDefaults.outlinedButtonColors(
//                                                    containerColor = if (severity == "Severe") colorResource(
//                                                        id = R.color.darkPrimary
//                                                    ) else Color.Transparent,
//                                                    contentColor = if (severity == "Severe") Color.White else Color.Black
//                                                ),
//                                                border = BorderStroke(1.dp, Color.Gray)
//                                            ) {
//                                                Text("Severe")
//                                            }
//                                        }


                                        Spacer(modifier = Modifier.height(32.dp))
                                        Button(
                                            onClick = {
                                                generatePdf(
                                                    context,
                                                    bitmap,
                                                    resultMessage,
                                                    bestConfidence,
                                                )
                                            },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        ) {
                                            Text("Download PDF")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isLoading) {
                        Text(text = "Loading...")
                    } else {
                        Text(text = "No results")
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    Box(contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.eyeball_loading),
            contentDescription = "Loading",
            modifier = Modifier
                .size(150.dp)
                .rotate(angle)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ImageClassificationScreenPreview() {
    EyeDiseaseAppTheme {
        ImageClassificationScreen(navController = rememberNavController())
    }
}