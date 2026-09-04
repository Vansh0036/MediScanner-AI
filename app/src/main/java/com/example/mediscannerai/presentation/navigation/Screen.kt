package com.example.mediscannerai.presentation.navigation


sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Scan : Screen("scan")
    data object Upload : Screen("upload")
    data object ReportPreview : Screen("report_preview")
    data object OcrResult : Screen("ocr_result")
    data object AiExplanation : Screen("ai_explanation")
    data object DoctorQuestions : Screen("doctor_questions")
    data object ReportHistory : Screen("report_history")
    data object MedicineInfo : Screen("medicine_info")
    data object MedicineReminders : Screen("medicine_reminders")
    data object HealthTrends : Screen("health_trends")
    data object Settings : Screen("settings")
}