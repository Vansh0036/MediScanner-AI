package com.example.mediscannerai.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediscannerai.data.local.ReportEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenReport: (Long) -> Unit = {},
    viewModel: HistoryViewModel = viewModel()
) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    var reportToDelete by remember { mutableStateOf<ReportEntity?>(null) }
    var reportToRename by remember { mutableStateOf<ReportEntity?>(null) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No saved reports yet. Scan or upload a report and tap Save Report.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    ReportCard(
                        report = report,
                        onClick = { onOpenReport(report.id) },
                        onRename = {
                            newName = report.name
                            reportToRename = report
                        },
                        onDelete = { reportToDelete = report }
                    )
                }
            }
        }
    }

    reportToRename?.let { report ->
        AlertDialog(
            onDismissRequest = { reportToRename = null },
            title = { Text("Rename report") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { if (it.length <= 60) newName = it },
                    label = { Text("Report name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        viewModel.rename(report.id, newName)
                        reportToRename = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { reportToRename = null }) { Text("Cancel") }
            }
        )
    }

    reportToDelete?.let { report ->
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text("Delete report?") },
            text = { Text("This will permanently remove \"${report.name}\" from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(report.id)
                    reportToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { reportToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ReportCard(
    report: ReportEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val savedOn = remember(report.createdAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(report.createdAt))
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(report.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Saved $savedOn",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = "Rename report")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete report")
            }
        }
    }
}