package com.example.eyediseaseapp

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class) // Use appropriate opt-in
@Composable
fun AppBarWithDrawerButton(
    title: String,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    TopAppBar(
        title = {  },
        navigationIcon = {
            IconButton(onClick = {
                scope.launch {
                    drawerState.open() // Open the drawer
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Menu, // Hamburger icon
                    contentDescription = "Open Drawer",
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent, // Set the background color to Transparent
            // Optional: Adjust other colors if needed for contrast against the content behind
            // titleContentColor = Color.Black,
            // navigationIconContentColor = Color.Black,
            // actionIconContentColor = Color.Black
        )
    )
}