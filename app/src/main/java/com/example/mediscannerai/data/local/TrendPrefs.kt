package com.example.mediscannerai.data.local

import android.content.Context

/** Remembers the order of tests on the Trends list (and the older pinned tests). */
object TrendPrefs {

    private const val FILE_NAME = "trend_prefs"
    private const val KEY_PINNED = "pinned_tests"
    private const val KEY_ORDER = "test_order"
    private const val SEPARATOR = "\n"

    /** Tests the user starred in the earlier version. Only used for the first ordering. */
    fun getPinned(context: Context): Set<String> =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_PINNED, emptySet())
            ?.toSet()
            ?: emptySet()

    /** The saved manual order, or null if the user has never reordered. */
    fun getOrder(context: Context): List<String>? {
        val saved = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ORDER, null) ?: return null
        return saved.split(SEPARATOR).filter { it.isNotBlank() }
    }

    fun setOrder(context: Context, order: List<String>) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ORDER, order.joinToString(SEPARATOR))
            .apply()
    }
}