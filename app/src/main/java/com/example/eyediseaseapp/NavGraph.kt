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
import kotlinx.coroutines.CoroutineScope

sealed class Screen(val route: String) {
    object AuthCheck : Screen("auth_check") // New: Will check login status
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object PatientHome : Screen("patient_home") // Renamed your original 'home'
    object AdminDashboard : Screen("admin_dashboard") // New: For Admins
    object AboutUs : Screen("about_us")
    object ImageClassification : Screen("image_classification")
    object Camera : Screen("camera")
    object LearnMore : Screen("learn_more")

    // Helper to create routes with arguments if needed later
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
             drawerState: DrawerState, // <-- Add this parameter
             scope: CoroutineScope
) {
    val coroutineScope = rememberCoroutineScope()
    NavHost(
        navController = navController,
        // Start at AuthCheck to determine where to go initially
        startDestination = Screen.AuthCheck.route
    ) {


        // 1. Authentication Check Screen (The New Start)
        composable(Screen.AuthCheck.route) {
            AuthCheckScreen(navController = navController)
        }

        // 2. Sign In Screen
        composable(Screen.SignIn.route) {
            SignInScreen( // Your SignInScreen Composable
                navController = navController,
                onSignInSuccess = { userId ->
                    // Login successful, now fetch role and navigate
                    coroutineScope.launch {
                        // Show loading indicator if needed (manage state within SignInScreen or hoist)
                        val destinationScreen = fetchUserRole(userId)
                        val targetRoute = destinationScreen?.route ?: Screen.SignIn.route // Stay on SignIn on failure? Or show error?
                        Log.d("GoogleSignIn", targetRoute)
                        Log.d("GoogleSignIn", "$destinationScreen")
                        if (destinationScreen != null) {
                            navController.navigate(targetRoute) {
                                // Clear the back stack up to the AuthCheck route (or graph start)
                                popUpTo(Screen.AuthCheck.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            // Handle role fetch failure after login (e.g., show error message in SignInScreen)
                            println("Error: Role fetch failed after successful login for UID: $userId. Staying on SignIn screen.")
                            // You might want to update an error state in SignInScreen here
                        }
                        // Hide loading indicator
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
                // Add Google Sign in logic here, calling the same onSignInSuccess lambda
            )
        }

        // 3. Sign Up Screen (You'll need to create SignUpScreen.kt)
        composable(Screen.SignUp.route) {
            SignUpScreen( // Your SignUpScreen Composable
                navController = navController,
                onSignUpSuccess = { userId ->
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true } // Remove SignUp
                        launchSingleTop = true
                    }

                },
                onNavigateToSignIn = {
                    navController.popBackStack(Screen.SignIn.route, inclusive = false) // Go back to Sign In
                }
            )
        }

        composable(Screen.PatientHome.route) {
            // Wrap content in Scaffold
            Scaffold(
                topBar = {
                    AppBarWithDrawerButton(
                        title = "Patient Home", // Title for this screen
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) { paddingValues ->
                // Your actual HomeScreen content
                HomeScreen(
                    navController = navController, // Pass navController if needed in HomeScreen
                    modifier = Modifier.padding(paddingValues) // Apply padding
                )
            }
        }

        // 5. Admin Dashboard Screen (New)
//        composable(Screen.AdminDashboard.route) {
//            // Create this Composable for Admin functions
//            AdminDashboardScreen(navController) // Replace with your Admin screen
//        }

        // --- Your Existing Screens ---
        composable(Screen.AboutUs.route) {
            AboutUsScreen(navController)
            Scaffold(
                topBar = {
                    AppBarWithDrawerButton(
                        title = "About Us", // Title for this screen
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            ) { paddingValues ->
                // Your actual HomeScreen content
                AboutUsScreen(
                    navController = navController, // Pass navController if needed in HomeScreen
                    modifier = Modifier.padding(paddingValues) // Apply padding
                )
            }
        }
        composable(Screen.ImageClassification.route) {
            ImageClassificationScreen(navController)
        }
        composable(Screen.Camera.route) {
            // Consider if CameraScreen needs authentication/role check
            CameraScreen()
        }
        composable(Screen.LearnMore.route) {
            EducationalContentScreen(navController)
        }
    }
}