package com.example.eyediseaseapp

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.eyediseaseapp.util.NavigationUtils.fetchUserRole
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.eyediseaseapp.util.CallNotification
import com.example.eyediseaseapp.util.MessageRepoUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope

sealed class Screen(val route: String) {
    object AuthCheck : Screen("auth_check")
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object PatientHome : Screen("patient_home")
    object DoctorHome : Screen("doctor_home")
    object AboutUs : Screen("about_us")
    object ImageClassification : Screen("image_classification")
    object Camera : Screen("camera")
    object LearnMore : Screen("learn_more")
    object ResultHistory : Screen("result_history")
    object MessageInbox : Screen("message_inbox")

    object PatientLists : Screen("patient_list") // Screen to list patients who initiated consult
    object PatientConsultedDetail : Screen("patient_consulted_detail/{patientId}") { // Screen to list consulted results for a specific patient
        // Define argument key
        const val PATIENT_ID_KEY = "patientId"
        // Helper to create the route with the argument value
        fun createRoute(patientId: String) = "patient_consulted_detail/$patientId"
    }

    object ConversationsList : Screen("conversations_list")
    object ConversationDetail : Screen("conversation_detail/{conversationId}") {
        // Define argument key
        const val CONVERSATION_ID_KEY = "conversationId"
        // Helper to create the route with the argument value
        fun createRoute(conversationId: String) = "conversation_detail/$conversationId"
    }

    object VideoCall : Screen("video_call/{conversationId}") {
        const val CONVERSATION_ID_KEY = "conversationId" // Use the same key as ConversationDetail for consistency
        fun createRoute(conversationId: String) = "video_call/$conversationId"
    }

    fun withArgs(vararg args: String): String {
        return buildString {
            append(route)
            args.forEach { arg ->
                append("/$arg")
            }
        }
    }
}

