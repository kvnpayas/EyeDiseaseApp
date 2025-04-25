package com.example.eyediseaseapp.util

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp // Firestore Timestamp type
import kotlinx.coroutines.tasks.await // For using await() on Firebase Tasks
import java.io.ByteArrayOutputStream
import java.util.UUID // To generate a unique ID for each result entry


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

// Repository class to handle data operations
class ResultRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /**
     * Saves the patient result metadata and image to Firebase.
     * Requires the user to be authenticated.
     *
     * @param bitmap The image bitmap (should be the one displayed, possibly with bounding boxes).
     * @param result The classification result message ("Cataract", "Glaucoma", etc.).
     * @param confidence The confidence level of the result.
     * @param patientName Optional: The name of the patient if different from the logged-in user.
     * @return The saved PatientResult object if successful.
     * @throws IllegalStateException if user is not authenticated.
     * @throws Exception if saving to Storage or Firestore fails.
     */
    suspend fun savePatientResult(
        bitmap: Bitmap,
        result: String,
        confidence: Double,
        patientName: String? = null // Add this parameter if you have a name input
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

        // Convert bitmap to byte array for upload
        val baos = ByteArrayOutputStream()
        // Use JPEG compression. Quality 90 is a good balance. Adjust if needed.
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
                patientName = patientName, // Use the provided patient name
                result = result,
                confidence = confidence,
                timestamp = timestamp,
                imageUrl = imageUrl,

                // Add bounding box data here if you have it structured
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
            // Log the error and re-throw it so the caller can handle the failure
            Log.e(
                "ResultRepository",
                "Failed to save patient result for UID $userId, Result ID $resultId: ${e.message}",
                e
            )
            // Optional: Clean up the partially uploaded image from Storage if Firestore save fails?
            // This adds complexity. For a basic implementation, just letting it fail might be ok initially.
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
            ).await() // Use update to modify specific fields
            Log.d("ResultRepo", "Consult status updated successfully for Result ID: $resultId")
        } catch (e: Exception) {
            Log.e("ResultRepo", "Failed to update consult status for Result ID $resultId: ${e.message}", e)
            throw e // Re-throw the exception
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


    // You might add other functions here, like getting results for a user
    // suspend fun getResultsForUser(userId: String): List<PatientResult> { ... }
}