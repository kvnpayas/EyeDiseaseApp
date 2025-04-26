package com.example.eyediseaseapp

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.eyediseaseapp.util.NavigationUtils
import com.example.eyediseaseapp.util.UserUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppDrawerContent(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {

    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    // --- State for fetching user role ---
    var userRole by remember { mutableStateOf<String?>(null) }
    var isLoadingRole by remember { mutableStateOf(true) }
    var roleError by remember { mutableStateOf<String?>(null) }

    var userName by remember { mutableStateOf<String?>(null) }
    var isLoadingName by remember { mutableStateOf(true) }
    var nameError by remember { mutableStateOf<String?>(null) }

    // Instance of UserRepository (or UserUtils)
    val userRepository = remember { UserUtils() }

    LaunchedEffect(currentUserId) {
        Log.d("AppDrawerContent", "LaunchedEffect: currentUserId changed to $currentUserId")
        if (currentUserId != null) {
            // Fetch role
            isLoadingRole = true
            roleError = null
            userRole = try {
                val destinationScreen = NavigationUtils.fetchUserRole(currentUserId)
                when (destinationScreen) {
                    Screen.PatientHome -> "user"
                    Screen.DoctorHome -> "admin"
                    else -> {
                        Log.w("AppDrawerContent", "Fetched unknown or missing role for $currentUserId, destination: $destinationScreen")
                        null
                    }
                }
            } catch (e: Exception) {
                roleError = e.message ?: "Failed to load user role."
                Log.e("AppDrawerContent", "Error fetching user role for $currentUserId: ${e.message}", e)
                null
            } finally {
                isLoadingRole = false
            }

            // --- Fetch user name ---
            isLoadingName = true
            nameError = null
            userName = try {

                val userProfile = userRepository.getUser(currentUserId)
                userProfile?.name
            } catch (e: Exception) {
                nameError = e.message ?: "Failed to load user name."
                Log.e("AppDrawerContent", "Error fetching user name for $currentUserId: ${e.message}", e)
                null
            } finally {
                isLoadingName = false
            }

        } else {

            userRole = null
            isLoadingRole = false
            roleError = "User not logged in."

            userName = null
            isLoadingName = false
            nameError = "User not logged in."

            Log.d("AppDrawerContent", "User ID is null, resetting state.")
        }
    }

    ModalDrawerSheet {
        // --- Drawer Header (App Icon and Title) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.primary))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.eye_logo),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(100.dp)
                    .padding(end = 16.dp)
            )
            Column {
                Text(
                    text = "Welcome",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 24.sp
                )

                when {
                    isLoadingName -> Text("Loading name...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    nameError != null -> Text("Error loading name", color = Color.Red.copy(alpha = 0.7f), fontSize = 14.sp)
                    userName != null -> Text(userName!!, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp) // Display the fetched name
                    else -> Text("Guest", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp) // Default for logged out or no name
                }
            }

        }

        Divider()
        Spacer(modifier = Modifier.height(30.dp))

        // --- Display Loading/Error for Role Fetch ---
        if (isLoadingRole) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading menu...")
            }
        } else if (roleError != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error loading menu: ${roleError}", color = Color.Red, textAlign = TextAlign.Center)
            }
        } else {


            val homeRoute = if (userRole == "admin") Screen.DoctorHome.route else Screen.PatientHome.route
            val homeLabel = "Home"

            NavigationDrawerItem(
                label = {
                    Text(
                        text = homeLabel,
                        fontWeight = if (navController.currentDestination?.route == homeRoute) FontWeight.Bold else FontWeight.Normal,
                        color = if (navController.currentDestination?.route == homeRoute) colorResource(
                            id = R.color.darkPrimary
                        ) else Color.Black
                    )
                },
                selected = navController.currentDestination?.route == homeRoute,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(homeRoute) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = colorResource(id = R.color.extraLightPrimary),
                    unselectedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(0.dp)
            )


            if (userRole == "user") {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Patient History",
                            fontWeight = if (navController.currentDestination?.route == Screen.ResultHistory.route) FontWeight.Bold else FontWeight.Normal,
                            color = if (navController.currentDestination?.route == Screen.ResultHistory.route) colorResource(
                                id = R.color.darkPrimary
                            ) else Color.Black
                        )
                    },
                    selected = navController.currentDestination?.route == Screen.ResultHistory.route,
                    onClick = {
                        scope.launch { drawerState.close() } // Close drawer on item click
                        navController.navigate(Screen.ResultHistory.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = colorResource(id = R.color.extraLightPrimary),
                        unselectedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
            }

            if (userRole == "admin") {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "List of Patients",
                            fontWeight = if (navController.currentDestination?.route == Screen.PatientLists.route) FontWeight.Bold else FontWeight.Normal,
                            color = if (navController.currentDestination?.route == Screen.PatientLists.route) colorResource(
                                id = R.color.darkPrimary
                            ) else Color.Black
                        )
                    },
                    selected = navController.currentDestination?.route == Screen.PatientLists.route,
                    onClick = {
                        scope.launch { drawerState.close() } // Close drawer on item click
                        navController.navigate(Screen.PatientLists.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = colorResource(id = R.color.extraLightPrimary),
                        unselectedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
            }

            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Messages",
                        fontWeight = if (navController.currentDestination?.route == Screen.ConversationsList.route) FontWeight.Bold else FontWeight.Normal,
                        color = if (navController.currentDestination?.route == Screen.ConversationsList.route) colorResource(
                            id = R.color.darkPrimary
                        ) else Color.Black
                    )
                },
                selected = navController.currentDestination?.route == Screen.ConversationsList.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.ConversationsList.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = colorResource(id = R.color.extraLightPrimary),
                    unselectedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(0.dp)
            )


            NavigationDrawerItem(
                label = {
                    Text(
                        text = "About Us",
                        fontWeight = if (navController.currentDestination?.route == Screen.AboutUs.route) FontWeight.Bold else FontWeight.Normal,
                        color = if (navController.currentDestination?.route == Screen.AboutUs.route) colorResource(
                            id = R.color.darkPrimary
                        ) else Color.Black
                    )
                },
                selected = navController.currentDestination?.route == Screen.AboutUs.route,
                onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.AboutUs.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = colorResource(id = R.color.extraLightPrimary),
                    unselectedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(0.dp)
            )
        }


        Spacer(modifier = Modifier.weight(1f))


        Divider()

        NavigationDrawerItem(
            label = {
                Text(
                    text = "Log Out",
                    color = colorResource(id = R.color.darkPrimary),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                FirebaseAuth.getInstance().signOut()
                Log.d("AppDrawerContent", "User logged out from Drawer.")
                navController.navigate(Screen.SignIn.route) {
                    popUpTo(Screen.AuthCheck.route) { inclusive = true }
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.log_out),
                    contentDescription = "Log Out Icon",
                    modifier = Modifier.size(35.dp),
                    tint = colorResource(id = R.color.darkPrimary)
                )
            },
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
            shape = RoundedCornerShape(0.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}