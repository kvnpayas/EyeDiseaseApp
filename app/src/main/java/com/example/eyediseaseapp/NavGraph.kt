package com.example.eyediseaseapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController)
        }
        composable("aboutUs") {
            AboutUsScreen(navController)
        }
        composable("imageClassification") {
            ImageClassificationScreen(navController)
        }
        composable("camera") {
            CameraScreen()
        }
        composable("learnMore") {
            EducationalContentScreen(navController)
        }
    }
}