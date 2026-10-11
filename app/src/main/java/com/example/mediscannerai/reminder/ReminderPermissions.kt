package com.example.mediscannerai.reminder

import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Checks and settings shortcuts for the things reminders need. */
object ReminderPermissions {

    /** True when the app is allowed to show notifications (permission and phone setting). */
    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** True when reminders can fire at the exact minute. Always true before Android 12. */
    fun exactAlarmsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun openNotificationSettings(context: Context) = open(context) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            open(context) {
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}")
                )
            }
        }
    }

    fun openAppSettings(context: Context) = open(context) {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }

    private fun open(context: Context, intent: () -> Intent) {
        try {
            context.startActivity(intent())
        } catch (e: ActivityNotFoundException) {
            // Some phones do not have this settings page. Nothing else to do.
        }
    }
}