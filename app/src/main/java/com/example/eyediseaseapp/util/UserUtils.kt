package com.example.eyediseaseapp.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp // Import Timestamp if you use it for creation time

// Data class for basic user profile (can expand later)
data class UserProfile(
    val userId: String = "",
    val email: String? = null,
    val role: String = "user", // Default role
    val createdAt: Timestamp = Timestamp.now() // Optional: timestamp of creation
)

// Repository class for user-related data operations (like creating the profile document)
class UserUtils {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createUserDocument(userId: String, email: String? = null) {
        val userRef = firestore.collection("users").document(userId)

        // Use the UserProfile data class for consistency
        val userProfile = UserProfile(
            userId = userId,
            email = email,
            role = "user", // Set default role
            createdAt = Timestamp.now()
        )

         try {
            Log.d("UserRepository", "Attempting to create user doc for UID: $userId")
            userRef.set(userProfile).await() // Create the document
            Log.d("UserRepository", "Successfully created user doc for UID: $userId")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error creating user doc for UID $userId: ${e.message}", e)
            throw e // Re-throw the exception
        }
    }

    // You could add other user-related functions here, e.g.,
    // suspend fun getUserProfile(userId: String): UserProfile? { ... }
    // suspend fun updateUserRole(userId: String, newRole: String) { ... }
}
