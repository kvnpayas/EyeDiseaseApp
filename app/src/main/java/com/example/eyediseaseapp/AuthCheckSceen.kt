package com.example.eyediseaseapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.example.eyediseaseapp.ui.theme.EyeDiseaseAppTheme
import com.example.eyediseaseapp.util.NavigationUtils.fetchUserRole
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AuthCheckScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }

    // Use LaunchedEffect to run the check only once when the Composable enters the composition
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        val destinationRoute: String

        if (currentUser != null) {
            // User is logged in, fetch their role
            val userId = currentUser.uid
            val destinationScreen = fetchUserRole(userId)

            destinationRoute = destinationScreen?.route ?: Screen.SignIn.route // Fallback
            println("AuthCheck: User ${userId} logged in. Role determined, navigating to: $destinationRoute")

        } else {
            // No user logged in, navigate to Sign In
            destinationRoute = Screen.SignIn.route
            println("AuthCheck: No user logged in. Navigating to Sign In.")
        }

        // Add a small delay for demonstration/visual effect if needed, otherwise remove
        // delay(500) // Optional: give the spinner a moment to show

        // Navigate after the check
        navController.navigate(destinationRoute) {
            // Clear the back stack entirely, so the user can't go back to AuthCheck or initial screens
            // popUpTo(navController.graph.startDestinationId) { inclusive = true } // Option 1: Clear to graph start
            popUpTo(Screen.AuthCheck.route) { inclusive = true } // Option 2: Clear up to AuthCheck
            launchSingleTop = true // Avoid creating multiple copies of the destination
        }

        isLoading = false // Hide loading once navigation is triggered (though screen will change)
    }

    // Show a loading indicator while checking authentication status
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    // Once navigation happens, this composable will be removed from the composition.
}

// You might want a simple preview for the loading state
@Preview(showBackground = true)
@Composable
fun AuthCheckScreenPreview() {
    // Assuming you have a theme
    EyeDiseaseAppTheme {
        // For preview, just show the loading indicator state
        // Real AuthCheck logic relies on LaunchedEffect and NavController,
        // which don't fully execute in a simple Composable preview.
        // This preview only shows the UI element that is displayed *while* checking.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}