package com.example.mediscannerai.presentation.reminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediscannerai.data.local.ReminderMedicineEntity
import com.example.mediscannerai.domain.model.MealSlot
import com.example.mediscannerai.domain.model.ScheduleMasks
import java.time.DayOfWeek
import java.time.format.TextStyle as DayStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicineEditScreen(
    medicineId: Long,
    onBack: () -> Unit,
    viewModel: ReminderViewModel = viewModel()
) {
    val isNew = medicineId <= 0L
    var loaded by rememberSaveable { mutableStateOf(isNew) }
    var missing by remember { mutableStateOf(false) }

    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var slotsMask by rememberSaveable { mutableIntStateOf(0) }
    var daysMask by rememberSaveable { mutableIntStateOf(ScheduleMasks.ALL_DAYS) }
    var startDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var endDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var paused by rememberSaveable { mutableStateOf(false) }
    var createdAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    var error by remember { mutableStateOf<String?>(null) }
    var datePickerFor by remember { mutableStateOf<Int?>(null) } // 0 = start, 1 = end
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(medicineId) {
        if (!isNew && !loaded) {
            val m = viewModel.load(medicineId)
            if (m == null) {
                missing = true
            } else {
                name = m.name
                amount = m.amountNote
                notes = m.notes
                slotsMask = m.slotsMask
                daysMask = m.daysMask
                startDay = m.startDay
                endDay = m.endDay
                paused = m.isPaused
                createdAt = m.createdAt
                loaded = true
            }
        }
    }

    fun save() {
        val cleanName = name.trim()
        val start = startDay
        val end = endDay
        error = when {
            cleanName.isEmpty() -> "Please enter the medicine name."
            slotsMask == 0 -> "Choose at least one time of day."
            daysMask == 0 -> "Choose at least one day."
            start != null && end != null && end < start -> "The end date cannot be before the start date."
            else -> null
        }
        if (error != null) return
        viewModel.save(
            ReminderMedicineEntity(
                id = if (isNew) 0L else medicineId,
                name = cleanName,
                amountNote = amount.trim(),
                notes = notes.trim(),
                slotsMask = slotsMask,
                daysMask = daysMask,
                startDay = start,
                endDay = end,
                isPaused = paused,
                createdAt = createdAt,
                profileId = 1L
            ),
            onDone = onBack
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add medicine" else "Edit medicine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew && loaded) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete medicine")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            missing -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("This medicine could not be found.", textAlign = TextAlign.Center)
                }
            }

            !loaded -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 60) name = it },
                        label = { Text("Medicine name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.length <= 60) amount = it },
                        label = { Text("How much (as written on your prescription)") },
                        placeholder = { Text("For example: 1 tablet, 75 mg") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("When do you take it?", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealSlot.entries.forEach { slot ->
                            FilterChip(
                                selected = (slotsMask and slot.bit) != 0,
                                onClick = { slotsMask = slotsMask xor slot.bit },
                                label = { Text(slot.label) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Which days?",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { daysMask = ScheduleMasks.ALL_DAYS }) {
                            Text("Every day")
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DayOfWeek.values().forEach { day ->
                            val bit = ScheduleMasks.dayBit(day)
                            FilterChip(
                                selected = (daysMask and bit) != 0,
                                onClick = { daysMask = daysMask xor bit },
                                label = {
                                    Text(day.getDisplayName(DayStyle.SHORT, Locale.getDefault()))
                                }
                            )
                        }
                    }

                    Text("For how long? (optional)", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { datePickerFor = 0 },
                            modifier = Modifier.weight(1f)
                        ) { Text("Start: " + (startDay?.let { formatDay(it) } ?: "not set")) }
                        OutlinedButton(
                            onClick = { datePickerFor = 1 },
                            modifier = Modifier.weight(1f)
                        ) { Text("End: " + (endDay?.let { formatDay(it) } ?: "not set")) }
                    }
                    if (startDay != null || endDay != null) {
                        TextButton(onClick = { startDay = null; endDay = null }) {
                            Text("Clear dates")
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 120) notes = it },
                        label = { Text("Note (optional)") },
                        placeholder = { Text("Copy any note from your prescription") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isNew) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reminders on", modifier = Modifier.weight(1f))
                            Switch(checked = !paused, onCheckedChange = { paused = !it })
                        }
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = { save() },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("Save") }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Enter this exactly as written on your prescription. The app does " +
                                    "not check, suggest or change doses. Ask your doctor or pharmacist " +
                                    "if you are unsure.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    datePickerFor?.let { which ->
        val initial = (if (which == 0) startDay else endDay)?.let { it * 86_400_000L }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val day = millis / 86_400_000L
                        if (which == 0) startDay = day else endDay = day
                    }
                    datePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerFor = null }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this medicine?") },
            text = { Text("\"${name}\" and its reminders will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(medicineId, onDone = onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}