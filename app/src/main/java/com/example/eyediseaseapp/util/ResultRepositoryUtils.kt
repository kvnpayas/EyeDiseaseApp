package com.example.eyediseaseapp.util

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID


data class PatientResult(
    var documentId: String = "",

    val userId: String = "",
    val patientName: String? = null,
    val result: String = "",
    val confidence: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val imageUrl: String = "",
    val storagePath: String = "",

    val conversationId: String? = null,
    val consult: Boolean = false
)

data class ConsultedPatient(
    val userId: String,
    val patientName: String?
)

// Repository class to handle data operations
class ResultRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val userUtils = UserUtils()


    suspend fun savePatientResult(
        bitmap: Bitmap,
        result: String,
        confidence: Double,
        patientName: String? = null
    ): PatientResult {
        // Ensure user is authenticated
        val userId = auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated. Cannot save result.")

        // Generate a unique ID for this specific result entry
        val resultId = UUID.randomUUID().toString()

        // Use the current timestamp
        val timestamp = Timestamp.now()

        // 1. Upload Image to Cloud Storage
        val storageRef = storage.reference
        // Define the path in Cloud Storage (e.g., results/<user_id>/<timestamp>_<result_id>.jpg)
        val imageFileName = "results/${userId}/${timestamp.toDate().time}_${resultId}.jpg"
        val imageRef = storageRef.child(imageFileName)


        val baos = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val imageData = baos.toByteArray()

        Log.d("ResultRepository", "Starting image upload for UID: $userId, Result ID: $resultId")

        return try {
            // Upload the image data
            val uploadTask = imageRef.putBytes(imageData).await()
            Log.d("ResultRepository", "Image uploaded successfully to ${uploadTask.metadata?.path}")

            // Get the download URL of the uploaded image
            val imageUrl = imageRef.downloadUrl.await().toString()
            Log.d("ResultRepository", "Image download URL: $imageUrl")

            // 2. Save Metadata to Firestore
            val patientResult = PatientResult(
                userId = userId,
                result = result,
                confidence = confidence,
                timestamp = timestamp,
                imageUrl = imageUrl,

            )

            // Save the result metadata in a collection (e.g., "patient_results")
            // Using resultId as the document ID
            val resultDocRef = firestore.collection("patient_results").document(resultId)

            Log.d(
                "ResultRepository",
                "Saving result metadata to Firestore for Result ID: $resultId"
            )
            // Use set() to create or overwrite the document
            resultDocRef.set(patientResult).await()
            Log.d("ResultRepository", "Result metadata saved successfully for Result ID: $resultId")

            // Return the successfully saved data object
            patientResult

        } catch (e: Exception) {
            Log.e(
                "ResultRepository",
                "Failed to save patient result for UID $userId, Result ID $resultId: ${e.message}",
                e
            )
            throw e
        }
    }

    suspend fun updatePatientResultConsultStatus(resultId: String, conversationId: String) {
        val resultDocRef = firestore.collection("patient_results").document(resultId)

         try {
            Log.d("ResultRepo", "Updating consult status for Result ID: $resultId with Conversation ID: $conversationId")
            resultDocRef.update(
                mapOf(
                    "consult" to true,
                    "conversationId" to conversationId
                )
            ).await()
            Log.d("ResultRepo", "Consult status updated successfully for Result ID: $resultId")
        } catch (e: Exception) {
            Log.e("ResultRepo", "Failed to update consult status for Result ID $resultId: ${e.message}", e)
            throw e
        }
    }

    suspend fun getResultById(resultId: String): PatientResult? {
        val resultDocRef = firestore.collection("patient_results").document(resultId)

        return try {
            Log.d("ResultRepo", "Fetching result by ID: $resultId")
            val document = resultDocRef.get().await()

            if (document.exists()) {
                val result = document.toObject(PatientResult::class.java)?.copy(documentId = document.id) // Copy ID
                Log.d("ResultRepo", "Result fetched successfully for ID: $resultId")
                result
            } else {
                Log.d("ResultRepo", "Result document not found for ID: $resultId")
                null
            }
        } catch (e: Exception) {
            Log.e("ResultRepo", "Failed to fetch result by ID $resultId: ${e.message}", e)
            throw e // Re-throw the exception
        }
    }

    suspend fun getResultsForUser(userId: String): List<PatientResult> {
        val db = FirebaseFirestore.getInstance()
        return try {
            Log.d("ResultRepository", "Fetching results for UID: $userId")
            val querySnapshot = db.collection("patient_results")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val results = querySnapshot.documents.mapNotNull { document ->

                try {
                    document.toObject(PatientResult::class.java)?.apply {
                        documentId = document.id
                    }
                } catch (e: Exception) {
                    Log.e(
                        "ResultRepository",
                        "Failed to convert document ${document.id} to PatientResult: ${e.message}",
                        e
                    )
                    null
                }
            }
            Log.d("ResultRepository", "Fetched ${results.size} results for UID: $userId")
            results

        } catch (e: Exception) {
            Log.e("ResultRepository", "Error fetching results for UID $userId: ${e.message}", e)
            throw e
        }
    }

    suspend fun getConsultedPatients(): List<ConsultedPatient> {
        return try {
            Log.d("ResultRepo", "Fetching list of consulted patients...")
            // Query for all results where consultation was initiated
            val querySnapshot = firestore.collection("patient_results")
                .whereEqualTo("consult", true)
                .get()
                .await()

            // Group results by userId to get unique patients
            val uniqueUserIds = querySnapshot.documents.mapNotNull { it.getString("userId") }.distinct()

            Log.d("ResultRepo", "Found ${uniqueUserIds.size} unique user IDs with initiated consultations.")

            // For each unique userId, fetch their name from the 'users' collection
            val consultedPatientsList = mutableListOf<ConsultedPatient>()
            for (userId in uniqueUserIds) {
                // Use UserUtils to get the user's name
                val userName = userUtils.getUserName(userId)
                consultedPatientsList.add(ConsultedPatient(userId = userId, patientName = userName))
                Log.d("ResultRepo", "Fetched name for UID $userId: $userName")
            }

            Log.d("ResultRepo", "Finished fetching names for consulted patients.")
            consultedPatientsList

        } catch (e: Exception) {
            Log.e("ResultRepo", "Error fetching consulted patients list: ${e.message}", e)
            throw e
        }
    }


    fun getConsultedResultsForPatient(patientId: String): Flow<List<PatientResult>> = callbackFlow {
        val resultsQuery = firestore.collection("patient_results")
            .whereEqualTo("userId", patientId)
            .whereEqualTo("consult", true) // Filter only consulted results
            .orderBy("timestamp", Query.Direction.DESCENDING) // Order by most recent first

        val registration = resultsQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ResultRepo", "Listen failed for consulted results for patient $patientId.", e)
                trySend(emptyList())
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val results = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(PatientResult::class.java)?.copy(documentId = doc.id) // Copy document ID
                    } catch (e: Exception) {
                        Log.e("ResultRepo", "Failed to parse consulted result document ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                Log.d("ResultRepo", "Received ${results.size} consulted results for patient $patientId")
                trySend(results)
            } else {
                Log.w("ResultRepo", "Received null snapshot for consulted results for patient $patientId with no error.")
                trySend(emptyList())
            }
        }

        awaitClose {
            Log.d("ResultRepo", "Stopping snapshot listener for consulted results for patient $patientId")
            registration.remove()
        }
    }

    suspend fun deletePatientResult(documentId: String, storagePath: String) {
        val resultDocRef = firestore.collection("patient_results").document(documentId)
        val storageRef = storage.reference

        try {
            Log.d(
                "ResultRepository",
                "Starting deletion for Doc ID: $documentId, Path: $storagePath"
            )

            val imageFileRef = storageRef.child(storagePath)
            Log.d("ResultRepository", "Attempting to delete image: $storagePath")
            try {
                imageFileRef.delete().await() // This returns Unit
                Log.d("ResultRepository", "Image deleted successfully: $storagePath")
            } catch (e: Exception) {
                Log.e("ResultRepository", "Failed to delete image $storagePath: ${e.message}", e)

            }

            resultDocRef.delete().await()
            Log.d(
                "ResultRepository",
                "Result metadata deleted successfully for Doc ID: $documentId"
            )

        } catch (e: Exception) {
            Log.e(
                "ResultRepository",
                "Failed to delete patient result for Doc ID $documentId: ${e.message}",
                e
            )
            throw e
        }
    }

}