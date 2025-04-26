package com.example.eyediseaseapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.* // Material 3 components
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.DrawerState // Import if using sidebar
import kotlinx.coroutines.CoroutineScope // Import if using sidebar
import kotlinx.coroutines.launch // Import launch
import java.text.SimpleDateFormat // For date formatting
import java.util.Locale // For date formatting
import android.util.Log // For logging
import android.widget.Toast // For showing Toast
import androidx.compose.ui.platform.LocalContext
import com.example.eyediseaseapp.util.MessageRepoUtils
import com.example.eyediseaseapp.util.Message
import com.example.eyediseaseapp.util.NavigationUtils
import com.example.eyediseaseapp.util.PatientResult
import com.example.eyediseaseapp.util.ResultRepository

@Composable
fun ConversationDetailScreen(
    navController: NavController,
    modifier: Modifier = Modifier,// Or NavHostController
    conversationId: String, // <-- This screen requires the conversationId argument
    drawerState: DrawerState? = null, // Accept nullable if sidebar is optional
    scope: CoroutineScope? = null
) {

    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid // Get the current user's UID

    val messageRepository = remember { MessageRepoUtils() } // Instance of MessageRepository
    val context = LocalContext.current // Get context for Toast

    val resultRepository = remember { ResultRepository() }

    val localCoroutineScope = rememberCoroutineScope()
    val actionScope = scope ?: localCoroutineScope

    // --- State for fetching messages ---
    // Collect the stream of messages for this conversation
    val messagesState = messageRepository.getMessages(conversationId).collectAsState(initial = emptyList())
    val messages = messagesState.value // The list of messages

    // --- State for fetching conversation details (for header) ---
    // Collect the stream of the single conversation document
    val conversationState = messageRepository.getConversationForPatient(conversationId).collectAsState(initial = null)
    val conversation = conversationState.value // The conversation details (or null)

    // --- State for message input ---
    var messageInput by remember { mutableStateOf("") }
    var isSendingMessage by remember { mutableStateOf(false) }

    // --- State for fetching current user's role ---
    var currentUserRole by remember { mutableStateOf<String?>(null) } // Holds the fetched role ('user', 'admin', or null)
    var isLoadingRole by remember { mutableStateOf(true) } // True while fetching the role
    var roleError by remember { mutableStateOf<String?>(null) }

    var selectedResultForViewDialog by remember { mutableStateOf<PatientResult?>(null) }

    LaunchedEffect(currentUserId) {
        Log.d("ConversationDetail", "LaunchedEffect: currentUserId changed to $currentUserId")
        if (currentUserId != null) {
            isLoadingRole = true
            roleError = null
            currentUserRole = try {
                // Fetch the role using your existing utility function
                val destinationScreen = NavigationUtils.fetchUserRole(currentUserId)
                when (destinationScreen) {
                    Screen.PatientHome -> "user"
                    Screen.DoctorHome -> "admin" // Use DoctorHomeScreen route name
                    else -> {
                        // If fetchUserRole returns SignIn or null, the role is not recognized or doc is missing
                        Log.w("ConversationDetail", "Fetched unknown or missing role for $currentUserId, destination: $destinationScreen")
                        null // Treat as unknown role
                    }
                }
            } catch (e: Exception) {
                roleError = e.message ?: "Failed to load user role."
                Log.e("ConversationDetail", "Error fetching user role for $currentUserId: ${e.message}", e)
                null
            } finally {
                isLoadingRole = false
            }
            Log.d("ConversationDetail", "Fetched current user role: $currentUserRole for UID: $currentUserId")
        } else {
            currentUserRole = null
            isLoadingRole = false
            roleError = "User not logged in."
            Log.d("ConversationDetail", "User ID is null, resetting role state.")
        }
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.header_bg),
                contentDescription = "Header Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth
            )
            val headerText = when {
                isLoadingRole || conversation == null -> "Loading..." // Show loading if role or conversation is loading
                roleError != null -> "Error loading name" // Show error if role fetch failed
                currentUserRole == "user" -> conversation.doctorName ?: "Doctor" // Patient sees Doctor's name
                currentUserRole == "admin" -> conversation.patientName ?: "Patient" // Doctor sees Patient's name
                else -> "Conversation" // Default for unknown role
            }

            Text(
                text = headerText, // Use the determined header text
                color = colorResource(id = R.color.darkPrimary),
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(top = 100.dp)
            )

        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 180.dp)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Takes up all available space
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp), // Horizontal padding for messages
                contentPadding = PaddingValues(vertical = 8.dp), // Padding around the messages content
                reverseLayout = true // Start list from bottom (most recent messages)
            ) {
                items(
                    items = messages.reversed(), // Display latest messages at the bottom
                    key = { it.timestamp.toDate().time } // Use timestamp as key (consider adding unique message ID if needed)
                ) { message -> // 'message' is a single Message object
                    // --- Composable for a Single Message Item ---
                    MessageItem(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId,
                        onViewResultClick = { resultId ->
                            actionScope.launch { // Use actionScope to launch the suspend function
                                try {
                                    // Fetch the result details when "View Result" is clicked
                                    val resultDetails = resultRepository.getResultById(resultId)
                                    selectedResultForViewDialog = resultDetails // Update state to show dialog
                                    Log.d("ConversationDetail", "Fetched result details for dialog: $resultId")
                                } catch (e: Exception) {
                                    Log.e("ConversationDetail", "Failed to fetch result details for $resultId: ${e.message}", e)
                                    Toast.makeText(context, "Failed to load result details.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            // --- Message Input Area ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp), // Padding around the input row
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Enter message") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp), // Take available space
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    enabled = currentUserId != null && !isSendingMessage // Disable if not logged in or sending
                )

                // Send Button
                Button(
                    onClick = {
                        val textToSend = messageInput.trim()
                        if (textToSend.isNotEmpty() && currentUserId != null) {
                            isSendingMessage = true // Start sending loading

                            // Launch coroutine to send the message
                            actionScope.launch { // Use the passed scope or local one
                                try {
                                    messageRepository.sendMessage(
                                        conversationId = conversationId, // The conversation ID
                                        senderId = currentUserId, // The current user's UID
                                        text = textToSend // The message text
                                    )
                                    messageInput = "" // Clear the input field on success
                                    Log.d("ConversationDetail", "Message sent successfully!")
                                } catch (e: Exception) {
                                    Log.e("ConversationDetail", "Failed to send message: ${e.message}", e)
                                    Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSendingMessage = false // Stop sending loading
                                }
                            }
                        }
                    },
                    enabled = messageInput.isNotBlank() && currentUserId != null && !isSendingMessage // Enable only if text is not blank, user is logged in, and not already sending
                ) {
                    if (isSendingMessage) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White) // Show loading on button
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send Message")
                    }
                }
            }
        }
    }

    selectedResultForViewDialog?.let { resultToDisplay ->
        ResultDetailsDialog(
            result = resultToDisplay, // Pass the fetched result data
            onDismiss = {
                selectedResultForViewDialog = null // Close the dialog
            }
        )
    }
}

