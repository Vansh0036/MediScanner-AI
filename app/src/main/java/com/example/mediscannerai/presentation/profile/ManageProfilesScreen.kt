package com.example.mediscannerai.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediscannerai.data.local.ProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProfilesScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ProfileEntity?>(null) }
    var deleting by remember { mutableStateOf<ProfileEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("People") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add person") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Reports, trend charts and reminders are kept separate for each person. " +
                        "Tap a person to view and save for them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            profiles.forEach { p ->
                val isActive = p.id == active?.id
                Card(
                    onClick = { viewModel.select(p.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isActive, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            if (isActive) {
                                Text(
                                    "Viewing now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { renaming = p }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename ${p.name}")
                        }
                        IconButton(
                            onClick = { deleting = p },
                            enabled = profiles.size > 1
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete ${p.name}")
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        NameDialog(
            title = "Add person",
            initial = "",
            takenNames = profiles.map { it.name },
            onSave = {
                viewModel.add(it)
                adding = false
            },
            onDismiss = { adding = false }
        )
    }

    renaming?.let { person ->
        NameDialog(
            title = "Rename",
            initial = person.name,
            takenNames = profiles.filter { it.id != person.id }.map { it.name },
            onSave = {
                viewModel.rename(person.id, it)
                renaming = null
            },
            onDismiss = { renaming = null }
        )
    }

    deleting?.let { person ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${person.name}?") },
            text = {
                Text(
                    "This permanently removes all of ${person.name}'s saved reports, " +
                            "medicines and reminders from this phone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(person.id)
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    takenNames: List<String>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    val clean = name.trim()
    val duplicate = takenNames.any { it.equals(clean, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) name = it },
                    label = { Text("Name") },
                    placeholder = { Text("For example: Dad") },
                    singleLine = true,
                    isError = duplicate,
                    modifier = Modifier.fillMaxWidth()
                )
                if (duplicate) {
                    Text(
                        "Someone with this name already exists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = clean.isNotEmpty() && !duplicate,
                onClick = { onSave(clean) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}