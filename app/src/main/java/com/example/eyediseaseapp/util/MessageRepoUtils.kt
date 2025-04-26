package com.example.eyediseaseapp.util

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class MessageRepoUtils {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userUtils = UserUtils()

    private suspend fun findAdminUid(): String? {
        return try {
            Log.d("MessageRepo", "Attempting to find admin UID...")
            val adminQuery = firestore.collection("users")
                .whereEqualTo("role", "admin")
                .limit(1)

            val snapshot = adminQuery.get().await()

            if (snapshot.isEmpty) {
                Log.w("MessageRepo", "No user with 'admin' role found.")
                null
            } else if (snapshot.size() > 1) {

                Log.w("MessageRepo", "Multiple users with 'admin' role found. Using the first one.")
                snapshot.documents.first().id
            }
            else {
                val adminUid = snapshot.documents.first().id
                Log.d("MessageRepo", "Admin UID found: $adminUid")
                adminUid
            }
        } catch (e: Exception) {
            Log.e("MessageRepo", "Error finding admin UID: ${e.message}", e)
            null // Return null on error
        }
    }

    suspend fun initiateConsultation(
        patientId: String,
        initialMessageText: String,
        resultId: String,
        patientName: String? = null,
        lastSenderName: String? = null
    ): String {
        val conversationRef = firestore.collection("conversations").document(patientId)

        // Check if conversation already exists
        val conversationDoc = conversationRef.get().await()

        val conversationId = patientId // Conversation ID is the patient's UID

        val doctorId = findAdminUid()
            ?: throw IllegalStateException("Admin user not found. Cannot initiate consultation.")
        val doctorName = userUtils.getUserName(doctorId)

        if (!conversationDoc.exists()) {
            // Conversation does not exist, create it
            Log.d("MessageRepo", "Creating new conversation for patient: $patientId")

            val newConversation = Conversation(
                patientId = patientId,
                doctorId = doctorId,
                patientName = patientName,
                doctorName = doctorName,
                lastSenderName = lastSenderName,
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
        sendMessage(
            conversationId = conversationId,
            senderId = patientId,
            text = initialMessageText,
            resultId = resultId
        )

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
        val lastSenderName = userUtils.getUserName(senderId)

        val newMessage = Message(
            senderId = senderId,
            text = text,
            timestamp = Timestamp.now(),
            read = false,
            resultId = resultId
        )

        Log.d("MessageRepo", "Sending message to conversation $conversationId from sender $senderId")

        // Add the new message document with an auto-generated ID
        messagesRef.add(newMessage).await()
        Log.d("MessageRepo", "Message added to conversation $conversationId")

        firestore.collection("conversations").document(conversationId)
            .update(
                mapOf(
                    "lastMessageTimestamp" to Timestamp.now(),
                    "lastMessageText" to text,
                    "lastSenderName" to lastSenderName
                )
            )
            .await()
        Log.d("MessageRepo", "Conversation $conversationId updated with last message info.")

    }

    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val messagesQuery = firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Order by time

        // Use snapshots to get real-time updates
        val registration = messagesQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("MessageRepo", "Listen failed for messages in $conversationId.", e)

                trySend(emptyList())
                close(e)
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

                trySend(messages)
            } else {

                Log.w("MessageRepo", "Received null snapshot for messages in $conversationId with no error.")
                trySend(emptyList())
            }
        }


        awaitClose {
            Log.d("MessageRepo", "Stopping snapshot listener for messages in $conversationId")
            registration.remove() // <-- Remove the listener when the flow is no longer collected
        }
    }



    fun getConversationsForDoctor(doctorId: String): Flow<List<Conversation>> = callbackFlow {
        val conversationsQuery = firestore.collection("conversations")
            .whereEqualTo("doctorId", doctorId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)

        // Use snapshots for real-time updates
        val registration = conversationsQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("MessageRepo", "Listen failed for doctor conversations $doctorId.", e)

                trySend(emptyList())
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {

                val conversations = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Conversation::class.java)
                    } catch (e: Exception) {
                        Log.e("MessageRepo", "Failed to parse conversation document ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                Log.d("MessageRepo", "Received ${conversations.size} conversations for doctor $doctorId")

                trySend(conversations)
            } else {

                Log.w("MessageRepo", "Received null snapshot for doctor conversations $doctorId with no error.")
                trySend(emptyList())
            }
        }


        awaitClose {
            Log.d("MessageRepo", "Stopping snapshot listener for doctor conversations $doctorId")
            registration.remove()
        }
    }


    fun getConversationForPatient(patientId: String): Flow<Conversation?> = callbackFlow {
        val conversationRef = firestore.collection("conversations").document(patientId)

        // Add the snapshot listener
        val registration = conversationRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("MessageRepo", "Listen failed for patient conversation $patientId.", e)

                trySend(null)
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    val conversation = snapshot.toObject(Conversation::class.java)
                    Log.d("MessageRepo", "Received conversation update for patient $patientId")
                    trySend(conversation)
                } catch (e: Exception) {
                    Log.e("MessageRepo", "Failed to parse patient conversation $patientId: ${e.message}", e)
                    trySend(null)
                }
            } else {
                Log.d("MessageRepo", "Conversation document does not exist for patient $patientId")
                trySend(null)
            }
        }


        awaitClose {
            Log.d("MessageRepo", "Stopping snapshot listener for patient conversation $patientId")
            registration.remove()
        }
    }

}