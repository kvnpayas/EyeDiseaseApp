package com.example.eyediseaseapp.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions

// Data class for basic user profile (can expand later)
data class UserProfile(
    val userId: String = "",
    val email: String? = null,
    val role: String = "user",
    val name: String? = null,
    val createdAt: Timestamp = Timestamp.now()
)


class UserUtils {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createUserDocument(
        userId: String,
        email: String? = null,
        name: String? = null,
        initialRole: String = "user"
    ) {
        val userRef = firestore.collection("users").document(userId)

         try {
            Log.d("UserUtils", "Attempting to create/update user doc for UID: $userId")


            val existingDoc = userRef.get().await()
            val existingProfile = if (existingDoc.exists()) existingDoc.toObject(UserProfile::class.java) else null
            val existingRole = existingProfile?.role


            val roleToSave = if (existingRole != null && existingRole != "user") {
                existingRole
            } else {
                initialRole
            }


            val userProfileToSave = UserProfile(
                userId = userId,
                email = email,
                name = name,
                role = roleToSave,
                createdAt = existingProfile?.createdAt ?: Timestamp.now()
            )

            userRef.set(userProfileToSave, SetOptions.merge()).await()

            Log.d("UserUtils", "Successfully created/updated user doc for UID: $userId with role: $roleToSave")

        } catch (e: Exception) {
            Log.e("UserUtils", "Error creating/updating user doc for UID $userId: ${e.message}", e)
            throw e
        }
    }

    suspend fun getUserName(userId: String): String? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                userDoc.getString("name")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MessageRepo", "Error fetching user name for UID $userId: ${e.message}", e)
            null
        }
    }

    suspend fun getUser(userId: String): UserProfile? {
        val userDocRef = firestore.collection("users").document(userId)

        return try {
            Log.d("UserUtils", "Fetching user document for UID: $userId")
            val document = userDocRef.get().await()

            if (document.exists()) {

                val userProfile = document.toObject(UserProfile::class.java)
                Log.d("UserUtils", "User document fetched successfully for UID: $userId")
                userProfile
            } else {
                Log.d("UserUtils", "User document not found for UID: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e("UserUtils", "Error fetching user document for UID $userId: ${e.message}", e)

            null
        }
    }

}
