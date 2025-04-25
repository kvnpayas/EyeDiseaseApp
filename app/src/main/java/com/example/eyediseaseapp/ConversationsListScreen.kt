package com.example.eyediseaseapp

import android.util.Log

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eyediseaseapp.util.Conversation
import com.example.eyediseaseapp.util.MessageRepoUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.eyediseaseapp.util.NavigationUtils

@Composable
fun ConversationsListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    drawerState: DrawerState? = null,
    scope: CoroutineScope? = null
) {
    val auth = FirebaseAuth.getInstance()
    val currentUserId =
        auth.currentUser?.uid

    val messageRepository = remember { MessageRepoUtils() }

    var userRole by remember { mutableStateOf<String?>(null) }
    var isLoadingRole by remember { mutableStateOf(true) }
    var roleError by remember { mutableStateOf<String?>(null) }

    var conversationsData by remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(currentUserId) {
        Log.d("ConversationsList", "LaunchedEffect: currentUserId changed to $currentUserId")
        if (currentUserId != null) {
            // Fetch role first
            isLoadingRole = true
            roleError = null
            userRole = try {
                val destinationScreen = NavigationUtils.fetchUserRole(currentUserId)
                when (destinationScreen) {
                    Screen.PatientHome -> "user"
                    Screen.DoctorHome -> "admin"
                    else -> null // Unknown role
                }
            } catch (e: Exception) {
                roleError = e.message ?: "Failed to load user role."
                Log.e("ConversationsList", "Error fetching user role for $currentUserId: ${e.message}", e)
                null // Role is null on error
            } finally {
                isLoadingRole = false
            }

            // Based on role, start collecting the appropriate conversation data
            if (userRole == "admin") {
                Log.d("ConversationsList", "Fetching conversations for doctor: $currentUserId")
                messageRepository.getConversationsForDoctor(currentUserId).collect { list ->
                    conversationsData = list // Update state with list of conversations
                    Log.d("ConversationsList", "Doctor received ${list.size} conversations.")
                }
            } else if (userRole == "user") {
                Log.d("ConversationsList", "Fetching conversation for patient: $currentUserId")
                messageRepository.getConversationForPatient(currentUserId).collect { conversation ->
                    conversationsData = conversation // Update state with single conversation or null
                    Log.d("ConversationsList", "Patient received conversation: $conversation")
                }
            } else {
                // Role is unknown or error occurred, conversationsData remains null or empty list
                Log.w("ConversationsList", "User role is unknown or error occurred, not fetching conversations.")
                conversationsData = emptyList<Conversation>() // Set to empty list for consistent check
            }

        } else {
            // If currentUserId is null (user logged out), reset state
            userRole = null
            isLoadingRole = false
            roleError = "User not logged in."
            conversationsData = emptyList<Conversation>() // Clear conversation data
            Log.d("ConversationsList", "User ID is null, resetting state.")
        }
    }

    // --- Header Box (Your Existing Header) ---
    // This header is positioned at the top of the Box
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), // Adjust the height as needed
        contentAlignment = Alignment.Center // Center the text inside the Box
    ) {
        Image(
            painter = painterResource(id = R.drawable.header_bg), // Use header_bg here
            contentDescription = "Header Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth // Changed to FillWidth
        )
        Text(
            text = "Inbox",
            color = colorResource(id = R.color.darkPrimary),
            fontSize = 30.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.padding(top = 100.dp) // Adjust padding to position text
        )
    }

    // --- Conversation List Content Area ---
    // This Column is positioned below the header using top padding.
    // It should NOT be scrollable itself as LazyColumn provides scrolling.
    Column(
        modifier = Modifier
            .fillMaxSize() // Fill the size below the header
            .padding(top = 180.dp)
            .padding(horizontal = 16.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (isLoadingRole) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading menu...") // Or "Loading conversations..."
            }
        } else if (roleError != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error: ${roleError}", color = Color.Red, textAlign = TextAlign.Center)
            }
        } else {
            // Role is loaded, display content based on role and conversation data

            if (userRole == "admin") {
                // --- Doctor's View: List of Conversations ---
                val conversationsList = conversationsData as? List<Conversation> // Cast to List<Conversation>
                if (conversationsList.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No conversations found.", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = conversationsList,
                            key = { it.patientId }
                        ) { conversation ->
                            ConversationListItem(
                                conversation = conversation,
                                onClick = {
                                    navController.navigate(Screen.ConversationDetail.createRoute(conversation.patientId)) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    } // End LazyColumn
                }

            } else if (userRole == "user") {
                // --- Patient's View: Single Conversation or Empty State ---
                val patientConversation = conversationsData as? Conversation // Cast to Conversation?

                if (patientConversation != null) {
                    // Display the single conversation item
                    Spacer(modifier = Modifier.height(16.dp)) // Add space below header
                    ConversationListItem(
                        conversation = patientConversation,
                        onClick = {
                            // Navigate to the detail screen for this conversation
                            navController.navigate(Screen.ConversationDetail.createRoute(patientConversation.patientId)) {
                                launchSingleTop = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f)) // Push content up
                } else {
                    // No conversation initiated yet for this patient
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You haven't started a consultation yet.", textAlign = TextAlign.Center)
                    // Optional: Add a button here to navigate to ResultHistory to start one
                    // Button(onClick = { navController.navigate(Screen.ResultHistory.route) }) { Text("Start Consultation") }
                    Spacer(modifier = Modifier.weight(1f)) // Push content up
                }

            } else {
                // --- Handle Unknown Role State ---
                Spacer(modifier = Modifier.height(16.dp))
                Text("Unable to determine user role.", color = Color.Red, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ConversationListItem(
    conversation: Conversation, // The conversation data for this item
    onClick: () -> Unit, // Lambda to call when the item is clicked
    modifier: Modifier = Modifier // Modifier for external customization
) {
    // Format the last message timestamp
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val timeString = try {
        conversation.lastMessageTimestamp.toDate() // Get Date from Timestamp
    } catch (e: Exception) {
        Log.e(
            "ConversationItem",
            "Error formatting date for conversation ${conversation.patientId}: ${e.message}",
            e
        )
        null // Return null Date on error
    }?.let { // If date is not null, format it
        dateFormat.format(it)
    } ?: "Invalid Date" // Fallback if date is null or invalid


    // Optional: Fetch patient name from user document if not stored in Conversation
    // This would require another LaunchedEffect and state variable within this composable
    // and a function in UserRepository to get a user's name by UID.
    // For simplicity, let's assume patientName is stored in the Conversation document.
    val patientDisplayName =
        conversation.patientName ?: "Patient ${conversation.patientId.take(4)}..." // Fallback name


    Column(
        modifier = modifier
            .fillMaxWidth() // Item fills width
            .clickable(onClick = onClick) // Make the whole item clickable
            .padding(vertical = 8.dp) // Padding inside the clickable area
            .background(
                Color.White,
                RoundedCornerShape(8.dp)
            ) // Optional: background color and shape
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)) // Optional border
        // Optional: Add elevation for visual separation
        // .shadow(1.dp, RoundedCornerShape(4.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, // Space out name and time
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Patient Name
            Text(
                text = patientDisplayName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colorResource(id = R.color.darkPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis, // Ellipsize long names
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp) // Take available space, add padding
            )
            // Last Message Timestamp
            Text(
                text = timeString,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Last Message Snippet
        Box(
            modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
        ) {
            Text(
                text = conversation.lastMessageText
                    ?: "No messages yet", // Display last message or placeholder
                fontSize = 14.sp,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Ellipsize long snippets
            )
        }
        // Optional: Indicator for unread messages
        // if (!conversation.read) {
        //     Icon( /* Unread indicator icon */ )
        // }
    }
}