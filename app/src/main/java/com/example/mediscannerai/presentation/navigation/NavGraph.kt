package com.example.mediscannerai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mediscannerai.presentation.home.HomeScreen
import com.example.mediscannerai.presentation.scanner.AiExplanationScreen
import com.example.mediscannerai.presentation.scanner.OcrResultScreen
import com.example.mediscannerai.presentation.scanner.ScanReportScreen
import com.example.mediscannerai.presentation.scanner.UploadReportScreen
import java.net.URLDecoder

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
            ScanReportScreen(
                onBack = { navController.popBackStack() },
                onReportCaptured = { uri ->
                    navController.navigate(Screen.OcrResult.buildRoute(uri.toString(), isPdf = false))
                }
            )
        }
        composable(Screen.Upload.route) {
            UploadReportScreen(
                onBack = { navController.popBackStack() },
                onReportSelected = { uri, isPdf ->
                    navController.navigate(Screen.OcrResult.buildRoute(uri.toString(), isPdf))
                }
            )
        }
        composable(Screen.ReportPreview.route) {
            PlaceholderScreen("Report Preview") { navController.popBackStack() }
        }
        composable(
            route = Screen.OcrResult.route,
            arguments = listOf(
                navArgument("fileUri") { type = NavType.StringType },
                navArgument("isPdf") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("fileUri") ?: ""
            val fileUri = URLDecoder.decode(encodedUri, "UTF-8")
            val isPdf = backStackEntry.arguments?.getBoolean("isPdf") ?: false
            OcrResultScreen(
                fileUriString = fileUri,
                isPdf = isPdf,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Screen.AiExplanation.route) }
            )
        }
        composable(Screen.AiExplanation.route) {
            AiExplanationScreen(onBack = { navController.popBackStack() })
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