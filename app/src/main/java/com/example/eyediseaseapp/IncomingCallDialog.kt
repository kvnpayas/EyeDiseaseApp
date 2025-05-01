package com.example.eyediseaseapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Material 3 components
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog // Use Dialog for a simple modal
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call // Import Call icon
import androidx.compose.material.icons.filled.CallEnd // Import CallEnd icon

// Assuming CallNotification data class is in data package
import com.example.eyediseaseapp.util.CallNotification


@Composable
fun IncomingCallDialog(
    callNotification: CallNotification, // The data for the incoming call
    onAccept: (CallNotification) -> Unit, // Lambda to call when "Accept" is clicked
    onReject: (CallNotification) -> Unit, // Lambda to call when "Reject" is clicked
    onDismiss: () -> Unit // Lambda to call when the dialog should be dismissed (e.g., by tapping outside)
) {
    // Use Dialog for a modal popup that floats above the current screen
    Dialog(onDismissRequest = onDismiss) {
        // The content inside the Dialog.
        Surface(
            modifier = Modifier
                .fillMaxWidth() // Fill width within the dialog window
                .padding(16.dp), // Add padding around the content
            shape = MaterialTheme.shapes.medium, // Use theme shapes for rounded corners
            color = MaterialTheme.colorScheme.surface // Use theme surface color
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp) // Padding inside the surface
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp) // Space between elements
            ) {
                // --- Call Icon ---
                Icon(
                    imageVector = Icons.Default.Call, // Use a relevant icon
                    contentDescription = "Incoming Call Icon",
                    modifier = Modifier.size(64.dp), // Adjust size as needed
                    tint = MaterialTheme.colorScheme.primary // Use theme primary color
                )

                // --- Title ---
                Text(
                    text = "Incoming Call",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // --- Caller Information (Doctor) ---
                // You might want to fetch the doctor's name here or pass it in if available
                // For now, just display Doctor ID or a generic message
                Text(
                    text = "From Doctor", // Or fetch doctor's name if available
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp)) // Space before buttons

                // --- Action Buttons (Accept and Reject) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly // Distribute buttons evenly
                ) {
                    // Reject Button
                    Button(
                        onClick = { onReject(callNotification) }, // Call reject lambda, passing the notification
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Red color for reject
                    ) {
                        Icon(imageVector = Icons.Default.CallEnd, contentDescription = "Reject Call")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }

                    // Accept Button
                    Button(
                        onClick = { onAccept(callNotification) }, // Call accept lambda, passing the notification
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) // Primary color for accept
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Accept Call")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Accept")
                    }
                }
            }
        }
    }
}

// --- Optional: Preview for the Dialog ---
/*
@Composable
fun PreviewIncomingCallDialog() {
    // Create a dummy CallNotification for preview
    val dummyNotification = CallNotification(
        patientId = "patient123",
        doctorId = "doctor456",
        channelName = "testchannel",
        rtcToken = "dummytoken",
        status = "calling"
    )
    IncomingCallDialog(
        callNotification = dummyNotification,
        onAccept = { notification -> println("Accepted call from ${notification.doctorId}") },
        onReject = { notification -> println("Rejected call from ${notification.doctorId}") },
        onDismiss = { println("Dialog dismissed") }
    )
}
*/