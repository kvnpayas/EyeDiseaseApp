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
    }
}