@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean, // True if the message was sent by the current user
    onViewResultClick: (resultId: String) -> Unit
) {
    val horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isCurrentUser) colorResource(id = R.color.darkPrimary) else Color.LightGray // Color of the message bubble
    val textColor = if (isCurrentUser) Color.White else Color.Black // Text color

    // Optional: Format message timestamp for display
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = try {
        message.timestamp.toDate()?.let { dateFormat.format(it) } ?: ""
    } catch (e: Exception) {
        "" // Empty string on error
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp), // Padding around the message item
        horizontalAlignment = horizontalAlignment // Apply the determined alignment
    ) {
        // Message Bubble
        Box(
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(8.dp)) // Background color and shape
                .padding(horizontal = 12.dp, vertical = 8.dp) // Padding inside the bubble
                .widthIn(max = 250.dp) // Optional: Limit bubble width
        ) {
            Column { // Use a Column inside the Box to stack text and button
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 16.sp
                )

                // --- Display "View Result" button if message has a resultId ---
                if (message.resultId != null) {
                    Spacer(modifier = Modifier.height(4.dp)) // Space between text and button
                    TextButton(
                        onClick = { onViewResultClick(message.resultId) }, // Call the lambda
                        // Optional: Customize button appearance
                        colors = ButtonDefaults.textButtonColors(contentColor = if(isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.DarkGray) // Make text button color adaptive
                    ) {
                        Text("View Result")
                    }
                }
            }
        }
        // Optional: Timestamp below the message bubble
        Text(
            text = timeString,
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp) // Small space above timestamp
        )
    }
}
