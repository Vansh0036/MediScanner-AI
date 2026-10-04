package com.example.mediscannerai.presentation.scanner

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
import com.example.mediscannerai.data.local.ReportSessionHolder
import com.example.mediscannerai.domain.usecase.GenerateAiExplanationUseCase
import kotlinx.coroutines.launch

private sealed class ExplanationState {
    data object Loading : ExplanationState()
    data class Success(val text: String) : ExplanationState()
    data class Error(val message: String) : ExplanationState()
    data object NoReport : ExplanationState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiExplanationScreen(
    onBack: () -> Unit,
    onViewDoctorQuestions: () -> Unit,
    onDone: () -> Unit
) {
    var state by remember {
        mutableStateOf<ExplanationState>(
            ReportSessionHolder.explanation?.let { ExplanationState.Success(it) }
                ?: ExplanationState.Loading
        )
    }
    var retryTrigger by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(ReportSessionHolder.savedReportId != null) }
    var saveFailed by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(retryTrigger) {
        val cached = ReportSessionHolder.explanation
        if (cached != null) {
            state = ExplanationState.Success(cached)
            return@LaunchedEffect
        }
        val report = ReportSessionHolder.currentReport
        if (report == null) {
            state = ExplanationState.NoReport
            return@LaunchedEffect
        }
        state = ExplanationState.Loading
        state = try {
            val explanation = GenerateAiExplanationUseCase().invoke(report)
            ReportSessionHolder.explanation = explanation
            ExplanationState.Success(explanation)
        } catch (e: Exception) {
            ExplanationState.Error(e.localizedMessage ?: "Something went wrong. Please try again.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Explanation") },
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
            when (val current = state) {
                is ExplanationState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generating an explanation…")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "This app does not diagnose. It only explains what is on your report.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is ExplanationState.NoReport -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No report was found to explain. Please scan or upload a report first.",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is ExplanationState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                current.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { retryTrigger++ }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is ExplanationState.Success -> {
                    Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        SelectionContainer {
                            MarkdownText(
                                markdown = current.text,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                saveFailed = false
                                val ok = saveCurrentReport(context)
                                if (ok) isSaved = true else saveFailed = true
                                isSaving = false
                            }
                        },
                        enabled = !isSaving && !isSaved,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                isSaving -> "Saving…"
                                isSaved -> "Saved ✓"
                                else -> "Save Report"
                            }
                        )
                    }
                    if (saveFailed && !showLeaveDialog) {
                        Text(
                            "Couldn't save the report. Please try again.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onViewDoctorQuestions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Questions for My Doctor")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            if (isSaved) onDone() else showLeaveDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Home")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
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

    if (showLeaveDialog) {
        UnsavedReportDialog(
            isSaving = isSaving,
            errorMessage = if (saveFailed) "Couldn't save the report. Please try again." else null,
            onSaveAndLeave = {
                scope.launch {
                    isSaving = true
                    saveFailed = false
                    val ok = saveCurrentReport(context)
                    isSaving = false
                    if (ok) {
                        isSaved = true
                        showLeaveDialog = false
                        onDone()
                    } else {
                        saveFailed = true
                    }
                }
            },
            onLeaveWithoutSaving = {
                showLeaveDialog = false
                onDone()
            },
            onCancel = {
                saveFailed = false
                showLeaveDialog = false
            }
        )
    }
}