package com.example.eyediseaseapp

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eyediseaseapp.util.PatientResult
import com.example.eyediseaseapp.util.ResultRepository
import com.example.eyediseaseapp.util.UserUtils
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import com.example.eyediseaseapp.util.NavigationUtils
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PatientConsultedDetailScreen(
    navController: NavController,
    patientId: String, // <-- Receive the patientId as a navigation argument
    modifier: Modifier = Modifier // Modifier from Scaffold padding if used
) {
    val auth = FirebaseAuth.getInstance()
    val currentUserId =
        auth.currentUser?.uid

    val resultRepository = remember { ResultRepository() } // Instance of ResultRepository
    val userRepository = remember { UserUtils() } // <-- Instance of UserRepository (or UserUtils)
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for fetching data

    // State for the list of consulted results for this patient
    val consultedResultsState = resultRepository.getConsultedResultsForPatient(patientId)
        .collectAsState(initial = emptyList())
    val consultedResults = consultedResultsState.value

    // State for fetching the patient's name for the header
    var patientName by remember { mutableStateOf<String?>(null) }
    var isLoadingName by remember { mutableStateOf(true) }
    var nameError by remember { mutableStateOf<String?>(null) }

    var currentUserRole by remember { mutableStateOf<String?>(null) }
    var isLoadingRole by remember { mutableStateOf(true) }
    var roleError by remember { mutableStateOf<String?>(null) }

    // State for showing Result Details Dialog
    var selectedResultForView by remember { mutableStateOf<PatientResult?>(null) }

    LaunchedEffect(patientId) { // Use patientId as the key
        Log.d("PatientConsultedDetail", "LaunchedEffect: Fetching patient name for UID: $patientId")
        isLoadingName = true
        nameError = null
        patientName = try {
            // Call UserUtils to get the patient's name
            userRepository.getUser(patientId)?.name
        } catch (e: Exception) {
            nameError = e.message ?: "Failed to load patient name."
            Log.e(
                "PatientConsultedDetail",
                "Error loading patient name for $patientId: ${e.message}",
                e
            )
            null
        } finally {
            isLoadingName = false
        }
        Log.d("PatientConsultedDetail", "Fetched patient name: $patientName for UID: $patientId")

        // Fetch current user role
        if (currentUserId != null) {
            isLoadingRole = true
            roleError = null
            currentUserRole = try {
                val destinationScreen = NavigationUtils.fetchUserRole(currentUserId)
                when (destinationScreen) {
                    Screen.PatientHome -> "user"
                    Screen.DoctorHome -> "admin"
                    else -> null
                }
            } catch (e: Exception) {
                roleError = e.message ?: "Failed to load user role."
                Log.e("PatientConsultedDetail", "Error fetching user role for $currentUserId: ${e.message}", e)
                null
            } finally {
                isLoadingRole = false
            }
            Log.d("PatientConsultedDetail", "Fetched current user role: $currentUserRole for UID: $currentUserId")
        } else {
            currentUserRole = null
            isLoadingRole = false
            roleError = "User not logged in."
            Log.d("PatientConsultedDetail", "Current User ID is null, cannot fetch role.")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val headerText = when {
            isLoadingName -> "Loading Patient..."
            nameError != null -> "Error loading name"
            patientName != null -> "${patientName}'s Consulted History" // Display patient's name
            else -> "Patient Consulted History" // Fallback
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.header_bg),
                contentDescription = "Header Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth
            )
            Text(
                text = headerText,
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


            // --- Loading, Empty, or Error State for Results ---
            // Note: The Flow handles its own loading state implicitly;
            // emptyList() as initial value means it starts empty until data arrives.
            if (consultedResults.isEmpty()) {
                // Check if name is still loading or there's a name error, or if results are expected
                if (isLoadingName || isLoadingRole) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading results...")
                } else if (nameError != null || roleError != null) {
                    val combinedError = listOfNotNull(nameError, roleError).joinToString("\n")
                    Text("Error loading data: $combinedError", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                } else {
                    // If name is loaded and no results, show empty message
                    Text(
                        "No consulted results found for this patient.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // --- Display the list of consulted results ---
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = consultedResults,
                        key = { it.documentId } // Use result documentId as the key
                    ) { result ->
                        // --- Reuse HistoryItem or create a dedicated ConsultedResultItem ---
                        // HistoryItem is suitable as it displays result details and has a View button
                        HistoryItem( // Assuming HistoryItem is accessible or copied here
                            result = result,
                            currentUserRole = currentUserRole,
                            onDeleteClick = {
                                // Decide if doctor can delete patient results.
                                // If yes, implement delete logic here (requires ResultRepository delete function).
                                Log.w(
                                    "PatientConsultedDetail",
                                    "Delete clicked for result ${result.documentId}. Delete functionality not implemented here."
                                )
                                // Example delete call (requires coroutineScope):
                                // coroutineScope.launch { resultRepository.deletePatientResult(result.documentId, result.storagePath) }
                            },
                            onViewClick = { clickedResult ->
                                // Show the ResultDetailsDialog for the selected result
                                selectedResultForView = clickedResult
                            },
                            onConsultClick = {
                                // Consult button shouldn't be shown here as these are already consulted results.
                                // The HistoryItem composable already handles hiding the Consult button if isConsultInitiated is true.
                                Log.d(
                                    "PatientConsultedDetail",
                                    "Consult clicked on an already consulted result. This button should be hidden."
                                )
                            }
                        )
                    }
                }
            }
        }

        // --- Result Details Dialog (Modal Popup) ---
        selectedResultForView?.let { resultToDisplay ->
            ResultDetailsDialog( // Assuming ResultDetailsDialog is accessible or copied here
                result = resultToDisplay,
                onDismiss = {
                    selectedResultForView = null // Close the dialog
                }
            )
        }

    }
}
