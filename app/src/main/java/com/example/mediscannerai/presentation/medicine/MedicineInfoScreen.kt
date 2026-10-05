package com.example.mediscannerai.presentation.medicine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediscannerai.data.local.SavedMedicineEntity
import com.example.mediscannerai.presentation.scanner.MarkdownText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineInfoScreen(
    onBack: () -> Unit,
    viewModel: MedicineViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var toDelete by remember { mutableStateOf<SavedMedicineEntity?>(null) }
    var showClearAll by remember { mutableStateOf(false) }
    val canSearch = query.isNotBlank() && state !is MedicineUiState.Loading

    fun runSearch() {
        if (canSearch) {
            focusManager.clearFocus()
            viewModel.search(query)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medicine Information") },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Search a medicine name to read general, educational information about it. " +
                        "Medicines you look up are saved below so you can read them again later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = query,
                onValueChange = { if (it.length <= 60) query = it },
                label = { Text("Medicine name") },
                placeholder = { Text("For example: Paracetamol") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { runSearch() },
                enabled = canSearch,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Search")
            }

            when (val current = state) {
                is MedicineUiState.Idle -> {
                    Text(
                        "Results will appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }

                is MedicineUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Looking that up…")
                        }
                    }
                }

                is MedicineUiState.Success -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(current.name, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (current.fromSaved) {
                                    "Saved copy from ${dateFormat.format(Date(current.savedAt))}"
                                } else {
                                    "Saved to your medicines"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SelectionContainer {
                                MarkdownText(markdown = current.text)
                            }
                            if (current.fromSaved) {
                                TextButton(
                                    onClick = { viewModel.search(current.name, forceRefresh = true) },
                                    enabled = canSearch || state is MedicineUiState.Success
                                ) {
                                    Text("Look up again")
                                }
                            }
                        }
                    }
                }

                is MedicineUiState.NotFound -> {
                    Text(
                        "We couldn't recognise \"${current.name}\" as a medicine. " +
                                "Check the spelling, or try the generic name printed on the packet.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                }

                is MedicineUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            current.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { runSearch() }, enabled = canSearch) {
                            Text("Retry")
                        }
                    }
                }
            }

            // ---- Saved medicines ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your saved medicines",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (saved.isNotEmpty()) {
                    TextButton(onClick = { showClearAll = true }) { Text("Clear all") }
                }
            }

            if (saved.isEmpty()) {
                Text(
                    "Nothing saved yet. Medicines you look up will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                saved.forEach { item ->
                    Card(
                        onClick = {
                            query = item.name
                            focusManager.clearFocus()
                            viewModel.openSaved(item)
                            scope.launch { scrollState.animateScrollTo(0) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Saved ${dateFormat.format(Date(item.savedAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { toDelete = item }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete ${item.name}"
                                )
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "This is general educational information generated by an AI service, " +
                            "and it can be wrong or incomplete. It is not medical advice. Never " +
                            "start, stop or change a medicine without asking your doctor or pharmacist.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete saved medicine?") },
            text = { Text("\"${item.name}\" will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item.id)
                    toDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearAll) {
        AlertDialog(
            onDismissRequest = { showClearAll = false },
            title = { Text("Clear all saved medicines?") },
            text = { Text("All saved medicine searches will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearAll = false
                }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { showClearAll = false }) { Text("Cancel") }
            }
        )
    }
}