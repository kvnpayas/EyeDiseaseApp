package com.example.eyediseaseapp.util

import android.util.Log
import com.example.eyediseaseapp.Screen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object NavigationUtils {

    suspend fun userDocumentExists(userId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            Log.d("FirestoreDebug", "Checking doc existence for UID: $userId")
            val docRef = db.collection("users").document(userId)
            val document = docRef.get().await()

            val exists = document.exists()
            Log.d("FirestoreDebug", "Doc existence check result for $userId: $exists")
            exists
        } catch (e: Exception) {
            Log.e("FirestoreDebug", "Error checking doc existence for UID $userId: ${e.message}", e)
            false
        }
    }

    suspend fun fetchUserRole(userId: String): Screen? {
        val db = FirebaseFirestore.getInstance()
        return try {
            val docRef = db.collection("users").document(userId)
            val document = docRef.get().await()
            Log.d("FirestoreUtils", "Doc exists for $userId: ${document.exists()}")
            if (document.exists()) {
                val role = document.getString("role")
                Log.d("FirestoreUtils", "Role for $userId: $role")
                when (role) {
                    "admin" -> Screen.DoctorHome
                    "user" -> Screen.PatientHome
                    else -> {
                        println("fetchUserRole: User $userId found, but role '$role' is unknown or missing.")

                        Screen.SignIn
                    }
                }
            } else {
                println("fetchUserRole: User document not found for UID: $userId. This user was authenticated but no profile exists.")

                Screen.SignIn
            }
        } catch (e: Exception) {
            println("fetchUserRole: Error fetching role for UID $userId: ${e.message}")
            e.printStackTrace()

            Screen.SignIn
        }
    }
}
