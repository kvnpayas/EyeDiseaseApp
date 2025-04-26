package com.example.eyediseaseapp

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import com.example.eyediseaseapp.util.ConsultedPatient

@Composable
fun PatientListsScreen(navController: NavController, modifier: Modifier = Modifier) {

    val resultRepository = remember { ResultRepository() } // Instance of ResultRepository
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for fetching data

    // State for the list of consulted patients
    var consultedPatients by remember { mutableStateOf<List<ConsultedPatient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) } // State for loading
    var errorMessage by remember { mutableStateOf<String?>(null) } // State for error messages

    // --- Fetch the list of consulted patients when the screen is first launched ---
    LaunchedEffect(Unit) { // Use Unit as the key to run this effect only once
        Log.d("PatientListScreen", "LaunchedEffect: Fetching consulted patients.")
        isLoading = true
        errorMessage = null
        try {
            // Call the repository function to get the list of unique patients who consulted
            consultedPatients = resultRepository.getConsultedPatients()
            Log.d("PatientListScreen", "Fetched ${consultedPatients.size} consulted patients.")
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load patient list."
            Log.e("PatientListScreen", "Error loading consulted patients: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                text = "List of Consulted Patients",
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

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading patients...")
            } else if (errorMessage != null) {
                Text("Error: ${errorMessage}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            } else if (consultedPatients.isEmpty()) {
                Text("No patients have initiated consultations yet.", textAlign = TextAlign.Center)
            } else {
                // --- Display the list of patients ---
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = consultedPatients,
                        key = { it.userId } // Use userId as the key
                    ) { patient ->
                        // --- Patient List Item ---
                        PatientListItem(
                            patient = patient,
                            onPatientClick = { selectedPatient ->
                                // Navigate to PatientConsultedDetailScreen, passing the userId
                                navController.navigate(Screen.PatientConsultedDetail.createRoute(selectedPatient.userId)) {
                                    // Optional: Pop up to this screen to avoid back stack issues?
                                    // popUpTo(Screen.PatientList.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun PatientListItem(
    patient: ConsultedPatient,
    onPatientClick: (ConsultedPatient) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPatientClick(patient) }, // Make the card clickable
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display patient name, use UID if name is null
            val displayName = patient.patientName ?: "Patient ID: ${patient.userId.take(6)}..."
            Text(
                text = displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f) // Allow text to take available space
            )
            Icon( // Optional: Add an arrow or indicator
                imageVector = Icons.Default.ArrowForward, // Requires material-icons-core
                contentDescription = "View Details"
            )
        }
    }
}