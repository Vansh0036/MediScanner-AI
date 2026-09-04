package com.example.mediscannerai.presentation.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mediscannerai.presentation.home.HomeScreen

@Composable
fun MediScannerNavGraph() {
    val navController: NavHostController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Scan.route) {
            PlaceholderScreen("Scan Report") { navController.popBackStack() }
        }
        composable(Screen.Upload.route) {
            PlaceholderScreen("Upload Report") { navController.popBackStack() }
        }
        composable(Screen.ReportPreview.route) {
            PlaceholderScreen("Report Preview") { navController.popBackStack() }
        }
        composable(Screen.OcrResult.route) {
            PlaceholderScreen("OCR Result") { navController.popBackStack() }
        }
        composable(Screen.AiExplanation.route) {
            PlaceholderScreen("AI Explanation") { navController.popBackStack() }
        }
        composable(Screen.DoctorQuestions.route) {
            PlaceholderScreen("Doctor Questions") { navController.popBackStack() }
        }
        composable(Screen.ReportHistory.route) {
            PlaceholderScreen("Report History") { navController.popBackStack() }
        }
        composable(Screen.MedicineInfo.route) {
            PlaceholderScreen("Medicine Information") { navController.popBackStack() }
        }
        composable(Screen.MedicineReminders.route) {
            PlaceholderScreen("Medicine Reminders") { navController.popBackStack() }
        }
        composable(Screen.HealthTrends.route) {
            PlaceholderScreen("Health Trends") { navController.popBackStack() }
        }
        composable(Screen.Settings.route) {
            PlaceholderScreen("Settings") { navController.popBackStack() }
        }
    }
}