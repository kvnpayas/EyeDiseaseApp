package com.example.eyediseaseapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppDrawerContent(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    ModalDrawerSheet { // Material 3 drawer sheet
        Row( // Use Row to place icon and text side by side
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.primary))// Make the row fill the width of the drawer
                .padding(16.dp), // Apply padding to the entire row
            verticalAlignment = Alignment.CenterVertically // Align icon and text vertically in the center
        ) {
            Image( // Add the app icon
                painter = painterResource(id = R.drawable.eye_logo), // Your app icon resource
                contentDescription = "App Icon", // Content description for accessibility
                modifier = Modifier
                    .size(100.dp) // Set the size of the icon (adjust as needed)
                    .padding(end = 16.dp) // Add space between the icon and the text
            )
            Text( // Add the menu title text
                text = "Eye Disease Menu",
                fontWeight = FontWeight.Bold, // Make the text bold
                color = Color.White, // Change the text color to white
                fontSize = 24.sp
            )
        }

        Divider() // Your divider below the title/icon row
        Spacer(modifier = Modifier.height(30.dp)) // Your spacer

        // --- Example menu items ---
        NavigationDrawerItem( // Material 3 item
            label = {
                Text(
                    text = "Patient Home", // Change the text string
                    fontWeight = if (navController.currentDestination?.route == Screen.PatientHome.route) FontWeight.Bold else FontWeight.Normal, // Bold when selected
                    color = if (navController.currentDestination?.route == Screen.PatientHome.route) colorResource(
                        id = R.color.darkPrimary
                    ) else Color.Black // White when selected
                )
            },
            selected = navController.currentDestination?.route == Screen.PatientHome.route,
            onClick = {
                scope.launch { drawerState.close() } // Close drawer on item click
                navController.navigate(Screen.PatientHome.route) {
                    // Optional: Pop back stack logic
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.padding(0.dp),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = colorResource(id = R.color.extraLightPrimary), // Change background color when selected
                unselectedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(0.dp)

        )
        NavigationDrawerItem(
            label = {
                Text(
                    text = "About Us", // Change the text string
                    fontWeight = if (navController.currentDestination?.route == Screen.AboutUs.route) FontWeight.Bold else FontWeight.Normal, // Bold when selected
                    color = if (navController.currentDestination?.route == Screen.AboutUs.route) colorResource(
                        id = R.color.darkPrimary
                    ) else Color.Black // White when selected
                )
            },
            selected = navController.currentDestination?.route == Screen.AboutUs.route,
            onClick = {
                scope.launch { drawerState.close() } // Close drawer on item click
                navController.navigate(Screen.AboutUs.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier.padding(0.dp),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = colorResource(id = R.color.extraLightPrimary), // Change background color when selected
                unselectedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(0.dp)

        )

        Spacer(modifier = Modifier.weight(1f))

        // --- Divider before Logout ---
        Divider()

        // --- Log Out Item ---
        NavigationDrawerItem(
            label = {
                Text(
                    text = "Log Out",
                    color = colorResource(id = R.color.darkPrimary),
                    fontSize = 20.sp, // Change the text size
                    fontWeight = FontWeight.Medium // Change the font weight
                )
            },
            selected = false, // Logout is an action, not a selected state
            onClick = {
                scope.launch { drawerState.close() } // Close drawer first
                FirebaseAuth.getInstance().signOut() // Perform logout action
                println("User logged out from Drawer.")
                navController.navigate(Screen.SignIn.route) { // Navigate back to sign-in
                    popUpTo(Screen.AuthCheck.route) { inclusive = true } // Clear stack
                    launchSingleTop = true
                }
            },
            // --- Add the icon parameter here ---
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.log_out), // <-- Use your custom drawable resource ID
                    contentDescription = "Log Out Icon", // Accessibility
                    modifier = Modifier.size(35.dp),
                    tint = colorResource(id = R.color.darkPrimary)
                )
            },
            // ... other parameters (modifier, colors, shape) ...
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp),
            shape = RoundedCornerShape(0.dp)
        )
        // --- End Log Out Item ---

        // Optional: Add some padding at the very bottom
        Spacer(modifier = Modifier.height(8.dp))
    }
}