package com.example.mediscannerai.presentation.scanner


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mediscannerai.data.local.ReportSessionHolder
import com.example.mediscannerai.domain.usecase.GenerateDoctorQuestionsUseCase

private sealed class QuestionsState {
    data object Loading : QuestionsState()
    data class Success(val questions: List<String>) : QuestionsState()
    data class Error(val message: String) : QuestionsState()
    data object NoReport : QuestionsState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorQuestionsScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf<QuestionsState>(QuestionsState.Loading) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTrigger) {
        val report = ReportSessionHolder.currentReport
        if (report == null) {
            state = QuestionsState.NoReport
            return@LaunchedEffect
        }
        state = QuestionsState.Loading
        state = try {
            val questions = GenerateDoctorQuestionsUseCase().invoke(report)
            QuestionsState.Success(questions)
        } catch (e: Exception) {
            QuestionsState.Error(e.localizedMessage ?: "Something went wrong. Please try again.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doctor Questions") },
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
                is QuestionsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Preparing questions for your doctor visit…")
                        }
                    }
                }

                is QuestionsState.NoReport -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No report was found. Please scan or upload a report first.",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is QuestionsState.Error -> {
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

                is QuestionsState.Success -> {
                    Text(
                        "Questions you might ask your doctor about this report:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(current.questions) { question ->
                            QuestionCard(question)
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
                            "These are discussion starters only, not medical advice. " +
                                    "Your doctor can give guidance specific to you.",
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
private fun QuestionCard(question: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.QuestionAnswer,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(question, style = MaterialTheme.typography.bodyMedium)
        }
    }
}