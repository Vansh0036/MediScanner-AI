package com.example.mediscannerai.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediscannerai.data.local.ProfileEntity

/** The small chip that shows whose data is on screen. */
@Composable
fun ProfileChip(name: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 90.dp)
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    )
}

/** Chip plus the "who is this for?" dialog. Reads the real data. */
@Composable
fun ProfileSwitcher(onManage: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val active by viewModel.active.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }

    ProfileChip(name = active?.name ?: "Me", onClick = { open = true })

    if (open) {
        ProfilePickerDialog(
            profiles = profiles,
            activeId = active?.id,
            onSelect = {
                viewModel.select(it)
                open = false
            },
            onManage = {
                open = false
                onManage()
            },
            onDismiss = { open = false }
        )
    }
}

@Composable
fun ProfilePickerDialog(
    profiles: List<ProfileEntity>,
    activeId: Long?,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Whose health data?") },
        text = {
            Column {
                profiles.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(p.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = p.id == activeId, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(p.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onManage) { Text("Manage people") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}