package com.example.mediscannerai.data.local

import android.content.Context

/** Remembers which tests the user pinned to the top of the Trends list. */
object TrendPrefs {

    private const val FILE_NAME = "trend_prefs"
    private const val KEY_PINNED = "pinned_tests"

    fun getPinned(context: Context): Set<String> =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_PINNED, emptySet())
            ?.toSet()
            ?: emptySet()

    fun setPinned(context: Context, pinned: Set<String>) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PINNED, pinned)
            .apply()
    }
}