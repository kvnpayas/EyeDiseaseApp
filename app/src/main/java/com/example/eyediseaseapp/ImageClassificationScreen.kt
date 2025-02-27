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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eyediseaseapp.util.ImageClassifierHelper
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
                "model_unquant.tflite",
                3
            ) // Replace with your model name
        } catch (e: IOException) {
            Log.e("ImageClassifierHelper", "Error initializing TFLite model", e)
            null
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        imageUri = uri
        // Reset results when a new image is selected
        results = emptyList()
        bestConfidence = 0f
        bestClassIndex = -1
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = Modifier.padding(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.darkPrimary)
            ),
            shape = RoundedCornerShape(10.dp),
            onClick = {
                // Reset results before launching the image picker
                results = emptyList()
                bestConfidence = 0f
                bestClassIndex = -1
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        ) {
            Text("Upload Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        imageUri?.let {
            var tempBitmap: Bitmap? = null
            if (Build.VERSION.SDK_INT < 28) {
                tempBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                tempBitmap = ImageDecoder.decodeBitmap(source)
            }
            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tempBitmap?.copy(Bitmap.Config.RGBA_F16, true)
            } else {
                tempBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            }

            bitmap?.let {
                AnimatedVisibility(visible = !isLoading) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Upload Image",
                        modifier = Modifier.size(250.dp)
                    )
                }
                AnimatedVisibility(visible = isLoading) {
                    LoadingAnimation()
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.padding(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.darkPrimary)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    onClick = {
                        isLoading = true
                        bestClassIndex = -1
                        bestConfidence = 0f
                        results = emptyList()
                    }
                ) {
                    Text("Scan Image")
                }
                LaunchedEffect(isLoading) {
                    if (isLoading) {
                        delay(5000)
                        results = imageClassifierHelper?.classifyImage(it) ?: emptyList()
                        if (results.isNotEmpty()) {
                            bestClassIndex = results.indexOf(results.maxOrNull() ?: 0f)
                            bestConfidence = results.maxOrNull() ?: 0f
                        }
                        isLoading = false
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (bestClassIndex != -1) {
                    val classLabels =
                        listOf(
                            "Normal",
                            "Cataract",
                            "Glaucoma"
                        ) // Replace with your actual class labels
                    Text(text = "Initial Result: ${classLabels[bestClassIndex]}")
                    Text(text = "Confidence: ${String.format("%.2f", bestConfidence * 100)}%")
                } else if (isLoading) {
                    Text(text = "Loading...")
                } else {
                    Text(text = "No results")
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