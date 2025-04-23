package com.example.eyediseaseapp.util

import android.util.Log
import com.example.eyediseaseapp.Screen // Import your Screen sealed class
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await // For using await

object NavigationUtils {

    suspend fun userDocumentExists(userId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            Log.d("FirestoreDebug", "Checking doc existence for UID: $userId") // Log entry
            val docRef = db.collection("users").document(userId)
            val document = docRef.get().await() // <-- Execution might stop here if rules deny read

            val exists = document.exists()
            Log.d("FirestoreDebug", "Doc existence check result for $userId: $exists") // Log result
            exists
        } catch (e: Exception) {
            Log.e("FirestoreDebug", "Error checking doc existence for UID $userId: ${e.message}", e) // Log error
            false // Assume it doesn't exist or couldn't be verified
        }
    }

    suspend fun createUserDocument(userId: String, email: String? = null) { // Email is optional
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)

        val userData = hashMapOf<String, Any>(
            "role" to "user", // <-- Set the default role here
            // You can add other default fields if needed
            // "createdAt" to com.google.firebase.Timestamp.now()
        )

        // Add email if provided and not null
        if (email != null) {
            userData["email"] = email
        }


        return try {
            Log.d("FirestoreDebug", "Attempting to create doc for UID: $userId")
            userRef.set(userData).await() // Create the document for the new user
            println("FirestoreUtils: User document created for $userId with role 'user'")
        } catch (e: Exception) {
            Log.e("FirestoreDebug", "Error creating doc for UID $userId: ${e.message}", e)
            println("FirestoreUtils: Error creating user document for $userId: ${e.message}")
            throw e // Re-throw the exception so the caller knows it failed
        }
    }

    suspend fun fetchUserRole(userId: String): Screen? {
        val db = FirebaseFirestore.getInstance()
        return try {
            val docRef = db.collection("users").document(userId)
            val document = docRef.get().await()
            Log.d("FirestoreUtils", "Doc exists for $userId: ${document.exists()}") // Use a distinct tag or include UID
            if (document.exists()) {
                val role = document.getString("role")
                Log.d("FirestoreUtils", "Role for $userId: $role") // Use a distinct tag or include UID
                when (role) {
                    "admin" -> Screen.AdminDashboard
                    "user" -> Screen.PatientHome
                    else -> {
                        println("fetchUserRole: User $userId found, but role '$role' is unknown or missing.")
                        // If role is missing or invalid, treat as needing sign-in or default user?
                        // Returning SignIn guides them back, or default to PatientHome if that's safer
                        Screen.SignIn // Or Screen.PatientHome depending on desired fallback
                    }
                }
            } else {
                println("fetchUserRole: User document not found for UID: $userId. This user was authenticated but no profile exists.")
                // If the document doesn't exist, the user wasn't fully onboarded.
                Screen.SignIn // Require them to go through sign-up/onboarding again?
            }
        } catch (e: Exception) {
            println("fetchUserRole: Error fetching role for UID $userId: ${e.message}")
            e.printStackTrace()
            // On error reading DB, safest might be to go back to sign-in or an error screen
            Screen.SignIn
        }
    }
}
