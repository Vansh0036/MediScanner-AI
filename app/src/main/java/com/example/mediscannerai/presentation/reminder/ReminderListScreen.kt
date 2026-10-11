package com.example.mediscannerai.presentation.reminder

import android.Manifest
import android.os.Build
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mediscannerai.data.local.ReminderMedicineEntity
import com.example.mediscannerai.domain.model.MealSlot
import com.example.mediscannerai.domain.model.ScheduleMasks
import com.example.mediscannerai.reminder.ReminderPermissions
import com.example.mediscannerai.reminder.ReminderScheduler
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayStyle
import java.util.Locale

internal fun formatMinutes(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60)
        .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))

internal fun formatDay(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))

internal fun daysText(mask: Int): String {
    if (mask == ScheduleMasks.ALL_DAYS) return "Every day"
    return DayOfWeek.values()
        .filter { (mask and ScheduleMasks.dayBit(it)) != 0 }
        .joinToString(", ") { it.getDisplayName(DayStyle.SHORT, Locale.getDefault()) }
}

private fun slotsText(mask: Int): String =
    MealSlot.entries.filter { (mask and it.bit) != 0 }.joinToString(", ") { it.label }

private fun datesText(m: ReminderMedicineEntity): String? = when {
    m.startDay != null && m.endDay != null -> "${formatDay(m.startDay)} to ${formatDay(m.endDay)}"
    m.startDay != null -> "From ${formatDay(m.startDay)}"
    m.endDay != null -> "Until ${formatDay(m.endDay)}"
    else -> null
}

