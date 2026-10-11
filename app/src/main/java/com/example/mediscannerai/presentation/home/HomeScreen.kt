package com.example.mediscannerai.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.mediscannerai.presentation.history.HistoryViewModel
import com.example.mediscannerai.presentation.navigation.Screen
import com.example.mediscannerai.presentation.profile.ProfileChip
import com.example.mediscannerai.presentation.profile.ProfileSwitcher
import com.example.mediscannerai.presentation.reminder.ReminderViewModel
import com.example.mediscannerai.reminder.ReminderScheduler
import com.example.mediscannerai.ui.theme.MediScannerAITheme

private data class HomeReport(val id: Long, val name: String)

/** Connects the screen to the real data and navigation. */
@Composable
fun HomeScreen(navController: NavHostController) {
    val historyViewModel: HistoryViewModel = viewModel()
    val reports by historyViewModel.reports.collectAsStateWithLifecycle()

    val reminderViewModel: ReminderViewModel = viewModel()
    val medicines by reminderViewModel.medicines.collectAsStateWithLifecycle()
    val slotTimes by reminderViewModel.slotTimes.collectAsStateWithLifecycle()
    val nextReminder = remember(medicines, slotTimes) {
        medicines?.let { ReminderScheduler.nextReminderLabel(it, slotTimes) }
    }

    HomeContent(
        reports = reports.map { HomeReport(it.id, it.name) },
        nextReminder = nextReminder,
        profileSwitcher = {
            ProfileSwitcher(onManage = { navController.navigate(Screen.Profiles.route) })
        },
        onNavigate = { route -> navController.navigate(route) },
        onOpenReport = { id -> navController.navigate(Screen.SavedReport.buildRoute(id)) }
    )
}

/** Only draws what it is given, so the Android Studio preview can show it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    reports: List<HomeReport>,
    nextReminder: String?,
    profileSwitcher: @Composable () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenReport: (Long) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MediScanner AI", fontWeight = FontWeight.Bold) },
                actions = {
                    profileSwitcher()
                    IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val tabs = listOf(
                    Triple("Home", Icons.Default.Home, null as String?),
                    Triple("History", Icons.Default.List, Screen.ReportHistory.route),
                    Triple("Reminders", Icons.Default.Notifications, Screen.MedicineReminders.route),
                    Triple("Medicine", Icons.Default.Medication, Screen.MedicineInfo.route),
                    Triple("Trends", Icons.Default.DateRange, Screen.HealthTrends.route)
                )
                tabs.forEachIndexed { index, (label, icon, route) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            // Home (no route) does nothing: we are already there.
                            route?.let(onNavigate)
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroBanner()

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PrimaryActionButton(
                    label = "Scan Report",
                    icon = Icons.Default.PhotoCamera,
                    container = colors.primary,
                    content = colors.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Scan.route) }
                )
                PrimaryActionButton(
                    label = "Upload Report",
                    icon = Icons.Default.UploadFile,
                    container = colors.secondary,
                    content = colors.onSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.Upload.route) }
                )
            }

            Card(
                onClick = { onNavigate(Screen.ReportHistory.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.List, colors.primary, colors.primaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Recent Reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (reports.isEmpty()) {
                        Text(
                            "No reports yet. Scan or upload your first report to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    } else {
                        reports.take(3).forEach { report ->
                            Text(
                                report.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenReport(report.id) }
                                    .padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            SectionCard(
                title = "Medicine Reminders",
                icon = Icons.Default.Notifications,
                description = nextReminder
                    ?: "Set your medicines by meal time and see your daily schedule.",
                accent = colors.primary,
                accentContainer = colors.primaryContainer,
                onClick = { onNavigate(Screen.MedicineReminders.route) }
            )

            SectionCard(
                title = "Medicine Information",
                icon = Icons.Default.Medication,
                description = "Search general educational information about medicines.",
                accent = colors.secondary,
                accentContainer = colors.secondaryContainer,
                onClick = { onNavigate(Screen.MedicineInfo.route) }
            )

            SectionCard(
                title = "Health Trends",
                icon = Icons.Default.DateRange,
                description = "Once you've saved a few reports, track changes over time here.",
                accent = colors.tertiary,
                accentContainer = colors.tertiaryContainer,
                onClick = { onNavigate(Screen.HealthTrends.route) }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeroBanner() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(colors.primary, colors.secondary)))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Understand your lab reports",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Scan or upload a medical report and get a simple, " +
                        "educational explanation. This app does not diagnose or " +
                        "replace your doctor.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimary.copy(alpha = 0.92f)
            )
        }
    }
}

@Composable
private fun PrimaryActionButton(
    label: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.height(104.dp),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color, background: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    description: String,
    accent: Color,
    accentContainer: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon, accent, accentContainer)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MediScannerAITheme {
        HomeContent(
            reports = listOf(
                HomeReport(1, "Lab report - 06/10/2026"),
                HomeReport(2, "Lab report - 17/03/2026")
            ),
            nextReminder = "Next: After dinner, today 08:30 pm \u00B7 2 medicines",
            profileSwitcher = { ProfileChip(name = "Me", onClick = {}) },
            onNavigate = {},
            onOpenReport = {}
        )
    }
}