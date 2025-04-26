package com.example.eyediseaseapp

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import com.example.eyediseaseapp.util.MessageRepoUtils
import com.example.eyediseaseapp.util.PatientResult
import com.example.eyediseaseapp.util.ResultRepository
import com.example.eyediseaseapp.util.UserUtils
import com.example.eyediseaseapp.util.generatePdf
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.drawable.toBitmap
import coil3.toBitmap
import com.example.eyediseaseapp.util.NavigationUtils

@Composable
fun ResultHistoryScreen(navController: NavController, modifier: Modifier = Modifier) {
    // --- State Variables ---
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid // Get current user ID
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for fetching and deleting
    val resultRepository = remember { ResultRepository() } // Repository instance

    var resultsList by remember { mutableStateOf<List<PatientResult>>(emptyList()) } // State for the list of results
    var isHistoryLoading by remember { mutableStateOf(true) } // State for initial history loading
    var historyErrorMessage by remember { mutableStateOf<String?>(null) } // State for history loading error
    // Optional: State for deletion errors if you want to show feedback for failures
    // var deletionErrorMessage by remember { mutableStateOf<String?>(null) }
    var selectedResultForView by remember { mutableStateOf<PatientResult?>(null) }

    val messageRepository = remember { MessageRepoUtils() }

    val userRepository = remember { UserUtils() }
    var currentUserName by remember { mutableStateOf<String?>(null) } // Holds the current user's name
    var isLoadingUserName by remember { mutableStateOf(true) } // True while fetching the name
    var userNameError by remember { mutableStateOf<String?>(null) }

    // --- State for fetching current user's role ---
    var currentUserRole by remember { mutableStateOf<String?>(null) } // Holds the fetched role ('user', 'admin', or null)
    var isLoadingRole by remember { mutableStateOf(true) } // True while fetching the role
    var roleError by remember { mutableStateOf<String?>(null) }



    // --- Fetch Results on Compose ---
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            isHistoryLoading = true
            historyErrorMessage = null
            try {
                resultsList = resultRepository.getResultsForUser(currentUserId)
            } catch (e: Exception) {
                historyErrorMessage = e.message ?: "Failed to load history."
                Log.e("ResultHistory", "Error loading history: ${e.message}", e)
            } finally {
                isHistoryLoading = false
            }

            isLoadingUserName = true
            userNameError = null
            currentUserName = try {
                val userProfile = userRepository.getUser(currentUserId) // <-- Fetch user profile
                userProfile?.name // Get the name
            } catch (e: Exception) {
                userNameError = e.message ?: "Failed to load user name."
                Log.e(
                    "ResultHistory",
                    "Error fetching user name for $currentUserId: ${e.message}",
                    e
                )
                null
            } finally {
                isLoadingUserName = false
            }

            isLoadingRole = true
            roleError = null
            currentUserRole = try {
                val destinationScreen = NavigationUtils.fetchUserRole(currentUserId)
                when (destinationScreen) {
                    Screen.PatientHome -> "user"
                    Screen.DoctorHome -> "admin" // Use DoctorHomeScreen route name
                    else -> {
                        // If fetchUserRole returns SignIn or null, the role is not recognized or doc is missing
                        Log.w("ResultHistory", "Fetched unknown or missing role for $currentUserId, destination: $destinationScreen")
                        null // Treat as unknown role
                    }
                }
            } catch (e: Exception) {
                roleError = e.message ?: "Failed to load user role."
                Log.e("ResultHistory", "Error fetching user role for $currentUserId: ${e.message}", e)
                null
            } finally {
                isLoadingRole = false
            }

        } else {
            // Handle the case where the user is not logged in (shouldn't happen if NavGraph is set up correctly)
            historyErrorMessage = "User not logged in."
            resultsList = emptyList() // Clear the list
            isHistoryLoading = false

            currentUserName = null
            isLoadingUserName = false
            userNameError = "User not logged in."

            currentUserRole = null
            isLoadingRole = false
            roleError = "User not logged in."
            // Optionally navigate back to sign-in if user is unexpectedly null here
            // navController.navigate(Screen.SignIn.route) { popUpTo(Screen.AuthCheck.route) { inclusive = true } }
        }
    }

    Box(
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
                text = "Patient History",
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
                .padding(top = 180.dp)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isHistoryLoading || isLoadingUserName || isLoadingRole) { // Include name loading in overall loading
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading history...")
                }
            } else if (historyErrorMessage != null || userNameError != null || roleError != null) { // Include name error
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val combinedError = listOfNotNull(historyErrorMessage, userNameError, roleError).joinToString("\n")
                    Text("Error loading data: $combinedError", color = Color.Red, textAlign = TextAlign.Center)
                }
            } else if (resultsList.isEmpty() && !isHistoryLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("No results history found.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.weight(1f))
            } else {
                // --- Display the List of Results ---
                // LazyColumn is efficient for displaying a potentially long list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(), // Make LazyColumn fill the available space in the parent Column
                    contentPadding = PaddingValues(vertical = 8.dp), // Add padding around the list items content
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Add spacing between list items
                ) {
                    // Use items to generate list items from your resultsList
                    items(
                        items = resultsList, // Your list of PatientResult objects
                        key = { it.documentId } // Use the unique Firestore Document ID as the key
                    ) { result -> // 'result' is a single PatientResult object
                        // --- Composable for a Single History Item ---
                        HistoryItem(
                            result = result, // Pass the result object
                            currentUserRole = currentUserRole,
                            onDeleteClick = {
                                // --- Handle Delete Action ---
                                Log.d(
                                    "ResultHistory",
                                    "Delete clicked for Doc ID: ${result.documentId}"
                                )
                                // Trigger the delete operation
                                coroutineScope.launch {
                                    try {
                                        // Call the delete function from the repository
                                        resultRepository.deletePatientResult(
                                            result.documentId,
                                            result.storagePath
                                        )
                                        Log.d(
                                            "ResultHistory",
                                            "Deletion successful for Doc ID: ${result.documentId}"
                                        )

                                        // Update the local list state by removing the deleted item
                                        resultsList =
                                            resultsList.filter { it.documentId != result.documentId }
                                        // Optional: Show a success message like a Toast

                                    } catch (e: Exception) {
                                        // Handle deletion error (e.g., show a Toast or Snackbar)
                                        Log.e(
                                            "ResultHistory",
                                            "Error deleting result ${result.documentId}: ${e.message}",
                                            e
                                        )
                                        // Optionally update a deletion error state variable to show feedback
                                        // deletionErrorMessage = "Failed to delete result: ${e.message}"
                                        // If you use a Snackbar, launch it here
                                    }
                                }
                            },
                            onViewClick = { clickedResult -> // <-- Implement onViewClick
                                // Set the selected result state to show the dialog
                                selectedResultForView = clickedResult
                            },
                            // --- Implement onConsultClick ---
                            onConsultClick = { resultToConsult ->
                                Log.d(
                                    "ResultHistory",
                                    "Consult clicked for Doc ID: ${resultToConsult.documentId}"
                                )
                                val patientId = currentUserId // The current user is the patient

                                if (patientId != null && currentUserRole == "user") { // Ensure it's a patient clicking consult
                                    coroutineScope.launch {
                                        try {
                                            // 1. Initiate the conversation/send the first message
                                            val conversationId =
                                                messageRepository.initiateConsultation(
                                                    patientId = patientId,
                                                    initialMessageText = "Hello Doctor, I would like to consult about my result: ${resultToConsult.result} (Confidence: ${
                                                        String.format(
                                                            "%.2f",
                                                            resultToConsult.confidence * 100
                                                        )
                                                    }%). Result ID: ${resultToConsult.documentId}",
                                                    resultId = resultToConsult.documentId,
                                                    patientName = currentUserName,
                                                    lastSenderName = currentUserName,
                                                )

                                            Log.d(
                                                "ResultHistory",
                                                "Consultation initiated/message sent. Conversation ID: $conversationId"
                                            )

                                            // 2. Update the PatientResult document in Firestore
                                            // This call persists the isConsultInitiated = true state
                                            resultRepository.updatePatientResultConsultStatus(
                                                resultId = resultToConsult.documentId,
                                                conversationId = conversationId
                                            )
                                            Log.d(
                                                "ResultHistory",
                                                "PatientResult document updated in Firestore."
                                            )


                                            // 3. Update the local list state
                                            // This causes the UI to update immediately
                                            resultsList = resultsList.map {
                                                if (it.documentId == resultToConsult.documentId) {
                                                    it.copy(
                                                        consult = true,
                                                        conversationId = conversationId
                                                    )
                                                } else {
                                                    it
                                                }
                                            }
                                            Log.d("ResultHistory", "Local resultsList updated.")


                                            // 4. Navigate to the conversation screen
                                            navController.navigate(
                                                Screen.ConversationDetail.createRoute(
                                                    conversationId
                                                )
                                            ) {
                                                // Optional: Pop up to history screen to avoid back stack issues? Depends on desired flow.
                                                // popUpTo(Screen.ResultHistory.route) { inclusive = false } // Example
                                                launchSingleTop = true
                                            }

                                        } catch (e: Exception) {
                                            Log.e(
                                                "ResultHistory",
                                                "Failed to initiate consultation for ${resultToConsult.documentId}: ${e.message}",
                                                e
                                            )
                                        }
                                    }
                                } else {
                                    Log.w(
                                        "ResultHistory",
                                        "Consult clicked by non-patient user or user not logged in."
                                    )
                                    // Handle case where consult is clicked by admin or logged out user (shouldn't happen if UI is correct)
                                }
                            }
                        )

                    }
                }
            }
        }
    }
    selectedResultForView?.let { resultToDisplay ->
        ResultDetailsDialog(
            result = resultToDisplay, // Pass the selected result
            onDismiss = {
                // When the dialog is dismissed (e.g., by clicking outside),
                // set the state back to null to close the dialog
                selectedResultForView = null
            }
        )
    }

}

