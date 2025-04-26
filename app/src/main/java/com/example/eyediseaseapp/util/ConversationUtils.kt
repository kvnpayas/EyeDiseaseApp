package com.example.eyediseaseapp.util

import com.google.firebase.Timestamp

data class Conversation(

    val patientId: String = "",
    val doctorId: String = "",
    val patientName: String? = null,
    val doctorName: String? = null,
    val lastSenderName: String? = null,
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val lastMessageText: String? = null,
    val initiatedResultId: String? = null
)