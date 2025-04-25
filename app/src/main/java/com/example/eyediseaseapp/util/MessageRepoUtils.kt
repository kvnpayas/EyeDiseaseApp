package com.example.eyediseaseapp.util

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose // <-- Import this
import kotlinx.coroutines.flow.callbackFlow // <-- Import this

class MessageRepoUtils {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Replace with the actual UID of your doctor/admin user
    // You might store this in config or fetch it, but for a single admin, hardcoding temporarily is possible if you're careful
   val doctorAdminId = "TzrIcq1oAbcFSE8pVGFBZK6GISY2" // <-- **IMPORTANT: Replace with the actual UID of your doctor user**


    /**
     * Initiates a new conversation or sends the first message if conversation exists.
     * Called when the patient clicks the "Consult" button.
     *
     * @param patientId The UID of the patient.
     * @param initialMessageText The text of the first message (e.g., linking to the result).
     * @param resultId The ID of the result that triggered the consult.
     * @return The ID of the conversation (which is the patientId).
     * @throws Exception if the operation fails.
     */
    suspend fun initiateConsultation(
        patientId: String,
        initialMessageText: String,
        resultId: String
    ): String {
        val conversationRef = firestore.collection("conversations").document(patientId)

        // Check if conversation already exists
        val conversationDoc = conversationRef.get().await()

        val conversationId = patientId // Conversation ID is the patient's UID

        if (!conversationDoc.exists()) {
            // Conversation does not exist, create it
            Log.d("MessageRepo", "Creating new conversation for patient: $patientId")

            val newConversation = Conversation(
                patientId = patientId,
                doctorId = doctorAdminId,
                // patientName: You might fetch the patient's name from their user doc here
                lastMessageTimestamp = Timestamp.now(), // Will be updated by first message
                lastMessageText = null, // Will be updated by first message
                initiatedResultId = resultId
            )

            // Use set() to create the document with patientId as the ID
            conversationRef.set(newConversation).await()
            Log.d("MessageRepo", "Conversation document created: $conversationId")

        } else {
            Log.d("MessageRepo", "Conversation already exists for patient: $patientId")
            // Conversation exists, just proceed to send the message
        }

        // Send the first message
        // Use the dedicated sendMessage function to keep logic consistent
        sendMessage(
            conversationId = conversationId,
            senderId = patientId,
            text = initialMessageText,
            resultId = resultId // Link the first message to the result
        )

        // Optional: Update the PatientResult document to mark consultation initiated
        // This requires access to ResultRepository or similar logic here
        // For now, let's assume this update happens elsewhere after initiateConsultation succeeds.
        // You might return the conversationId and then update the PatientResult in ResultHistoryScreen.

        return conversationId // Return the conversation ID
    }


    /**
     * Sends a message within an existing conversation.
     *
     * @param conversationId The ID of the conversation (patientId).
     * @param senderId The UID of the sender (patient or doctor).
     * @param text The message content.
     * @param resultId Optional: Link message to a result (used for the first message).
     * @throws Exception if the operation fails.
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        resultId: String? = null
    ) {
        val messagesRef = firestore.collection("conversations").document(conversationId).collection("messages")

        val newMessage = Message(
            senderId = senderId,
            text = text,
            timestamp = Timestamp.now(), // Use server timestamp for accuracy
            read = false, // Mark as unread initially
            resultId = resultId // Include result ID if provided
        )

        Log.d("MessageRepo", "Sending message to conversation $conversationId from sender $senderId")

        // Add the new message document with an auto-generated ID
        messagesRef.add(newMessage).await()
        Log.d("MessageRepo", "Message added to conversation $conversationId")

        // Optional: Update the last message timestamp and text in the conversation document
        // This is important for ordering the doctor's inbox
        firestore.collection("conversations").document(conversationId)
            .update(
                mapOf(
                    "lastMessageTimestamp" to Timestamp.now(),
                    "lastMessageText" to text // Update last message text
                )
            )
            .await()
        Log.d("MessageRepo", "Conversation $conversationId updated with last message info.")

    }

    /**
     * Gets a real-time stream of messages for a specific conversation.
     *
     * @param conversationId The ID of the conversation (patientId).
     * @return A Flow emitting Lists of Message objects whenever messages change.
     */
    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow { // <-- Use callbackFlow
        val messagesQuery = firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Order by time

        // Use snapshots to get real-time updates
        val registration = messagesQuery.addSnapshotListener { snapshot, e -> // <-- Store the registration
            if (e != null) {
                Log.w("MessageRepo", "Listen failed for messages in $conversationId.", e)
                // Handle error - send empty list and close the channel
                trySend(emptyList()) // Safely emit empty list on error
                close(e) // Close the flow with the error
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Map the documents to Message data class
                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Message::class.java)
                    } catch (e: Exception) {
                        Log.e("MessageRepo", "Failed to parse message document ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                Log.d("MessageRepo", "Received ${messages.size} messages for conversation $conversationId")
                // Emit the new list of messages to the Flow
                trySend(messages) // <-- Use trySend
            } else {
                // Snapshot is null but no error? Should not happen often, but handle defensively
                Log.w("MessageRepo", "Received null snapshot for messages in $conversationId with no error.")
                trySend(emptyList()) // Safely emit empty list
            }
        }

        // This block keeps the flow alive as long as there are collectors.
        // When the flow is cancelled or finishes, the awaitClose block is executed.
        awaitClose { // <-- Use awaitClose
            Log.d("MessageRepo", "Stopping snapshot listener for messages in $conversationId")
            registration.remove() // <-- Remove the listener when the flow is no longer collected
        }
    }


