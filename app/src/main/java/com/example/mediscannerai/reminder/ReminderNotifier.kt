package com.example.mediscannerai.reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mediscannerai.MainActivity
import com.example.mediscannerai.R
import com.example.mediscannerai.data.local.ReminderMedicineEntity
import com.example.mediscannerai.domain.model.MealSlot

/** Builds and shows the reminder notifications. Plain notifications only: no alarm screen. */
object ReminderNotifier {

    const val CHANNEL_ID = "medicine_reminders"
    private const val TEST_ID = 999
    private const val TAKEN_REQUEST_BASE = 200
    private const val SNOOZE_REQUEST_BASE = 300

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medicine reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders to take your medicines at the times you set"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, slot: MealSlot, medicines: List<ReminderMedicineEntity>) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)

        val lines = medicines.map { m ->
            m.name + if (m.amountNote.isNotBlank()) " \u2014 ${m.amountNote}" else ""
        }
        val summary = if (lines.size == 1) {
            "Take ${lines[0]}"
        } else {
            "Take: " + medicines.joinToString(", ") { it.name }
        }

        // Shown on the lock screen: no medicine names.
        val publicVersion = baseBuilder(context)
            .setContentTitle("Medicine reminder")
            .setContentText("Tap to open")
            .build()

        val notification = baseBuilder(context)
            .setContentTitle("Medicine time: ${slot.label}")
            .setContentText(summary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(lines.joinToString("\n") { "\u2022 $it" })
            )
            .setPublicVersion(publicVersion)
            .addAction(
                0,
                "Taken \u2713",
                actionIntent(context, slot, ReminderScheduler.ACTION_TAKEN, TAKEN_REQUEST_BASE)
            )
            .addAction(
                0,
                "Remind in 10 min",
                actionIntent(context, slot, ReminderScheduler.ACTION_SNOOZE, SNOOZE_REQUEST_BASE)
            )
            .build()

        manager.notify(1000 + slot.ordinal, notification)
    }

    /** Removes the slot's notification, for example after "Taken" or "Remind in 10 min". */
    fun cancel(context: Context, slot: MealSlot) {
        NotificationManagerCompat.from(context).cancel(1000 + slot.ordinal)
    }

    @SuppressLint("MissingPermission")
    fun showTest(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)
        manager.notify(
            TEST_ID,
            baseBuilder(context)
                .setContentTitle("Test reminder")
                .setContentText("Reminders are working on this phone.")
                .build()
        )
    }

    private fun baseBuilder(context: Context): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_medicine)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))

    private fun actionIntent(
        context: Context,
        slot: MealSlot,
        action: String,
        requestBase: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderScheduler.EXTRA_SLOT, slot.name)
        return PendingIntent.getBroadcast(
            context,
            requestBase + slot.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}