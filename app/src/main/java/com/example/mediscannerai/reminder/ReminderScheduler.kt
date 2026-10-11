package com.example.mediscannerai.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.mediscannerai.data.local.AppDatabase
import com.example.mediscannerai.data.local.ReminderMedicineEntity
import com.example.mediscannerai.data.local.ReminderPrefs
import com.example.mediscannerai.domain.model.MealSlot
import com.example.mediscannerai.domain.model.ScheduleMasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Sets one timer per slot (for example "After breakfast") for the next day that
 * has a medicine due. When it fires, the receiver shows one notification for the
 * slot and calls rescheduleAll again to set the next one.
 */
object ReminderScheduler {

    const val EXTRA_SLOT = "slot"
    const val EXTRA_TEST = "test"
    const val ACTION_TAKEN = "com.example.mediscannerai.action.TAKEN"
    const val ACTION_SNOOZE = "com.example.mediscannerai.action.SNOOZE"
    const val ACTION_SNOOZE_FIRE = "com.example.mediscannerai.action.SNOOZE_FIRE"

    private const val TEST_REQUEST_CODE = 900
    private const val SNOOZE_REQUEST_BASE = 100
    private const val SNOOZE_MINUTES = 10

    data class NextReminder(
        val slot: MealSlot,
        val trigger: ZonedDateTime,
        val medicines: List<ReminderMedicineEntity>
    )

    /** Is this medicine due in this slot on this date? */
    fun isDue(m: ReminderMedicineEntity, slot: MealSlot, date: LocalDate): Boolean {
        val day = date.toEpochDay()
        return !m.isPaused &&
                (m.slotsMask and slot.bit) != 0 &&
                (m.daysMask and ScheduleMasks.dayBit(date.dayOfWeek)) != 0 &&
                (m.startDay == null || day >= m.startDay) &&
                (m.endDay == null || day <= m.endDay)
    }

    /** The soonest upcoming reminder across all slots, or null if there is none. */
    fun nextReminder(
        medicines: List<ReminderMedicineEntity>,
        minutesFor: (MealSlot) -> Int,
        now: ZonedDateTime
    ): NextReminder? =
        MealSlot.entries.mapNotNull { slot ->
            val trigger = nextTrigger(slot, medicines, minutesFor(slot), now)
                ?: return@mapNotNull null
            NextReminder(
                slot = slot,
                trigger = trigger,
                medicines = medicines.filter { isDue(it, slot, trigger.toLocalDate()) }
            )
        }.minByOrNull { it.trigger.toInstant() }

    /** A short line for the Home card, for example "Next: After dinner, today 08:30 pm · 2 medicines". */
    fun nextReminderLabel(
        medicines: List<ReminderMedicineEntity>,
        times: Map<MealSlot, Int>
    ): String? {
        val next = nextReminder(medicines, { times[it] ?: it.defaultMinutes }, ZonedDateTime.now())
            ?: return null
        val time = next.trigger.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
        val day = if (next.trigger.toLocalDate() == LocalDate.now()) {
            "today"
        } else {
            next.trigger.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        }
        val count = next.medicines.size
        return "Next: ${next.slot.label}, $day $time \u00B7 $count medicine" + if (count == 1) "" else "s"
    }

    suspend fun rescheduleAll(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        // Only the schedule numbers are needed here, so nothing has to be decrypted.
        val medicines = AppDatabase.getInstance(app).reminderMedicineDao().getAll()
        val alarmManager = app.getSystemService(AlarmManager::class.java)
        val now = ZonedDateTime.now()

        MealSlot.entries.forEach { slot ->
            val pending = slotIntent(app, slot)
            val minutes = ReminderPrefs.getMinutes(app, slot)
            val trigger = nextTrigger(slot, medicines, minutes, now)
            if (trigger == null) {
                alarmManager.cancel(pending)
            } else {
                schedule(alarmManager, trigger.toInstant().toEpochMilli(), pending)
            }
        }
    }

    /** Shows the slot's notification again in 10 minutes. */
    fun scheduleSnooze(context: Context, slot: MealSlot) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java)
        val intent = Intent(app, ReminderReceiver::class.java)
            .setAction(ACTION_SNOOZE_FIRE)
            .putExtra(EXTRA_SLOT, slot.name)
        val pending = PendingIntent.getBroadcast(
            app,
            SNOOZE_REQUEST_BASE + slot.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        schedule(alarmManager, System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L, pending)
    }

    /** Schedules one test notification about a minute from now. */
    fun scheduleTest(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java)
        val intent = Intent(app, ReminderReceiver::class.java).putExtra(EXTRA_TEST, true)
        val pending = PendingIntent.getBroadcast(
            app,
            TEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        schedule(alarmManager, System.currentTimeMillis() + 60_000L, pending)
    }

    private fun nextTrigger(
        slot: MealSlot,
        medicines: List<ReminderMedicineEntity>,
        minutes: Int,
        now: ZonedDateTime
    ): ZonedDateTime? {
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val trigger = date.atTime(minutes / 60, minutes % 60).atZone(now.zone)
            if (!trigger.isAfter(now)) continue
            if (medicines.any { isDue(it, slot, date) }) return trigger
        }
        return null
    }

    private fun slotIntent(app: Context, slot: MealSlot): PendingIntent {
        val intent = Intent(app, ReminderReceiver::class.java).putExtra(EXTRA_SLOT, slot.name)
        return PendingIntent.getBroadcast(
            app,
            slot.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun schedule(am: AlarmManager, millis: Long, pending: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }
}