private fun untilText(minutes: Long): String = when {
    minutes < 1 -> "in less than a minute"
    minutes < 60 -> "in $minutes min"
    minutes < 24 * 60 -> "in ${minutes / 60} h" +
            if (minutes % 60 != 0L) " ${minutes % 60} min" else ""
    else -> {
        val days = minutes / (24 * 60)
        "in $days day" + if (days > 1) "s" else ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ReminderViewModel = viewModel()
) {
    val context = LocalContext.current
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    val times by viewModel.slotTimes.collectAsStateWithLifecycle()
    val taken by viewModel.takenToday.collectAsStateWithLifecycle()
    val todayEpoch by viewModel.todayEpoch.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingSlot by remember { mutableStateOf<MealSlot?>(null) }

    var notificationsOk by remember { mutableStateOf(true) }
    var exactOk by remember { mutableStateOf(true) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Checked again every time the screen comes back, for example from the settings page.
    LifecycleResumeEffect(Unit) {
        notificationsOk = ReminderPermissions.notificationsAllowed(context)
        exactOk = ReminderPermissions.exactAlarmsAllowed(context)
        viewModel.refreshDay()
        nowMillis = System.currentTimeMillis()
        onPauseOrDispose { }
    }

    // Keeps "in 25 min" fresh while the screen stays open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsOk = ReminderPermissions.notificationsAllowed(context)
        // If the phone will not show the permission box again, send the user to the settings page.
        if (!notificationsOk) ReminderPermissions.openNotificationSettings(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medicine Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add medicine") }
            )
        }
    ) { innerPadding ->
        val list = medicines
        if (list == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!notificationsOk) {
                    StatusCard(
                        title = "Notifications are off",
                        body = "Turn them on so your reminders can appear.",
                        button = "Turn on"
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            ReminderPermissions.openNotificationSettings(context)
                        }
                    }
                }
                if (!exactOk) {
                    StatusCard(
                        title = "Exact timing is off",
                        body = "Without it, reminders can arrive a few minutes late.",
                        button = "Allow"
                    ) { ReminderPermissions.openExactAlarmSettings(context) }
                }

                if (list.isNotEmpty()) {
                    val next = remember(list, times, nowMillis) {
                        ReminderScheduler.nextReminder(
                            medicines = list,
                            minutesFor = { times[it] ?: it.defaultMinutes },
                            now = ZonedDateTime.ofInstant(
                                Instant.ofEpochMilli(nowMillis),
                                ZonedDateTime.now().zone
                            )
                        )
                    }
                    NextReminderCard(next, nowMillis)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        label = { Text("Schedule") }
                    )
                    FilterChip(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        label = { Text("Medicines (${list.size})") }
                    )
                }

                when (tab) {
                    0 -> {
                        val todayDate = LocalDate.ofEpochDay(todayEpoch)
                        val dueToday = MealSlot.entries
                            .sortedBy { times[it] ?: it.defaultMinutes }
                            .map { slot ->
                                slot to list.filter { ReminderScheduler.isDue(it, slot, todayDate) }
                            }
                            .filter { it.second.isNotEmpty() }
                        val total = dueToday.sumOf { it.second.size }
                        val takenCount = dueToday.sumOf { (slot, meds) ->
                            meds.count { (it.id to slot.bit) in taken }
                        }

                        if (total == 0) {
                            Text(
                                if (list.isEmpty()) {
                                    "No medicines yet. Tap Add medicine to start."
                                } else {
                                    "Nothing is scheduled for today."
                                },
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            )
                        } else {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "$takenCount of $total taken today",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { takenCount.toFloat() / total },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            dueToday.forEach { (slot, meds) ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(
                                            horizontal = 16.dp, vertical = 12.dp
                                        )
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                slot.label,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                formatMinutes(times[slot] ?: slot.defaultMinutes),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        meds.forEach { m ->
                                            val isTaken = (m.id to slot.bit) in taken
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.setTaken(m.id, slot, !isTaken) }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isTaken,
                                                    onCheckedChange = { viewModel.setTaken(m.id, slot, it) }
                                                )
                                                Text(
                                                    m.name +
                                                            if (m.amountNote.isNotBlank()) " \u2014 ${m.amountNote}" else "",
                                                    textDecoration = if (isTaken) TextDecoration.LineThrough else null,
                                                    color = if (isTaken) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                "Tick a medicine when you have taken it. This is only your own checklist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    1 -> {
                        Text(
                            "Tap a time to change it. Every medicine linked to that slot follows.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MealSlot.entries
                            .sortedBy { times[it] ?: it.defaultMinutes }
                            .forEach { slot ->
                                val minutes = times[slot] ?: slot.defaultMinutes
                                val inSlot = list.filter {
                                    !it.isPaused && (it.slotsMask and slot.bit) != 0
                                }
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                slot.label,
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedButton(onClick = { editingSlot = slot }) {
                                                Icon(
                                                    Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(formatMinutes(minutes))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (inSlot.isEmpty()) {
                                            Text(
                                                "No medicines",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            inSlot.forEach { m ->
                                                Text(
                                                    "\u2022  " + m.name +
                                                            if (m.amountNote.isNotBlank()) " \u2014 ${m.amountNote}" else "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                    }

                    else -> {
                        if (list.isEmpty()) {
                            Text(
                                "No medicines yet. Tap Add medicine to create your first reminder.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            )
                        }
                        list.forEach { m ->
                            Card(onClick = { onEdit(m.id) }, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 16.dp, top = 12.dp, bottom = 12.dp, end = 12.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(m.name, style = MaterialTheme.typography.titleMedium)
                                        if (m.amountNote.isNotBlank()) {
                                            Text(m.amountNote, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            slotsText(m.slotsMask),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            daysText(m.daysMask) +
                                                    (datesText(m)?.let { "  \u00B7  $it" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (m.isPaused) {
                                            Text(
                                                "Paused",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = !m.isPaused,
                                        onCheckedChange = { viewModel.setPaused(m.id, !it) }
                                    )
                                }
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "If reminders do not appear on time",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Some phones (Xiaomi, Oppo, Vivo, Samsung) stop apps in the " +
                                    "background to save battery. Open the app settings, set " +
                                    "Battery saver to \"No restrictions\", and turn on " +
                                    "\"Autostart\" if your phone has it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { ReminderPermissions.openAppSettings(context) }
                            ) { Text("App settings") }
                            OutlinedButton(
                                onClick = {
                                    if (ReminderPermissions.notificationsAllowed(context)) {
                                        ReminderScheduler.scheduleTest(context)
                                        Toast.makeText(
                                            context,
                                            "A test reminder will appear in about a minute.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Turn notifications on first.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) { Text("Send test (1 min)") }
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
                        "These reminders only repeat what you enter. The app does not check " +
                                "doses or interactions. Always follow your doctor's prescription.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    editingSlot?.let { slot ->
        val current = times[slot] ?: slot.defaultMinutes
        val state = rememberTimePickerState(
            initialHour = current / 60,
            initialMinute = current % 60,
            is24Hour = DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text("${slot.label} time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSlotTime(slot, state.hour * 60 + state.minute)
                    editingSlot = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingSlot = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NextReminderCard(next: ReminderScheduler.NextReminder?, nowMillis: Long) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Next reminder",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (next == null) {
                Text(
                    "No upcoming reminders. Check that your medicines are not paused.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                val minutesLeft = Duration.between(
                    Instant.ofEpochMilli(nowMillis),
                    next.trigger.toInstant()
                ).toMinutes()
                val day = if (next.trigger.toLocalDate() == LocalDate.now()) {
                    "today"
                } else {
                    next.trigger.dayOfWeek.getDisplayName(DayStyle.SHORT, Locale.getDefault())
                }
                val time = formatMinutes(next.trigger.hour * 60 + next.trigger.minute)
                Text(
                    "${next.slot.label}, $day at $time",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    untilText(minutesLeft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    next.medicines.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, body: String, button: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onClick) { Text(button) }
        }
    }
}