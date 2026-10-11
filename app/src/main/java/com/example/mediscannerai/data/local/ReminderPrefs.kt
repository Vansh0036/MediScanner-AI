package com.example.mediscannerai.data.local

import android.content.Context
import com.example.mediscannerai.domain.model.MealSlot

/** Remembers the clock time (minutes after midnight) the user chose for each slot. */
object ReminderPrefs {

    private const val FILE_NAME = "reminder_prefs"

    fun getMinutes(context: Context, slot: MealSlot): Int =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getInt("slot_${slot.name}", slot.defaultMinutes)

    fun setMinutes(context: Context, slot: MealSlot, minutes: Int) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("slot_${slot.name}", minutes)
            .apply()
    }
}