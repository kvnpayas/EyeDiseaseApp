package com.example.eyediseaseapp

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.eyediseaseapp.util.ResultRepository
import com.example.eyediseaseapp.util.UserUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class) // Required for OutlinedTextField trailingIcon
@Composable
fun SignUpScreen(
    navController: NavHostController,
    onSignUpSuccess: (userId: String) -> Unit, // This lambda should eventually trigger navigation
    onNavigateToSignIn: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope() // Need a coroutine scope for createUserDocument
    val context = LocalContext.current // Get context for Toast

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) } // State for overall loading (Auth + Firestore)
    var errorMessage by remember { mutableStateOf<String?>(null) } // State for error messages

    // State for password visibility toggle
    var passwordVisible by remember { mutableStateOf(false) }
    val userUtils = remember { UserUtils() }


    Column(
        modifier = Modifier
            .fillMaxSize() // Use fillMaxSize for better centering potential
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // Center horizontally
        verticalArrangement = Arrangement.Center // Center vertically
    ) {
        // Optional: Add your app logo or image here
        Image(
            painter = painterResource(id = R.drawable.eye_logo),
            contentDescription = "image 1",
            modifier = Modifier
                .width(150.dp)
                .height(150.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Create New Account",
            fontSize = 24.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = colorResource(id = R.color.darkPrimary) // Example color
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading // Disable while loading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading, // Disable while loading
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Default.Visibility
                else Icons.Default.VisibilityOff

                // Localized description for accessibility
                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, description)
                }
            }
        )

        Spacer(modifier = Modifier.height(15.dp))

        // Error Message Text
        AnimatedVisibility(visible = errorMessage != null) { // Show error message if not null
            Text(
                text = errorMessage ?: "",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error, // Use error color from theme
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center // Center the error text
            )
            Spacer(modifier = Modifier.height(15.dp)) // Add space below error
        }


        // Sign Up Button
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.darkPrimary) // Example color
            ),
            onClick = {
                // Basic validation
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter email and password."
                    return@Button // Stop execution here
                }
                if (password.length < 6) { // Firebase requires minimum 6 characters for password
                    errorMessage = "Password must be at least 6 characters long."
                    return@Button
                }

                isLoading = true // Start loading
                errorMessage = null // Clear previous errors

                // 1. Create user account with Firebase Authentication
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = task.result?.user?.uid
                            val userEmail = task.result?.user?.email // Get email for Firestore document

                            if (userId != null) {
                                // 2. If Auth successful, create user document in Firestore
                                coroutineScope.launch { // Use coroutineScope to call suspend function
                                    try {
                                        // Call the function to create the user document with default role
                                        // Assuming createUserDocument is accessible here (e.g., in ResultRepository)
                                        userUtils.createUserDocument(userId, userEmail) // Call the suspend function

                                        println("SignUpScreen: User account created and document saved for $userId.")

                                        // 3. If Firestore document created successfully, signal success to NavGraph
                                        isLoading = false // Stop loading
                                        // This lambda will trigger navigation to SignInScreen as defined in NavGraph
                                        onSignUpSuccess(userId)

                                    } catch (e: Exception) {
                                        // Handle errors during Firestore document creation
                                        isLoading = false // Stop loading
                                        errorMessage = "Account created, but failed to save user data: ${e.message}"
                                        println("SignUpScreen: Error saving user document after signup: ${e.message}")
                                        // Decide what to do here: maybe log out the Firebase Auth user
                                        // because they are authenticated but have no profile data.
                                        // auth.signOut()
                                    }
                                }
                            } else {
                                isLoading = false // Stop loading
                                errorMessage = "Account created, but user ID not found."
                            }
                        } else {
                            // Handle Firebase Authentication creation failure
                            isLoading = false // Stop loading
                            errorMessage = task.exception?.message ?: "Account creation failed."
                            Toast.makeText(context, "Account creation failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            enabled = !isLoading // Disable button while loading
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Link to Sign In
        TextButton(
            onClick = onNavigateToSignIn, // Call the lambda provided by NavGraph
            enabled = !isLoading // Disable while loading
        ) {
            Text("Already have an account? Sign In")
        }

        // Show progress indicator if loading
        AnimatedVisibility(visible = isLoading) {
            Spacer(modifier = Modifier.height(16.dp)) // Add space above indicator
            CircularProgressIndicator()
        }
    }
}