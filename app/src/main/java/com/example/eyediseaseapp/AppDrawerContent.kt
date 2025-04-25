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
    val currentUserId = auth.currentUser?.uid // Get the current user's UID

    // --- State for fetching user role ---
    var userRole by remember { mutableStateOf<String?>(null) } // Holds the fetched role ('user', 'admin', or null)
    var isLoadingRole by remember { mutableStateOf(true) } // True while fetching the role
    var roleError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUserId) {
        Log.d("AppDrawerContent", "LaunchedEffect: currentUserId changed to $currentUserId")
        if (currentUserId != null) {
            isLoadingRole = true
            roleError = null
            try {
                // Fetch the role using your existing utility function
                // fetchUserRole returns Screen? which we need to map back to a role string
                val destinationScreen = NavigationUtils.fetchUserRole(currentUserId)
                userRole = when (destinationScreen) {
                    Screen.PatientHome -> "user"
                    Screen.DoctorHome -> "admin"
                    else -> {
                        // If fetchUserRole returns SignIn or null, the role is not recognized or doc is missing
                        Log.w("AppDrawerContent", "Fetched unknown or missing role for $currentUserId, destination: $destinationScreen")
                        null // Treat as unknown role
                    }
                }
                Log.d("AppDrawerContent", "Fetched user role: $userRole for UID: $currentUserId")

            } catch (e: Exception) {
                roleError = e.message ?: "Failed to load user role."
                Log.e("AppDrawerContent", "Error fetching user role for $currentUserId: ${e.message}", e)
                userRole = null
            } finally {
                isLoadingRole = false
            }
        } else {

            userRole = null
            isLoadingRole = false
            roleError = "User not logged in."
            Log.d("AppDrawerContent", "User ID is null, resetting role state.")
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
            Text(
                text = "Eye Disease Menu",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp
            )
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
            // --- Conditionally Display Navigation Items based on Role ---

            // --- Patient Home / Doctor Home Item ---
            // If user is 'user', show Patient Home. If user is 'admin', show Doctor Home.
            val homeRoute = if (userRole == "admin") Screen.DoctorHome.route else Screen.PatientHome.route
            val homeLabel = if (userRole == "admin") "Doctor Home" else "Patient Home"

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