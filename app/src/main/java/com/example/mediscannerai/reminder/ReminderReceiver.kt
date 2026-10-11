package com.example.mediscannerai.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mediscannerai.data.local.AppDatabase
import com.example.mediscannerai.data.local.IntakeEntity
import com.example.mediscannerai.data.repository.ReminderRepository
import com.example.mediscannerai.domain.model.MealSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Handles three things: a reminder time arriving (also the 10-minute repeat),
 * the "Taken" button, and the "Remind in 10 min" button.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext

        if (intent.getBooleanExtra(ReminderScheduler.EXTRA_TEST, false)) {
            ReminderNotifier.showTest(app)
            return
        }

        val slot = MealSlot.entries.firstOrNull {
            it.name == intent.getStringExtra(ReminderScheduler.EXTRA_SLOT)
        } ?: return
        val action = intent.action

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(app)
                val intakeDao = database.intakeDao()
                val today = LocalDate.now()
                val dueToday = ReminderRepository(database.reminderMedicineDao())
                    .getAll()
                    .filter { ReminderScheduler.isDue(it, slot, today) }

                when (action) {
                    ReminderScheduler.ACTION_TAKEN -> {
                        dueToday.forEach {
                            intakeDao.insert(
                                IntakeEntity(
                                    medicineId = it.id,
                                    slot = slot.bit,
                                    day = today.toEpochDay(),
                                    takenAt = System.currentTimeMillis()
                                )
                            )
                        }
                        ReminderNotifier.cancel(app, slot)
                    }

                    ReminderScheduler.ACTION_SNOOZE -> {
                        ReminderNotifier.cancel(app, slot)
                        ReminderScheduler.scheduleSnooze(app, slot)
                    }

                    else -> {
                        // The normal reminder time, or the 10-minute repeat.
                        val takenIds = intakeDao.getForDay(today.toEpochDay())
                            .filter { it.slot == slot.bit }
                            .map { it.medicineId }
                            .toSet()
                        val toShow = dueToday.filter { it.id !in takenIds }
                        if (toShow.isNotEmpty()) ReminderNotifier.show(app, slot, toShow)
                        ReminderScheduler.rescheduleAll(app)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}