@Composable
fun NavGraph(navController: NavHostController,
             drawerState: DrawerState,
             scope: CoroutineScope
) {
    val localCoroutineScope = rememberCoroutineScope()
    val currentScope = scope ?: localCoroutineScope

    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    // --- State for incoming call notification ---
    var incomingCallNotification by remember { mutableStateOf<CallNotification?>(null) }
    val messageRepository = remember { MessageRepoUtils() }

    var currentUserRole by remember { mutableStateOf<String?>(null) } // Holds the fetched role ('user', 'admin', or null)
    var isLoadingRole by remember { mutableStateOf(true) } // True while fetching the role
    var roleError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUserId) {
        Log.d("NavGraph", "LaunchedEffect: currentUserId changed to $currentUserId for role fetch.")
        if (currentUserId != null) {
            isLoadingRole = true
            roleError = null
            currentUserRole = try {
                val destinationScreen = fetchUserRole(currentUserId)
                when (destinationScreen) {
                    Screen.PatientHome -> "user"
                    Screen.DoctorHome -> "admin"
                    else -> null
                }
            } catch (e: Exception) {
                roleError = e.message ?: "Failed to load user role."
                Log.e("NavGraph", "Error fetching user role for $currentUserId: ${e.message}", e)
                null
            } finally {
                isLoadingRole = false
            }
            Log.d("NavGraph", "Fetched current user role: $currentUserRole for UID: $currentUserId")
        } else {
            currentUserRole = null
            isLoadingRole = false
            roleError = "User not logged in."
            Log.d("NavGraph", "Current User ID is null, cannot fetch role.")
        }
    }

    LaunchedEffect(currentUserId, currentUserRole) { // Re-run if user or role changes
        Log.d("NavGraph", "LaunchedEffect (Call Listener): currentUserId: $currentUserId, currentUserRole: $currentUserRole")
        if (currentUserId != null && currentUserRole == "user") {
            Log.d("NavGraph", "User is a patient, starting call notification listener for UID: $currentUserId")
            // Start collecting the notification document for the current patient
            messageRepository.getCallNotificationForPatient(currentUserId).collect { notification ->
                Log.d("NavGraph", "Received call notification update: $notification")
                // Update the state. The modal will be shown if status is "calling".
                incomingCallNotification = notification
            }
        } else {
            // If user logs out or is not a patient, clear the notification state
            incomingCallNotification = null
            if (currentUserId != null) {
                Log.d("NavGraph", "User is not a patient (role: $currentUserRole), stopping call notification listener.")
            } else {
                Log.d("NavGraph", "User ID is null, stopping call notification listener.")
            }
        }
    }

    if (incomingCallNotification != null && incomingCallNotification?.status == "calling") {
        IncomingCallDialog(
            callNotification = incomingCallNotification!!, // Pass the notification data
            onAccept = { notification ->
                Log.d("NavGraph", "Call Accepted for patient ${notification.patientId}. Navigating to VideoCall.")
                currentScope.launch { // Use the local scope for coroutines
                    try {
                        // 1. Update the notification status to "accepted"
                        messageRepository.updateCallNotificationStatus(notification.patientId, "accepted")
                        Log.d("NavGraph", "Call notification status updated to 'accepted' for patient ${notification.patientId}")

                        // 2. Navigate to the VideoCallScreen
                        // Pass the patientId (which is the conversationId)
                        navController.navigate(Screen.VideoCall.createRoute(notification.patientId)) {
                            launchSingleTop = true // Avoid multiple copies
                        }
                        // The dialog will be dismissed because incomingCallNotification will be updated by the listener
                        // to null once the status changes from "calling".

                    } catch (e: Exception) {
                        Log.e("NavGraph", "Failed to accept call and update status: ${e.message}", e)
                        // Handle error (e.g., show a Toast)
                        // Keep the dialog open or dismiss it based on desired UX on error
                        incomingCallNotification = null // Dismiss dialog on error
                    }
                }
            },
            onReject = { notification ->
                Log.d("NavGraph", "Call Rejected for patient ${notification.patientId}. Updating notification status.")
                currentScope.launch { // Use the local scope for coroutines
                    try {
                        // 1. Update the notification status to "rejected"
                        messageRepository.deleteCallNotification(notification.patientId)
                        Log.d("NavGraph", "Call notification status updated to 'rejected' for patient ${notification.patientId}")

                        // The dialog will be dismissed because incomingCallNotification will be updated by the listener
                        // to null once the status changes from "calling".

                    } catch (e: Exception) {
                        Log.e("NavGraph", "Failed to reject call and update status: ${e.message}", e)
                        // Handle error (e.g., show a Toast)
                        // Keep the dialog open or dismiss it based on desired UX on error
                        incomingCallNotification = null // Dismiss dialog on error
                    }
                }
            },
            onDismiss = {
                // Handle dismissal if the user taps outside the dialog.
                // You might want to treat this as a missed call after a timeout,
                // but for now, we'll just dismiss the dialog without changing status immediately.
                // The listener will eventually update the status if the doctor ends the call.
                // If the doctor doesn't end it, the notification will remain "calling" until the patient accepts/rejects or app is closed/killed.
                Log.d("NavGraph", "Incoming call dialog dismissed by user.")
                // We don't clear incomingCallNotification here on manual dismiss,
                // as the listener should be the source of truth for clearing it when the status changes.
                // If you want manual dismiss to also update status, call updateCallNotificationStatus("missed") here.
                // For simplicity, we'll rely on the doctor ending the call or the patient accepting/rejecting.
            }
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.AuthCheck.route
    ) {


        composable(Screen.AuthCheck.route) {
            AuthCheckScreen(navController = navController)
        }


        composable(Screen.SignIn.route) {
            SignInScreen(
                navController = navController,
                onSignInSuccess = { userId ->
                    currentScope.launch {
                        val destinationScreen = fetchUserRole(userId)
                        val targetRoute = destinationScreen?.route ?: Screen.SignIn.route
                        Log.d("GoogleSignIn", targetRoute)
                        Log.d("GoogleSignIn", "$destinationScreen")
                        if (destinationScreen != null) {
                            navController.navigate(targetRoute) {
                                popUpTo(Screen.AuthCheck.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            println("Error: Role fetch failed after successful login for UID: $userId. Staying on SignIn screen.")
                        }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                navController = navController,
                onSignUpSuccess = { userId ->
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                        launchSingleTop = true
                    }

                },
                onNavigateToSignIn = {
                    navController.popBackStack(Screen.SignIn.route, inclusive = false) // Go back to Sign In
                }
            )
        }

        composable(Screen.PatientHome.route) {
            Scaffold(
                topBar = {
                    AppBarWithDrawerButton(
                        title = "Patient Home",
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) { paddingValues ->

                HomeScreen(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }


        composable(Screen.AboutUs.route) {
            AboutUsScreen(navController)
            Scaffold(
                topBar = {
                    AppBarWithDrawerButton(
                        title = "About Us",
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) { paddingValues ->

                AboutUsScreen(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        composable(Screen.ResultHistory.route) {
            ResultHistoryScreen(navController)
            Scaffold(
                topBar = {
                    AppBarWithDrawerButton(
                        title = "History",
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) { paddingValues ->
                ResultHistoryScreen(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
        composable(Screen.ImageClassification.route) {
            ImageClassificationScreen(navController)
        }
        composable(Screen.Camera.route) {
            CameraScreen(navController)
        }
        composable(Screen.LearnMore.route) {
            EducationalContentScreen(navController)
        }

        composable(Screen.DoctorHome.route) {
            DoctorHomeScreen(navController)
            Scaffold(
                topBar = {
                    AppBarWithDrawerButton(
                        title = "Doctor Home",
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) { paddingValues ->

                DoctorHomeScreen(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable(Screen.PatientLists.route) {
            PatientListsScreen(
                navController = navController,
            )
            Scaffold(
                topBar = {
                    AppBarWithDrawerButton(
                        title = "Conversation Lists",
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) { paddingValues ->

                PatientListsScreen(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable(
            route = Screen.PatientConsultedDetail.route,
            arguments = listOf(navArgument(Screen.PatientConsultedDetail.PATIENT_ID_KEY) {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString(Screen.PatientConsultedDetail.PATIENT_ID_KEY)
            if (patientId != null) {
                PatientConsultedDetailScreen(
                    navController = navController,
                    patientId = patientId,
                )
                Scaffold(
                    topBar = {
                        AppBarWithDrawerButton(
                            title = "Patient Consulted Details",
                            drawerState = drawerState,
                            scope = scope
                        )
                    }
                ) { paddingValues ->

                    PatientConsultedDetailScreen(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues),
                        patientId = patientId,
                    )
                }
            } else {
                Log.e("NavGraph", "PatientConsultedDetailScreen: Missing patientId argument!")
                 navController.popBackStack()
            }
        }

        composable(Screen.MessageInbox.route) {
            MessageInboxScreen(navController)
            Scaffold(
                topBar = {
                    AppBarWithDrawerButton(
                        title = "Patient Lists",
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) { paddingValues ->

                MessageInboxScreen(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        composable(Screen.ConversationsList.route) {
            ConversationsListScreen(
                navController = navController,
                drawerState = drawerState,
                scope = currentScope
            )
            Scaffold(
                topBar = {
                    AppBarWithDrawerButton(
                        title = "Conversation Lists",
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) { paddingValues ->

                ConversationsListScreen(
                    navController = navController,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }

        // Conversation Detail screen with argument
        composable(
            route = Screen.ConversationDetail.route, // Route with argument placeholder
            arguments = listOf(navArgument(Screen.ConversationDetail.CONVERSATION_ID_KEY) {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString(Screen.ConversationDetail.CONVERSATION_ID_KEY)
            if (conversationId != null) {
                ConversationDetailScreen(
                    navController = navController,
                    conversationId = conversationId,
                    drawerState = drawerState,
                    scope = currentScope
                )
                Scaffold(
                    topBar = {
                        AppBarWithDrawerButton(
                            title = "Conversation Details",
                            drawerState = drawerState,
                            scope = scope
                        )
                    }
                ) { paddingValues ->

                    ConversationDetailScreen(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues),
                        conversationId = conversationId,
                    )
                }
            } else {
                Log.e("NavGraph", "ConversationDetailScreen: Missing conversationId argument!")
                 navController.popBackStack()
            }
        }

        composable(
            route = Screen.VideoCall.route, // Route with conversationId argument
            arguments = listOf(navArgument(Screen.VideoCall.CONVERSATION_ID_KEY) {
                type = NavType.StringType // Specify argument type
            })
        ) { backStackEntry ->
            // Get the conversationId argument value
            val conversationId = backStackEntry.arguments?.getString(Screen.VideoCall.CONVERSATION_ID_KEY)
            if (conversationId != null) {
                // Pass conversationId and other necessary parameters
                VideoCallScreen(
                    navController = navController, // Pass navController
                    conversationId = conversationId, // Pass the conversation ID
                    // Pass drawerState and scope if needed (though likely not needed for a full-screen call)
                    // drawerState = drawerState,
                    // scope = currentScope
                ) // You need to create this composable
            } else {
                // Handle case where conversationId is missing
                Log.e("NavGraph", "VideoCallScreen: Missing conversationId argument!")
                // Navigate back or show an error
                navController.popBackStack() // Go back if essential argument is missing
            }
        }
    }
}