package com.example.mediscannerai.presentation.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mediscannerai.data.local.AppDatabase
import com.example.mediscannerai.data.local.ReportEntity
import com.example.mediscannerai.presentation.scanner.MarkdownText

private enum class SavedTab { Explanation, Questions, RawText }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedReportScreen(reportId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<ReportEntity?>(null) }
    var tab by remember { mutableStateOf(SavedTab.Explanation) }

    LaunchedEffect(reportId) {
        report = AppDatabase.getInstance(context).reportDao().getById(reportId)
        isLoaded = true
    }

    val loadedReport = report

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(loadedReport?.name ?: "Saved Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when {
                !isLoaded -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                loadedReport == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "This report could not be found. It may have been deleted.",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = tab == SavedTab.Explanation,
                            onClick = { tab = SavedTab.Explanation },
                            label = { Text("Explanation") }
                        )
                        FilterChip(
                            selected = tab == SavedTab.Questions,
                            onClick = { tab = SavedTab.Questions },
                            label = { Text("Doctor Questions") }
                        )
                        FilterChip(
                            selected = tab == SavedTab.RawText,
                            onClick = { tab = SavedTab.RawText },
                            label = { Text("Raw Text") }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (tab) {
                            SavedTab.Explanation -> {
                                if (loadedReport.aiSummary.isBlank()) {
                                    CenteredNote("No explanation was saved for this report.")
                                } else {
                                    SelectionContainer {
                                        MarkdownText(
                                            markdown = loadedReport.aiSummary,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .verticalScroll(rememberScrollState())
                                        )
                                    }
                                }
                            }

                            SavedTab.Questions -> {
                                val questions = loadedReport.doctorQuestions
                                    .lines()
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                if (questions.isEmpty()) {
                                    CenteredNote("No doctor questions were saved for this report.")
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        questions.forEach { question ->
                                            Text("•  $question")
                                        }
                                    }
                                }
                            }

                            SavedTab.RawText -> {
                                SelectionContainer {
                                    Text(
                                        text = loadedReport.rawText,
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .verticalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "This is educational information only, not a medical diagnosis. " +
                                    "Always consult a qualified healthcare professional about your results.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredNote(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}