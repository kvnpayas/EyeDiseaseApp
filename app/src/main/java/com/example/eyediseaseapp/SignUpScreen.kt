package com.example.eyediseaseapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.eyediseaseapp.util.NavigationUtils.createUserDocument
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    navController: NavHostController,
    onSignUpSuccess: (userId: String) -> Unit, // This lambda should eventually trigger navigation
    onNavigateToSignIn: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope() // Need a coroutine scope
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // Center horizontally
        verticalArrangement = Arrangement.Center) {

        Button(onClick = {
            if (email.isNotBlank() && password.isNotBlank()) {
                isLoading = true
                errorMessage = null
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            val userId = task.result?.user?.uid
                            val userEmail = task.result?.user?.email // Get email if needed
                            if (userId != null) {

                                coroutineScope.launch { // Use coroutineScope to call suspend function
                                    try {
                                        createUserDocument(userId, userEmail) // Create the document

                                        onSignUpSuccess(userId)
                                        println("User account created and document saved for $userId. Navigating to Sign In.")


                                    } catch (e: Exception) {

                                        errorMessage = "Account created, but failed to save user data: ${e.message}"

                                        println("Error saving user document after signup: ${e.message}")
                                    }
                                }
                            } else {
                                errorMessage = "Account created, but user ID not found."
                            }
                        } else {
                            errorMessage = task.exception?.message ?: "Account creation failed."
                        }
                    }
            } else {
                errorMessage = "Please enter email and password."
            }
        }, enabled = !isLoading) {
            Text("Sign Up")
        }

    }
}