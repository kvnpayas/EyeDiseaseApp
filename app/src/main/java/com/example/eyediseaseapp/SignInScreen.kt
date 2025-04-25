package com.example.eyediseaseapp

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.eyediseaseapp.ui.theme.EyeDiseaseAppTheme
import com.example.eyediseaseapp.util.NavigationUtils.userDocumentExists
import com.example.eyediseaseapp.util.UserUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    navController: NavHostController, // Or remove if only using lambdas
    onSignInSuccess: (userId: String) -> Unit,
    onNavigateToSignUp: () -> Unit
    // Potentially add: onGoogleSignInRequested: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


// --- Google Sign-In Setup ---
    // Configure Google Sign In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // Use your web_client_id from google-services.json
            .requestEmail()
            .build()
    }

    // Build a GoogleSignInClient with the options specified by gso.
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val userUtils = remember { UserUtils() }

    // ActivityResultLauncher for Google Sign-In Intent
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // isLoading is handled within the launch block below
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                val userEmail =
                    account.email // Get email from Google account if needed for document

                if (idToken != null) {
                    isLoading = true // Start loading for Firebase auth and document check/creation
                    val credential = GoogleAuthProvider.getCredential(idToken, null)

                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                val userId = authTask.result?.user?.uid
                                if (userId != null) {
                                    // *** Firebase Auth with Google SUCCESSFUL ***

                                    // --- NOW, CHECK/CREATE USER DOCUMENT AND THEN CALL onSignInSuccess ---
                                    coroutineScope.launch { // Use coroutine scope here
                                        isLoading = true // Ensure loading stays true during async work

                                        val maxRetries = 3 // Define how many times to retry
                                        val initialDelayMillis = 1000L // Initial delay in milliseconds (e.g., 1 second)
                                        var success = false
                                        var lastException: Exception? = null

                                        for (attempt in 1..maxRetries) {
                                            println("SignInScreen Debug: Firestore attempt $attempt for UID: $userId")
                                            try {
                                                val docExists = userDocumentExists(userId) // <-- Calls utility
                                                println("SignInScreen Debug: userDocumentExists returned: $docExists on attempt $attempt.")

                                                if (!docExists) {
                                                    println("SignInScreen Debug: Document does not exist, creating...")
                                                    userUtils.createUserDocument(userId, userEmail) // <-- Calls utility
                                                    println("SignInScreen Debug: Document creation completed.")
                                                } else {
                                                    println("SignInScreen Debug: Document already exists.")
                                                }

                                                success = true // If we got here, Firestore ops succeeded for this attempt
                                                break // Exit the retry loop

                                            } catch (e: Exception) {
                                                lastException = e // Store the error
                                                Log.e("SignInScreen Debug", "Attempt $attempt failed for UID $userId: ${e.message}", e)

                                                if (attempt < maxRetries) {
                                                    val delayTime = initialDelayMillis * attempt
                                                    println("SignInScreen Debug: Retrying Firestore in $delayTime ms...")
                                                    delay(delayTime) // Wait before the next attempt
                                                }
                                                // If max retries, loop finishes
                                            }
                                        }

                                        isLoading = false // <-- Correctly set false AFTER the loop finishes

                                        if (success) { // Check if any attempt in the loop succeeded
                                            println("SignInScreen Debug: Firestore ops successful for UID: $userId. Calling success callback.")
                                            onSignInSuccess(userId) // <-- Correctly call success callback on success
                                        } else {
                                            // All retries failed
                                            println("SignInScreen Debug: Firestore ops failed after $maxRetries attempts for UID: ${userId}.")
                                            errorMessage = "Failed to set up user data after login: ${lastException?.message ?: "Unknown error"}." // Set final error
                                            // Decide what to do on final failure (e.g., auth.signOut())
                                        }
                                    }


                                } else {
                                    isLoading = false // Stop loading
                                    errorMessage =
                                        "Google Sign-In succeeded but Firebase user ID not found."
                                }
                            } else {
                                isLoading = false // Stop loading
                                // If sign in fails at Firebase auth step
                                errorMessage = authTask.exception?.message
                                    ?: "Firebase Authentication with Google failed."
                                Toast.makeText(
                                    context,
                                    "Authentication Failed.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    isLoading = false // Stop loading
                    errorMessage = "Google Sign-In failed: ID Token not found."
                    Toast.makeText(context, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: ApiException) {
                isLoading = false // Stop loading
                // Google Sign In failed at the getAccount step
                errorMessage = "Google sign in failed: ${e.message}"
                Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                isLoading = false // Stop loading
                // Catch any other exceptions before ApiException
                errorMessage =
                    "An unexpected error occurred during Google Sign-In flow: ${e.message}"
                Toast.makeText(
                    context,
                    "An unexpected error occurred: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            isLoading = false // Stop loading on result (even if not OK)
            // Handle non-OK results (e.g., user cancelled, error before starting)
            if (result.resultCode == Activity.RESULT_CANCELED) {
                println("Google Sign-In cancelled by user.")
                // Clear error message if it was from a previous attempt
                if (errorMessage != null && errorMessage!!.contains("Google Sign-In activity failed")) {
                    errorMessage = null
                }
            } else {
                errorMessage =
                    "Google Sign-In activity failed with result code: ${result.resultCode}"
                Toast.makeText(context, "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // --- End Google Sign-In Setup ---


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // Center horizontally
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.scan_eye),
            contentDescription = "image 1",
            colorFilter = ColorFilter.tint(colorResource(id = R.color.darkPrimary)),
            modifier = Modifier
                .width(80.dp)
                .height(80.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(32.dp))
        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        var passwordVisible by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(15.dp))

        // Error Message Text
        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.darkPrimary)
            ),
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    errorMessage = null
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val userId = task.result?.user?.uid
                                if (userId != null) {
                                    onSignInSuccess(userId) // Notify NavGraph
                                } else {
                                    errorMessage = "Login succeeded but user ID not found."
                                }
                            } else {
                                errorMessage = task.exception?.message ?: "Login failed."
                            }
                        }
                } else {
                    errorMessage = "Please enter email and password."
                }
            }, enabled = !isLoading
        ) {
            Text("Sign In")
        }

        Spacer(modifier = Modifier.height(15.dp))
        Text(
            text = "OR",
        )
        Spacer(modifier = Modifier.height(15.dp))
        // Google Sign In Button
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.google_blue) // Assuming google_blue is in colors.xml
            ),
            onClick = {
                isLoading = true // Indicate loading starts when initiating Google flow
                errorMessage = null // Clear previous errors
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            },
            enabled = !isLoading // Disable button while loading
        ) {
            Text("Sign in with Google")
        }

        Spacer(modifier = Modifier.height(15.dp))

        TextButton(
            onClick = onNavigateToSignUp,
            enabled = !isLoading // Disable button while loading
        ) {
            Text("Don't have an account? Sign Up")
        }

        // Show progress indicator if loading
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp)) // Add space above indicator
            CircularProgressIndicator()
        }
    }
}
