package com.example.eyediseaseapp.util

import com.google.firebase.Timestamp

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false,
    val resultId: String? = null
)