    /**
     * Gets a real-time stream of conversations for the doctor's inbox.
     *
     * @param doctorId The UID of the doctor/admin.
     * @return A Flow emitting Lists of Conversation objects whenever conversations change.
     */
    fun getConversationsForDoctor(doctorId: String): Flow<List<Conversation>> = callbackFlow { // <-- Use callbackFlow
        val conversationsQuery = firestore.collection("conversations")
            .whereEqualTo("doctorId", doctorId) // Filter conversations for this doctor
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING) // Order by most recent message

        // Use snapshots for real-time updates
        val registration = conversationsQuery.addSnapshotListener { snapshot, e -> // <-- Store the registration
            if (e != null) {
                Log.w("MessageRepo", "Listen failed for doctor conversations $doctorId.", e)
                // Handle error - send empty list and close the channel
                trySend(emptyList()) // <-- Use trySend
                close(e) // Close the flow with the error
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Map the documents to Conversation data class
                val conversations = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Conversation::class.java)
                    } catch (e: Exception) {
                        Log.e("MessageRepo", "Failed to parse conversation document ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                Log.d("MessageRepo", "Received ${conversations.size} conversations for doctor $doctorId")
                // Emit the new list of conversations
                trySend(conversations) // <-- Use trySend
            } else {
                // Snapshot is null but no error?
                Log.w("MessageRepo", "Received null snapshot for doctor conversations $doctorId with no error.")
                trySend(emptyList()) // Safely emit empty list
            }
        }

        // This block keeps the flow alive as long as there are collectors.
        // When the flow is cancelled or finishes, the awaitClose block is executed.
        awaitClose { // <-- Use awaitClose
            Log.d("MessageRepo", "Stopping snapshot listener for doctor conversations $doctorId")
            registration.remove() // <-- Remove the listener when the flow is no longer collected
        }
    }

    /**
     * Gets a real-time stream of a single conversation for a patient.
     *
     * @param patientId The UID of the patient.
     * @return A Flow emitting a single Conversation object or null.
     */
    fun getConversationForPatient(patientId: String): Flow<Conversation?> = callbackFlow { // <-- Use callbackFlow
        val conversationRef = firestore.collection("conversations").document(patientId)

        // Add the snapshot listener
        val registration = conversationRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("MessageRepo", "Listen failed for patient conversation $patientId.", e)
                // Handle error - send null and close the channel
                trySend(null) // Safely emit null
                close(e) // Close the flow with the error
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    val conversation = snapshot.toObject(Conversation::class.java)
                    Log.d("MessageRepo", "Received conversation update for patient $patientId")
                    trySend(conversation) // Safely emit the conversation object
                } catch (e: Exception) {
                    Log.e("MessageRepo", "Failed to parse patient conversation $patientId: ${e.message}", e)
                    trySend(null) // Safely emit null on parsing error
                }
            } else {
                Log.d("MessageRepo", "Conversation document does not exist for patient $patientId")
                trySend(null) // Safely emit null if the document doesn't exist
            }
        }

        // This block keeps the flow alive as long as there are collectors.
        // When the flow is cancelled or finishes, the awaitClose block is executed.
        awaitClose {
            Log.d("MessageRepo", "Stopping snapshot listener for patient conversation $patientId")
            registration.remove() // Remove the listener when the flow is no longer collected
        }
    }

}