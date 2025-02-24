package com.example.eyediseaseapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eyediseaseapp.util.ImageClassifierHelper
import java.io.IOException

@Composable
fun ImageClassificationScreen(navController: NavController) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var results by remember { mutableStateOf<List<Float>>(emptyList()) }
    val context = LocalContext.current
    val imageClassifierHelper = remember {
        try {
            ImageClassifierHelper(context, "eye_diseased.tflite") // Replace with your model name
        } catch (e: IOException) {
            Log.e("ImageClassifierHelper", "Error initializing TFLite model", e)
            null
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) {
            Text("Select Image")
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
            bitmap = tempBitmap?.copy(Bitmap.Config.RGBA_F16, true)

            bitmap?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = "Selected Image")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    results = imageClassifierHelper?.classifyImage(it) ?: emptyList()
                }) {
                    Text("Classify Image")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Results: ${results.joinToString(", ")}")
            }
        }
    }
}