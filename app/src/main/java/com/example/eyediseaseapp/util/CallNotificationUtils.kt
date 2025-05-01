package com.example.eyediseaseapp.util

import com.google.firebase.Timestamp // Import Firestore Timestamp

data class CallNotification(
    // Document ID will be the patientId to easily query for a specific patient's notification
    val patientId: String = "",
    val doctorId: String = "", // The UID of the doctor initiating the call
    val channelName: String = "", // The Agora channel name for this call
    val rtcToken: String = "", // The RTC token for the patient to join
    val timestamp: Timestamp = Timestamp.now(), // Timestamp of when the call was initiated
    val status: String = "calling" // Status of the call (e.g., "calling", "accepted", "ended")
)