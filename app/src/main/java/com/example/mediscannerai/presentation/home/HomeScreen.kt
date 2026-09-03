

package com.example.mediscannerai.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mediscannerai.ui.theme.MediScannerAITheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MediScanner AI", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Triple("Home", Icons.Default.Home, 0),
                    Triple("History", Icons.Default.List, 1),
                    Triple("Medicine", Icons.Default.Favorite, 2),
                    Triple("Trends", Icons.Default.DateRange, 3),
                    Triple("Settings", Icons.Default.Settings, 4)
                )
                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Welcome + description
            Column {
                Text(
                    text = "Understand your lab reports",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scan or upload a medical report and get a simple, " +
                            "educational explanation. This app does not diagnose or " +
                            "replace your doctor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Primary action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PrimaryActionButton(
                    label = "Scan Report",
                    icon = Icons.Default.PhotoCamera,
                    modifier = Modifier.weight(1f),
                    onClick = { /* Phase 3: navigate to Scan screen */ }
                )
                PrimaryActionButton(
                    label = "Upload Report",
                    icon = Icons.Default.UploadFile,
                    modifier = Modifier.weight(1f),
                    onClick = { /* Phase 3: navigate to Upload screen */ }
                )
            }

            SectionCard(
                title = "Recent Reports",
                icon = Icons.Default.List,
                emptyText = "No reports yet. Scan or upload your first report to get started."
            )

            SectionCard(
                title = "Medicine Information",
                icon = Icons.Default.Favorite,
                emptyText = "Search general educational information about medicines."
            )

            SectionCard(
                title = "Health Trends",
                icon = Icons.Default.DateRange,
                emptyText = "Once you've saved a few reports, track changes over time here."
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PrimaryActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.height(96.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    emptyText: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MediScannerAITheme {
        HomeScreen()
    }
}