@Composable
fun ResultDetailsDialog(
    result: PatientResult, // The result data to display
    onDismiss: () -> Unit // Lambda to call when the dialog should be closed
) {
    val userUtils = remember { UserUtils() }
    var fetchedPatientName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(result.userId) { // Use result.userId as the key
        Log.d("ResultDetailsDialog", "LaunchedEffect: Fetching patient name for UID: ${result.userId}")
        fetchedPatientName = userUtils.getUserName(result.userId)
    }
    // Use Dialog for a simple modal popup
    Dialog(onDismissRequest = onDismiss) {
        // The content inside the Dialog. We'll reuse the design from ImageClassificationScreen.
        // Use a Surface or Card for background and elevation within the dialog.
        Surface(
            modifier = Modifier
                .fillMaxWidth(), // Fill width within the dialog window
            shape = RoundedCornerShape(8.dp), // Rounded corners for the dialog content
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface // Use theme surface color
        ) {

            Column(
                modifier = Modifier
                    .padding(16.dp) // Padding inside the surface
                    .verticalScroll(rememberScrollState()), // Make content scrollable
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                var isImageLoading by remember { mutableStateOf(true) }
                var imageLoadError by remember { mutableStateOf<String?>(null) }
                var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
                val context = LocalContext.current // Get the context
                val painter = rememberAsyncImagePainter( // Use rememberAsyncImagePainter
                    ImageRequest.Builder(context)
                        .data(result.imageUrl)
                        .crossfade(true)
                        .listener(
                            onStart = {
                                Log.d("CoilDebug", "Image load started for URL: ${result.imageUrl}")
                                isImageLoading = true
                                imageLoadError = null
                                loadedBitmap = null
                            },
                            onSuccess = { request: ImageRequest, successResult: SuccessResult ->
                                Log.d(
                                    "CoilDebug",
                                    "Image load successful for URL: ${result.imageUrl}"
                                )
                                isImageLoading = false
                                imageLoadError = null
                                loadedBitmap = successResult.image.toBitmap()
                            },
                            onError = { request: ImageRequest, errorResult: ErrorResult ->
                                Log.e(
                                    "CoilDebug",
                                    "Image load failed for URL: ${result.imageUrl}. Error: ${errorResult.throwable.message}",
                                    errorResult.throwable
                                )
                                isImageLoading = false
                                imageLoadError =
                                    errorResult.throwable.message ?: "Unknown image load error"
                                loadedBitmap = null
                            }
                        )
                        .build()
                )
                Image(
                    painter = painter,
                    contentDescription = "Saved Result Image",
                    modifier = Modifier
                        .size(250.dp) // Adjust size as needed
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)) // Optional border
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Display Classification Results Details ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = colorResource(id = R.color.extraLightPrimary),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Preliminary Assessment",
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.padding(2.dp))
                        Text(
                            text = result.result, // Display the saved result
                            color = colorResource(id = R.color.darkPrimary),
                            fontSize = 24.sp,
                            style = TextStyle(fontWeight = FontWeight.ExtraBold),
                        )

                        if (result.confidence > 0) {
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text(
                                text = "Confidence: ${
                                    String.format(
                                        "%.2f",
                                        result.confidence * 100
                                    )
                                }%",
                                color = colorResource(id = R.color.darkPrimary),
                                fontSize = 14.sp,
                            )
                        }

                        Spacer(modifier = Modifier.padding(10.dp))

                        if (result.result == "Cataract" || result.result == "Glaucoma") {
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


                        } else if (result.result == "Normal") {
                            Text(
                                text = "If you are experiencing any eye discomfort, vision changes, or have any concerns about your eye health, it is always recommended to consult with a qualified ophthalmologist for a comprehensive eye examination.",
                                color = colorResource(id = R.color.darkPrimary),
                                fontSize = 14.sp,
                            )
                        } else { // Unclassified or other result
                            Text(
                                text = "The uploaded image could not be clearly classified. This may be due to image quality, variations in eye appearance, or limitations in the analysis model. It is essential to consult an ophthalmologist for a thorough eye examination and diagnosis.",
                                color = colorResource(id = R.color.darkPrimary),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Call the generatePdf utility function
                        generatePdf(
                            context = context,
                            imageBitmap = loadedBitmap, // Pass the loaded bitmap (Bitmap?)
                            patientName = fetchedPatientName, // Pass the patient name from the result (String?)
                            className = result.result,
                            confidence = result.confidence.toFloat() // Convert Double to Float
                        )
                    },
                    enabled = loadedBitmap != null // Disable if image hasn't loaded
                ) {
                    Text("Download PDF")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray,
                        contentColor = Color.DarkGray,
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    result: PatientResult, // The result data for this item
    onDeleteClick: () -> Unit,
    currentUserRole: String?,// Lambda to call when delete button is clicked
    onViewClick: (PatientResult) -> Unit,
    onConsultClick: (PatientResult) -> Unit,
    modifier: Modifier = Modifier // Modifier for external customization
) {
    // Format the timestamp from Firestore Timestamp to a readable date/time string
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateString = try {
        // Timestamp.toDate() can sometimes fail if the timestamp is invalid/null default
        dateFormat.format(result.timestamp.toDate())
    } catch (e: Exception) {
        "Invalid Date" // Fallback for invalid timestamps
    }


    Column(
        modifier = modifier
            .fillMaxWidth() // Ensure item fills width
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)) // Add a border
            .padding(16.dp) // Add padding inside the item content
    ) {
        // --- Title: Date ---
        Text(
            text = dateString,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = colorResource(id = R.color.darkPrimary) // Example color
        )

        Spacer(modifier = Modifier.height(8.dp)) // Space after date

        // --- Result Details ---
        Text(
            text = "Result: ${result.result}",
            fontSize = 14.sp,
            color = Color.Black // Example color
        )
        // Display Confidence only if it makes sense (e.g., > 0)
        if (result.confidence > 0) {
            Text(
                text = "Confidence: ${String.format("%.2f", result.confidence * 100)}%",
                fontSize = 14.sp,
                color = Color.Black // Example color
            )
        }

        if (result.consult) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Consultation initiated",
                fontSize = 14.sp,
                color = colorResource(id = R.color.normalPrimary),
                fontStyle = FontStyle.Italic
            )
        }


        // --- Action Buttons (Remove, View) ---
        Spacer(modifier = Modifier.height(16.dp)) // Space before buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center // Align buttons to the end
        ) {
            // Remove Button
//            Button(
//                onClick = onDeleteClick, // Call the lambda passed from parent
//                // Optional: Customize button colors/shape
//                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Red) // Example: Use red for delete
//            ) {
//                Text("Remove")
//            }
//
//            Spacer(modifier = Modifier.width(8.dp)) // Space between buttons

            Button(
                onClick = { onViewClick(result) },
                modifier = Modifier.padding(1.dp),
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.lightPrimary),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "View",
                    fontSize = 12.sp
                )
            }

            if (currentUserRole == "user") {
                Spacer(modifier = Modifier.width(8.dp)) // Space between buttons

                // --- Consult Button (Only if not already initiated) ---
                if (!result.consult) {
                    Button(
                        onClick = { onConsultClick(result) },
                        modifier = Modifier.padding(1.dp),
                        shape = RoundedCornerShape(5.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.darkPrimary),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Consult",
                            fontSize = 12.sp
                        )
                    }
                } else {

                    Button(
                        onClick = { /* Do nothing when disabled */ },
                        modifier = Modifier.padding(1.dp),
                        shape = RoundedCornerShape(5.dp),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray,
                            contentColor = Color.DarkGray,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = "Consult",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}